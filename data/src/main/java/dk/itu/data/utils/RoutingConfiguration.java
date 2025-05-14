package dk.itu.data.utils;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.enums.RoutingType;
import dk.itu.data.models.db.osm.OsmElement;
import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.models.db.osm.OsmWay;
import dk.itu.data.services.Services;
import dk.itu.util.LoggerFactory;
import kotlin.Pair;
import org.apache.logging.log4j.Logger;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class RoutingConfiguration {
    // Routing configurations
    private OsmNode startNode, endNode, sharedNode = null;
    private OsmElement cachedRoute;
    private RoutingType routingType = RoutingType.Dijkstra;
    private double waterLevel, cachedRouteWaterLevel;

    // General functionality
    private static final Logger logger = LoggerFactory.getLogger();
    private volatile Set<OsmNode> touchedNodes = Collections.synchronizedSet(new HashSet<>());
    private boolean shouldVisualize; // A configuration for drawing visited nodes through the search

    // Multithreading
    private Thread calculationThread;
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    public OsmNode getStartNode() {
        return startNode;
    }
    public void setStartNode(OsmNode startNode){
        this.startNode = startNode;
    }

    public OsmNode getEndNode() {
        return endNode;
    }
    public void setEndNode(OsmNode endNode){
        this.endNode = endNode;
    }

    public void setWaterLevel(double waterLevel){this.waterLevel = waterLevel;}

    public boolean getShouldVisualize(){return shouldVisualize;}
    public void toggleShouldVisualize(){shouldVisualize = !shouldVisualize;}

    public void setRoutingMethod(RoutingType routingType){
        logger.info("Routing type set to {}", routingType);
        this.routingType = routingType;
    }
    public RoutingType getRoutingMethod(){return routingType;}

    public OsmElement getRoute(boolean isWithDb, double currentWaterLevel){
        if(cachedRouteWaterLevel == currentWaterLevel) return cachedRoute;

        if(calculationThread != null && calculationThread.isAlive()){
            //Routing is currently being processed -> return null for now
            return null;
        }

        try{
            var thread = calculateRoute(isWithDb);
            thread.join();
        } catch(InterruptedException e){
            logger.error("An exception occured while awaiting route calculation: {}", e.getMessage());
        }

        return cachedRoute;
    }

    public List<OsmNode> getTouchedNodes(){
        return List.copyOf(touchedNodes);
    }

    public Thread calculateRoute(boolean isWithDb) throws RuntimeException{
        logger.info("Attempting to calculate route");
        if(calculationThread != null && calculationThread.isAlive()){
            logger.info("Can not compute route while thread is busy");
            throw new RuntimeException("A route is already being calculated. Please wait");
        }

        calculationThread = new Thread(() -> {
            try{
                Services.withServices(services -> {
                    cachedRouteWaterLevel = waterLevel; //Register height for caching

                    var nodes = services.getOsmService(isWithDb).getTraversableOsmNodes();

                    if (startNode == null || endNode == null)
                        throw new IllegalArgumentException("Start node and end node must be selected");
                    if (startNode.getId() == endNode.getId())
                        throw new IllegalArgumentException("Start node and end node can not be the same");

                    touchedNodes = Collections.synchronizedSet(new HashSet<>());

                    nodes.remove(startNode.getId());
                    nodes.remove(endNode.getId());
                    nodes.put(startNode.getId(), startNode);
                    nodes.put(endNode.getId(), endNode);

                    var route = routingType == RoutingType.AStarBidirectional ?
                            createAStarBidirectional(startNode, endNode, nodes, services) :
                            createPath(createCoordinateList(createRoute(startNode, endNode, nodes, services, null), startNode, endNode));

                    if (route == null)
                        logger.warn("No possible route could be found between: {}, {}", startNode.getId(), endNode.getId());

                    this.cachedRoute = route;
                });
            } catch (Exception ex){
                logger.error("An exception occurred while calculating route: {}", ex.getMessage());
                this.cachedRoute = null;
                calculationThread = null;
            }
        });

        calculationThread.start();
        return calculationThread;
    }

    private OsmElement createAStarBidirectional(OsmNode startNode, OsmNode endNode, Map<Long, OsmNode> nodes, Services services){
        logger.info("Beginning A-star bidirectional route");
        try{
            sharedNode = null;

            var forwardSet = Collections.synchronizedSet(new HashSet<OsmNode>());
            var backwardSet = Collections.synchronizedSet(new HashSet<OsmNode>());

            Callable<Map<OsmNode, OsmNode>> forwardTask = () -> createRoute(startNode, endNode, nodes, services, new Pair<>(forwardSet, backwardSet));
            Callable<Map<OsmNode, OsmNode>> backwardTask = () -> createRoute(endNode, startNode, nodes, services, new Pair<>(backwardSet, forwardSet));

            Future<Map<OsmNode, OsmNode>> forwardFuture = executorService.submit(forwardTask);
            Future<Map<OsmNode, OsmNode>> backwardFuture = executorService.submit(backwardTask);

            var forwardsRouteMap = forwardFuture.get();
            var backwardsRouteMap = backwardFuture.get();

            if(sharedNode == startNode || sharedNode == endNode){
                //Routing is too short for multithreading -> fallback to regular A*
                logger.info("Falling back to regular A* - route was to short to efficiently calculate with multithreading");
                return createPath(createCoordinateList(createRoute(startNode, endNode, nodes, services, null), startNode, endNode));
            }

            var forwardsRoute = createCoordinateList(forwardsRouteMap, startNode, sharedNode);
            var backwardsRoute = reverseCoordinateList(createCoordinateList(backwardsRouteMap, endNode, sharedNode));

            logger.info("Created A-star bidirectional routing with {} visited nodes", touchedNodes.size());
            return createPath(stitchCoordinates(forwardsRoute, backwardsRoute));
        } catch (Exception e){
            logger.error("An exception occurred when creating A-Star bidirectional", e);
            return null;
        }
    }

    private Map<OsmNode, OsmNode> createRoute(OsmNode startNode, OsmNode endNode, Map<Long, OsmNode> nodes, Services services, Pair<Set<OsmNode>, Set<OsmNode>> connectionSet){
        logger.debug("Starting routing from {} to {}", startNode.getId(), endNode.getId());
        Map<OsmNode, OsmNode> previousConnections = new HashMap<>();
        Map<OsmNode, Double> knownDistances = new HashMap<>();
        Map<OsmNode, Double> heuristicDistances = new HashMap<>();

        PriorityQueue<OsmNode> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> heuristicDistances.getOrDefault(n, Double.MAX_VALUE)));

        nodes.values().forEach(n -> {
            knownDistances.put(n, n == startNode ? 0.0 : Double.MAX_VALUE);
            heuristicDistances.put(n, n == endNode ? 0.0 : Double.MAX_VALUE);
        });

        pq.offer(startNode);

        int delay = CommonConfiguration.getInstance().getRoutingDelay();
        boolean shouldDelay = delay > 0;

        while(!pq.isEmpty()){
            try{
                if(shouldDelay) Thread.sleep(delay);
            } catch(InterruptedException e){
                logger.error("An interruption occurred when using route delaying");
            }

            OsmNode node = pq.poll();
            touchedNodes.add(node);

            if(connectionSet != null){
                //This will happen if createRoute is called as an A* bidirectional multithreaded
                if (sharedNode != null) return previousConnections;
                connectionSet.getFirst().add(node);

                if(connectionSet.getSecond().contains(node)){
                    logger.debug("A shared node was found to be: {}", node.getId());
                    sharedNode = node;
                    return previousConnections;
                }
            } else if (node == endNode){
                //This will happen if createRoute is called as a regular dijkstra / A*
                return previousConnections;
            }

            var nodeDistance = knownDistances.get(node);

            for(var conn : node.getConnectionMap().entrySet()){
                OsmNode nextNode;
                try{
                    nextNode = nodes.get(conn.getKey());
                    if(nextNode == null) throw new NoSuchElementException("No next node with chosen ID");
                } catch(NoSuchElementException e){
                    continue;
                }

                var nextNodeCurve = nextNode.getContainingCurve();

                if(nextNodeCurve == null) {
                    //If no curve has been mapped to node yet -> force load curve
                    nextNodeCurve = services.getHeightCurveService().getHeightCurveForPoint(nextNode.getLon(), nextNode.getLat());
                    nextNode.setContainingCurve(nextNodeCurve);
                }

                if(nextNodeCurve != null && !nextNodeCurve.getIsAboveWater()) continue;//Road is flooded

                double connDistance = conn.getValue();
                double distance = connDistance + nodeDistance;
                if(distance < knownDistances.get(nextNode)){
                    knownDistances.put(nextNode, distance);
                    previousConnections.put(nextNode, node);
                    heuristicDistances.put(nextNode, distance + (routingType == RoutingType.Dijkstra ? 0.0 : RoutingUtils.distanceMeters(nextNode.getLat(), nextNode.getLon(), endNode.getLat(), endNode.getLon())));
                    pq.offer(nextNode);
                }
            }
        }

        logger.debug("No route found for route between {} and {}", startNode.getId(), endNode.getId());
        return null;
    }

    private OsmElement createPath(double[] coordinateList){
        logger.info("Creating path with {} coordinates", coordinateList.length / 2);
        return OsmWay.createWayForRouting(coordinateList);
    }

    private double[] createCoordinateList(Map<OsmNode, OsmNode> previousConnections, OsmNode startNode, OsmNode endNode) throws RuntimeException{
        if(previousConnections == null) throw new RuntimeException("Previous connections can not be null");

        List<OsmNode> path = new ArrayList<>();
        var curNode = endNode;

        while(curNode != startNode){
            path.addFirst(curNode);
            curNode = previousConnections.get(curNode);
        }

        path.addFirst(startNode);

        var coordinateList = new double[path.size() * 2];
        var count = 0;
        for(var x : path){
            coordinateList[count] = x.getLon();
            coordinateList[count+1] = x.getLat();
            count = count + 2;
        }

        return coordinateList;
    }

    //TODO: This could be optimized
    private double[] reverseCoordinateList(double[] coordinates){
        var result = new double[coordinates.length];

        List<List<Double>> pairs = new ArrayList<>();
        for(int i = 0; i < coordinates.length; i += 2){
            pairs.add(Arrays.asList(coordinates[i], coordinates[i+1]));
        }

        var list = new ArrayList<Double>();
        for(int i = pairs.size() - 1; i >=0; i--){
            list.addAll(pairs.get(i));
        }

        for(int i = 0; i < list.size(); i++){
            result[i] = list.get(i);
        }

        return result;
    }

    private double[] stitchCoordinates(double[] firstList, double[] secondList){
        boolean removeDuplicate = (firstList[firstList.length - 2] == secondList[0] && firstList[firstList.length - 1] == secondList[1]);

        var result = new double[
                removeDuplicate ? firstList.length + secondList.length - 2 :
                firstList.length + secondList.length
        ];

        System.arraycopy(firstList, 0, result, 0, firstList.length);
        System.arraycopy(secondList, removeDuplicate ? 2 : 0, result, firstList.length, result.length - firstList.length);

        return result;
    }
}
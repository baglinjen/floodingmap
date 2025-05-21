package dk.itu.data.services;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.enums.RoutingType;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import dk.itu.data.models.osm.OsmWay;
import dk.itu.data.utils.RoutingUtils;
import dk.itu.util.LoggerFactory;
import kotlin.Pair;
import org.apache.logging.log4j.Logger;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class RoutingService {
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
    private volatile boolean stopCalculationFlag = false;
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

    public OsmElement getRoute(double currentWaterLevel){
        if(cachedRouteWaterLevel == currentWaterLevel) return cachedRoute;

        if(calculationThread != null && calculationThread.isAlive()){
            //Routing is currently being processed -> return null for now
            return null;
        }

        try{
            var thread = calculateRoute();
            thread.join();
        } catch(InterruptedException e){
            logger.error("An exception occurred while awaiting route calculation: {}", e.getMessage());
        }

        return cachedRoute;
    }

    public void cancelRouteCalculation(){
        stopCalculationFlag = true;
    }

    public List<OsmNode> getTouchedNodes(){
        return List.copyOf(touchedNodes);
    }

    public Thread calculateRoute() throws RuntimeException{
        logger.info("Attempting to calculate route");
        if(calculationThread != null && calculationThread.isAlive()){
            logger.info("Can not compute route while thread is busy");
            throw new RuntimeException("A route is already being calculated. Please wait!");
        }

        calculationThread = new Thread(() -> {
            try{
                Services.withServices(services -> {
                    stopCalculationFlag = false;
                    cachedRouteWaterLevel = waterLevel; //Register height for caching

                    if (startNode == null || endNode == null)
                        throw new IllegalArgumentException("Start node and end node must be selected");
                    if (startNode.getId() == endNode.getId())
                        throw new IllegalArgumentException("Start node and end node can not be the same");

                    touchedNodes = Collections.synchronizedSet(new HashSet<>());

//                    nodes.removeIf(node -> node.getId() == startNode.getId() || node.getId() == endNode.getId());

                    var route = routingType == RoutingType.AStarBidirectional ?
                            createAStarBidirectional(startNode, endNode, services) :
                            createPath(createCoordinateList(createRoute(startNode, endNode, services, null), startNode, endNode));

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

    private OsmElement createAStarBidirectional(OsmNode startNode, OsmNode endNode, Services services){
        logger.info("Beginning A-star bidirectional route");
        try{
            sharedNode = null;
            if(executorService.isShutdown()) executorService = Executors.newFixedThreadPool(2);

            var forwardSet = Collections.synchronizedSet(new HashSet<OsmNode>());
            var backwardSet = Collections.synchronizedSet(new HashSet<OsmNode>());

            Callable<Map<OsmNode, OsmNode>> forwardTask = () -> createRoute(startNode, endNode, services, new Pair<>(forwardSet, backwardSet));
            Callable<Map<OsmNode, OsmNode>> backwardTask = () -> createRoute(endNode, startNode, services, new Pair<>(backwardSet, forwardSet));

            Future<Map<OsmNode, OsmNode>> forwardFuture = executorService.submit(forwardTask);
            Future<Map<OsmNode, OsmNode>> backwardFuture = executorService.submit(backwardTask);

            var forwardsRouteMap = forwardFuture.get();
            var backwardsRouteMap = backwardFuture.get();

            if(sharedNode == startNode || sharedNode == endNode){
                //Routing is too short for multithreading -> fallback to regular A*
                logger.info("Falling back to regular A* - route was to short to efficiently calculate with multithreading");
                return createPath(createCoordinateList(createRoute(startNode, endNode, services, null), startNode, endNode));
            }

            var forwardsRoute = createCoordinateList(forwardsRouteMap, startNode, sharedNode);
            var backwardsRoute = reverseCoordinateList(createCoordinateList(backwardsRouteMap, endNode, sharedNode));

            logger.info("Created A-star bidirectional routing with {} visited nodes", touchedNodes.size());
            return createPath(stitchCoordinates(forwardsRoute, backwardsRoute));
        } catch (Exception e){
            logger.error("An exception occurred when creating A-Star bidirectional", e);
            executorService.shutdownNow(); // Forcefully shut down the executorService
            return null;
        }
    }

    private Map<OsmNode, OsmNode> createRoute(OsmNode startNode, OsmNode endNode, Services services, Pair<Set<OsmNode>, Set<OsmNode>> connectionSet){
        logger.debug("Starting routing from {} to {}", startNode.getId(), endNode.getId());
        Map<OsmNode, OsmNode> previousConnections = new HashMap<>();
        Map<OsmNode, Float> knownDistances = new HashMap<>();
        Map<OsmNode, Float> heuristicDistances = new HashMap<>();

        PriorityQueue<OsmNode> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> heuristicDistances.getOrDefault(n, Float.MAX_VALUE)));

        knownDistances.put(startNode, 0f);
        heuristicDistances.put(endNode, 0f);

        pq.offer(startNode);

        int delay = CommonConfiguration.getInstance().getRoutingDelay();
        boolean shouldDelay = delay > 0;

        while(!pq.isEmpty()){
            if(stopCalculationFlag){
                logger.info("Stopping calculation as stopCalculation flag is positive");
                return null;
            }

            // This is used for simulating delay by system environments
            try{
                if(shouldDelay) Thread.sleep(delay);
            } catch(InterruptedException e){
                logger.error("An interruption occurred when using route delaying");
            }

            OsmNode node = pq.poll();
            if(node == null) continue;

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

            var nodeDistance = knownDistances.putIfAbsent(node, Float.MAX_VALUE);

            Pair<OsmNode[],float[]> connections = node.getConnections();
            var nodeConnections = connections.getFirst();
            var nodeDistances = connections.getSecond();

            for (int i = 0; i < connections.getSecond().length; i++) {
                var nextNode = nodeConnections[i];
                if (nextNode == null) continue;
                var nextNodeCurve = nextNode.getContainingCurve();

                if(nextNodeCurve == null) {
                    //If no curve has been mapped to node yet -> force load curve
                    nextNodeCurve = services.getHeightCurveService().getHeightCurveForPoint(nextNode.getLon(), nextNode.getLat());
                    nextNode.setContainingCurve(nextNodeCurve);
                }

                if(nextNodeCurve != null && !nextNodeCurve.getIsAboveWater()) continue;//Road is flooded

                float connDistance = nodeDistances[i];
                float distance = connDistance + nodeDistance;
                if (distance < knownDistances.getOrDefault(nextNode, Float.MAX_VALUE)) {
                    knownDistances.put(nextNode, distance);
                    previousConnections.put(nextNode, node);
                    heuristicDistances.put(nextNode, distance + (routingType == RoutingType.Dijkstra ? 0f : RoutingUtils.distanceMetersFloat(nextNode.getLat(), nextNode.getLon(), endNode.getLat(), endNode.getLon())));
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
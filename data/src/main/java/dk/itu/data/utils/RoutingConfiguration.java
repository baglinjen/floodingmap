package dk.itu.data.utils;
import dk.itu.data.models.db.osm.OsmElement;
import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.models.db.osm.OsmWay;
import dk.itu.data.services.Services;
import dk.itu.util.LoggerFactory;
import it.unimi.dsi.fastutil.objects.ObjectArrayPriorityQueue;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import kotlin.Pair;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RoutingConfiguration {
    private static final Logger logger = LoggerFactory.getLogger();
    private boolean shouldVisualize, isAStar;
    private OsmNode startNode, endNode;
    private double waterLevel = 0.0;
    private Pair<OsmElement, Double> route;
    private final List<OsmNode> touchedNodes = new ReferenceArrayList<>();
    private Thread calculationThread;

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
    public void toggleShouldVisualize(){
        shouldVisualize = !shouldVisualize;
    }

    public boolean getIsAStar(){
        return this.isAStar;
    }
    public void setIsAStar(boolean isAStar){
        this.isAStar = isAStar;
    }

    public OsmElement getRoute(boolean isWithDb, double currentWaterLevel){
        if (route == null) return null;

        if (route.getSecond() != currentWaterLevel) calculateRoute(isWithDb);

        return (route == null ? null : route.getFirst());
    }

    public List<OsmNode> getTouchedNodes(){
        return List.copyOf(touchedNodes); // Test without making copy
    }

    public Thread calculateRoute(boolean isWithDb) {
        calculationThread = new Thread(() -> {
            Services.withServices(services -> {
                var nodes = services.getOsmService(isWithDb).getTraversableOsmNodes();

                if (startNode == null || endNode == null) throw new IllegalArgumentException("Start node and end node must be selected");
                if (startNode.getId() == endNode.getId()) throw new IllegalArgumentException("Start node and end node can not be the same");

                touchedNodes.clear();

                var route = createDijkstra(startNode, endNode, nodes, services);

                if (route == null) logger.warn("No possible route could be found between: {}, {}", startNode.getId(), endNode.getId());

                this.route = new Pair<>(route, waterLevel);
            });
        });

        calculationThread.start();
        return calculationThread;
    }

    private OsmElement createDijkstra(OsmNode startNode, OsmNode endNode, Map<Long, OsmNode> nodes, Services services){
        nodes.remove(startNode.getId());
        nodes.remove(endNode.getId());
        nodes.put(startNode.getId(), startNode);
        nodes.put(endNode.getId(), endNode);

        Map<OsmNode, OsmNode> previousNodes = new ConcurrentHashMap<>();
        Map<OsmNode, Double> distances = new ConcurrentHashMap<>();
        Map<OsmNode, Double> heuristicDistances = new ConcurrentHashMap<>();

//        ObjectArrayPriorityQueue
//        ObjectHeapPriorityQueue

        ObjectArrayPriorityQueue<OsmNode> pq = new ObjectArrayPriorityQueue<>(Comparator.comparingDouble(n -> heuristicDistances.getOrDefault(n, Double.MAX_VALUE)));

        nodes
                .values()
                .parallelStream()
                .forEach(v -> {
                    distances.put(v, v == startNode ? 0.0 : Double.MAX_VALUE);
                    heuristicDistances.put(v, v == endNode ? 0.0 : Double.MAX_VALUE);
                });

        pq.enqueue(startNode);

        while(!pq.isEmpty()){
            OsmNode curNode = pq.dequeue(); // Find out if it is dequeue or first
            touchedNodes.add(curNode);

                if (curNode == endNode) {
                    return createPath(previousNodes, startNode, endNode);
                }

                double currDistance = distances.get(curNode);

                for(var connection : curNode.getConnectionMap().entrySet()){
                    OsmNode nextNode;
                    try{
                        nextNode = nodes.get(connection.getKey());
                        if(nextNode == null) throw new NoSuchElementException("No next node with chosen ID");
                    } catch(NoSuchElementException e){
                        continue;
                    }

                    var height = services.getHeightCurveService().getHeightCurveForPoint(nextNode.getLon(), nextNode.getLat()).getHeight();
                    if (height < waterLevel) continue; // Road is flooded

                    double connectionDistance = connection.getValue();
                    double distance = connectionDistance + currDistance;
                    if(distance < distances.get(nextNode)){
                        //A new shorter way has been found
                        distances.put(nextNode, distance);
                        previousNodes.put(nextNode, curNode);
                        heuristicDistances.put(nextNode, distance + (isAStar ? RoutingUtils.distanceMeters(nextNode.getLat(), nextNode.getLon(), endNode.getLat(), endNode.getLon()) : 0.0));
                        pq.enqueue(nextNode);
                    }
                }
        }

        return null; // No path found
    }

    private OsmElement createPath(Map<OsmNode, OsmNode> previousNodes, OsmNode startNode, OsmNode endNode){
        List<OsmNode> path = new ArrayList<>();
        var curNode = endNode;

        while(curNode != startNode){
            path.addFirst(curNode);
            curNode = previousNodes.get(curNode);
        }

        path.addFirst(startNode);

        var coordinateList = new double[path.size() * 2];
        var count = 0;
        for(var x : path){
            coordinateList[count] = x.getLon();
            coordinateList[count+1] = x.getLat();
            count = count + 2;
        }

        return OsmWay.createWayForRouting(coordinateList);
    }
}

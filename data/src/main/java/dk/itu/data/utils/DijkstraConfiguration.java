package dk.itu.data.utils;
import dk.itu.data.models.db.OsmElement;
import dk.itu.data.models.db.OsmNode;
import dk.itu.data.models.db.OsmWay;
import dk.itu.data.datastructure.curvetree.CurveTree;
import dk.itu.data.services.Services;
import dk.itu.util.LoggerFactory;
import kotlin.Pair;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.List;

public class DijkstraConfiguration {
    private static final Logger logger = LoggerFactory.getLogger();
    private boolean shouldVisualize, isAStar;

    private OsmNode startNode, endNode;
    private long startNodeId, endNodeId;

    private double waterLevel = 0.0;
    private Pair<OsmElement, Double> route;
    private final List<OsmNode> touchedNodes = new ArrayList<>();

    public OsmNode getStartNode() {
        return startNode;
    }
    public void setStartNode(OsmNode startNode){
        this.startNode = startNode;
    }

    //Setters for testing purposes
    public void setStartNodeId(long startNodeId){
        this.startNodeId = startNodeId;
    }
    public void setEndNodeId(long endNodeId){
        this.endNodeId = endNodeId;
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

    public void setIsAStar(boolean isAStar){
        this.isAStar = isAStar;
    }

    public OsmElement getRoute(boolean isWithDb, double currentWaterLevel){
        if (route == null) return null;

        if (route.getSecond() != currentWaterLevel) calculateRoute(isWithDb);

        return (route == null ? null : route.getFirst());
    }

    public List<OsmNode> getTouchedNodes(){
        return touchedNodes;
    }

    public void calculateRoute(boolean isWithDb) {
        Services.withServices(s -> {
            var nodes = s.getOsmService(isWithDb).getTraversableOsmNodes();

            //If no nodes have been set -> attempt to find nodes by id
            if(startNode == null) startNode = nodes.stream().filter(c -> c.getId() == startNodeId).findFirst().get();
            if(endNode == null) endNode = nodes.stream().filter(c -> c.getId() == endNodeId).findFirst().get();

            if (startNode.getId() == endNode.getId()) throw new IllegalArgumentException("Start node and end node can not be the same");

            touchedNodes.clear();

            var route = createRoute(startNode, endNode, nodes, s.getGeoJsonService().getCurveTree());

            if (route == null) logger.warn("No possible route could be found between: {}, {}", startNode.getId(), endNode.getId());

            this.route = new Pair<>(route, waterLevel);
        });

    }

    private OsmElement createRoute(OsmNode startNode, OsmNode endNode, List<OsmNode> nodes, CurveTree curveTree){
        nodes.removeIf(e -> e.getId() == startNode.getId() || e.getId() == endNode.getId());
        nodes.add(startNode);
        nodes.add(endNode);

        Map<OsmNode, OsmNode> previousNodes = new HashMap<>();
        Map<OsmNode, Double> distances = new HashMap<>();
        Map<OsmNode, Double> heuristicDistances = new HashMap<>();

        PriorityQueue<OsmNode> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> heuristicDistances.getOrDefault(n, Double.MAX_VALUE)));

        nodes.forEach(e -> {
            distances.put(e, e == startNode ? 0.0 : Double.MAX_VALUE);
            heuristicDistances.put(e, e == endNode ? 0.0 : Double.MAX_VALUE);
        });

        pq.offer(startNode);

        while(!pq.isEmpty()){
            OsmNode curNode = pq.poll();
            touchedNodes.add(curNode);

                if (curNode == endNode) {
                    return createPath(previousNodes, startNode, endNode);
                }

                double currDistance = distances.get(curNode);

                for(var connection : curNode.getConnectionMap().entrySet()){
                    OsmNode nextNode;
                    try{
                        nextNode = nodes.parallelStream().filter(o -> o.getId() == connection.getKey()).findFirst().orElseThrow();
                    } catch(NoSuchElementException e){
                        continue;
                    }

                    var height = curveTree.getHeightCurveForPoint(nextNode.getLon(), nextNode.getLat()).getHeight();
                    if (height < waterLevel) continue; //Road is flooded

                    double connectionDistance = connection.getValue();
                    double distance = connectionDistance + currDistance;
                    if(distance < distances.get(nextNode)){
                        //A new shorter way has been found
                        distances.put(nextNode, distance);
                        previousNodes.put(nextNode, curNode);
                        heuristicDistances.put(nextNode, distance + (isAStar ? DijkstraUtils.distanceMeters(nextNode.getLat(), nextNode.getLon(), endNode.getLat(), endNode.getLon()) : 0.0));
                        pq.offer(nextNode);
                    }
                }
        }

        return null; //No path found
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

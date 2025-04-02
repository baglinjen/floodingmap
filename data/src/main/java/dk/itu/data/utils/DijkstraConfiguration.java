package dk.itu.data.utils;
import dk.itu.common.models.OsmElement;
import dk.itu.data.datastructure.curvetree.CurveTree;
import dk.itu.data.models.db.DbNode;
import dk.itu.data.models.db.DbWay;
import dk.itu.data.services.Services;
import dk.itu.util.PolygonUtils;
import kotlin.Pair;

import java.awt.*;
import java.util.*;
import java.util.List;

public class DijkstraConfiguration {
    private long startNodeId, endNodeId;
    private double waterLevel = 0.0;
    private Pair<OsmElement, Double> route;

    //Getters and setters for start node
    public void setStartNodeId(String startNodeId){
        this.startNodeId = Long.parseLong(startNodeId);
    }

    //Getters and setters for end node
    public void setEndNodeId(String endNodeId){
        this.endNodeId = Long.parseLong(endNodeId);
    }

    public void setWaterLevel(double waterLevel){this.waterLevel = waterLevel;}

    public OsmElement getRoute(double currentWaterLevel){
        if(route == null) return null;

        if(route.getSecond() != currentWaterLevel) calculateRoute();

        return (route == null ? null : route.getFirst());
    }

    public void calculateRoute(){
        Services.withServices(s -> {
            var nodes = s.getOsmService().getOsmNodes();

            if(startNodeId == endNodeId) throw new IllegalArgumentException("Start node and end node can not be the same");

            var startNode = nodes.parallelStream().filter(o -> o.getId() == startNodeId).findFirst().orElseThrow(() -> new IllegalArgumentException("No start node found with ID: " + startNodeId));
            var endNode = nodes.parallelStream().filter(o -> o.getId() == endNodeId).findFirst().orElseThrow(() -> new IllegalArgumentException("No end node found with ID: " + endNodeId));

            var route = createDijkstra(startNode, endNode, nodes, s.getGeoJsonService().getCurveTree());

            if(route == null) throw new RuntimeException("No possible route could be found between: " + startNodeId + ", " + endNodeId);

            this.route = new Pair<>(route, waterLevel);
        });

    }

    private OsmElement createDijkstra(OsmElement startNode, OsmElement endNode, List<OsmElement> nodes, CurveTree curveTree){
        Map<OsmElement, OsmElement> previousNodes = new HashMap<>();
        Map<OsmElement, Double> distances = new HashMap<>();

        PriorityQueue<OsmElement> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> distances.getOrDefault(n, Double.MAX_VALUE)));

        for(var node : nodes) distances.put(node, node == startNode ? 0.0 : Double.MAX_VALUE);

        pq.offer(startNode);

        while(!pq.isEmpty()){
                DbNode curNode = (DbNode)pq.poll();

                if(curNode == endNode){
                    return createDijkstraPath(previousNodes, startNode, endNode, curveTree);
                }

                double currDistance = distances.get(curNode);

                for(var connection : curNode.getConnectionMap().entrySet()){
                    OsmElement nextNode;
                    try{
                        nextNode = nodes.parallelStream().filter(o -> o.getId() == connection.getKey()).findFirst().get();
                    } catch(NoSuchElementException e){
                        continue;
                    }

                    var casted = (DbNode)nextNode;
                    var height = curveTree.getHeightCurveForPoint(casted.getLon(), casted.getLat()).getHeight();
                    if(height < waterLevel) continue;//Road is flooded

                    double connectionDistance = connection.getValue();
                    double newDist = currDistance + connectionDistance;

                    if(newDist < distances.get(nextNode)){
                        distances.put(nextNode, newDist);
                        previousNodes.put(nextNode, curNode);
                        pq.offer(nextNode);
                    }
                }
        }

        return null;//No path found
    }

    private OsmElement createDijkstraPath(Map<OsmElement, OsmElement> previousNodes, OsmElement startNode, OsmElement endNode, CurveTree curveTree){
        List<OsmElement> path = new ArrayList<>();
        var curNode = endNode;

        while(curNode != startNode){
            path.addFirst(curNode);
            curNode = previousNodes.get(curNode);
        }

        path.addFirst(startNode);

        var coordinateList = new double[path.size() * 2];
        var count = 0;
        for(var x : path){
            var dbNodeX = (DbNode)x;
            coordinateList[count] = dbNodeX.getLon();
            coordinateList[count+1] = dbNodeX.getLat();
            count = count + 2;
        }

        return new DbWay(1L, PolygonUtils.pathFromShape(coordinateList, false), "line", Color.yellow.hashCode());
    }
}

package dk.itu.data.utils;
import dk.itu.common.models.OsmElement;
import dk.itu.data.models.db.DbNode;
import dk.itu.data.models.db.DbWay;
import dk.itu.data.services.Services;
import dk.itu.util.PolygonUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

public class DijkstraConfiguration {
    private long startNodeId, endNodeId;
    private OsmElement route;

    //Getters and setters for start node
    public long getStartNodeId(){return startNodeId;}
    public void setStartNodeId(String startNodeId){
        this.startNodeId = Long.parseLong(startNodeId);
    }

    //Getters and setters for end node
    public long getEndNodeId(){return endNodeId;}
    public void setEndNodeId(String endNodeId){
        this.endNodeId = Long.parseLong(endNodeId);
    }

    public OsmElement getRoute(){
        return route;
    }

    public void calculateRoute(){
        Services.withServices(s -> {
            var nodes = s.getOsmService().getOsmNodes();

            if(startNodeId == endNodeId) throw new IllegalArgumentException("Start node and end node can not be the same");

            var startNode = nodes.stream().filter(o -> o.getId() == startNodeId).findFirst().orElseThrow(() -> new IllegalArgumentException("No start node found with ID: " + startNodeId));
            var endNode = nodes.stream().filter(o -> o.getId() == endNodeId).findFirst().orElseThrow(() -> new IllegalArgumentException("No end node found with ID: " + endNodeId));

            var route = createDijkstra(startNode, endNode, nodes);

            if(route == null) throw new RuntimeException("No possible route could be found between: " + startNodeId + ", " + endNodeId);

            this.route = route;
        });

    }

    private OsmElement createDijkstra(OsmElement startNode, OsmElement endNode, List<OsmElement> nodes){
        Map<OsmElement, OsmElement> previousNodes = new HashMap<>();
        Map<OsmElement, Double> distances = new HashMap<>();

        PriorityQueue<OsmElement> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> distances.getOrDefault(n, Double.MAX_VALUE)));

        for(var node : nodes) distances.put(node, node == startNode ? 0.0 : Double.MAX_VALUE);

        pq.offer(startNode);

        while(!pq.isEmpty()){
            try{
                DbNode curNode = (DbNode)pq.poll();

                if(curNode == endNode){
                    return createDijkstraPath(previousNodes, startNode, endNode);
                }

                double currDistance = distances.get(curNode);

                for(var connection : curNode.getConnectionMap().entrySet()){
                    OsmElement nextNode = nodes.stream().filter(o -> o.getId() == connection.getKey()).findFirst().get();
                    double connectionDistance = connection.getValue();
                    double newDist = currDistance + connectionDistance;

                    if(newDist < distances.get(nextNode)){
                        distances.put(nextNode, newDist);
                        previousNodes.put(nextNode, curNode);
                        pq.offer(nextNode);
                    }
                }
            } catch(Exception ex){
                System.out.println("WAIT HERE");
            }

        }

        return null;//No path found
    }

    private OsmElement createDijkstraPath(Map<OsmElement, OsmElement> previousNodes, OsmElement startNode, OsmElement endNode){
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

package dk.itu.data.utils;
import dk.itu.data.models.db.OsmElement;
import dk.itu.data.models.db.OsmNode;
import dk.itu.data.models.db.OsmWay;
import dk.itu.data.services.Services;

import java.util.*;
import java.util.List;

public class DijkstraConfiguration {
    private OsmNode startNode, endNode;
    private OsmElement route;

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

    public OsmElement getRoute(){
        return route;
    }

    public void calculateRoute(boolean isWithDb) {
        Services.withServices(s -> {
            var nodes = s.getOsmService(isWithDb).getTraversableOsmNodes();

            if (startNode.getId() == endNode.getId()) throw new IllegalArgumentException("Start node and end node can not be the same");

            var route = createDijkstra(startNode, endNode, nodes);

            if(route == null) throw new RuntimeException("No possible route could be found between: " + startNode.getId() + ", " + endNode.getId());

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
            OsmNode curNode = (OsmNode) pq.poll();

                if(curNode == endNode){
                    return createDijkstraPath(previousNodes, startNode, endNode);
                }

                double currDistance = distances.get(curNode);

                for(var connection : curNode.getConnectionMap().entrySet()){
                    OsmElement nextNode;
                    try{
                        nextNode = nodes.parallelStream().filter(o -> o.getId() == connection.getKey()).findFirst().get();
                    } catch(NoSuchElementException e){
                        continue;
                    }

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
            var dbNodeX = (OsmNode) x;
            coordinateList[count] = dbNodeX.getLon();
            coordinateList[count+1] = dbNodeX.getLat();
            count = count + 2;
        }

        return OsmWay.createWayForDijkstra(coordinateList);
    }
}

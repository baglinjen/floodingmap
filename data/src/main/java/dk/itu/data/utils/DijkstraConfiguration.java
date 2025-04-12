package dk.itu.data.utils;
import dk.itu.data.models.db.osm.OsmElement;
import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.models.db.osm.OsmWay;
import dk.itu.data.services.Services;
import dk.itu.util.LoggerFactory;
import kotlin.Pair;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.List;

public class DijkstraConfiguration {
    private static final Logger logger = LoggerFactory.getLogger();
    private OsmNode startNode, endNode;
    private double waterLevel = 0.0;
    private Pair<OsmElement, Double> route;

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

    public OsmElement getRoute(boolean isWithDb, double currentWaterLevel){
        if (route == null) return null;

        if (route.getSecond() != currentWaterLevel) calculateRoute(isWithDb);

        return (route == null ? null : route.getFirst());
    }

    public void calculateRoute(boolean isWithDb) {
        Services.withServices(services -> {
            var nodes = services.getOsmService(isWithDb).getTraversableOsmNodes();

            if (startNode.getId() == endNode.getId()) throw new IllegalArgumentException("Start node and end node can not be the same");

            var route = createDijkstra(startNode, endNode, nodes, services);

            if (route == null) logger.warn("No possible route could be found between: {}, {}", startNode.getId(), endNode.getId());

            this.route = new Pair<>(route, waterLevel);
        });

    }

    private OsmElement createDijkstra(OsmNode startNode, OsmNode endNode, List<OsmNode> nodes, Services services){
        nodes.removeIf(e -> e.getId() == startNode.getId() || e.getId() == endNode.getId());
        nodes.add(startNode);
        nodes.add(endNode);

        Map<OsmNode, OsmNode> previousNodes = new HashMap<>();
        Map<OsmNode, Double> distances = new HashMap<>();

        PriorityQueue<OsmNode> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> distances.getOrDefault(n, Double.MAX_VALUE)));

        nodes.forEach(e -> distances.put(e, e == startNode ? 0.0 : Double.MAX_VALUE));

        pq.offer(startNode);

        while(!pq.isEmpty()){
            OsmNode curNode = pq.poll();

                if (curNode == endNode) {
                    return createDijkstraPath(previousNodes, startNode, endNode, services);
                }

                double currDistance = distances.get(curNode);

                for(var connection : curNode.getConnectionMap().entrySet()){
                    OsmNode nextNode;
                    try{
                        nextNode = nodes.parallelStream().filter(o -> o.getId() == connection.getKey()).findFirst().orElseThrow();
                    } catch(NoSuchElementException e){
                        continue;
                    }

                    var height = services.getHeightCurveService().getHeightCurveForPoint(nextNode.getLon(), nextNode.getLat()).getHeight();
                    if (height < waterLevel) continue; // Road is flooded

                    double connectionDistance = connection.getValue();
                    double newDist = currDistance + connectionDistance;

                    if (newDist < distances.get(nextNode)) {
                        distances.put(nextNode, newDist);
                        previousNodes.put(nextNode, curNode);
                        pq.offer(nextNode);
                    }
                }
        }

        return null; // No path found
    }

    private OsmElement createDijkstraPath(Map<OsmNode, OsmNode> previousNodes, OsmNode startNode, OsmNode endNode, Services services){
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

        return OsmWay.createWayForDijkstra(coordinateList);
    }
}

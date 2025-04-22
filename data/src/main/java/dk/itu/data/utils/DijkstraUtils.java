package dk.itu.data.utils;

import dk.itu.data.models.parser.ParserOsmNode;

import java.util.HashMap;
import java.util.Map;

import static dk.itu.util.CoordinateUtils.haversineDistance;

public class DijkstraUtils {
    public static Map<Long, Double> buildConnectionMap(ParserOsmNode node){
        var result = new HashMap<Long, Double>();

        for (ParserOsmNode connection : node.getConnections()) {
            result.put(connection.getId(), haversineDistance(node.getLat(), node.getLon(), connection.getLat(), connection.getLon()));
        }

        return result;
    }
}

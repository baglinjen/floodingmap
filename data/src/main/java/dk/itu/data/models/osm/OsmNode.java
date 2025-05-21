package dk.itu.data.models.osm;

import dk.itu.data.models.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserOsmNode;
import kotlin.Pair;

import java.awt.*;

import static dk.itu.data.utils.RoutingUtils.distanceMeters;

public class OsmNode extends OsmElement {
    private final double lat, lon;
    private final Pair<OsmNode[], double[]> connections;
    private HeightCurveElement containingCurve = null;

    public OsmNode(long id, double lon, double lat, double[] boundingBox, int connectionsCount) {
        super(id, boundingBox);
        this.lon = lon;
        this.lat = lat;
        connections = connectionsCount > 0 ?
                new Pair<>(
                        new OsmNode[connectionsCount],
                        new double[connectionsCount]
                )
                : null;
    }

    public static OsmNode mapToOsmNode(ParserOsmNode parserOsmNode) {
        var osmNode = new OsmNode(
                parserOsmNode.getId(),
                parserOsmNode.getLon(),
                parserOsmNode.getLat(),
                new double[]{parserOsmNode.getLon(), parserOsmNode.getLat(), parserOsmNode.getLon(), parserOsmNode.getLat()},
                parserOsmNode.getConnectionIdsCount()
        );
        osmNode.setStyle(parserOsmNode.getColor(), parserOsmNode.getStroke());

        return osmNode;
    }

    public void addConnection(OsmNode connection) {
        if (connections == null) return;
        var nodeConnections = connections.getFirst();
        int i = 0;
        for (; i < nodeConnections.length; i++) {
            if (nodeConnections[i] == null) break;
        }
        nodeConnections[i] = connection;
        connections.getSecond()[i] = distanceMeters(this.lat, this.lon, connection.getLat(), connection.getLon());
    }

    public void setContainingCurve(HeightCurveElement containingCurve){
        this.containingCurve = containingCurve;
    }

    public HeightCurveElement getContainingCurve(){
        return containingCurve;
    }

    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }

    public Pair<OsmNode[], double[]> getConnections() {
        return connections;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) { /* Nodes are not drawn for now */ }
}
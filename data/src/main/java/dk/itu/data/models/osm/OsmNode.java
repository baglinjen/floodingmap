package dk.itu.data.models.osm;

import dk.itu.data.models.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserOsmNode;
import kotlin.Pair;

import java.awt.*;

import static dk.itu.data.utils.RoutingUtils.distanceMetersFloat;

public class OsmNode extends OsmElement {
    private final double lat, lon;
    private OsmNode[] connections;
    private float[] distances;
    private HeightCurveElement containingCurve = null;

    public OsmNode(long id, double lon, double lat, double[] boundingBox, int connectionsCount) {
        super(id, boundingBox);
        this.lon = lon;
        this.lat = lat;
        if (connectionsCount > 0) {
            this.connections = new OsmNode[connectionsCount];
            this.distances = new float[connectionsCount];
        }
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
        int i = 0;
        for (; i < connections.length; i++) {
            if (connections[i] == null) break;
        }
        connections[i] = connection;
        distances[i] = distanceMetersFloat(this.lat, this.lon, connection.getLat(), connection.getLon());
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

    public Pair<OsmNode[], float[]> getConnections() {
        return new Pair<>(connections, distances);
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) { /* Nodes are not drawn for now */ }
}
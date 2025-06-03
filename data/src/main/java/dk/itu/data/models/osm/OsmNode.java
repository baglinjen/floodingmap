package dk.itu.data.models.osm;

import dk.itu.data.models.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserOsmNode;
import kotlin.Pair;

import static dk.itu.data.utils.RoutingUtils.distanceMetersFloat;

public class OsmNode implements OsmElement {
    private final long id;
    private final double lat, lon;
    private OsmNode[] connections;
    private float[] distances;
    private HeightCurveElement containingCurve = null;

    public OsmNode(long id, double lon, double lat, int connectionsCount) {
        this.id = id;
        this.lon = lon;
        this.lat = lat;
        if (connectionsCount > 0) {
            this.connections = new OsmNode[connectionsCount];
            this.distances = new float[connectionsCount];
        }
    }

    public static OsmNode mapToOsmNode(ParserOsmNode parserOsmNode) {
        return new OsmNode(
                parserOsmNode.getId(),
                parserOsmNode.getLon(),
                parserOsmNode.getLat(),
                parserOsmNode.getConnectionIdsCount()
        );
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

    public HeightCurveElement getContainingCurve() {
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
    public long getId() {
        return id;
    }

    @Override
    public double minLon() {
        return this.lon;
    }

    @Override
    public double minLat() {
        return this.lat;
    }

    @Override
    public double maxLon() {
        return this.lon;
    }

    @Override
    public double maxLat() {
        return this.lat;
    }

    @Override
    public double getArea() {
        return 0;
    }
}
package dk.itu.data.models.parser;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ParserOsmNode extends ParserOsmElement {
    private final double lat, lon;
    private long[] connectionIds;

    public ParserOsmNode(long id, double lat, double lon) {
        super(id);
        this.lat = lat;
        this.lon = lon;
    }

    public int getConnectionIdsCount() {
        return connectionIds == null ? 0 : connectionIds.length;
    }
    public List<Long> getConnectionIds() {
        if (connectionIds == null) return null;
        return Arrays.stream(connectionIds)
                .boxed()
                .toList();
    }

    public void addConnectionId(long connection) {
        if (connectionIds == null) {
            connectionIds = new long[1];
        } else {
            connectionIds = Arrays.copyOf(connectionIds, connectionIds.length + 1);
        }
        connectionIds[connectionIds.length - 1] = connection;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public double getArea() {
        return 0;
    }

    @Override
    public double[] getBounds() {
        return new double[]{lon, lat, lon, lat};
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) { /* Nodes are not drawn for now */ }
}
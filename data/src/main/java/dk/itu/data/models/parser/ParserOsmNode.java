package dk.itu.data.models.parser;

import java.util.Arrays;
import java.util.List;

public class ParserOsmNode implements ParserOsmElement {
    private final long id;
    private final double lat, lon;
    private long[] connectionIds;

    public ParserOsmNode(long id, double lat, double lon) {
        this.id = id;
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
    public long getId() {
        return id;
    }

    @Override
    public void setStyleId(byte styleId) { /* Nodes have no style */ }

    @Override
    public byte getStyleId() {
        return -1;
    }

    @Override
    public boolean shouldBeDrawn() {
        return false;
    }
}
package dk.itu.data.models.parser;

import java.util.List;

import static dk.itu.util.PolygonUtils.*;

public class ParserOsmWay implements ParserOsmElement {
    private final long id;
    private byte styleId;
    private final float[] coordinates;

    public ParserOsmWay(long id, List<ParserOsmNode> nodes, byte styleId) {
        this.id = id;
        coordinates = new float[nodes.size()*2];
        for (int i = 0; i < nodes.size(); i++) {
            coordinates[i*2] = nodes.get(i).getLon();
            coordinates[i*2 + 1] = nodes.get(i).getLat();
        }

        this.styleId = styleId;
    }

    public boolean isLine() {
        return !isClosed(coordinates);
    }

    public float[] getCoordinates() {
        return coordinates;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setStyleId(byte styleId) {
        this.styleId = styleId;
    }

    @Override
    public byte getStyleId() {
        return this.styleId;
    }

    @Override
    public boolean shouldBeDrawn() {
        return this.styleId >= 0;
    }
}
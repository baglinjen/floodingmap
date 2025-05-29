package dk.itu.data.models.parser;

import dk.itu.util.shape.WayPath;

import java.util.List;

import static dk.itu.util.PolygonUtils.*;

public class ParserOsmWay implements ParserOsmElement {
    private final long id;
    private byte styleId;
    private final WayPath path;

    public ParserOsmWay(long id, List<ParserOsmNode> nodes, byte styleId) {
        this.id = id;
        double[] coordinatesRaw = new double[nodes.size()*2];
        for (int i = 0; i < nodes.size(); i++) {
            coordinatesRaw[i*2] = nodes.get(i).getLon();
            coordinatesRaw[i*2 + 1] = nodes.get(i).getLat();
        }

        path = new WayPath(!isClosed(coordinatesRaw) ? coordinatesRaw : forceCounterClockwise(coordinatesRaw));
        this.styleId = styleId;
    }

    public WayPath getPath() {
        return path;
    }

    public double[] getCoordinates() {
        return path.getOuterCoordinates();
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
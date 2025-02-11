package dk.itu.models;

import java.awt.geom.Path2D;
import java.util.List;

public class OsmWay implements OsmElement {
    private final long id;
    private final OsmNode[] osmNodes;
    private float minX, minY, maxX, maxY;
    private final Path2D.Float path;

    public OsmWay(long _id, List<OsmNode> _osmNodes) {
        id = _id;
        osmNodes = _osmNodes.toArray(new OsmNode[0]);

        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;

        for (OsmElement osmNode : _osmNodes) {
            if (osmNode.getMinX() < minX) {
                minX = osmNode.getMinX();
            } else if (osmNode.getMaxX() > maxX) {
                maxX = osmNode.getMaxX();
            }
            if (osmNode.getMinY() < minY) {
                minY = osmNode.getMinY();
            } else if (osmNode.getMaxY() > maxY) {
                maxY = osmNode.getMaxY();
            }
        }

        path = new Path2D.Float();

        path.moveTo(0.56* osmNodes[0].getMinX(), -osmNodes[0].getMinY());

        for (int i = 1; i < osmNodes.length; i+=1) {
            path.lineTo(0.56* osmNodes[i].getMinX(), -osmNodes[i].getMinY());
        }
    }

    public OsmNode[] getOsmNodes() {
        return osmNodes;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public float getMinX() {
        return minX;
    }
    @Override
    public float getMaxX() {
        return maxX;
    }
    @Override
    public float getMinY() {
        return minY;
    }
    @Override
    public float getMaxY() {
        return maxY;
    }
    public Path2D.Float getPath() {
        return path;
    }
}
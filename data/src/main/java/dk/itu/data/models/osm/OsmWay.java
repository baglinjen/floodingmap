package dk.itu.data.models.osm;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.OsmElement;

import java.awt.*;
import java.util.List;

public class OsmWay implements OsmElement {
    private final long id;
    private List<OsmNode> nodes;

    public OsmWay(long id, List<OsmNode> nodes) {
        this.id = id;
        this.nodes = nodes;
    }

    public BoundingBox getBoundingBox() {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (OsmNode node : nodes) {
            minX = Math.min(minX, node.lon());
            minY = Math.min(minY, node.lat());
            maxX = Math.max(maxX, node.lon());
            maxY = Math.max(maxY, node.lat());
        }

        return new BoundingBox(minX, minY, maxX, maxY);
    }

    public List<OsmNode> getNodes() {
        return nodes;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public void setStyle(DrawingConfiguration.Style style) {

    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {

    }
}

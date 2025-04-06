package dk.itu.data.models.db;

import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.utils.DijkstraUtils;

import java.awt.*;
import java.util.Map;

public class OsmNode extends OsmElement {
    private final double lat, lon;
    private final Map<Long, Double> connectionMap;
    public OsmNode(long id, double lon, double lat, BoundingBox boundingBox, Map<Long, Double> connectionMap) {
        super(id, boundingBox, 0);
        this.lon = lon;
        this.lat = lat;
        this.connectionMap = connectionMap;
    }

    public static OsmNode mapToOsmNode(ParserOsmNode parserOsmNode) {
        BoundingBox boundingBox = new BoundingBox(
                parserOsmNode.getLon(),
                parserOsmNode.getLat(),
                parserOsmNode.getLon(),
                parserOsmNode.getLat()
        );
        var osmNode = new OsmNode(parserOsmNode.getId(), parserOsmNode.getLon(), parserOsmNode.getLat(), boundingBox, DijkstraUtils.buildConnectionMap(parserOsmNode));
        osmNode.setStyle(parserOsmNode.getRgbaColor(), parserOsmNode.getStroke());

        return osmNode;
    }

    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }
    public Map<Long, Double> getConnectionMap() {
        return connectionMap;
    }

    @Override
    public void prepareDrawing(Graphics2D g2d) { /* Nothing to prepare */ }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) { /* Nodes are not drawn for now */ }
}
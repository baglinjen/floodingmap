package dk.itu.data.models.osm;

import dk.itu.data.models.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.utils.RoutingUtils;

import java.awt.*;
import java.util.Map;

public class OsmNode extends OsmElement {
    private final double lat, lon;
    private final Map<Long, Double> connectionMap;
    private HeightCurveElement containingCurve = null;

    public OsmNode(long id, double lon, double lat, double[] boundingBox, Map<Long, Double> connectionMap) {
        super(id, boundingBox);
        this.lon = lon;
        this.lat = lat;
        this.connectionMap = connectionMap;
    }

    public static OsmNode mapToOsmNode(ParserOsmNode parserOsmNode) {
        var osmNode = new OsmNode(
                parserOsmNode.getId(),
                parserOsmNode.getLon(),
                parserOsmNode.getLat(),
                new double[]{parserOsmNode.getLon(), parserOsmNode.getLat(), parserOsmNode.getLon(), parserOsmNode.getLat()},
                RoutingUtils.buildConnectionMap(parserOsmNode)
        );
        osmNode.setStyle(parserOsmNode.getColor(), parserOsmNode.getStroke());

        return osmNode;
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
    public Map<Long, Double> getConnectionMap() {
        return connectionMap;
    }

    @Override
    public void prepareDrawing(Graphics2D g2d) { /* Nothing to prepare */ }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) { /* Nodes are not drawn for now */ }
}
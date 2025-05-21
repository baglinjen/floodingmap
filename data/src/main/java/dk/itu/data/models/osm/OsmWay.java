package dk.itu.data.models.osm;

import dk.itu.data.models.parser.ParserOsmWay;
import dk.itu.util.shape.WayPath;

import java.awt.*;

public class OsmWay extends OsmElement {
    private final boolean isLine;
    private final WayPath path;

    public OsmWay(long id, boolean isLine, double[] boundingBox, WayPath path) {
        super(id, boundingBox);
        this.isLine = isLine;
        this.path = path;
    }

    public double[] getOuterCoordinates(){
        return this.path.getOuterCoordinates();
    }

    public static OsmWay mapToOsmWay(ParserOsmWay parserOsmWay) {
        var osmWay = new OsmWay(
                parserOsmWay.getId(),
                parserOsmWay.isLine(),
                parserOsmWay.getBounds(),
                parserOsmWay.getPath()
        );

        osmWay.setStyle(parserOsmWay.getColor(), parserOsmWay.getStroke());

        return osmWay;
    }

    public static OsmWay createWayForRouting(double[] coordinates) {
        var way = new OsmWay(0, true, new double[]{0.0, 0.0, 0.0, 0.0}, new WayPath(coordinates));
        way.setStyle(Color.yellow, 6);
        return way;
    }

    public boolean isLine() {
        return isLine;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getColor());
        if (isLine && getStroke() != null) {
            g2d.setStroke(new BasicStroke(strokeBaseWidth * getStroke()));
            g2d.draw(path);
        } else {
            g2d.fill(path);
        }
    }
}
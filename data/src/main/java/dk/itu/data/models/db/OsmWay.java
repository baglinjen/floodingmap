package dk.itu.data.models.db;

import dk.itu.data.models.parser.ParserOsmWay;
import dk.itu.util.PolygonUtils;

import java.awt.*;

public class OsmWay extends OsmElement {
    private final Shape shape;
    private final boolean isLine;

    public OsmWay(long id, Shape shape, boolean isLine, BoundingBox boundingBox, double area) {
        super(id, boundingBox, area);
        this.shape = shape;
        this.isLine = isLine;
    }

    public static OsmWay mapToOsmWay(ParserOsmWay parserOsmWay) {
        var bounds = parserOsmWay.getBounds();

        BoundingBox boundingBox = new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3]);
        var osmWay = new OsmWay(parserOsmWay.getId(), parserOsmWay.getShape(), parserOsmWay.isLine(), boundingBox, parserOsmWay.isLine() ? 0 : parserOsmWay.getArea());

        osmWay.setStyle(parserOsmWay.getRgbaColor(), parserOsmWay.getStroke());

        return osmWay;
    }

    public static OsmWay createWayForDijkstra(double[] coordinates) {
        var way = new OsmWay(0, PolygonUtils.pathFromShape(coordinates, false), true, null, 0);
        way.setStyle(Color.yellow, 6);
        return way;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getRgbaColor());
        if (isLine) {
            g2d.setStroke(new BasicStroke(strokeBaseWidth * getStroke()));
            g2d.draw(shape);
        } else {
            g2d.fill(shape);
        }
    }
}

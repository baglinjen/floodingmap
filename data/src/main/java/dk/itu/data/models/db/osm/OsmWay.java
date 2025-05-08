package dk.itu.data.models.db.osm;

import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.parser.ParserOsmWay;

import java.awt.*;
import java.awt.geom.Path2D;

import static dk.itu.util.ShapePreparer.prepareLinePath;
import static dk.itu.util.ShapePreparer.preparePolygonPath;

public class OsmWay extends OsmElement {
    private final double[] outerCoordinates;
    private final boolean isLine;
    private Path2D.Double path = null;

    public OsmWay(long id, boolean isLine, BoundingBox boundingBox, double area, double[] outerCoordinates) {
        super(id, boundingBox, area);
        this.isLine = isLine;
        this.outerCoordinates = outerCoordinates;
    }

    public double[] getOuterCoordinates(){
        return outerCoordinates;
    }

    public static OsmWay mapToOsmWay(ParserOsmWay parserOsmWay) {
        var bounds = parserOsmWay.getBounds();

        BoundingBox boundingBox = new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3]);
        var osmWay = new OsmWay(
                parserOsmWay.getId(),
                parserOsmWay.isLine(), boundingBox,
                parserOsmWay.getArea(),
                parserOsmWay.getCoordinates()
        );

        osmWay.setStyle(parserOsmWay.getRgbaColor(), parserOsmWay.getStroke());

        return osmWay;
    }

    public static OsmWay createWayForRouting(double[] coordinates) {
        var way = new OsmWay(0, true, null, 0, coordinates);
        way.setStyle(Color.yellow, 6);
        return way;
    }

    public boolean isLine() {
        return isLine;
    }

    @Override
    public boolean shouldDraw() {
        return path != null;
    }

    @Override
    public void prepareDrawing(Graphics2D g2d) {
        path = isLine ? prepareLinePath(g2d, outerCoordinates, DRAWING_TOLERANCE) : preparePolygonPath(g2d, outerCoordinates, DRAWING_TOLERANCE);
//        if (this.getBoundingBox() != null && this.getBoundingBox().minimumLength() * g2d.getTransform().getScaleX() >= DRAWING_AREA_TOLERANCE) {
//            path = isLine ? prepareLinePath(g2d, outerCoordinates, DRAWING_TOLERANCE) : preparePolygonPath(g2d, outerCoordinates, DRAWING_TOLERANCE);
//        } else {
//            path = null;
//        }
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        if (path == null) return;

        g2d.setColor(getRgbaColor());
        if (isLine && getStroke() != null) {
            g2d.setStroke(new BasicStroke(strokeBaseWidth * getStroke()));
            g2d.draw(path);
        } else {
            g2d.fill(path);
        }
    }
}

package dk.itu.data.models.osm;

import dk.itu.data.models.parser.ParserOsmWay;

import java.awt.*;
import java.awt.geom.Path2D;

import static dk.itu.util.ShapePreparer.prepareLinePath;
import static dk.itu.util.ShapePreparer.preparePolygonPath;

public class OsmWay extends OsmElement {
    private final double[] outerCoordinates;
    private final boolean isLine;
    private Path2D.Double path = null;

    public OsmWay(long id, boolean isLine, double[] boundingBox, double[] outerCoordinates) {
        super(id, boundingBox);
        this.isLine = isLine;
        this.outerCoordinates = outerCoordinates;
    }

    public double[] getOuterCoordinates(){
        return outerCoordinates;
    }

    public static OsmWay mapToOsmWay(ParserOsmWay parserOsmWay) {
        var bounds = parserOsmWay.getBounds();

        var osmWay = new OsmWay(
                parserOsmWay.getId(),
                parserOsmWay.isLine(),
                bounds,
                parserOsmWay.getCoordinates()
        );

        osmWay.setStyle(parserOsmWay.getColor(), parserOsmWay.getStroke());

        return osmWay;
    }

    public static OsmWay createWayForRouting(double[] coordinates) {
        var way = new OsmWay(0, true, new double[]{0.0, 0.0, 0.0, 0.0}, coordinates);
        way.setStyle(Color.yellow, 6);
        return way;
    }

    public boolean isLine() {
        return isLine;
    }

    @Override
    public void prepareDrawing(Graphics2D g2d) {
        path = isLine ? prepareLinePath(g2d, outerCoordinates, DRAWING_TOLERANCE) : preparePolygonPath(g2d, outerCoordinates, DRAWING_TOLERANCE);
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        if (path == null) return;

        g2d.setColor(getColor());
        if (isLine && getStroke() != null) {
            g2d.setStroke(new BasicStroke(strokeBaseWidth * getStroke()));
            g2d.draw(path);
        } else {
            g2d.fill(path);
        }
    }
}
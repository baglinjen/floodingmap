package dk.itu.data.models.osm;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.Drawable;
import dk.itu.data.models.parser.ParserOsmWay;
import dk.itu.util.shape.WayPath;

import java.awt.*;

import static dk.itu.util.PolygonUtils.forceCounterClockwise;
import static dk.itu.util.PolygonUtils.isClosed;
import static dk.itu.common.models.WithBoundingBoxAndArea.calculateArea;

public class OsmWay implements OsmElement, Drawable {
    private final long id;
    private final byte styleId;
    private double minLon, minLat, maxLon, maxLat;
    private final double area;
    private final WayPath path;

    public OsmWay(long id, WayPath path, byte styleId) {
        this.id = id;

        minLon = maxLon = path.getOuterCoordinates()[0];
        minLat = maxLat = path.getOuterCoordinates()[1];

        // Calculate Bounding Box
        for (int i = 2; i < path.getOuterCoordinates().length; i+=2) {
            if (path.getOuterCoordinates()[i] < minLon) minLon = path.getOuterCoordinates()[i];
            if (path.getOuterCoordinates()[i] > maxLon) maxLon = path.getOuterCoordinates()[i];
            if (path.getOuterCoordinates()[i+1] < minLat) minLat = path.getOuterCoordinates()[i+1];
            if (path.getOuterCoordinates()[i+1] > maxLat) maxLat = path.getOuterCoordinates()[i+1];
        }
        this.area = calculateArea(minLon, minLat, maxLon, maxLat);

        this.path = path;
        this.styleId = styleId;
    }

    public double[] getOuterCoordinates(){
        return this.path.getOuterCoordinates();
    }

    public static OsmWay mapToOsmWay(ParserOsmWay parserOsmWay) {
        return new OsmWay(
                parserOsmWay.getId(),
                new WayPath(parserOsmWay.isLine() ? parserOsmWay.getCoordinates() : forceCounterClockwise(parserOsmWay.getCoordinates())),
                parserOsmWay.getStyleId()
        );
    }

    public static OsmWay createWayForRouting(double[] coordinates) {
        return new OsmWay(
                0,
                new WayPath(coordinates),
                (byte) 3 // ID of routing style is always 3 - see DrawingConfiguration#addCommonStyles()
        );
    }

    public boolean isLine() {
        return !isClosed(this.path.getOuterCoordinates());
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public double minLon() {
        return this.minLon;
    }

    @Override
    public double minLat() {
        return this.minLat;
    }

    @Override
    public double maxLon() {
        return this.maxLon;
    }

    @Override
    public double maxLat() {
        return this.maxLat;
    }

    @Override
    public double getArea() {
        return this.area;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(DrawingConfiguration.getInstance().getColor(styleId));
        if (isLine() && DrawingConfiguration.getInstance().getStroke(styleId) != null) {
            g2d.setStroke(new BasicStroke(strokeBaseWidth * DrawingConfiguration.getInstance().getStroke(styleId)));
            g2d.draw(path);
        } else {
            g2d.fill(path);
        }
    }
}
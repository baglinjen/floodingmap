package dk.itu.data.models.parser;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

public class ParserOsmWay extends ParserOsmElement {
    private final double[] bounds = new double[4];
    private final double[] coordinates;

    private final boolean isLine;
    private final Path2D.Double shape;

    public ParserOsmWay(long id, List<ParserOsmNode> nodes) {
        super(id);
        double minLat = nodes.getFirst().getLat(), minLon = nodes.getFirst().getLon(), maxLat = nodes.getFirst().getLat(), maxLon = nodes.getFirst().getLon();
        var coordinatesRaw = new double[nodes.size()*2];

        coordinatesRaw[0] = nodes.getFirst().getLon();
        coordinatesRaw[1] = nodes.getFirst().getLat();
        for (int i = 1; i < nodes.size(); i++) {
            nodes.get(i).setShouldBeDrawn(false);
            var lon = nodes.get(i).getLon();
            var lat = nodes.get(i).getLat();
            if (lat < minLat) minLat = lat;
            if (lon < minLon) minLon = lon;
            if (lat > maxLat) maxLat = lat;
            if (lon > maxLon) maxLon = lon;
            coordinatesRaw[i*2] = lon;
            coordinatesRaw[i*2 + 1] = lat;
        }
        bounds[0] = minLat;
        bounds[1] = minLon;
        bounds[2] = maxLat;
        bounds[3] = maxLon;

        isLine = nodes.getFirst().id() != nodes.getLast().id();

        if (!isLine) {
            coordinates = isClockwise(coordinatesRaw) ? reversePairs(coordinatesRaw) : coordinatesRaw;
        } else {
            coordinates = coordinatesRaw;
        }

        shape = new Path2D.Double(Path2D.WIND_NON_ZERO);
        shape.moveTo(0.56*coordinates[0], -coordinates[1]);
        for (int i = 2; i < coordinates.length; i+=2) {
            shape.lineTo(0.56*coordinates[i], -coordinates[i+1]);
        }

        if (!isLine) {
            shape.closePath();
        }
    }

    @Override
    public double getArea() {
        return (bounds[2]-bounds[0]) * (bounds[3]-bounds[1]);
    }

    @Override
    public double[] getBounds() {
        return bounds;
    }

    @Override
    public Path2D.Double getShape() {
        return shape;
    }

    public boolean isLine() {
        return isLine;
    }

    public double[] getCoordinates() {
        return coordinates;
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

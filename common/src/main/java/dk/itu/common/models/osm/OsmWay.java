package dk.itu.common.models.osm;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.List;

public class OsmWay extends OsmElement {
    private final double[] bounds = new double[4];

    private final Shape shape;

    public OsmWay(long id, List<OsmNode> nodes) {
        super(id);
        double minLat = nodes.getFirst().getLat(), minLon = nodes.getFirst().getLon(), maxLat = nodes.getFirst().getLat(), maxLon = nodes.getFirst().getLon();
        Path2D path = new Path2D.Double();

        path.moveTo(0.56*nodes.getFirst().getLon(), -nodes.getFirst().getLat());
        for (int i = 1; i < nodes.size(); i++) {
            nodes.get(i).setShouldBeDrawn(false);
            var lon = nodes.get(i).getLon();
            var lat = nodes.get(i).getLat();
            if (lat < minLat) minLat = lat;
            if (lon < minLon) minLon = lon;
            if (lat > maxLat) maxLat = lat;
            if (lon > maxLon) maxLon = lon;
            path.lineTo(0.56*lon, -lat);
        }
        bounds[0] = minLat;
        bounds[1] = minLon;
        bounds[2] = maxLat;
        bounds[3] = maxLon;

        shape = nodes.getFirst().id == nodes.getLast().id ? new Area(path) : path;
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
    public Shape getShape() {
        return shape;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getRgbaColor());
        switch (shape) {
            case Area _ -> {
                g2d.fill(shape);
            }
            case Path2D _ -> {
                // Draw polyline
                g2d.setStroke(new BasicStroke(strokeBaseWidth * getStroke()));
                g2d.draw(shape);
            }
            default -> {}
        }
    }
}

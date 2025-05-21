package dk.itu.data.models.parser;

import dk.itu.util.shape.WayPath;

import java.awt.*;
import java.util.List;

import static dk.itu.util.PolygonUtils.*;

public class ParserOsmWay extends ParserOsmElement {
    private final double[] bounds = new double[4];
    private final WayPath path;

    private final boolean isLine;

    public ParserOsmWay(long id, List<ParserOsmNode> nodes) {
        super(id);
        double minLat = nodes.getFirst().getLat(), minLon = nodes.getFirst().getLon(), maxLat = nodes.getFirst().getLat(), maxLon = nodes.getFirst().getLon();
        double[] coordinatesRaw = new double[nodes.size()*2];

        coordinatesRaw[0] = nodes.getFirst().getLon();
        coordinatesRaw[1] = nodes.getFirst().getLat();
        for (int i = 1; i < nodes.size(); i++) {
            nodes.get(i).setShouldBeDrawn(false);
            var lon = nodes.get(i).getLon();
            var lat = nodes.get(i).getLat();
            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
            coordinatesRaw[i*2] = lon;
            coordinatesRaw[i*2 + 1] = lat;
        }
        bounds[0] = minLon;
        bounds[1] = minLat;
        bounds[2] = maxLon;
        bounds[3] = maxLat;

        isLine = nodes.getFirst().getId() != nodes.getLast().getId();

        path = new WayPath(isLine ? coordinatesRaw : forceCounterClockwise(coordinatesRaw));
    }

    public WayPath getPath() {
        return path;
    }

    @Override
    public double getArea() {
        return (bounds[2]-bounds[0]) * (bounds[3]-bounds[1]);
    }

    @Override
    public double[] getBounds() {
        return bounds;
    }

    public boolean isLine() {
        return isLine;
    }

    public double[] getCoordinates() {
        return path.getOuterCoordinates();
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        // No need to draw
    }
}
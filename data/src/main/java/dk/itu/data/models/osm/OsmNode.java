package dk.itu.data.models.osm;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.OsmElement;

import java.awt.*;

public record OsmNode(long id, double lat, double lon) implements OsmElement {
    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lon;
    }

    public BoundingBox getBoundingBox() {
        return new BoundingBox(lon, lat, lon, lat);
    }

    @Override
    public void setStyle(DrawingConfiguration.Style style) {

    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {

    }
}

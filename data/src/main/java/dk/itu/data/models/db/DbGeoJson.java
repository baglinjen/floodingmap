package dk.itu.data.models.db;

import dk.itu.common.models.Colored;
import dk.itu.common.models.GeoJsonElement;
import org.locationtech.jts.geom.Polygon;

import java.awt.*;

public class DbGeoJson extends Colored implements GeoJsonElement {

    public DbGeoJson(Polygon shape, float height) {
    }

    @Override
    public float getHeight() {
        return 0;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {}
}

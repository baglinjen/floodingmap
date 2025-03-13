package dk.itu.data.dbmodels;

import dk.itu.common.models.osm.OsmElement;
import org.locationtech.jts.geom.MultiPolygon;

import java.awt.*;

public class DbGeoJson extends OsmElement {
    private final float height;

    public DbGeoJson(long id, MultiPolygon shape, float height) {
        super(id, shape);
        this.height = height;
    }

    @Override
    public double getArea() {
        return ((MultiPolygon) geometry).getArea();
    }

    @Override
    public boolean shouldBeDrawn() {
        return true;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {

    }

    public float getHeight() {
        return height;
    }

    @Override
    public double[] getBounds() {
        return new double[0];
    }
}

package dk.itu.data.dbmodels;

import dk.itu.common.models.osm.OsmElement;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import java.awt.*;

public class DbWay extends OsmElement {

    public DbWay(long id, Geometry geometry) {
        super(id, geometry);
    }

    @Override
    public double getArea() {
        if (geometry instanceof Polygon) {
            return geometry.getArea();
        }
        return 0;
    }

    @Override
    public boolean shouldBeDrawn() {
        return true;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {

    }

    @Override
    public double[] getBounds() {
        return new double[0];
    }
}

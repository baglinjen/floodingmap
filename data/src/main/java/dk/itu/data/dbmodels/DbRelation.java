package dk.itu.data.dbmodels;

import dk.itu.common.models.osm.OsmElement;
import org.locationtech.jts.geom.MultiPolygon;
import java.awt.*;

public class DbRelation extends OsmElement {

    public DbRelation(long id, MultiPolygon shape) {
        super(id, shape);
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

    @Override
    public double[] getBounds() {
        return new double[0];
    }
}

package dk.itu.data.dbmodels;

import dk.itu.common.models.osm.OsmElement;
import org.locationtech.jts.geom.Point;

import java.awt.*;

public class DbNode extends OsmElement {

    public DbNode(long id, Point point) {
        super(id, point);
    }

    @Override
    public double getArea() {
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

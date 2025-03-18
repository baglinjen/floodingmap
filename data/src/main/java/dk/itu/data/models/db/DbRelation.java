package dk.itu.data.models.db;

import java.awt.*;
import java.awt.geom.Path2D;

public class DbRelation extends DbOsmElement {
    private final Shape shape;
    public DbRelation(long id, Path2D shape, int color) {
        super(id);
        setRgbaColor(color);
        this.shape = shape;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getRgbaColor());
        g2d.fill(shape);
    }
}
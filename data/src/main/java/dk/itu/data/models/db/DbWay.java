package dk.itu.data.models.db;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Objects;

public class DbWay extends DbOsmElement {
    private final Shape shape;
    private final boolean isLine;
    public DbWay(long id, Path2D shape, String type, int color) {
        super(id);
        this.setRgbaColor(color);
        this.shape = shape;
        this.isLine = Objects.equals(type, "line");
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

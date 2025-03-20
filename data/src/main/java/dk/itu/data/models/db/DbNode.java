package dk.itu.data.models.db;

import java.awt.*;

public class DbNode extends DbOsmElement {
    public DbNode(long id) {
        super(id);
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {}
}
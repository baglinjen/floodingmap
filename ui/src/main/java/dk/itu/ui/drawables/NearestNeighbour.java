package dk.itu.ui.drawables;

import dk.itu.common.models.Colored;
import dk.itu.data.models.db.osm.OsmNode;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class NearestNeighbour extends Colored {
    private final OsmNode selectedOsmElement;
    private final Point2D.Double mousePos;
    public NearestNeighbour(OsmNode selectedOsmElement, Point2D.Double mouseLonLat) {
        this.selectedOsmElement = selectedOsmElement;
        this.mousePos = mouseLonLat;
    }

    public OsmNode getSelectedOsmElement() {
        return selectedOsmElement;
    }

    @Override
    public void prepareDrawing(Graphics2D g2d) { /* Nothing to prepare */ }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        var path = new Path2D.Double();
        path.moveTo(0.56*mousePos.getX(), -mousePos.getY());
        path.lineTo(0.56*selectedOsmElement.getLon(), -selectedOsmElement.getLat());
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(strokeBaseWidth * 3));
        g2d.draw(path);

        g2d.setColor(Color.RED);
        g2d.fill(new Ellipse2D.Double(0.56*mousePos.getX() - strokeBaseWidth*8/2, -mousePos.getY() - strokeBaseWidth*8/2, strokeBaseWidth*8, strokeBaseWidth*8));
        g2d.fill(new Ellipse2D.Double(0.56*selectedOsmElement.getLon() - strokeBaseWidth*8/2, -selectedOsmElement.getLat() - strokeBaseWidth*8/2, strokeBaseWidth*8, strokeBaseWidth*8));
    }
}

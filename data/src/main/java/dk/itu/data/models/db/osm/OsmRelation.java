package dk.itu.data.models.db.osm;

import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.parser.ParserOsmRelation;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

import static dk.itu.util.ShapePreparer.*;

public class OsmRelation extends OsmElement {
    private final List<double[]> outerPolygons;
    private final List<double[]> innerPolygons;
    private Path2D.Double path = null;

    public OsmRelation(long id, BoundingBox boundingBox, double area, List<double[]> outerPolygons, List<double[]> innerPolygons) {
        super(id, boundingBox, area);
        this.outerPolygons = outerPolygons;
        this.innerPolygons = innerPolygons;
    }

    public static OsmRelation mapToOsmRelation(ParserOsmRelation parserOsmRelation) {
        var bounds = parserOsmRelation.getBounds();

        BoundingBox boundingBox = new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3]);
        var osmRelation = new OsmRelation(
                parserOsmRelation.getId(),
                boundingBox,
                parserOsmRelation.getArea(),
                parserOsmRelation.getOuterPolygons(),
                parserOsmRelation.getInnerPolygons()
        );

        osmRelation.setStyle(parserOsmRelation.getRgbaColor(), parserOsmRelation.getStroke());

        return osmRelation;
    }

    @Override
    public void prepareDrawing(Graphics2D g2d) {
        path = prepareComplexPolygon(g2d, outerPolygons, innerPolygons, DRAWING_TOLERANCE);
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        if (path == null) return;

        g2d.setColor(getRgbaColor());
        g2d.fill(path);
    }
}

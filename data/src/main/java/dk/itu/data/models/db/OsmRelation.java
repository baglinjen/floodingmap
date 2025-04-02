package dk.itu.data.models.db;

import dk.itu.data.models.parser.ParserOsmRelation;

import java.awt.*;

public class OsmRelation extends OsmElement {
    private final Shape shape;

    public OsmRelation(long id, Shape shape, BoundingBox boundingBox, double area) {
        super(id, boundingBox, area);
        this.shape = shape;
    }

    public static OsmRelation mapToOsmRelation(ParserOsmRelation parserOsmRelation) {
        var bounds = parserOsmRelation.getBounds();

        BoundingBox boundingBox = new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3]);
        var osmRelation = new OsmRelation(parserOsmRelation.getId(), parserOsmRelation.getShape(), boundingBox, parserOsmRelation.getArea());

        osmRelation.setStyle(parserOsmRelation.getRgbaColor(), parserOsmRelation.getStroke());

        return osmRelation;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getRgbaColor());
        g2d.fill(shape);
    }
}

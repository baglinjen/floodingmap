package dk.itu.data.models.osm;

import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.util.shape.RelationPath;

import java.awt.*;
import java.util.List;

public class OsmRelation extends OsmElement {
    private final RelationPath complexPathNodeSkip;

    public OsmRelation(long id, double[] boundingBox, List<double[]> outerPolygons, List<double[]> innerPolygons) {
        super(id, boundingBox);
        this.complexPathNodeSkip = new RelationPath(outerPolygons, innerPolygons);
    }

    public static OsmRelation mapToOsmRelation(ParserOsmRelation parserOsmRelation) {
        var bounds = parserOsmRelation.getBounds();

        var osmRelation = new OsmRelation(
                parserOsmRelation.getId(),
                bounds,
                parserOsmRelation.getOuterPolygons(),
                parserOsmRelation.getInnerPolygons()
        );

        osmRelation.setStyle(parserOsmRelation.getColor(), parserOsmRelation.getStroke());

        return osmRelation;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getColor());
        g2d.fill(complexPathNodeSkip);
    }
}
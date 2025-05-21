package dk.itu.data.models.osm;

import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.util.shape.RelationPath;

import java.awt.*;
import java.util.List;

public class OsmRelation extends OsmElement {
    private final RelationPath path;

    public OsmRelation(long id, double[] boundingBox, RelationPath relationPath) {
        super(id, boundingBox);
        this.path = relationPath;
    }

    public static OsmRelation mapToOsmRelation(ParserOsmRelation parserOsmRelation) {
        var osmRelation = new OsmRelation(
                parserOsmRelation.getId(),
                parserOsmRelation.getBounds(),
                parserOsmRelation.getPath()
        );

        osmRelation.setStyle(parserOsmRelation.getColor(), parserOsmRelation.getStroke());

        return osmRelation;
    }

    public List<double[]> getOuterPolygons() {
        return path.getOuterPolygons();
    }

    public List<double[]> getInnerPolygons() {
        return path.getInnerPolygons();
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getColor());
        g2d.fill(path);
    }
}
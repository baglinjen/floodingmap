package dk.itu.data.models.osm;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.Drawable;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.util.shape.RelationPath;

import java.awt.*;
import java.util.List;

import static dk.itu.common.models.WithBoundingBoxAndArea.calculateArea;

public class OsmRelation implements OsmElement, Drawable {
    private final long id;
    private final byte styleId;
    private float minLon, minLat, maxLon, maxLat;
    private final float area;
    private final RelationPath path;

    public OsmRelation(long id, byte styleId, RelationPath relationPath) {
        this.id = id;
        this.styleId = styleId;

        minLon = maxLon = relationPath.getOuterPolygons().getFirst()[0];
        minLat = maxLat = relationPath.getOuterPolygons().getFirst()[1];

        // Calculate Bounding Box
        for (int i = 0; i < relationPath.getOuterPolygons().size(); i++) {
            var outerPolygon = relationPath.getOuterPolygons().get(i);
            for (int j = 2; j < outerPolygon.length; j+=2) {
                if (outerPolygon[j] < minLon) minLon = outerPolygon[j];
                if (outerPolygon[j] > maxLon) maxLon = outerPolygon[j];
                if (outerPolygon[j+1] < minLat) minLat = outerPolygon[j+1];
                if (outerPolygon[j+1] > maxLat) maxLat = outerPolygon[j+1];
            }
        }

        this.area = calculateArea(minLon, minLat, maxLon, maxLat);

        this.path = relationPath;
    }

    public static OsmRelation mapToOsmRelation(ParserOsmRelation parserOsmRelation) {
        return new OsmRelation(
                parserOsmRelation.getId(),
                parserOsmRelation.getStyleId(),
                parserOsmRelation.getPath()
        );
    }

    public List<float[]> getOuterPolygons() {
        return path.getOuterPolygons();
    }

    public List<float[]> getInnerPolygons() {
        return path.getInnerPolygons();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public float minLon() {
        return this.minLon;
    }

    @Override
    public float minLat() {
        return this.minLat;
    }

    @Override
    public float maxLon() {
        return this.maxLon;
    }

    @Override
    public float maxLat() {
        return this.maxLat;
    }

    @Override
    public float getArea() {
        return this.area;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(DrawingConfiguration.getInstance().getColor(styleId));
        g2d.fill(path);
    }
}
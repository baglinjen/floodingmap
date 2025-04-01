package dk.itu.data.models.memory;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.OsmElement;
import dk.itu.data.models.parser.ParserOsmWay;
import kotlin.Pair;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmWay extends OsmElementMemory {
    private final Shape shape;
    private final boolean isLine;

    public OsmWay(long id, Shape shape, boolean isLine, BoundingBox boundingBox, double area) {
        super(id, boundingBox, area);
        this.shape = shape;
        this.isLine = isLine;
    }

    public static OsmWay mapToOsmWay(ParserOsmWay parserOsmWay) {
        var bounds = parserOsmWay.getBounds();

        BoundingBox boundingBox = new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3]);
        var osmWay = new OsmWay(parserOsmWay.getId(), parserOsmWay.getShape(), parserOsmWay.isLine(), boundingBox, parserOsmWay.getArea());

        osmWay.setStyle(parserOsmWay.getRgbaColor(), parserOsmWay.getStroke());

        return osmWay;
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

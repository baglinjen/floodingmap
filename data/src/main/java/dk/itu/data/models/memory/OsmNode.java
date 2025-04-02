package dk.itu.data.models.memory;

import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import kotlin.Pair;

import java.awt.*;

public class OsmNode extends OsmElementMemory {

    public OsmNode(long id, BoundingBox boundingBox) {
        super(id, boundingBox, 0);
    }

    public static OsmNode mapToOsmNode(ParserOsmNode parserOsmNode) {
        BoundingBox boundingBox = new BoundingBox(
                parserOsmNode.getLon(),
                parserOsmNode.getLat(),
                parserOsmNode.getLon(),
                parserOsmNode.getLat()
        );

        var osmNode = new OsmNode(parserOsmNode.getId(), boundingBox);
        osmNode.setStyle(parserOsmNode.getRgbaColor(), parserOsmNode.getStroke());

        return osmNode;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
    }
}

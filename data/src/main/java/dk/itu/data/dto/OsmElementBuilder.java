package dk.itu.data.dto;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;
import dk.itu.util.LoggerFactory;
import kotlin.Pair;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmElementBuilder {
    private static final Logger logger = LoggerFactory.getLogger();
    // Parsed so far
    private final OsmParserResult osmParserResult;
    // Fields
    private long currentId;
    private float lat, lon;
    private boolean idAdded = false, latLonAdded = false;
    private OsmElementType type = null;
    private final Map<String, String> tags = new HashMap<>();
    // Ways
    private final List<ParserOsmNode> wayNodes = new ArrayList<>();
    // Relations
    private final List<Pair<ParserOsmElement, ParserOsmRelation.OsmRelationMemberType>> members = new ArrayList<>();
    // Validity
    private boolean invalidElement = false;

    public OsmElementBuilder(OsmParserResult osmParserResult) {
        this.osmParserResult = osmParserResult;
    }

    public void buildAndAddElement() {
        byte styleId = DrawingConfiguration.getInstance().getStyle(tags);

        switch (type) {
            case NODE:
                if (
                        idAdded &&
                        latLonAdded &&
                        !invalidElement
                ) {
                    osmParserResult.addNode(new ParserOsmNode(currentId, lat, lon));
                } else {
                    logger.warn("Parsing node with id {} is invalid", currentId);
                }
                break;
            case WAY:
                if (
                        idAdded &&
                        !wayNodes.isEmpty() &&
                        !invalidElement
                ) {
                    osmParserResult.addWay(new ParserOsmWay(currentId, wayNodes, styleId));

                    //If the way is traversable -> modify the containing nodes
                    if (this.tags.containsKey("highway")) {
                        osmParserResult.addTraversableNodes(wayNodes);
                        // Connect the nodes between each other
                        for (int i = 0; i < wayNodes.size(); i++){
                            var curNode = wayNodes.get(i);
                            if (i != 0) curNode.addConnectionId(wayNodes.get(i-1).getId());
                            if (i != wayNodes.size() - 1) curNode.addConnectionId(wayNodes.get(i+1).getId());
                        }
                    }
                } else {
                    logger.warn("Parsing way with id {} is invalid", currentId);
                }
                break;
            case RELATION:
                if (
                        idAdded &&
                        !members.isEmpty() &&
                        !invalidElement
                ) {
                    osmParserResult.addRelation(new ParserOsmRelation(currentId, members, ParserOsmRelation.OsmRelationType.fromTags(tags), styleId));
                } else {
                    logger.warn("Parsing relation with id {} is invalid", currentId);
                }
                break;
            default:
                logger.error("Unknown element type {} when parsing:\n{}", type, this);
                break;
        }

        // Reset fields
        idAdded = false;
        latLonAdded = false;
        type = null;
        tags.clear();
        wayNodes.clear();
        members.clear();
        invalidElement = false;
    }

    // ALL
    public void withId(Long currentId) {
        this.currentId = currentId;
        this.idAdded = true;
    }
    public void withTag(String k, String v) {
        this.tags.put(k, v);
    }
    public OsmElementBuilder withType(OsmElementType type) {
        this.type = type;
        return this;
    }

    // NODES
    public OsmElementBuilder withCoordinates(float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
        this.latLonAdded = true;
        return this;
    }

    // WAYS
    public void withNodeReference(long referencedNodeId) {
        if (invalidElement) return;

        ParserOsmElement node = osmParserResult.findNode(referencedNodeId);
        if (node instanceof ParserOsmNode) {
            wayNodes.add((ParserOsmNode) node);
            node.setStyleId((byte) -1);
        } else {
            invalidElement = true;
        }
    }

    // RELATIONS
    public void withMemberReference(long referencedMemberId, OsmElementType type, ParserOsmRelation.OsmRelationMemberType memberType) {
        if (invalidElement) return;

        switch (type) {
            case NODE:
                ParserOsmElement node = osmParserResult.findNode(referencedMemberId);
                if (node != null) {
                    members.add(new Pair<>(node, memberType));
                } else {
                    invalidElement = true;
                }
                break;
            case WAY:
                ParserOsmElement way = osmParserResult.findWay(referencedMemberId);
                if (way != null) {
                    members.add(new Pair<>(way, memberType));
                } else {
                    invalidElement = true;
                }
                break;
            case RELATION:
                ParserOsmElement relation = osmParserResult.findRelation(referencedMemberId);
                if (relation != null) {
                    members.add(new Pair<>(relation, memberType));
                } else {
                    invalidElement = true;
                }
                break;
        }
    }

    public void clear() {
        tags.clear();
        wayNodes.clear();
        members.clear();
    }

    public enum OsmElementType {
        NODE,
        WAY,
        RELATION;

        public static OsmElementType fromString(String type) {
            return switch (type) {
                case "node" -> NODE;
                case "way" -> WAY;
                case "relation" -> RELATION;
                default -> null;
            };
        }
    }
}
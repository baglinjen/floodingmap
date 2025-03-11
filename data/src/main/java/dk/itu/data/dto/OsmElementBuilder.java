package dk.itu.data.dto;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.osm.OsmElement;
import dk.itu.common.models.osm.OsmNode;
import dk.itu.common.models.osm.OsmRelation;
import dk.itu.common.models.osm.OsmWay;
import dk.itu.util.LoggerFactory;
import kotlin.Pair;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmElementBuilder {
    // Util
    private static final Logger logger = LoggerFactory.getLogger();
    // Parsed so far
    private final OsmParserResult osmParserResult;
    // Fields
    private Long currentId = null;
    private Double lat = null, lon = null;
    private OsmElementType type = null;
    private final Map<String, String> tags = new HashMap<>();
    // Ways
    private final List<OsmNode> wayNodes = new ArrayList<>();
    // Relations
    private final List<Pair<OsmElement, OsmRelation.OsmRelationMemberType>> members = new ArrayList<>();
    // Validity
    private boolean invalidElement = false;

    public OsmElementBuilder(OsmParserResult osmParserResult) {
        this.osmParserResult = osmParserResult;
    }

    public void buildAndAddElement() {
        DrawingConfiguration.Style style = DrawingConfiguration.getInstance().getStyle(tags);

        switch (type) {
            case NODE:
                if (
                        currentId != null &&
                        lat != null &&
                        lon != null &&
                        !invalidElement
                ) {
                    var newNode = new OsmNode(currentId, lat, lon);
                    newNode.setShouldBeDrawn(style != null);
                    newNode.setStyle(style);
                    osmParserResult.addNode(newNode);
                } else {
                    logger.warn("Parsing node with id {} is invalid", currentId);
                }
                break;
            case WAY:
                if (
                        currentId != null &&
                        !wayNodes.isEmpty() &&
                        !invalidElement
                ) {
                    var newWay = new OsmWay(currentId, wayNodes);
                    newWay.setShouldBeDrawn(style != null);
                    newWay.setStyle(style);
                    osmParserResult.addWay(newWay);
                } else {
                    logger.warn("Parsing way with id {} is invalid", currentId);
                }
                break;
            case RELATION:
                if (
                        currentId != null &&
                        !members.isEmpty() &&
                        !invalidElement
                ) {
                    var newRelation = new OsmRelation(currentId, members, OsmRelation.OsmRelationType.fromTags(tags));
                    newRelation.setShouldBeDrawn(style != null);
                    newRelation.setStyle(style);
                    osmParserResult.addRelation(newRelation);
                } else {
                    logger.warn("Parsing relation with id {} is invalid", currentId);
                }
                break;
            default:
                logger.error("Unknown element type {} when parsing:\n{}", type, this);
                break;
        }

        // Reset fields
        currentId = null;
        lat = lon = null;
        type = null;
        tags.clear();
        wayNodes.clear();
        members.clear();
        invalidElement = false;
    }

    // ALL
    public void withId(Long currentId) {
        this.currentId = currentId;
    }
    public void withTag(String k, String v) {
        this.tags.put(k, v);
    }
    public OsmElementBuilder withType(OsmElementType type) {
        this.type = type;
        return this;
    }

    // NODES
    public OsmElementBuilder withCoordinates(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        return this;
    }

    // WAYS
    public void withNodeReference(long referencedNodeId) {
        if (invalidElement) return;

        OsmElement node = osmParserResult.findNode(referencedNodeId);
        if (node instanceof OsmNode) {
            wayNodes.add((OsmNode) node);
            node.setShouldBeDrawn(false);
        } else {
            invalidElement = true;
        }
    }

    // RELATIONS
    public void withMemberReference(long referencedMemberId, OsmElementType type, OsmRelation.OsmRelationMemberType memberType) {
        if (invalidElement) return;

        switch (type) {
            case NODE:
                OsmElement node = osmParserResult.findNode(referencedMemberId);
                if (node != null) {
                    members.add(new Pair<>(node, memberType));
                } else {
                    invalidElement = true;
                }
                break;
            case WAY:
                OsmElement way = osmParserResult.findWay(referencedMemberId);
                if (way != null) {
                    members.add(new Pair<>(way, memberType));
                } else {
                    invalidElement = true;
                }
                break;
            case RELATION:
                OsmElement relation = osmParserResult.findRelation(referencedMemberId);
                if (relation != null) {
                    members.add(new Pair<>(relation, memberType));
                } else {
                    invalidElement = true;
                }
                break;
        }
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

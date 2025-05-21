package dk.itu.data.dto;

import dk.itu.common.models.WithId;
import dk.itu.data.models.BoundingBox;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import dk.itu.data.models.osm.OsmRelation;
import dk.itu.data.models.osm.OsmWay;
import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class OsmParserResult {
    private final List<ParserOsmNode> nodes = new ArrayList<>();
    private final List<ParserOsmElement> ways = new ArrayList<>();
    private final List<ParserOsmElement> relations = new ArrayList<>();
    private final Long2ObjectAVLTreeMap<ParserOsmNode> traversableNodes = new Long2ObjectAVLTreeMap<>();

    private List<OsmElement> elementsToBeDrawn;
    private List<OsmNode> traversals;

    public List<OsmElement> getElementsToBeDrawn() {
        return elementsToBeDrawn;
    }
    public List<OsmNode> getTraversals() {
        return traversals;
    }

    public void sanitize() {
        List<OsmElement> allElements = new ArrayList<>();
        allElements.addAll(this.nodes.parallelStream().filter(ParserOsmElement::shouldBeDrawn).map(this::mapToOsmElement).toList());
        this.nodes.clear();
        allElements.addAll(this.ways.parallelStream().filter(ParserOsmElement::shouldBeDrawn).map(this::mapToOsmElement).toList());
        this.ways.clear();
        allElements.addAll(this.relations.parallelStream().filter(ParserOsmElement::shouldBeDrawn).map(this::mapToOsmElement).toList());
        this.relations.clear();

        this.elementsToBeDrawn = allElements.parallelStream().sorted(Comparator.comparing(BoundingBox::getArea)).toList();

        // Building connection map
        List<ParserOsmNode> traversableNodesSorted = this.traversableNodes.values().stream().toList();
        this.traversableNodes.clear();
        this.traversals = traversableNodesSorted.parallelStream().map(OsmNode::mapToOsmNode).toList();

        final int[] hits = new int[] {0}; // 1800+ for Bornholm

        IntStream.range(0, traversableNodesSorted.size())
                .parallel()
                .forEach(i -> {
                    var connections = traversableNodesSorted.get(i).getConnectionIds();
                    if (connections != null) {
                        for (Long connection : traversableNodesSorted.get(i).getConnectionIds()) {
                            OsmNode node = findElement(connection, this.traversals);
                            if (node != null) {
                                this.traversals.get(i).addConnection(node);
                            } else {
                                hits[0]++;
                                // Should update connection with -1
                            }
                        }

                        // Check if elements is still traversable
                    }
                });



        System.out.println(hits[0]);
    }

    private OsmElement mapToOsmElement(ParserOsmElement osmElement) {
        return switch (osmElement) {
            case ParserOsmNode node -> OsmNode.mapToOsmNode(node);
            case ParserOsmWay way -> OsmWay.mapToOsmWay(way);
            case ParserOsmRelation relation -> OsmRelation.mapToOsmRelation(relation);
            default -> null;
        };
    }

    public void addNode(ParserOsmNode node) {
        this.nodes.add(node);
    }
    public void addTraversableNode(ParserOsmNode node) {
        this.traversableNodes.put(node.getId(), node);
    }
    public void addWay(ParserOsmWay way) {
        this.ways.add(way);
    }
    public void addRelation(ParserOsmRelation relation) {
        this.relations.add(relation);
    }

    public ParserOsmElement findNode(long id) {
        return findElement(id, this.nodes);
    }
    public ParserOsmElement findWay(long id) {
        return findElement(id, this.ways);
    }
    public ParserOsmElement findRelation(long id) {
        return findElement(id, this.relations);
    }

    public <T extends WithId> T findElement(long id, List<T> elements) {
        int left = 0;
        int right = elements.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            T midNode = elements.get(mid);

            if (midNode.getId() == id) {
                return midNode;
            } else if (midNode.getId() < id) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return null;
    }
}
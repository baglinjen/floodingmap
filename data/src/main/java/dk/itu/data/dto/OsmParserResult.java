package dk.itu.data.dto;

import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OsmParserResult {
    private final List<ParserOsmNode> nodes = new ArrayList<>();
    private final List<ParserOsmElement> ways = new ArrayList<>();
    private final List<ParserOsmElement> relations = new ArrayList<>();
    private final List<ParserOsmNode> traversableNodes = new ArrayList<>();
    private List<ParserOsmElement> elementsToBeDrawn;

    public List<ParserOsmElement> getElementsToBeDrawn() {
        return elementsToBeDrawn;
    }
    public List<ParserOsmNode> getTraversableNodes() {
        return traversableNodes;
    }

    public void sanitize() {
        List<ParserOsmElement> allElements = new ArrayList<>(this.nodes.parallelStream().filter(ParserOsmElement::shouldBeDrawn).toList());
        this.nodes.clear();
        allElements.addAll(this.ways.parallelStream().filter(ParserOsmElement::shouldBeDrawn).toList());
        this.ways.clear();
        allElements.addAll(this.relations.parallelStream().filter(ParserOsmElement::shouldBeDrawn).toList());
        this.relations.clear();

        this.elementsToBeDrawn = allElements.parallelStream().sorted(Comparator.comparing(ParserOsmElement::getArea).reversed()).toList();
    }

    public void addNode(ParserOsmNode node) {
        this.nodes.add(node);
    }
    public void addTraversableNode(ParserOsmNode node) {
        this.traversableNodes.add(node);
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

    public <T extends ParserOsmElement> T findElement(long id, List<T> elements) {
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
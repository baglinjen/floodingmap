package dk.itu.data.dto;

import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OsmParserResult {
    private double minLon, minLat, maxLon, maxLat;
    private final List<ParserOsmElement> nodes = new ArrayList<>();
    private final List<ParserOsmElement> ways = new ArrayList<>();
    private final List<ParserOsmElement> relations = new ArrayList<>();
    private List<ParserOsmElement> elementsToBeDrawn;

    public List<ParserOsmElement> getElementsToBeDrawn() {
        return elementsToBeDrawn;
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

    public double[] getBounds() {
        return new double[]{minLat, minLon, maxLat, maxLon};
    }

    public void setBounds(double minLon, double minLat, double maxLon, double maxLat) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
    }

    public void addNode(ParserOsmNode node) {
        this.nodes.add(node);
    }
    public void addWay(ParserOsmWay way) {
        this.ways.add(way);
    }
    public void addRelation(ParserOsmRelation relation) {
        this.relations.add(relation);
    }
    public List<ParserOsmElement> getNodes() {
        return nodes;
    }
    public List<ParserOsmElement> getWays() {
        return ways;
    }
    public List<ParserOsmElement> getRelations() {
        return relations;
    }

    public ParserOsmElement findNode(long id) {
        return findElement(id, 0, this.nodes.size(), this.nodes);
    }
    public ParserOsmElement findWay(long id) {
        return findElement(id, 0, this.ways.size(), this.ways);
    }
    public ParserOsmElement findRelation(long id) {
        return findElement(id, 0, this.relations.size(), this.relations);
    }

    private ParserOsmElement findElement(long id, int leftIndexBoundInclusive, int rightIndexBoundExclusive, List<ParserOsmElement> elements) {
        if (leftIndexBoundInclusive > rightIndexBoundExclusive-1) {
            return null;
        } else if (leftIndexBoundInclusive == rightIndexBoundExclusive-1) {
            var lastElement = elements.get(leftIndexBoundInclusive);
            if (lastElement.id() == id) {
                return lastElement;
            } else {
                return null;
            }
        }

        var halfwayIndex = leftIndexBoundInclusive + ((rightIndexBoundExclusive - leftIndexBoundInclusive) / 2);
        var halfWayElement = elements.get(halfwayIndex);
        if (id > halfWayElement.id()) {
            // Look at right side
            return findElement(id, halfwayIndex+1, rightIndexBoundExclusive, elements);
        } else if (id < halfWayElement.id()) {
            // Look at left side
            return findElement(id, leftIndexBoundInclusive, halfwayIndex, elements);
        } else {
            // Hit
            return halfWayElement;
        }
    }
}
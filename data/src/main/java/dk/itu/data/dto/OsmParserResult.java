package dk.itu.data.dto;

import dk.itu.common.models.osm.OsmElement;
import dk.itu.common.models.osm.OsmNode;
import dk.itu.common.models.osm.OsmRelation;
import dk.itu.common.models.osm.OsmWay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OsmParserResult {
    private double minLon, minLat, maxLon, maxLat;
    private final List<OsmElement> nodes = new ArrayList<>();
    private final List<OsmElement> ways = new ArrayList<>();
    private final List<OsmElement> relations = new ArrayList<>();
    private List<OsmElement> elementsToBeDrawn;

    public List<OsmElement> getElementsToBeDrawn() {
        return elementsToBeDrawn;
    }

    public void sanitize() {
        List<OsmElement> allElements = new ArrayList<>(this.nodes.parallelStream().filter(OsmElement::shouldBeDrawn).toList());
        this.nodes.clear();
        allElements.addAll(this.ways.parallelStream().filter(OsmElement::shouldBeDrawn).toList());
        this.ways.clear();
        allElements.addAll(this.relations.parallelStream().filter(OsmElement::shouldBeDrawn).toList());
        this.relations.clear();

        this.elementsToBeDrawn = allElements.parallelStream().sorted(Comparator.comparing(OsmElement::getArea).reversed()).toList();
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

    public void addNode(OsmNode node) {
        this.nodes.add(node);
    }
    public void addWay(OsmWay way) {
        this.ways.add(way);
    }
    public void addRelation(OsmRelation relation) {
        this.relations.add(relation);
    }
    public List<OsmElement> getNodes() {
        return nodes;
    }
    public List<OsmElement> getWays() {
        return ways;
    }
    public List<OsmElement> getRelations() {
        return relations;
    }

    public OsmElement findNode(long id) {
        return findElement(id, 0, this.nodes.size(), this.nodes);
    }
    public OsmElement findWay(long id) {
        return findElement(id, 0, this.ways.size(), this.ways);
    }
    public OsmElement findRelation(long id) {
        return findElement(id, 0, this.relations.size(), this.relations);
    }

    private OsmElement findElement(long id, int leftIndexBoundInclusive, int rightIndexBoundExclusive, List<OsmElement> elements) {
        if (leftIndexBoundInclusive > rightIndexBoundExclusive-1) {
            return null;
        } else if (leftIndexBoundInclusive == rightIndexBoundExclusive-1) {
            var lastElement = elements.get(leftIndexBoundInclusive);
            if (lastElement.id == id) {
                return lastElement;
            } else {
                return null;
            }
        }

        var halfwayIndex = leftIndexBoundInclusive + ((rightIndexBoundExclusive - leftIndexBoundInclusive) / 2);
        var halfWayElement = elements.get(halfwayIndex);
        if (id > halfWayElement.id) {
            // Look at right side
            return findElement(id, halfwayIndex+1, rightIndexBoundExclusive, elements);
        } else if (id < halfWayElement.id) {
            // Look at left side
            return findElement(id, leftIndexBoundInclusive, halfwayIndex, elements);
        } else {
            // Hit
            return halfWayElement;
        }
    }
}
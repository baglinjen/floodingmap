package dk.itu.data.dto;

import dk.itu.common.models.OsmElement;
import dk.itu.data.models.memory.OsmNode;
import dk.itu.data.models.memory.OsmRelation;
import dk.itu.data.models.memory.OsmWay;

import java.util.*;

public class OsmResult {
    private final Map<Long, OsmNode> nodes = new HashMap<>();
    private final Map<Long, OsmWay> ways = new HashMap<>();
    private final Map<Long, OsmRelation> relations = new HashMap<>();
    private final List<OsmElement> elements = new ArrayList<>();
    private double[] bounds;

    public void addNode(OsmNode node) { elements.add(node); }
    public void addWay(OsmWay way) { elements.add(way); }
    public void addRelation(OsmRelation relation) { elements.add(relation); }

    public Map<Long, OsmNode> getNodes() { return nodes; }
    public Map<Long, OsmWay> getWays() { return ways; }
    public Map<Long, OsmRelation> getRelations() { return relations; }

    public OsmNode getNodeById(long id) { return nodes.get(id); }
    public OsmWay getWayById(long id) { return ways.get(id); }
    public OsmRelation getRelationById(long id) { return relations.get(id); }

    public List<OsmElement> getElements() { return elements; }

    public void setBounds(double minLon, double minLat, double maxLon, double maxLat) {
        this.bounds = new double[]{minLon, minLat, maxLon, maxLat};
    }

    public double[] getBounds() { return bounds; }

//    public void sanitize() {
//        List<OsmElement> allElements = new ArrayList<>(this.nodes.parallelStream().filter(OsmElement::shouldBeDrawn).toList());
//        this.nodes.clear();
//        allElements.addAll(this.ways.parallelStream().filter(ParserOsmElement::shouldBeDrawn).toList());
//        this.ways.clear();
//        allElements.addAll(this.relations.parallelStream().filter(ParserOsmElement::shouldBeDrawn).toList());
//        this.relations.clear();
//
//        this.elementsToBeDrawn = allElements.parallelStream().sorted(Comparator.comparing(ParserOsmElement::getArea).reversed()).toList();
//    }
}

package dk.itu.data.datastructure.rtree;

import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.db.OsmElement;
import dk.itu.data.models.db.OsmNode;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RTree {
    private static final int MAX_ENTRIES = 10; // TODO: Test most efficient max per node before split
    private RTreeNode root;

    public List<OsmElement> search(double minLon, double minLat, double maxLon, double maxLat) {
        if (root == null) {
            return List.of();
        }

        return new ArrayList<>(searchRecursive(root, new BoundingBox(minLon, minLat, maxLon, maxLat))
                .parallelStream()
                .map(RTreeNode::getElements)
                .flatMap(List::stream)
                .sorted(Comparator.comparingDouble(OsmElement::getArea).reversed())
                .toList());
    }

    public void insert(OsmElement element) {
        if (root == null) {
            root = new RTreeNode();
        }

        RTreeNode leaf = chooseLeaf(root, element.getBoundingBox());
        leaf.addEntry(element);

        if (leaf.elements.size() > MAX_ENTRIES) {
            splitLeaf(leaf);
//          adjustTree(leaf, newLeaf);
        }
    }

    public OsmNode nn2(double lon, double lat) {
        var point = new Point2D.Double(lon, lat);
        if (root == null || root.mbr == null || !root.mbr.contains(point)) {
            return null;
        }
        return getNearestElement(root, point); // TODO: Do second search through neighbouring bounding boxes when R* is implemented
    }

    private OsmNode getNearestElement(RTreeNode node, Point2D.Double point) {
        if (node.isLeaf()) {
            return  (OsmNode) node.elements.parallelStream().sorted(Comparator.comparing(e -> e.getBoundingBox().distanceTo(point))).toList().getFirst();
        } else {
            var bbContaining = node.children.parallelStream().filter(c -> c.mbr.contains(point)).toList();
            if (bbContaining.isEmpty()) {
                var closest = node.children.parallelStream().sorted(Comparator.comparingDouble(e -> e.mbr.distanceTo(point))).toList().getFirst();
                return getNearestElement(closest, point);
            } else {
                return getNearestElement(bbContaining.getFirst(), point);
            }
        }
    }

    private RTreeNode chooseLeaf(RTreeNode node, BoundingBox bbox) {
        if (node.isLeaf()) return node;

        RTreeNode bestChild = null;
        double minExpansion = Double.MAX_VALUE;

        for (RTreeNode child : node.children) {
            double originalArea = child.mbr.area();
            BoundingBox expandedMBR = new BoundingBox(
                    Math.min(child.mbr.getMinLon(), bbox.getMinLon()),
                    Math.min(child.mbr.getMinLat(), bbox.getMinLat()),
                    Math.max(child.mbr.getMaxLon(), bbox.getMaxLon()),
                    Math.max(child.mbr.getMaxLat(), bbox.getMaxLat())
            );
            double expansion = expandedMBR.area() - originalArea;

            if (expansion < minExpansion) {
                minExpansion = expansion;
                bestChild = child;
            }
        }

        assert bestChild != null;
        node.mbr.expand(bbox);
        return chooseLeaf(bestChild, bbox);
    }

    private void splitLeaf(RTreeNode leaf) {
        List<OsmElement> elements = new ArrayList<>(leaf.elements);
        leaf.elements.clear();

        OsmElement entryA = elements.removeFirst(); // TODO: Choose the two elements furthest from each other
        OsmElement entryB = elements.removeLast();

        RTreeNode groupA = new RTreeNode();
        RTreeNode groupB = new RTreeNode();

        groupA.addEntry(entryA);
        groupB.addEntry(entryB);

        for (OsmElement element : elements) {
            BoundingBox bbox = element.getBoundingBox();
            double expansionA = groupA.mbr.getExpanded(bbox).area() - groupA.mbr.area();
            double expansionB = groupB.mbr.getExpanded(bbox).area() - groupB.mbr.area();

            if (expansionA < expansionB) {
                groupA.addEntry(element);
            } else {
                groupB.addEntry(element);
            }
        }

        leaf.addChild(groupA);
        leaf.addChild(groupB);
    }

//    private BoundingBox getBoundingBox(OsmElement element) {
//        if (element instanceof OsmNode node) {
//            return new BoundingBox(node.getLongitude(), node.getLatitude(), node.getLongitude(), node.getLatitude());
//        } else if (element instanceof OsmWay way) {
//            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
//            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
//
//            for (OsmNode node : way.getNodes()) {
//                minX = Math.min(minX, node.getLongitude());
//                minY = Math.min(minY, node.getLatitude());
//                maxX = Math.max(maxX, node.getLongitude());
//                maxY = Math.max(maxY, node.getLatitude());
//            }
//
//            return new BoundingBox(minX, minY, maxX, maxY);
//        } else if (element instanceof OsmRelation relation) {
//            BoundingBox bbox = null;
//            for (OsmWay way : relation.getWayMembers()) {
//                BoundingBox wayBbox = getBoundingBox(way);
//                if (bbox == null) {
//                    bbox = new BoundingBox(wayBbox.getMinX(), wayBbox.getMinY(), wayBbox.getMaxX(), wayBbox.getMaxY());
//                } else {
//                    bbox.expand(wayBbox);
//                }
//            }
//            return bbox;
//        }
//
//        throw new IllegalArgumentException("Unsupported element type");
//    }

    private RTreeNode findParent(RTreeNode current, RTreeNode child) {
        // If the current node contains the child node, return current as the parent
        for (RTreeNode childNode : current.children) {
            if (childNode == child) {
                return current;
            }
        }

        // Search for the parent
        for (RTreeNode childNode : current.children) {
            RTreeNode parent = findParent(childNode, child);
            if (parent != null) {
                return parent;
            }
        }

        // No parent found
        return null;
    }

    private RTreeNode splitInternal(RTreeNode node) {
        // Choose the axis to split
        int axis = chooseSplitAxis(node);

        // Sort the entries along that axis
        List<RTreeNode> sortedEntries = sortEntries(node, axis);

        // Split the entries into two groups
        int splitIndex = sortedEntries.size() / 2; // Simple split at the midpoint
        List<RTreeNode> groupA = sortedEntries.subList(0, splitIndex);
        List<RTreeNode> groupB = sortedEntries.subList(splitIndex, sortedEntries.size());

        // Create new child nodes for each group
        RTreeNode newNodeA = new RTreeNode();
        RTreeNode newNodeB = new RTreeNode();

        for (RTreeNode child : groupA) {
            newNodeA.addChild(child);
        }
        for (RTreeNode child : groupB) {
            newNodeB.addChild(child);
        }

        // Update bounding boxes for the new nodes
        newNodeA.updateBoundingBox();
        newNodeB.updateBoundingBox();

        // Create a new parent node if necessary
        RTreeNode newParent = new RTreeNode();
        newParent.addChild(newNodeA);
        newParent.addChild(newNodeB);

        // Return the new parent
        return newParent;
    }

    private int chooseSplitAxis(RTreeNode node) {
        double width = node.mbr.getMaxLon() - node.mbr.getMinLon();
        double height = node.mbr.getMaxLat() - node.mbr.getMinLat();

        return width > height ? 0 : 1; // 0 for X-axis, 1 for Y-axis
    }

    private List<RTreeNode> sortEntries(RTreeNode node, int axis) {
        List<RTreeNode> sortedEntries = new ArrayList<>(node.children);

        sortedEntries.sort((a, b) -> {
            double aCoord = axis == 0 ? a.mbr.getMinLon() : a.mbr.getMinLat();
            double bCoord = axis == 0 ? b.mbr.getMinLon() : b.mbr.getMinLat();
            return Double.compare(aCoord, bCoord);
        });

        return sortedEntries;
    }

    private void adjustTree(RTreeNode node, RTreeNode newNode) {
        if (node == root) {
            // If we split the root, we need a new root
            RTreeNode newRoot = new RTreeNode();
            newRoot.addChild(node);
            newRoot.addChild(newNode);
            root = newRoot;
            return;
        }

        // Find the parent of the current node
        RTreeNode parent = findParent(root, node);

        if (parent == null) {
            throw new IllegalStateException("Parent node not found for adjustment!");
        }

        // Add the new node as a sibling
        parent.addChild(newNode);

        // If parent overflows, split it recursively
        if (parent.children.size() > MAX_ENTRIES) {
            RTreeNode splitParent = splitInternal(parent);
            adjustTree(parent, splitParent);
        }
    }

    private List<RTreeNode> searchRecursive(RTreeNode node, BoundingBox queryBox) {
        List<RTreeNode> results = new ArrayList<>();

        if (!node.mbr.intersects(queryBox)) {
            return results; // No intersection, skip this branch
        }

        if (node.isLeaf()) { // If it's a leaf, add matching elements
            for (OsmElement element : node.elements) {
                if (!(element instanceof OsmNode) && element.getBoundingBox().intersects(queryBox)) {
                    results.add(node);
                }
            }
        } else {
            for (RTreeNode child : node.children) {   // Recurse into children
                results.addAll(searchRecursive(child, queryBox));
            }
        }

        return results;
    }

    public List<OsmNode> getNodes() {
        List<OsmNode> nodes = new ArrayList<>();
        getNodes(root, nodes);
        return nodes;
    }
    private void getNodes(RTreeNode node, List<OsmNode> nodes) {
        nodes.addAll(node.elements.parallelStream().filter(e -> e instanceof OsmNode).map(e -> (OsmNode) e).toList());
        for (RTreeNode child : node.children) {
            getNodes(child, nodes);
        }
    }

    public BoundingBox getBoundingBox() {
        return root == null ? null : root.mbr;
    }
}

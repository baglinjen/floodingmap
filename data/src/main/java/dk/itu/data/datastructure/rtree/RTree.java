package dk.itu.data.datastructure.rtree;

import dk.itu.common.models.OsmElement;
import dk.itu.data.models.osm.BoundingBox;
import dk.itu.data.models.osm.OsmNode;
import dk.itu.data.models.osm.OsmRelation;
import dk.itu.data.models.osm.OsmWay;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RTree {
    private static final int MAX_ENTRIES = 10; // TODO: Test most efficient max per node before split
    private RTreeNode root;

    public RTree() {
        this.root = new RTreeNode();
    }

    private RTreeNode chooseLeaf(RTreeNode node, BoundingBox bbox) {
        if (node.isLeaf()) return node;

        RTreeNode bestChild = null;
        double minExpansion = Double.MAX_VALUE;

        for (RTreeNode child : node.children) {
            double originalArea = child.mbr.area();
            BoundingBox expandedMBR = new BoundingBox(
                    Math.min(child.mbr.getMinX(), bbox.getMinX()),
                    Math.min(child.mbr.getMinY(), bbox.getMinY()),
                    Math.max(child.mbr.getMaxX(), bbox.getMaxX()),
                    Math.max(child.mbr.getMaxY(), bbox.getMaxY())
            );
            double expansion = expandedMBR.area() - originalArea;

            if (expansion < minExpansion) {
                minExpansion = expansion;
                bestChild = child;
            }
        }

        assert bestChild != null;
        return chooseLeaf(bestChild, bbox);
    }

    public void insert(OsmElement element, BoundingBox bbox) {
        RTreeNode leaf = chooseLeaf(root, bbox);
        leaf.addEntry(element, bbox);

        if (leaf.elements.size() > MAX_ENTRIES) {
            RTreeNode newLeaf = splitLeaf(leaf);
            adjustTree(leaf, newLeaf);
        }
    }

    private RTreeNode splitLeaf(RTreeNode leaf) {
        List<OsmElement> elements = new ArrayList<>(leaf.elements);
        leaf.elements.clear();

        OsmElement entryA = elements.removeFirst();
        OsmElement entryB = elements.removeLast();

        RTreeNode groupA = new RTreeNode();
        RTreeNode groupB = new RTreeNode();

        groupA.addEntry(entryA, getBoundingBox(entryA));
        groupB.addEntry(entryB, getBoundingBox(entryB));

        for (OsmElement element : elements) {
            BoundingBox bbox = getBoundingBox(element);
            double expansionA = groupA.mbr.getExpanded(bbox).area() - groupA.mbr.area();
            double expansionB = groupB.mbr.getExpanded(bbox).area() - groupB.mbr.area();

            if (expansionA < expansionB) {
                groupA.addEntry(element, bbox);
            } else {
                groupB.addEntry(element, bbox);
            }
        }

        return groupB;
    }

    public OsmElement nearestNeighbor(RTreeNode node, Point query, OsmElement bestSoFar, double bestDistance) {
        if (node.isLeaf()) {
            for (OsmElement element : node.elements) {
                BoundingBox bbox = getBoundingBox(element);
                double distance = bbox.distanceTo(query);
                if (distance < bestDistance) {
                    bestSoFar = element;
                    bestDistance = distance;
                }
            }
        } else {
            node.children.sort(Comparator.comparingDouble(e -> e.mbr.distanceTo(query)));
            for (RTreeNode child : node.children) {
                double distance = child.mbr.distanceTo(query);
                if (distance < bestDistance) {
                    bestSoFar = nearestNeighbor(child, query, bestSoFar, bestDistance);
                }
            }
        }
        return bestSoFar;
    }

    private BoundingBox getBoundingBox(Object element) {
        if (element instanceof OsmNode node) {
            return new BoundingBox(node.getLongitude(), node.getLatitude(), node.getLongitude(), node.getLatitude());
        } else if (element instanceof OsmWay way) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

            for (OsmNode node : way.getNodes()) {
                minX = Math.min(minX, node.getLongitude());
                minY = Math.min(minY, node.getLatitude());
                maxX = Math.max(maxX, node.getLongitude());
                maxY = Math.max(maxY, node.getLatitude());
            }

            return new BoundingBox(minX, minY, maxX, maxY);
        } else if (element instanceof OsmRelation relation) {
            BoundingBox bbox = null;
            for (OsmWay way : relation.getWays()) {
                BoundingBox wayBbox = getBoundingBox(way);
                if (bbox == null) {
                    bbox = new BoundingBox(wayBbox.getMinX(), wayBbox.getMinY(), wayBbox.getMaxX(), wayBbox.getMaxY());
                } else {
                    bbox.expand(wayBbox);
                }
            }
            return bbox;
        }

        throw new IllegalArgumentException("Unsupported element type");
    }

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
        double width = node.mbr.getMaxX() - node.mbr.getMinX();
        double height = node.mbr.getMaxY() - node.mbr.getMinY();

        return width > height ? 0 : 1; // 0 for X-axis, 1 for Y-axis
    }

    private List<RTreeNode> sortEntries(RTreeNode node, int axis) {
        List<RTreeNode> sortedEntries = new ArrayList<>(node.children);

        sortedEntries.sort((a, b) -> {
            double aCoord = axis == 0 ? a.mbr.getMinX() : a.mbr.getMinY();
            double bCoord = axis == 0 ? b.mbr.getMinX() : b.mbr.getMinY();
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

}

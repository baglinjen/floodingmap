package dk.itu.data.datastructure.rtree;

import dk.itu.common.models.OsmElement;
import dk.itu.data.models.memory.*;

import java.awt.*;
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
                .sorted(Comparator.comparingDouble(OsmElementMemory::getArea).reversed())
                .toList());
    }

    public void insert(OsmElementMemory element) {
        if (root == null) {
            root = new RTreeNode();
        }

        RTreeNode leaf = chooseLeaf(root, element.getBoundingBox());
        leaf.addEntry(element);
        System.out.println("Amount of children: " + leaf.getElements().size());

        if (leaf.isLeaf()) {
            if (leaf.elements.size() >= MAX_ENTRIES) {
                RTreeNode newLeaf = splitLeaf(leaf);
                adjustTree(leaf, newLeaf);
            }
        } else {
            forcedReinsert(leaf);
            if (leaf.elements.size() >= MAX_ENTRIES) {
                RTreeNode newInternal = splitInternal(leaf);
                adjustTree(leaf, newInternal);
            }
        }
    }

    public OsmElementMemory nearestNeighbor(RTreeNode node, Point query, OsmElementMemory bestSoFar, double bestDistance) {
        if (node.isLeaf()) {
            for (OsmElementMemory element : node.elements) {
                BoundingBox bbox = element.getBoundingBox();
                double distance = bbox.distanceFromPoint(query);
                if (distance < bestDistance) {
                    bestSoFar = element;
                    bestDistance = distance;
                }
            }
        } else {
            node.children.sort(Comparator.comparingDouble(e -> e.mbr.distanceFromPoint(query)));
            for (RTreeNode child : node.children) {
                double distance = child.mbr.distanceFromPoint(query);
                if (distance < bestDistance) {
                    bestSoFar = nearestNeighbor(child, query, bestSoFar, bestDistance);
                }
            }
        }
        return bestSoFar;
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

    private OsmElementMemory[] pickSeeds(List<OsmElementMemory> elements) {
        OsmElementMemory seedA = null, seedB = null;
        double maxDistance = -1;

        for (int i = 0; i < elements.size(); i++) {
            for (int j = i + 1; j < elements.size(); j++) {
                double distance = elements.get(i).getBoundingBox().distanceToBoundingBox(elements.get(j).getBoundingBox());
                if (distance > maxDistance) {
                    maxDistance = distance;
                    seedA = elements.get(i);
                    seedB = elements.get(j);
                }
            }
        }
        return new OsmElementMemory[]{seedA, seedB};
    }

//    private RTreeNode chooseSubtree(RTreeNode parent, OsmElementMemory newEntry) {
//        RTreeNode bestNode = null;
//        double minOverlapIncrease = Double.MAX_VALUE;
//        double minAreaIncrease = Double.MAX_VALUE;
//
//        for (RTreeNode child : parent.children) {
//            double originalOverlap = computeOverlap(parent.children);
//            double newOverlap = computeOverlapAfterInsertion(parent.children, newEntry, child);
//
//            double overlapIncrease = newOverlap - originalOverlap;
//            double areaIncrease = child.mbr.getExpanded(newEntry.getBoundingBox()).area() - child.mbr.area();
//
//            if (overlapIncrease < minOverlapIncrease ||
//                    (overlapIncrease == minOverlapIncrease && areaIncrease < minAreaIncrease)) {
//                bestNode = child;
//                minOverlapIncrease = overlapIncrease;
//                minAreaIncrease = areaIncrease;
//            }
//        }
//        return bestNode;
//    }

//    private double computeOverlap(List<RTreeNode> children) {
//        double overlap = 0;
//        for (int i = 0; i < children.size(); i++) {
//            for (int j = i + 1; j < children.size(); j++) {
//                overlap += children.get(i).mbr.intersectionArea(children.get(j).mbr);
//            }
//        }
//        return overlap;
//    }
//
//    private double computeOverlapAfterInsertion(List<RTreeNode> children, OsmElementMemory newEntry, RTreeNode targetChild) {
//        BoundingBox newMBB = targetChild.mbr.getExpanded(newEntry.getBoundingBox());
//
//        double overlap = 0;
//        for (RTreeNode child : children) {
//            if (child != targetChild) {
//                overlap += newMBB.intersectionArea(child.mbr);
//            }
//        }
//        return overlap;
//    }

    private double computeOverlapIncrease(RTreeNode node, BoundingBox newBBox) {
        BoundingBox expanded = node.mbr.getExpanded(newBBox);
        return expanded.area() - node.mbr.area();
    }

    private void forcedReinsert(RTreeNode node) {
        if (node.elements.size() <= MAX_ENTRIES) return;

        // Sort elements by distance from node's center
        BoundingBox nodeCenter = node.mbr;
        node.elements.sort(Comparator.comparingDouble(
                e -> e.getBoundingBox().distanceToBoundingBox(nodeCenter))
        );

        // Remove a fraction (e.g., 30%) for reinsertion
        int reinsertCount = (int) (node.elements.size() * 0.3);
        List<OsmElementMemory> toReinsert = new ArrayList<>(
                node.elements.subList(node.elements.size() - reinsertCount, node.elements.size())
        );

        // Remove from the node
        node.elements.removeAll(toReinsert);

        // Reinsert removed elements
        for (OsmElementMemory e : toReinsert) {
            insert(e);
        }
    }

    private RTreeNode splitLeaf(RTreeNode leaf) {
        List<OsmElementMemory> elements = new ArrayList<>(leaf.elements);
        leaf.elements.clear();

        // Pick two seeds that are geographically far apart
        OsmElementMemory[] seeds = pickSeeds(elements);
        OsmElementMemory entryA = seeds[0];
        OsmElementMemory entryB = seeds[1];
        elements.remove(entryA);
        elements.remove(entryB);

        // Create two new leaf nodes
        RTreeNode groupA = new RTreeNode();
        RTreeNode groupB = new RTreeNode();

        groupA.addEntry(entryA);
        groupB.addEntry(entryB);

        // Distribution of the rest
        while (!elements.isEmpty()) {
            OsmElementMemory element = elements.removeFirst();
            double overlapA = computeOverlapIncrease(groupA, element.getBoundingBox());
            double overlapB = computeOverlapIncrease(groupB, element.getBoundingBox());

            if (overlapA < overlapB) {
                groupA.addEntry(element);
            } else if (overlapB < overlapA) {
                groupB.addEntry(element);
            } else {
                // Choose the one with smaller area
                double areaA = groupA.mbr.area();
                double areaB = groupB.mbr.area();
                if (areaA < areaB) {
                    groupA.addEntry(element);
                } else {
                    groupB.addEntry(element);
                }
            }
        }

        // GroupA is copied back to the original node
        leaf.elements.addAll(groupA.elements);
        leaf.updateBoundingBox();

        return groupB; // New sibling node
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
        parent.updateBoundingBox();

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
            for (OsmElementMemory element : node.elements) {
                if (element.getBoundingBox().intersects(queryBox)) {
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
}

package dk.itu.data.datastructure.rtree;
import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.db.osm.OsmElement;
import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.models.db.osm.OsmWay;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import javafx.util.Pair;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dk.itu.data.models.db.BoundingBox.expand;
import static dk.itu.data.models.db.BoundingBox.intersectionArea;

/*
* The R* Tree is based on https://infolab.usc.edu/csci599/Fall2001/paper/rstar-tree.pdf
* Min entries and reinsert percentage are based on the report (p. 327)
* */

public class RStarTree {
    private static final int MAX_ENTRIES = 100;  // Maximum entries in a node
    private static final int MIN_ENTRIES = MAX_ENTRIES / 2;  // Minimum entries (40-50% of max is typical)
    private static final double REINSERT_PERCENTAGE = 0.3;  // Percentage of entries to reinsert (30% is typical)
    private static final int MAX_CHILDREN = 10;
    private static final int MIN_CHILDREN = MAX_CHILDREN / 2;
    private static final int REINSERT_LEVELS = 5;  // Max levels for forced reinsert to prevent recursion issues

    // Track levels for forced reinsert to prevent excessive reinsertion
    private final Set<Integer> reinsertLevels = new HashSet<>();

    private RTreeNode root;
    private boolean isEmpty = true;

    /**
     * Helper class for nearest neighbor queue entries
     */
    private static class NNEntry implements Comparable<NNEntry> {
        RTreeNode node;
        double distance;

        public NNEntry(RTreeNode node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NNEntry other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    /**
     * Calculate minimum possible distance from point to bounding box
     * @param px Point x coordinate (longitude)
     * @param py Point y coordinate (latitude)
     * @param box The bounding box
     * @return The minimum possible Euclidean distance
     */
    private double minDist(double px, double py, BoundingBox box) { //TODO: Make method in BoundingBox class
        double dx = 0.0;
        double dy = 0.0;

        // Distance in x-dimension
        if (px < box.getMinLon()) {
            dx = box.getMinLon() - px;
        } else if (px > box.getMaxLon()) {
            dx = px - box.getMaxLon();
        }

        // Distance in y-dimension
        if (py < box.getMinLat()) {
            dy = box.getMinLat() - py;
        } else if (py > box.getMaxLat()) {
            dy = py - box.getMaxLat();
        }

        // Euclidean distance
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculate the exact distance between two points
     * @param x1 First point x coordinate
     * @param y1 First point y coordinate
     * @param x2 Second point x coordinate
     * @param y2 Second point y coordinate
     * @return The Euclidean distance between the points
     */
    private double pointDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        // Euclidean distance
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Get all elements in the R-tree. Used for routing
     * @return List of all OsmNode elements in the tree
     */
    public List<OsmNode> getNodes() {
        // TODO: Use concurrent queue
        List<OsmNode> result = new ArrayList<>();
        if (root != null) {
            collectNodes(root, result);
        }
        return result;
    }

    /**
     * Helper method to collect all OsmNode elements from the tree
     * @param node Current node to process
     * @param result List to collect nodes into
     */
    private void collectNodes(RTreeNode node, List<OsmNode> result) {
        if (node.isLeaf()) {
            for (OsmElement element : node.elements) {
                if (element instanceof OsmNode) {
                    result.add((OsmNode) element);
                }
            }
        } else {
            for (RTreeNode child : node.getChildren()) {
                collectNodes(child, result);
            }
        }
    }

    /**
     * Find the leaf node where the element should be inserted
     * @param node current node
     * @param elementBox the node is within
     * @return leaf node
     */
    private RTreeNode chooseLeaf(RTreeNode node, BoundingBox elementBox) {
        if (node.isLeaf()) {
            return node;
        }

        RTreeNode bestChild = null;
        double minEnlargement = Double.POSITIVE_INFINITY;
        double minArea = Double.POSITIVE_INFINITY;

        for (RTreeNode child : node.getChildren()) {
            // Calculate how much the child's MBR would need to be enlarged
            double enlargement = child.getEnlargementArea(elementBox) - child.getArea();
            double area = child.getArea();

            // Choose the child that requires the least enlargement
            if (enlargement < minEnlargement ||
                    (Math.abs(enlargement - minEnlargement) < 1e-10 && area < minArea)) {
                minEnlargement = enlargement;
                minArea = area;
                bestChild = child;
            }
        }

        assert bestChild != null;
        return chooseLeaf(bestChild, elementBox);
    }

    /**
     * Split a leaf node according to R*-tree algorithm
     */
    private RTreeNode splitLeaf(RTreeNode leaf) {
        // Calculate the level of this node to track reinsertions
        int level = calculateLevel(leaf);

        // Check if we should do a forced reinsert instead of splitting
        if (!reinsertLevels.contains(level)) {
            boolean didReinsert = forcedReinsert(leaf, level);
            if (didReinsert && leaf.elements.size() < MAX_ENTRIES) {
                return null; // No split needed after reinsert
            }
        }

        // If we still need to split after reinsertion or reinsert isn't applicable
        return splitNodeR(leaf);
    }

    /**
     * Split an internal node according to R*-tree algorithm
     */
    private RTreeNode splitInternal(RTreeNode node) {
        return splitNodeR(node);
    }

    /**
     * Choose the split axis that minimizes the sum of perimeters for internal nodes
     */
    private int chooseSplitAxisForInternal(List<RTreeNode> children) {
        double minPerimeterSum = Double.POSITIVE_INFINITY;
        int bestAxis = 0;

        // Check both X and Y axes
        for (int axis = 0; axis < 2; axis++) {
            // Sort children by their center along this axis
            sortChildrenByAxis(children, axis);

            // Compute S, the sum of all perimeter-values of the different distributions
            double perimeterSum = computeDistributionPerimeterSumForInternal(children);

            if (perimeterSum < minPerimeterSum) {
                minPerimeterSum = perimeterSum;
                bestAxis = axis;
            }
        }

        return bestAxis;
    }

    /**
     * Choose the distribution with minimum overlap for internal nodes
     */
    private int[] chooseSplitIndexForInternal(List<RTreeNode> children, int axis) {
        // Sort by the chosen axis
        sortChildrenByAxis(children, axis);

        double minOverlap = Double.POSITIVE_INFINITY;
        double minArea = Double.POSITIVE_INFINITY;
        int splitIndex = MIN_ENTRIES;

        // Try distributions and pick the one with minimum overlap
        for (int i = MIN_CHILDREN; i <= children.size() - MIN_CHILDREN; i++) {
            // Create two groups
            double[] mbr1 = computeMBRForChildren(children.subList(0, i));
            double[] mbr2 = computeMBRForChildren(children.subList(i, children.size()));

            // Calculate overlap
            double overlap = intersectionArea(mbr1, mbr2);
            double area = mbr1[4] + mbr2[4];

            // Choose distribution with minimum overlap, breaking ties with minimum area
            if (overlap < minOverlap || (Math.abs(overlap - minOverlap) < 1e-10 && area < minArea)) {
                minOverlap = overlap;
                minArea = area;
                splitIndex = i;
            }
        }

        return new int[]{splitIndex, children.size() - splitIndex};
    }

    /**
     * Sort children nodes by their center along the specified axis
     */
    private void sortChildrenByAxis(List<RTreeNode> children, int axis) {
        children.sort((c1, c2) -> {
            double center1 = (axis == 0) ?
                    (c1.getMinLon() + c1.getMaxLon()) / 2 :
                    (c1.getMinLat() + c1.getMaxLat()) / 2;

            double center2 = (axis == 0) ?
                    (c2.getMinLon() + c2.getMaxLon()) / 2 :
                    (c2.getMinLat() + c2.getMaxLat()) / 2;

            return Double.compare(center1, center2);
        });
    }

    /**
     * Compute the sum of perimeters for all possible distributions of internal nodes
     */
    private double computeDistributionPerimeterSumForInternal(List<RTreeNode> children) {
        double sum = 0;

        for (int i = MIN_ENTRIES; i <= children.size() - MIN_ENTRIES; i++) {
            double[] mbr1 = computeMBRForChildren(children.subList(0, i));
            double[] mbr2 = computeMBRForChildren(children.subList(i, children.size()));

            // Sum the perimeters (perimeter = 2 * (width + height))
            sum += 2 * ((mbr1[2] - mbr1[0]) + (mbr1[3] - mbr1[1]));
            sum += 2 * ((mbr2[2] - mbr2[0]) + (mbr2[3] - mbr2[1]));
        }

        return sum;
    }

    /**
     * Choose the split axis that minimizes the sum of perimeters for leaf nodes
     */
    private int chooseSplitAxisForLeaf(List<OsmElement> elements) {
        double minPerimeterSum = Double.POSITIVE_INFINITY;
        int bestAxis = 0;

        // Check both X and Y axes
        for (int axis = 0; axis < 2; axis++) {
            // Sort elements by their center along this axis
            sortElementsByAxis(elements, axis);

            // Compute S, the sum of all perimeter-values of the different distributions
            double perimeterSum = computeDistributionPerimeterSumForLeaf(elements);

            if (perimeterSum < minPerimeterSum) {
                minPerimeterSum = perimeterSum;
                bestAxis = axis;
            }
        }

        return bestAxis;
    }

    /**
     * Choose the distribution with minimum overlap for leaf nodes
     */
    private int[] chooseSplitIndexForLeaf(List<OsmElement> elements, int axis) {
        // Sort by the chosen axis
        sortElementsByAxis(elements, axis);

        double minOverlap = Double.POSITIVE_INFINITY;
        double minArea = Double.POSITIVE_INFINITY;
        int splitIndex = MIN_ENTRIES;

        // Try distributions and pick the one with minimum overlap
        for (int i = MIN_ENTRIES; i <= elements.size() - MIN_ENTRIES; i++) {
            // Create two groups
            double[] mbr1 = computeMBRForElements(elements.subList(0, i));
            double[] mbr2 = computeMBRForElements(elements.subList(i, elements.size()));

            // Calculate overlap
            double overlap = intersectionArea(mbr1, mbr2);
            double area = mbr1[4] + mbr2[4];

            // Choose distribution with minimum overlap, breaking ties with minimum area
            if (overlap < minOverlap || (Math.abs(overlap - minOverlap) < 1e-10 && area < minArea)) {
                minOverlap = overlap;
                minArea = area;
                splitIndex = i;
            }
        }

        return new int[]{splitIndex, elements.size() - splitIndex};
    }

    /**
     * Sort elements by their center along the specified axis
     */
    private void sortElementsByAxis(List<OsmElement> elements, int axis) {
        elements.sort(Comparator.comparingDouble(e -> (axis == 0) ?
                // Center 1
                (e.getMinLon() + e.getMaxLon()) / 2 :
                // Center 2
                (e.getMinLat() + e.getMaxLat()) / 2));
    }

    /**
     * Compute the sum of perimeters for all possible distributions of leaf elements
     */
    private double computeDistributionPerimeterSumForLeaf(List<OsmElement> elements) {
        double sum = 0;

        for (int i = MIN_ENTRIES; i <= elements.size() - MIN_ENTRIES; i++) {
            double[] mbr1 = computeMBRForElements(elements.subList(0, i));
            double[] mbr2 = computeMBRForElements(elements.subList(i, elements.size()));

            // Sum the perimeters
            sum += 2 * ((mbr1[2] - mbr1[0]) + (mbr1[3] - mbr1[1]));
            sum += 2 * ((mbr2[2] - mbr2[0]) + (mbr2[3] - mbr2[1]));
        }

        return sum;
    }

    /**
     * Compute the MBR for a list of child nodes
     */
    private double[] computeMBRForChildren(List<RTreeNode> children) {
        if (children.isEmpty()) {
            return new double[]{0, 0, 0, 0};
        }

        double[] mbr = children.getFirst().getBoundingBoxWithArea();

        for (int i = 1; i < children.size(); i++) {
            expand(mbr, children.get(i));
        }

        return mbr;
    }

    /**
     * Compute the MBR for a list of elements
     */
    private double[] computeMBRForElements(List<OsmElement> elements) {
        if (elements.isEmpty()) {
            return new double[]{0, 0, 0, 0};
        }

        double[] mbr = elements.getFirst().getBoundingBoxWithArea();

        for (int i = 1; i < elements.size(); i++) {
            expand(mbr, elements.get(i).getBoundingBoxWithArea());
        }

        return mbr;
    }

    /**
     * Split RTree nodes method
     */
    private RTreeNode splitNodeR(RTreeNode node) {
        RTreeNode newNode = new RTreeNode();

        if (node.isLeaf()) {
            // Handle leaf node split
            List<OsmElement> elements = new ArrayList<>(node.elements);

            // Choose split axis and distribution
            int bestAxis = chooseSplitAxisForLeaf(elements);
            int[] distribution = chooseSplitIndexForLeaf(elements, bestAxis);

            // Sort elements by the chosen axis
            sortElementsByAxis(elements, bestAxis);

            // Distribute elements
            List<OsmElement> group1 = new ArrayList<>(
                    elements.subList(0, distribution[0]));
            List<OsmElement> group2 = new ArrayList<>(
                    elements.subList(distribution[0], elements.size()));

            // Update nodes
            node.elements = group1;
            newNode.elements = group2;
        } else {
            // Handle internal node split
            List<RTreeNode> children = new ArrayList<>(node.getChildren());

            // Choose split axis and distribution
            int bestAxis = chooseSplitAxisForInternal(children);
            int[] distribution = chooseSplitIndexForInternal(children, bestAxis);

            // Sort children by the chosen axis
            sortChildrenByAxis(children, bestAxis);

            // Distribute children
            List<RTreeNode> group1 = new ArrayList<>(
                    children.subList(0, distribution[0]));
            List<RTreeNode> group2 = new ArrayList<>(
                    children.subList(distribution[0], children.size()));

            // Update nodes
            node.setChildren(group1);
            newNode.setChildren(group2);
        }

        // Update MBRs
        node.updateBoundingBox();
        newNode.updateBoundingBox();

        forceCreateMBR(newNode);

        return newNode;
    }

//    /**
//     * Helper method to force MBR creation in case a node's MBR is null after node split
//     **/
    private void forceCreateMBR(RTreeNode node) {
        if (node.isLeaf() && !node.elements.isEmpty()) {
            // Get first element's bounding box
            node.setBoundingBox(node.elements.getFirst());

            // Expand for remaining elements
            for (int i = 1; i < node.elements.size(); i++) {
                node.expand(node.elements.get(i));
            }
        } else if (!node.isLeaf() && !node.getChildren().isEmpty()) {
            // Get first element's bounding box
            node.setBoundingBox(node.getChildren().getFirst());

            // Expand for remaining children
            for (int i = 1; i < node.getChildren().size(); i++) {
                if (node.getChildren().get(i) != null) {
                    node.expand(node.getChildren().get(i));
                }
            }
        }
    }

    /**
     * Forced reinsert procedure for R*-tree
     */
    private boolean forcedReinsert(RTreeNode node, int level) {
        // Check if reinsert is needed
        boolean isFull = node.isLeaf() ?
                node.elements.size() > MAX_ENTRIES :
                node.getChildren().size() > MAX_CHILDREN;

        if (!isFull) {
            return false;
        }

        // Mark this level as having done a reinsert
        reinsertLevels.add(level);

        // Number of entries to reinsert
        int p = (int) Math.ceil(REINSERT_PERCENTAGE * MAX_ENTRIES);
        Pair<Double, Double> center = getCenter(node);

        if (node.isLeaf()) {
            // Handle leaf node - work directly with elements list
            List<OsmElement> elements = new ArrayList<>(node.elements);

            // Sort by distance from center
            elements.sort((e1, e2) -> {
                double dist1 = getDistance(e1, center);
                double dist2 = getDistance(e2, center);
                return Double.compare(dist2, dist1); // Descending order
            });

            // Select entries to reinsert (farthest p entries)
            int reinsertCount = Math.min(p, elements.size());
            List<OsmElement> entriesToReinsert =
                    new ArrayList<>(elements.subList(0, reinsertCount));

            // Keep the rest
            node.elements = new ArrayList<>(elements.subList(reinsertCount, elements.size()));
            node.updateBoundingBox();

            // Reinsert entries
            for (OsmElement element : entriesToReinsert) {
                insert(element);
            }
        } else {
            // Handle internal node - work directly with children list
            List<RTreeNode> children = new ArrayList<>(node.getChildren());

            // Sort by distance from center
            children.sort((c1, c2) -> {
                double dist1 = getDistance(c1, center);
                double dist2 = getDistance(c2, center);
                return Double.compare(dist2, dist1); // Descending order
            });

            // Select entries to reinsert (farthest p entries)
            int reinsertCount = Math.min(p, children.size());
            List<RTreeNode> childrenToReinsert =
                    new ArrayList<>(children.subList(0, reinsertCount));

            // Keep the rest
            node.setChildren(new ArrayList<>(children.subList(reinsertCount, children.size())));
            node.updateBoundingBox();

            // Reinsert children
            for (RTreeNode child : childrenToReinsert) {
                insertInternal(child, level);
            }
        }

        return true;
    }

    /**
     * Insert internal node after forced reinsert
     */
    private void insertInternal(RTreeNode nodeToInsert, int level) {
        // Start from root and traverse down to the appropriate level
        RTreeNode targetNode = findTargetNodeAtLevel(root, nodeToInsert, level - 1);

        // Add child to the target node (should not be a leaf)
        if (targetNode.isLeaf()) {
            throw new IllegalStateException("Target node at level " + (level - 1) + " should not be a leaf");
        }

        targetNode.addChild(nodeToInsert);

        // Find parent for adjustment
        // Check if we need to split
        RTreeNode newNode = null;
        if (targetNode.getChildren().size() > MAX_CHILDREN) {
            newNode = splitInternal(targetNode);
        }

        // Adjust tree upward
        adjustTree(targetNode, newNode, targetNode.getParent());
    }

    /**
     * Find a node at the specified level for insertion
     */
    private RTreeNode findTargetNodeAtLevel(RTreeNode node, BoundingBox mbr, int targetLevel) {
        if (targetLevel == 0 || node.isLeaf()) {
            return node;
        }

        // Choose best child based on minimum enlargement
        RTreeNode bestChild = null;
        double minEnlargement = Double.POSITIVE_INFINITY;

        for (RTreeNode child : node.getChildren()) {
            double enlargement = child.getEnlargementArea(mbr) - child.getArea();
            if (bestChild == null || enlargement < minEnlargement) {
                minEnlargement = enlargement;
                bestChild = child;
            }
        }

        if (bestChild == null) {
            return node; // No children, return current node
        }

        return findTargetNodeAtLevel(bestChild, mbr, targetLevel - 1);
    }

    /**
     * Adjust the tree after an insertion or split
     * @param node The node that was modified
     * @param newNode The new node if there was a split, or null
     * @param parent The parent of the modified node
     **/
    private void adjustTree(RTreeNode node, RTreeNode newNode, RTreeNode parent) {
        // Update the MBR of the node
        node.updateBoundingBox();

        if (parent == null) {
            // We've reached the root
            if (newNode != null) {
                // Need to create a new root
                RTreeNode newRoot = new RTreeNode();
                newRoot.addChild(node);
                newRoot.addChild(newNode);
                newRoot.updateBoundingBox();
                root = newRoot;
            }
            return;
        }

        // If we have a new node after splitting, add it to the parent
        if (newNode != null) {
            parent.addChild(newNode);
        }

        // Check if parent needs splitting
        RTreeNode splitParent = null;
        if (parent.getChildren().size() > MAX_CHILDREN) {
            splitParent = splitInternal(parent);
        }

        // Continue adjusting up the tree
        adjustTree(parent, splitParent, parent.getParent());
    }

    private double getDistance(BoundingBox box, Pair<Double, Double> center) {
        Pair<Double, Double> boxCenter = getCenter(box);
        double dx = boxCenter.getKey() - center.getKey();
        double dy = boxCenter.getValue() - center.getValue();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private Pair<Double, Double> getCenter(BoundingBox box) {
        double x = (box.getMinLon() + box.getMaxLon()) / 2;
        double y = (box.getMinLat() + box.getMaxLat()) / 2;
        return new Pair<>(x, y);
    }


    private int calculateLevel(RTreeNode node) {
        return calculateLevel(root, node, 0);
    }
    private int calculateLevel(RTreeNode node, RTreeNode nodeToFind, int level) {
        if (node == nodeToFind) return level;

        return node
                .getChildren()
                .parallelStream()
                .filter(c -> c.intersects(nodeToFind))
                .map(c -> calculateLevel(c, nodeToFind, level+1))
                .max(Integer::compareTo)
                .orElse(level);
    }

    public void insert(OsmElement element) {
        // Create root if it doesn't exist
        if (root == null) {
            root = new RTreeNode();
            root.addEntry(element);
            return;
        }

        // Find the appropriate leaf node
        RTreeNode leaf = chooseLeaf(root, element);
        leaf.addEntry(element);

        // Adjust the tree (handles splits if necessary)
        RTreeNode newNode = null;
        if (leaf.elements.size() > MAX_ENTRIES) {
            newNode = splitLeaf(leaf);
        }

        // Find parent and propagate changes upward
        adjustTree(leaf, newNode, leaf.getParent());
        isEmpty = false;
    }


    public List<OsmElement> search(double minLon, double minLat, double maxLon, double maxLat) {
        Collection<OsmElement> elementsConcurrent = new ConcurrentLinkedQueue<>();
        double[] box = {minLon, minLat, maxLon, maxLat};

        searchRecursive(root, box, elementsConcurrent);

        return elementsConcurrent
                .parallelStream()
                .filter(e -> e.intersects(box))
                .sorted(Comparator.comparingDouble(OsmElement::getArea).reversed())
                .toList();
    }
    private void searchRecursive(RTreeNode node, double[] box, Collection<OsmElement> results) {
        if (node == null || !node.intersects(box)) return; // No intersection, skip this branch

        if (node.isLeaf()) {
            results.addAll(node.elements);
        } else {
            node.getChildren().parallelStream().forEach(child -> searchRecursive(child, box, results));
        }
    }

    public List<OsmElement> searchScaled(double minLon, double minLat, double maxLon, double maxLat, double minBoundingBoxArea) {
        List<OsmElement> elementsConcurrent = Collections.synchronizedList(new ReferenceArrayList<>());
        double[] box = {minLon, minLat, maxLon, maxLat};

        searchScaledRecursive(root, box, minBoundingBoxArea, elementsConcurrent);

        return elementsConcurrent
                .parallelStream()
                .filter(e -> e.intersects(box) && e.getArea() >= minBoundingBoxArea) // Slow => consider filtering on results.addAll
                .sorted(Comparator.comparing((e1) -> switch (e1) {
                    case OsmWay way -> way.isLine() ? way.getId() : -way.getArea();
                    default -> -e1.getArea();
                }))
                .toList();
    }
    private void searchScaledRecursive(RTreeNode node, double[] queryBox, double minBoundingBoxArea, Collection<OsmElement> results) {
        if (node == null || !node.intersects(queryBox)) return; // No intersection, skip this branch

        if (node.isLeaf()) {
            synchronized (results) {
                results.addAll(node.elements);
            }
        } else {
            node.getChildren()
                    .parallelStream()
                    .filter(child -> child.getArea() >= minBoundingBoxArea)
                    .forEach(child -> searchScaledRecursive(child, queryBox, minBoundingBoxArea, results));
        }
    }

    public List<BoundingBox> getBoundingBoxes() {
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        int level = 1, levelsToCheck = Integer.MAX_VALUE;
        getBoundingBoxesRecursive(root, boundingBoxes, level, levelsToCheck);
        return boundingBoxes.stream().toList();
    }
    private void getBoundingBoxesRecursive(RTreeNode node, Collection<BoundingBox> results, int level, int levelsToCheck) {
        results.add(node);
        level++;
        if (level < levelsToCheck) {
            int finalLevel = level;
            node.getChildren().forEach(child -> getBoundingBoxesRecursive(child, results, finalLevel, levelsToCheck));
        }
    }

    /*
     * The root of the R tree
     * */
    public RTreeNode getRoot() {
        return root;
    }

    /**
     * Find the nearest OsmNode to the specified coordinates
     * @param lon Longitude of the query point
     * @param lat Latitude of the query point
     * @return The nearest OsmNode or null if tree is empty
     */
    public OsmNode getNearest(double lon, double lat) {
        if (root == null) {
            return null;
        }

        // Priority queue to sort by distance
        PriorityQueue<NNEntry> queue = new PriorityQueue<>();

        // Add root node to queue with its minimum destination
        queue.add(new NNEntry(root, minDist(lon, lat, root)));

        OsmNode nearest = null;
        double nearestDist = Double.MAX_VALUE;

        // Process queue until empty, or we can guarantee we found the nearest
        while (!queue.isEmpty()) {
            NNEntry entry = queue.poll();

            // If minimum dist > nearest distance found so far, we're done
            if (entry.distance > nearestDist) {
                break;
            }

            if (entry.node.isLeaf()) {
                // Check each element in the leaf
                for (OsmElement element : entry.node.elements) {
                    // Only consider elements that are OsmNodes
                    if (element instanceof OsmNode node) {
                        double dist = pointDistance(lon, lat, node.getLon(), node.getLat());

                        if (dist < nearestDist) {
                            nearest = node;
                            nearestDist = dist;
                        }
                    }
                }
            } else {
                // Add all children to the queue
                for (RTreeNode child : entry.node.getChildren()) {
                    double childDist = minDist(lon, lat, child);

                    // Only add if it could contain a closer point
                    if (childDist < nearestDist) {
                        queue.add(new NNEntry(child, childDist));
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Clears all elements from the R-tree.
     */
    public void clear() {
        root = null;
        reinsertLevels.clear();
        isEmpty = true;
    }

    public boolean isEmpty() {
        return isEmpty;
    }
}



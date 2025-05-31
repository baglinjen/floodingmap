package dk.itu.data.datastructure.rtree;

import dk.itu.common.models.Drawable;
import dk.itu.common.models.WithBoundingBoxAndArea;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import it.unimi.dsi.fastutil.floats.Float2ReferenceMap;
import it.unimi.dsi.fastutil.objects.AbstractReferenceCollection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import static dk.itu.data.datastructure.rtree.RStartTreeUtilities.*;

/*
* The R* Tree is based on https://infolab.usc.edu/csci599/Fall2001/paper/rstar-tree.pdf
* Min entries and reinsert percentage are based on the report (p. 327)
* */

public class RStarTree {
    private static final int MAX_ENTRIES = 100;  // Maximum entries in a node
    private static final int MIN_ENTRIES = MAX_ENTRIES / 2;  // Minimum entries (40-50% of max is typical)
    private static final float REINSERT_PERCENTAGE = 0.3f;  // Percentage of entries to reinsert (30% is typical)
//    private static final int MAX_CHILDREN = 750;
    private static final int MAX_CHILDREN = 10;
    private static final int MIN_CHILDREN = MAX_CHILDREN / 2;

    // Track levels for forced reinsert to prevent excessive reinsertion
    private final Set<Integer> reinsertLevels = new HashSet<>();

    private RTreeNode root;
    private boolean isEmpty = true;

    /**
     * Helper class for nearest neighbor queue entries
     */
    private static class NNEntry implements Comparable<NNEntry> {
        RTreeNode node;
        float distance;

        public NNEntry(RTreeNode node, float distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NNEntry other) {
            return Float.compare(this.distance, other.distance);
        }
    }

    /**
     * Calculate minimum possible distance from point to bounding box
     * @param px Point x coordinate (longitude)
     * @param py Point y coordinate (latitude)
     * @param box The bounding box
     * @return The minimum possible Euclidean distance
     */
    private float minDist(float px, float py, WithBoundingBoxAndArea box) {
        float dx = 0;
        float dy = 0;

        // Distance in x-dimension
        if (px < box.minLon()) {
            dx = box.minLon() - px;
        } else if (px > box.maxLon()) {
            dx = px - box.maxLon();
        }

        // Distance in y-dimension
        if (py < box.minLat()) {
            dy = box.minLat() - py;
        } else if (py > box.maxLat()) {
            dy = py - box.maxLat();
        }

        // Euclidean distance
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Get all elements in the R-tree. Used for routing
     * @return List of all OsmNode elements in the tree
     */
    public List<OsmNode> getNodes() {
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
            for (int i = 0; i < node.elements.size(); i++) {
                var element = node.elements.get(i);
                if (element instanceof OsmNode) {
                    result.add((OsmNode) element);
                }
            }
        } else {
            for (int i = 0; i < node.getChildren().size(); i++) {
                collectNodes(node.getChildren().get(i), result);
            }
        }
    }

    /**
     * Find the leaf node where the element should be inserted
     * @param node current node
     * @param elementBox the node is within
     * @return leaf node
     */
    private RTreeNode chooseLeaf(RTreeNode node, WithBoundingBoxAndArea elementBox) {
        if (node.isLeaf()) {
            return node;
        }

        RTreeNode bestChild = null;
        float minEnlargement = Float.POSITIVE_INFINITY;
        float minArea = Float.POSITIVE_INFINITY;

        for (RTreeNode child : node.getChildren()) {
            // Calculate how much the child's MBR would need to be enlarged
            float enlargement = getEnlargementArea(elementBox, child);
            float area = child.getArea();

            // Choose the child that requires the least enlargement
            if (enlargement < minEnlargement || (Math.abs(enlargement - minEnlargement) < 1e-10 && area < minArea)) {
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
//        int level = calculateLevel(leaf);
        int level = calculateLevelUsingParent(leaf);

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
     * Choose the split axis that minimizes the sum of perimeters for leaf nodes or elements
     */
    private <T extends WithBoundingBoxAndArea> int chooseSplitAxis(List<T> elements) {
        float minPerimeterSum = Float.POSITIVE_INFINITY;
        int bestAxis = 0;

        // Check both X and Y axes
        for (int axis = 0; axis < 2; axis++) {
            // Sort elements by their center along this axis
            if (elements instanceof ObjectArrayList<T> oal) {
                sortElementsByAxis(oal, axis);
            } else if (elements instanceof ReferenceArrayList<T> ral) {
                sortElementsByAxis(ral, axis);
            } else {
                throw new IllegalArgumentException("Unsupported type: " + elements.getClass());
            }

            // Compute S, the sum of all perimeter-values of the different distributions
            float perimeterSum = computeDistributionPerimeterSum(elements);

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
        return chooseSplitIndex(elements, axis, MIN_ENTRIES);
    }

    /**
     * Choose the distribution with minimum overlap for internal nodes
     */
    private int[] chooseSplitIndexForInternal(List<RTreeNode> children, int axis) {
        return chooseSplitIndex(children, axis, MIN_CHILDREN);
    }

    private <T extends WithBoundingBoxAndArea> int[] chooseSplitIndex(List<T> elements, int axis, int minElements) {
        // Sort by the chosen axis
        if (elements instanceof ObjectArrayList<T> oal) {
            sortElementsByAxis(oal, axis);
        } else if (elements instanceof ReferenceArrayList<T> ral) {
            sortElementsByAxis(ral, axis);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + elements.getClass());
        }

        float minOverlap = Float.POSITIVE_INFINITY;
        float minArea = Float.POSITIVE_INFINITY;
        int splitIndex = minElements;

        // Try distributions and pick the one with minimum overlap
        for (int i = minElements; i <= elements.size() - minElements; i++) {
            // Create two groups
            WithBoundingBoxAndArea mbr1 = computeMBRForElements(elements.subList(0, i));
            WithBoundingBoxAndArea mbr2 = computeMBRForElements(elements.subList(i, elements.size()));

            // Calculate overlap
            float overlap = getOverlap(mbr1, mbr2);
            float area = mbr1.getArea() + mbr2.getArea();

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
     * Sort elements/children by their center along the specified axis
     */
    private <T extends WithBoundingBoxAndArea> void sortElementsByAxis(ReferenceArrayList<T> elements, int axis) {
        Object[] elementsArray = elements.elements();
        ObjectArrays.parallelQuickSort(elementsArray, Comparator.comparingDouble(e -> getCenterOfAxis((T) e, axis == 0)));
    }
    private <T extends WithBoundingBoxAndArea> void sortElementsByAxis(ObjectArrayList<T> elements, int axis) {
        Object[] elementsArray = elements.elements();
        ObjectArrays.parallelQuickSort(elementsArray, Comparator.comparingDouble(e -> getCenterOfAxis((T) e, axis == 0)));
    }

    /**
     * Compute the sum of perimeters for all possible distributions of internal nodes
     */
    private <T extends WithBoundingBoxAndArea> float computeDistributionPerimeterSum(List<T> children) {
        float sum = 0;

        for (int i = MIN_ENTRIES; i <= children.size() - MIN_ENTRIES; i++) {
            WithBoundingBoxAndArea mbr1 = computeMBRForElements(children.subList(0, i));
            WithBoundingBoxAndArea mbr2 = computeMBRForElements(children.subList(i, children.size()));

            // Sum the perimeters (perimeter = 2 * (width + height))
            sum += 2 * ((mbr1.maxLon() - mbr1.minLon()) + (mbr1.maxLat() - mbr1.minLat()));
            sum += 2 * ((mbr2.maxLon() - mbr2.minLon()) + (mbr2.maxLat() - mbr2.minLat()));
        }

        return sum;
    }

    /**
     * Split RTree nodes method
     */
    private RTreeNode splitNodeR(RTreeNode node) {
        RTreeNode newNode = new RTreeNode();

        if (node.isLeaf()) {
            // Handle leaf node split
            ObjectArrayList<OsmElement> elements = new ObjectArrayList<>(node.elements);

            // Choose split axis and distribution
            int bestAxis = chooseSplitAxis(elements);
            int[] distribution = chooseSplitIndexForLeaf(elements, bestAxis);

            // Sort elements by the chosen axis
            sortElementsByAxis(elements, bestAxis);

            // Distribute elements
            ObjectArrayList<OsmElement> group1 = new ObjectArrayList<>(elements.subList(0, distribution[0]));
            ObjectArrayList<OsmElement> group2 = new ObjectArrayList<>(elements.subList(distribution[0], elements.size()));

            // Update nodes
            node.elements = group1;
            newNode.elements = group2;
        } else {
            // Handle internal node split
            ReferenceArrayList<RTreeNode> children = new ReferenceArrayList<>(node.getChildren());

            // Choose split axis and distribution
            int bestAxis = chooseSplitAxis(children);
            int[] distribution = chooseSplitIndexForInternal(children, bestAxis);

            // Sort children by the chosen axis
            sortElementsByAxis(children, bestAxis);

            // Distribute children
            ReferenceArrayList<RTreeNode> group1 = new ReferenceArrayList<>(children.subList(0, distribution[0]));
            ReferenceArrayList<RTreeNode> group2 = new ReferenceArrayList<>(children.subList(distribution[0], children.size()));

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

    /**
     * Compute the MBR for a list of elements or nodes with bounding boxes
     */
    public static <T extends WithBoundingBoxAndArea> WithBoundingBoxAndArea computeMBRForElements(List<T> elements) {
        if (elements.isEmpty()) {
            return createWithBoundingBoxAndArea(0, 0, 0, 0);
        }

        float minLon = elements.getFirst().minLon(), minLat = elements.getFirst().minLat(), maxLon = elements.getFirst().maxLon(), maxLat = elements.getFirst().maxLat();

        for (int i = 1; i < elements.size(); i++) {
            minLon = Math.min(minLon, elements.get(i).minLon());
            minLat = Math.min(minLat, elements.get(i).minLat());
            maxLon = Math.max(maxLon, elements.get(i).maxLon());
            maxLat = Math.max(maxLat, elements.get(i).maxLat());
        }

        return createWithBoundingBoxAndArea(minLon, minLat, maxLon, maxLat);
    }

    /**
    * Helper method to force MBR creation in case a node's MBR is null after node split
    **/
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
        float centerLon = getCenterOfAxis(node, true);
        float centerLat = getCenterOfAxis(node, false);

        if (node.isLeaf()) {
            // Handle leaf node - work directly with elements list
            ObjectArrayList<OsmElement> elements = new ObjectArrayList<>(node.elements);

            // Sort by distance from center in descending order
            elements.sort((e1, e2) -> Float.compare(getDistance(e2, centerLon, centerLat), getDistance(e1, centerLon, centerLat)));

            // Select entries to reinsert (farthest p entries)
            int reinsertCount = Math.min(p, elements.size());
            List<OsmElement> entriesToReinsert = new ArrayList<>(elements.subList(0, reinsertCount));

            // Keep the rest
            node.elements = new ObjectArrayList<>(elements.subList(reinsertCount, elements.size()));
            node.updateBoundingBox();

            // Reinsert entries
            for (OsmElement element : entriesToReinsert) {
                insert(element);
            }
        } else {
            // Handle internal node - work directly with children list
            ReferenceArrayList<RTreeNode> children = new ReferenceArrayList<>(node.getChildren());

            // Sort by distance from center in descending order
            children.sort((c1, c2) -> Float.compare(getDistance(c2, centerLon, centerLat), getDistance(c1, centerLon, centerLat)));

            // Select entries to reinsert (farthest p entries)
            int reinsertCount = Math.min(p, children.size());
            ReferenceArrayList<RTreeNode> childrenToReinsert = new ReferenceArrayList<>(children.subList(0, reinsertCount));

            // Keep the rest
            node.setChildren(new ReferenceArrayList<>(children.subList(reinsertCount, children.size())));
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
    private RTreeNode findTargetNodeAtLevel(RTreeNode node, RTreeNode mbr, int targetLevel) {
        if (targetLevel == 0 || node.isLeaf()) {
            return node;
        }

        // Choose the best child based on minimum enlargement
        RTreeNode bestChild = null;
        float minEnlargement = Float.POSITIVE_INFINITY;

        for (RTreeNode child : node.getChildren()) {
            float enlargement = getEnlargementArea(child, mbr);
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


    private int calculateLevel(RTreeNode node) {
        return calculateLevel(root, node, 0);
    }
    private int calculateLevel(RTreeNode node, RTreeNode nodeToFind, int level) {
        if (node == nodeToFind) return level;

        return node
                .getChildren()
                .parallelStream()
                .filter(c -> intersects(c, nodeToFind))
                .map(c -> calculateLevel(c, nodeToFind, level+1))
                .max(Integer::compareTo)
                .orElse(level);
    }
    private int calculateLevelUsingParent(RTreeNode node) {
        int level = 0;
        RTreeNode parent = node.getParent();
        while (parent != null) {
            level++;
            parent = parent.getParent();
        }

        return level;
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


    public List<OsmElement> search(float minLon, float minLat, float maxLon, float maxLat) {
        Collection<OsmElement> elementsConcurrent = new ConcurrentLinkedQueue<>();

        search(root, minLon, minLat, maxLon, maxLat, elementsConcurrent);

        return elementsConcurrent
                .parallelStream()
                .filter(e -> intersects(e, minLon, minLat, maxLon, maxLat))
                .sorted(Comparator.comparing(OsmElement::getArea).reversed())
                .toList();
    }
    private void search(RTreeNode node, float minLon, float minLat, float maxLon, float maxLat, Collection<OsmElement> results) {
        if (node == null || !intersects(node, minLon, minLat, maxLon, maxLat)) return; // No intersection, skip this branch

        if (node.isLeaf()) {
            results.addAll(node.elements);
        } else {
            node.getChildren().parallelStream().forEach(child -> search(child, minLon, minLat, maxLon, maxLat, results));
        }
    }

    public void searchScaled(float minLon, float minLat, float maxLon, float maxLat, float minBoundingBoxArea, Float2ReferenceMap<Drawable> osmElements) {
        searchScaled(root, minLon, minLat, maxLon, maxLat, minBoundingBoxArea, osmElements);
    }
    private void searchScaled(RTreeNode node, float minLon, float minLat, float maxLon, float maxLat, float minBoundingBoxArea, Float2ReferenceMap<Drawable> results) {
        if (node == null || node.getArea() < minBoundingBoxArea || !intersects(node, minLon, minLat, maxLon, maxLat)) return; // No intersection, skip this branch

        if (node.isLeaf()) {
            for (int i = 0; i < node.elements.size(); i++) {
                OsmElement element = node.elements.get(i);
                if (element instanceof Drawable drawable) {
                    if (intersects(element, minLon, minLat, maxLon, maxLat) && element.getArea() >= minBoundingBoxArea) {
                        results.put(-element.getArea(), drawable);
                    }
                }
            }
        } else {
            for (int i = 0; i < node.getChildren().size(); i++) {
                searchScaled(node.getChildren().get(i), minLon, minLat, maxLon, maxLat, minBoundingBoxArea, results);
            }
        }
    }

    public List<RTreeNode> getBoundingBoxes() {
        List<RTreeNode> boundingBoxes = new ArrayList<>();
        int level = 1, levelsToCheck = Integer.MAX_VALUE;
        getBoundingBoxesRecursive(root, boundingBoxes, level, levelsToCheck);
        return boundingBoxes.stream().toList();
    }
    private void getBoundingBoxesRecursive(RTreeNode node, Collection<RTreeNode> results, int level, int levelsToCheck) {
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
    public OsmNode getNearest(float lon, float lat) {
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
                    float childDist = minDist(lon, lat, child);

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
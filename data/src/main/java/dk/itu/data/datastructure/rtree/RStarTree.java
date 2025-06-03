package dk.itu.data.datastructure.rtree;

import dk.itu.common.models.Drawable;
import dk.itu.common.models.WithBoundingBoxAndArea;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceMap;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dk.itu.data.datastructure.rtree.RStartTreeUtilities.*;

/*
* The R* Tree is based on https://infolab.usc.edu/csci599/Fall2001/paper/rstar-tree.pdf
* Min entries and reinsert percentage are based on the report (p. 327)
* */

public class RStarTree {
    private static final int MAX_ENTRIES = 100;  // Maximum entries in a node
    private static final int MIN_ENTRIES = MAX_ENTRIES / 2;  // Minimum entries (40-50% of max is typical)
    private static final float REINSERT_PERCENTAGE = 0.3f;  // Percentage of entries to reinsert (30% is typical)
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
    private double minDist(double px, double py, WithBoundingBoxAndArea box) {
        double dx = 0;
        double dy = 0;

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
        return Math.sqrt(dx * dx + dy * dy);
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
            for (int i = 0; i < node.getElements().size(); i++) {
                var element = node.getElements().get(i);
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
        double minEnlargement = Double.POSITIVE_INFINITY;
        double minArea = Double.POSITIVE_INFINITY;

        for (RTreeNode child : node.getChildren()) {
            // Calculate how much the child's MBR would need to be enlarged
            double enlargement = getEnlargementArea(elementBox, child);
            double area = child.getArea();

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
        int level = calculateLevel(leaf);

        // Check if we should do a forced reinsert instead of splitting
        if (!reinsertLevels.contains(level)) {
            boolean didReinsert = forcedReinsert(leaf, level);
            if (didReinsert && leaf.getElements().size() < MAX_ENTRIES) {
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

    private record DistributionAndSortedElements<T extends WithBoundingBoxAndArea>(List<T> entries, int[] distribution) {}

    private <T extends WithBoundingBoxAndArea> DistributionAndSortedElements<T> chooseSplitAxisAndGetSplitIndexAndElementsSorted(List<T> entries) {
        boolean isForLeaf = entries.getFirst() instanceof OsmElement;

        List<T> entriesSortedByX = new ArrayList<>(entries);
        sortElementsByAxis(entriesSortedByX, 0);
        double perimeterSumX = computeDistributionPerimeterSum(entriesSortedByX, isForLeaf ? MIN_ENTRIES : MIN_CHILDREN);
        List<T> entriesSortedByY = new ArrayList<>(entries);
        sortElementsByAxis(entriesSortedByY, 1);
        double perimeterSumY = computeDistributionPerimeterSum(entriesSortedByY, isForLeaf ? MIN_ENTRIES : MIN_CHILDREN);

        if (perimeterSumX < perimeterSumY) {
            // Use x => clear y and make it GC eligible
            entriesSortedByY.clear();
            entriesSortedByY = null;

            if (isForLeaf) {
                return new DistributionAndSortedElements<>(entriesSortedByX, chooseSplitIndex(entriesSortedByX, MIN_ENTRIES));
            } else {
                return new DistributionAndSortedElements<>(entriesSortedByX, chooseSplitIndex(entriesSortedByX, MIN_CHILDREN));
            }
        } else {
            // Use y => clear x and make it GC eligible
            entriesSortedByX.clear();
            entriesSortedByX = null;

            if (isForLeaf) {
                return new DistributionAndSortedElements<>(entriesSortedByY, chooseSplitIndex(entriesSortedByY, MIN_ENTRIES));
            } else {
                return new DistributionAndSortedElements<>(entriesSortedByY, chooseSplitIndex(entriesSortedByY, MIN_CHILDREN));
            }
        }
    }

    private <T extends WithBoundingBoxAndArea> int[] chooseSplitIndex(List<T> elements, int minElements) {
        double minOverlap = Double.POSITIVE_INFINITY;
        double minArea = Double.POSITIVE_INFINITY;
        int splitIndex = minElements;

        // Try distributions and pick the one with minimum overlap
        for (int i = minElements; i <= elements.size() - minElements; i++) {
            // Create two groups
            WithBoundingBoxAndArea mbr1 = computeMBRForElements(elements.subList(0, i));
            WithBoundingBoxAndArea mbr2 = computeMBRForElements(elements.subList(i, elements.size()));

            // Calculate overlap
            double overlap = getOverlap(mbr1, mbr2);
            double area = mbr1.getArea() + mbr2.getArea();

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
    private <T extends WithBoundingBoxAndArea> void sortElementsByAxis(List<T> elements, int axis) {
        elements.sort(Comparator.comparingDouble(e -> getCenterOfAxis(e, axis == 0)));
    }

    /**
     * Compute the sum of perimeters for all possible distributions of internal nodes
     */
    private <T extends WithBoundingBoxAndArea> double computeDistributionPerimeterSum(List<T> children, int minEntries) {
        double sum = 0;

        for (int i = minEntries; i <= children.size() - minEntries; i++) {
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
            // Choose split axis and distribution
            DistributionAndSortedElements<OsmElement> distributionResult = chooseSplitAxisAndGetSplitIndexAndElementsSorted(node.getElements());

            // Distribute elements
            List<OsmElement> group1 = new ArrayList<>(distributionResult.entries.subList(0, distributionResult.distribution[0]));
            List<OsmElement> group2 = new ArrayList<>(distributionResult.entries.subList(distributionResult.distribution[0], distributionResult.entries.size()));

            // Update nodes
            node.setElements(group1);
            newNode.setElements(group2);
        } else {
            // Choose split axis and distribution
            DistributionAndSortedElements<RTreeNode> distributionResult = chooseSplitAxisAndGetSplitIndexAndElementsSorted(node.getChildren());

            // Distribute children
            List<RTreeNode> group1 = new ArrayList<>(distributionResult.entries.subList(0, distributionResult.distribution[0]));
            List<RTreeNode> group2 = new ArrayList<>(distributionResult.entries.subList(distributionResult.distribution[0], distributionResult.entries.size()));

            // Update nodes
            node.setChildren(group1);
            newNode.setChildren(group2);
        }

        return newNode;
    }

    /**
     * Compute the MBR for a list of elements or nodes with bounding boxes
     */
    public static <T extends WithBoundingBoxAndArea> WithBoundingBoxAndArea computeMBRForElements(List<T> elements) {
        if (elements.isEmpty()) {
            return createWithBoundingBoxAndArea(0, 0, 0, 0);
        }

        double minLon = elements.getFirst().minLon(), minLat = elements.getFirst().minLat(), maxLon = elements.getFirst().maxLon(), maxLat = elements.getFirst().maxLat();

        for (int i = 1; i < elements.size(); i++) {
            minLon = Math.min(minLon, elements.get(i).minLon());
            minLat = Math.min(minLat, elements.get(i).minLat());
            maxLon = Math.max(maxLon, elements.get(i).maxLon());
            maxLat = Math.max(maxLat, elements.get(i).maxLat());
        }

        return createWithBoundingBoxAndArea(minLon, minLat, maxLon, maxLat);
    }

    /**
     * Forced reinsert procedure for R*-tree
     */
    private boolean forcedReinsert(RTreeNode node, int level) {
        // Check if reinsert is needed
        boolean isFull = node.isLeaf() ?
                node.getElements().size() > MAX_ENTRIES :
                node.getChildren().size() > MAX_CHILDREN;

        if (!isFull) {
            return false;
        }

        // Mark this level as having done a reinsert
        reinsertLevels.add(level);

        // Number of entries to reinsert
        int p = (int) Math.ceil(REINSERT_PERCENTAGE * MAX_ENTRIES);
        double centerLon = getCenterOfAxis(node, true);
        double centerLat = getCenterOfAxis(node, false);

        if (node.isLeaf()) {
            // Handle leaf node - work directly with elements list
            List<OsmElement> elements = new ArrayList<>(node.getElements());

            // Sort by distance from center in descending order
            elements.sort((e1, e2) -> Double.compare(getDistance(e2, centerLon, centerLat), getDistance(e1, centerLon, centerLat)));

            // Select entries to reinsert (farthest p entries)
            int reinsertCount = Math.min(p, elements.size());
            List<OsmElement> entriesToReinsert = new ArrayList<>(elements.subList(0, reinsertCount));

            // Keep the rest
            node.setElements(new ArrayList<>(elements.subList(reinsertCount, elements.size())));

            // Reinsert entries
            for (OsmElement element : entriesToReinsert) {
                insert(element);
            }
        } else {
            // Handle internal node - work directly with children list
            List<RTreeNode> children = new ArrayList<>(node.getChildren());

            // Sort by distance from center in descending order
            children.sort((c1, c2) -> Double.compare(getDistance(c2, centerLon, centerLat), getDistance(c1, centerLon, centerLat)));

            // Select entries to reinsert (farthest p entries)
            int reinsertCount = Math.min(p, children.size());
            List<RTreeNode> childrenToReinsert =
                    new ArrayList<>(children.subList(0, reinsertCount));

            // Keep the rest
            node.setChildren(new ArrayList<>(children.subList(reinsertCount, children.size())));

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
        double minEnlargement = Double.POSITIVE_INFINITY;

        for (RTreeNode child : node.getChildren()) {
            double enlargement = getEnlargementArea(child, mbr);
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
        if (parent == null) {
            // We've reached the root
            if (newNode != null) {
                // Need to create a new root
                RTreeNode newRoot = new RTreeNode();
                newRoot.addChild(node);
                newRoot.addChild(newNode);
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
        if (leaf.getElements().size() > MAX_ENTRIES) {
            newNode = splitLeaf(leaf);
        }

        // Find parent and propagate changes upward
        adjustTree(leaf, newNode, leaf.getParent());
        isEmpty = false;
    }


    public List<OsmElement> search(double minLon, double minLat, double maxLon, double maxLat) {
        Collection<OsmElement> elementsConcurrent = new ConcurrentLinkedQueue<>();

        search(root, minLon, minLat, maxLon, maxLat, elementsConcurrent);

        return elementsConcurrent
                .parallelStream()
                .filter(e -> intersects(e, minLon, minLat, maxLon, maxLat))
                .sorted(Comparator.comparing(OsmElement::getArea).reversed())
                .toList();
    }
    private void search(RTreeNode node, double minLon, double minLat, double maxLon, double maxLat, Collection<OsmElement> results) {
        if (node == null || !intersects(node, minLon, minLat, maxLon, maxLat)) return; // No intersection, skip this branch

        if (node.isLeaf()) {
            results.addAll(node.getElements());
        } else {
            node.getChildren().parallelStream().forEach(child -> search(child, minLon, minLat, maxLon, maxLat, results));
        }
    }

    public static class RTreeSearchStats {
        private int nodeCheck, passedNodeCheck, startLeafCheck, elementInvestigated, elementPassed, startChildrenCheck, startChildCheck;
        private double timeElapsed;
        private boolean hasBeenPrintedOnce = false;

        public void reset() {
            nodeCheck = passedNodeCheck = startChildCheck = elementPassed = startLeafCheck = elementInvestigated = startChildrenCheck = 0;
            hasBeenPrintedOnce = true;
        }

        @Override
        public String toString() {
            if (hasBeenPrintedOnce) {
                return "timeElapsed: " + timeElapsed;
            } else {
                return String.format(
                        """
                                nodeCheck: %s,
                                passedNodeCheck: %s,
                                startLeafCheck: %s,
                                elementInvestigated: %s,
                                elementPassed: %s,
                                startChildrenCheck: %s,
                                startChildCheck: %s,
                                timeElapsed: %s
                                """,
                        nodeCheck,
                        passedNodeCheck,
                        startLeafCheck,
                        elementInvestigated,
                        elementPassed,
                        startChildrenCheck,
                        startChildCheck,
                        timeElapsed
                );
            }
        }

        public void addNodeCheck() {
            nodeCheck++;
        }

        public void addPassedNodeCheck() {
            passedNodeCheck++;
        }

        public void addStartLeafCheck() {
            startLeafCheck++;
        }

        public void addElementInvestigated() {
            elementInvestigated++;
        }

        public void addElementPassed() {
            elementPassed++;
        }

        public void addStartChildrenCheck() {
            startChildrenCheck++;
        }

        public void addStartChildCheck() {
            startChildCheck++;
        }

        public void setTimeElapsed(double timeElapsed) {
            this.timeElapsed = timeElapsed;
        }
    }

    private static final RTreeSearchStats stats = new RTreeSearchStats();
    public void searchScaled(double minLon, double minLat, double maxLon, double maxLat, double minBoundingBoxArea, Double2ReferenceMap<Drawable> results) {
        long startTime = System.nanoTime();
        searchScaled(root, minLon, minLat, maxLon, maxLat, minBoundingBoxArea, results);
        stats.setTimeElapsed(System.nanoTime() - startTime);
        System.out.println(stats);
        stats.reset();
    }
    private void searchScaled(RTreeNode node, double minLon, double minLat, double maxLon, double maxLat, double minBoundingBoxArea, Double2ReferenceMap<Drawable> results) {
        stats.addNodeCheck();
        if (node == null || node.getArea() < minBoundingBoxArea || !intersects(node, minLon, minLat, maxLon, maxLat)) return; // No intersection, skip this branch
        stats.addPassedNodeCheck();

        if (node.isLeaf()) {
            stats.addStartLeafCheck();
            for (int i = 0; i < node.getElements().size(); i++) {
                stats.addElementInvestigated();
                OsmElement element = node.getElements().get(i);
                if (intersects(element, minLon, minLat, maxLon, maxLat) && element.getArea() >= minBoundingBoxArea) {
                    stats.addElementPassed();
                    results.put(-element.getArea(), (Drawable) element);
                }
            }
        } else {
            stats.addStartChildrenCheck();
            for (int i = 0; i < node.getChildren().size(); i++) {
                stats.addStartChildCheck();
                searchScaled(node.getChildren().get(i), minLon, minLat, maxLon, maxLat, minBoundingBoxArea, results);
            }
        }
    }

    public List<RTreeNode> getBoundingBoxes() {
        List<RTreeNode> boundingBoxes = new ArrayList<>();
        int level = 1, levelsToCheck = Integer.MAX_VALUE;
        getBoundingBoxes(root, boundingBoxes, level, levelsToCheck);
        return boundingBoxes.stream().toList();
    }
    private void getBoundingBoxes(RTreeNode node, Collection<RTreeNode> results, int level, int levelsToCheck) {
        results.add(node);
        level++;
        if (level < levelsToCheck) {
            int finalLevel = level;
            node.getChildren().forEach(child -> getBoundingBoxes(child, results, finalLevel, levelsToCheck));
        }
    }

    public boolean testBoundingBoxesAreValid() {
        Deque<RTreeNode> boundingBoxesToCheck = new ArrayDeque<>();
        boolean areBoundingBoxesValid = true;
        boundingBoxesToCheck.push(root);
        while (!boundingBoxesToCheck.isEmpty()) {
            RTreeNode node = boundingBoxesToCheck.pop();
            areBoundingBoxesValid = areBoundingBoxesValid && testBoundingBoxesAreValid(node);
            node.getChildren().forEach(boundingBoxesToCheck::push);
        }
        return areBoundingBoxesValid;
    }
    private boolean testBoundingBoxesAreValid(RTreeNode node) {
        List<WithBoundingBoxAndArea> entries = new ArrayList<>(node.isLeaf() ? node.getElements() : node.getChildren());
        if (entries.isEmpty()) {
            throw new IllegalStateException("RTreeNode must have children or elements");
        }
        double minLon = entries.getFirst().minLon();
        double minLat = entries.getFirst().minLat();
        double maxLon = entries.getFirst().maxLon();
        double maxLat = entries.getFirst().maxLat();
        for (int i = 1; i < entries.size(); i++) {
            var entry = entries.get(i);
            if (entry.minLon() < minLon) minLon = entry.minLon();
            if (entry.minLat() < minLat) minLat = entry.minLat();
            if (entry.maxLon() > maxLon) maxLon = entry.maxLon();
            if (entry.maxLat() > maxLat) maxLat = entry.maxLat();
        }
        return
                minLon == node.minLon() &&
                minLat == node.minLat() &&
                maxLon == node.maxLon() &&
                maxLat == node.maxLat();
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
                for (OsmElement element : entry.node.getElements()) {
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
package dk.itu.data.datastructure.rtree;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.Drawable;
import dk.itu.common.models.WithBoundingBoxAndArea;
import dk.itu.data.models.osm.OsmElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static dk.itu.common.models.WithBoundingBoxAndArea.calculateArea;

public class RTreeNode implements WithBoundingBoxAndArea, Drawable {
    private RTreeNode parent;
    private final List<OsmElement> elements = new ObjectArrayList<>();            // For leaf nodes
    private final List<RTreeNode> children = new ReferenceArrayList<>();  // For internal nodes
    private float minLon = Float.MAX_VALUE, minLat = Float.MAX_VALUE;
    private float maxLon = Float.MIN_VALUE, maxLat = Float.MIN_VALUE;
    private float area = 0;

    public RTreeNode()  {
        super();
    }

    public RTreeNode getParent() {
        return parent;
    }

    private void setParent(RTreeNode parent) {
        this.parent = parent;
        // No bounding box expansion needed, since it should be done after the call to this function
    }

    public List<RTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<RTreeNode> children) {
        this.children.clear();
        this.resetBoundingBox();
        for (RTreeNode child : children) {
            this.children.add(child);
            child.setParent(this);
            this.expandBoundingBoxAndParent(child);
        }
    }

    public List<OsmElement> getElements() {
        return elements;
    }

    public void setElements(List<OsmElement> elements) {
        this.elements.clear();
        this.resetBoundingBox();
        for (OsmElement osmElement : elements) {
            this.elements.add(osmElement);
            this.expandBoundingBoxAndParent(osmElement);
        }
    }

    /**
     * Adds a new entry to the node and updates its bounding box.
     * @param entry the entry to add to the node.
     */
    public void addEntry(OsmElement entry) {
        elements.add(entry);

        if (elements.size() > 1) {
            expandBoundingBoxAndParent(entry);
        } else {
            setBoundingBoxAndExpandParent(entry);
        }
    }

    /**
     * Adds a new child to the node and updates its bounding box.
     * @param child the child to add to the node.
     */
    public void addChild(RTreeNode child) {
        children.add(child);
        child.setParent(this);

        if (children.size() > 1) {
            expandBoundingBoxAndParent(child);
        } else {
            setBoundingBoxAndExpandParent(child);
        }
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    private void recalculateBoundingBox() {
        if (isLeaf()) {
            // Recalculate based off of elements
            recalculateBoundingBox(elements);
        } else {
            // Recalculate based off of children
            recalculateBoundingBox(children);
        }
    }

    private <T extends WithBoundingBoxAndArea> void recalculateBoundingBox(List<T> entries) {
        switch (entries.size()) {
            case 0:
                // Do nothing
                throw new IllegalStateException("RStarTreeNode should have at least one child or element");
            case 1:
                // Set it to entry's bounding box and expand parent
                setBoundingBoxAndExpandParent(entries.getFirst());
            default:
                // Calculate bounding box based off of entries
                float minLon = Float.MAX_VALUE, minLat = Float.MAX_VALUE;
                float maxLon = Float.MIN_VALUE, maxLat = Float.MIN_VALUE;
                for (T entry : entries) {
                    if (entry.minLon() < minLon) {
                        minLon = entry.minLon();
                    }
                    if (entry.minLat() < minLat) {
                        minLat = entry.minLat();
                    }
                    if (entry.maxLon() > maxLon) {
                        maxLon = entry.maxLon();
                    }
                    if (entry.maxLat() > maxLat) {
                        maxLat = entry.maxLat();
                    }
                }

                if (
                        this.minLon != minLon ||
                        this.minLat != minLat ||
                        this.maxLon != maxLon ||
                        this.maxLat != maxLat
                ) {
                    // There has been changes
                    if (
                            this.minLon >= minLon &&
                            this.minLat >= minLat &&
                            this.maxLon <= maxLon &&
                            this.maxLat <= maxLat
                    ) {
                        // Expansion happened and should be propagated
                        setBoundingBoxAndExpandParent(minLon, minLat, maxLon, maxLat);
                    } else {
                        // Bounding boxes need to be recalculated
                        setBoundingBoxAndUpdateParent(minLon, minLat, maxLon, maxLat);
                    }
                }
        }
    }

    /**
     * Expand the node's bounding box by another. Also propagates changes upwards to parent.
     * @param boundingBox the bounding box to expand by.
     */
    private void expandBoundingBoxAndParent(WithBoundingBoxAndArea boundingBox) {
        setBoundingBoxAndExpandParent(
                Math.min(this.minLon, boundingBox.minLon()),
                Math.min(this.minLat, boundingBox.minLat()),
                Math.max(this.maxLon, boundingBox.maxLon()),
                Math.max(this.maxLat, boundingBox.maxLat())
        );
    }

    private void setBoundingBoxAndExpandParent(WithBoundingBoxAndArea boundingBox) {
        setBoundingBoxAndExpandParent(
                boundingBox.minLon(),
                boundingBox.minLat(),
                boundingBox.maxLon(),
                boundingBox.maxLat()
        );
    }

    private void setBoundingBoxAndExpandParent(float minLon, float minLat, float maxLon, float maxLat) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        updateArea();
        // Update parent
        if (this.parent != null) {
            this.parent.expandBoundingBoxAndParent(this);
        }
    }

    private void setBoundingBoxAndUpdateParent(float minLon, float minLat, float maxLon, float maxLat) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        updateArea();
        // Recalculate parent
        if (this.parent != null) {
            this.parent.recalculateBoundingBox();
        }
    }

    private void resetBoundingBox() {
        this.minLon = Float.MAX_VALUE;
        this.minLat = Float.MAX_VALUE;
        this.maxLon = Float.MIN_VALUE;
        this.maxLat = Float.MIN_VALUE;
        this.area = 0;
        // Update parent bounding box
        if (this.parent != null) {
            this.parent.recalculateBoundingBox();
        }
    }

    /**
     * Updates the bounding box area.
     */
    private void updateArea() {
        this.area = calculateArea(minLon, minLat, maxLon, maxLat);
    }

    @Override
    public float getArea() {
        return this.area;
    }

    @Override
    public float minLon() {
        return minLon;
    }

    @Override
    public float minLat() {
        return minLat;
    }

    @Override
    public float maxLon() {
        return maxLon;
    }

    @Override
    public float maxLat() {
        return maxLat;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(DrawingConfiguration.getInstance().getColor((byte) 0));
        g2d.setStroke(new BasicStroke(strokeBaseWidth * DrawingConfiguration.getInstance().getStroke((byte) 0)));
        g2d.draw(new Rectangle2D.Double(0.56*minLon, -maxLat, 0.56*(maxLon - minLon), maxLat - minLat));
    }
}
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
    RTreeNode parent;
    List<OsmElement> elements = new ObjectArrayList<>();            // For leaf nodes
    private List<RTreeNode> children = new ReferenceArrayList<>();  // For internal nodes
    private float minLon = 0, minLat = 0, maxLon = 0, maxLat = 0, area = 0;

    public RTreeNode()  {
        super();
    }

    public RTreeNode getParent() {
        return parent;
    }
    public void setParent(RTreeNode parent) {
        this.parent = parent;
    }

    public List<RTreeNode> getChildren() {
        return children;
    }
    public void setChildren(List<RTreeNode> children) {
        this.children = children;
        this.children.forEach(child -> child.setParent(this));
    }

    public List<OsmElement> getElements() {
        return elements;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Adds a new entry to the node and updates its bounding box.
     * @param entry the entry to add to the node.
     */
    public void addEntry(OsmElement entry) {
        elements.add(entry);

        if (elements.size() > 1) {
            expand(entry);
        } else {
            setBoundingBox(entry);
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
            expand(child);
        } else {
            setBoundingBox(child.minLon, child.minLat, child.maxLon, child.maxLat);
        }
    }

    /**
     * Ensures correct bounding box for node.
     */
    public void updateBoundingBox() {
        if (children.isEmpty()) {
            updateBoundingBoxFromList(elements);
        } else if (elements.isEmpty()) {
            updateBoundingBoxFromList(children);
        }
    }

    /**
     * Updates the node's bounding box based on its contents.
     * @param elements the list which Bounding Boxes should be used to update the node's bounding box.
     * @param <T> generic type implementing {@link WithBoundingBoxAndArea}.
     */
    private <T extends WithBoundingBoxAndArea> void updateBoundingBoxFromList(List<T> elements) {
        if (elements.size() > 1) {
            float minLon = Float.MAX_VALUE, minLat = Float.MAX_VALUE;
            float maxLon = Float.MIN_VALUE, maxLat = Float.MIN_VALUE;

            // Using standard for loop to avoid Java overhead from iterators
            for (int i = 0; i < elements.size(); i++) {
                T element = elements.get(i);
                minLon = Math.min(minLon, element.minLon());
                minLat = Math.min(minLat, element.minLat());
                maxLon = Math.max(maxLon, element.maxLon());
                maxLat = Math.max(maxLat, element.maxLat());
            }
            setBoundingBox(minLon, minLat, maxLon, maxLat);
        } else {
            setBoundingBox(children.getFirst());
        }
    }

    /**
     * Sets the node's bounding box.
     * @param boundingBox the node's new bounding box.
     */
    public void setBoundingBox(WithBoundingBoxAndArea boundingBox) {
        setBoundingBox(boundingBox.minLon(), boundingBox.minLat(), boundingBox.maxLon(), boundingBox.maxLat());
    }

    /**
     * Expand the node's bounding box by another.
     * @param boundingBox the bounding box to expand by.
     */
    public void expand(WithBoundingBoxAndArea boundingBox) {
        setBoundingBox(
                Math.min(this.minLon, boundingBox.minLon()),
                Math.min(this.minLat, boundingBox.minLat()),
                Math.max(this.maxLon, boundingBox.maxLon()),
                Math.max(this.maxLat, boundingBox.maxLat())
        );
    }

    /**
     * Sets the node's bounding box.
     * @param minLon the new {@code minLon}.
     * @param minLat the new {@code minLat}.
     * @param maxLon the new {@code maxLon}.
     * @param maxLat the new {@code maxLat}.
     */
    private void setBoundingBox(float minLon, float minLat, float maxLon, float maxLat) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        updateArea();
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
package dk.itu.data.datastructure.rtree;

import dk.itu.data.models.BoundingBox;
import dk.itu.data.models.osm.OsmElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.awt.*;
import java.util.List;

public class RTreeNode extends BoundingBox {
    RTreeNode parent;
    List<OsmElement> elements = new ObjectArrayList<>();    // For leaf nodes
    private List<RTreeNode> children = new ReferenceArrayList<>();           // For internal nodes
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

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public void addEntry(OsmElement entry) {
        elements.add(entry);
        if (elements.size() > 1) {
            expand(entry);
        } else {
            setBoundingBox(entry);
        }
    }

    public List<OsmElement> getElements() {
        return elements;
    }

    public void addChild(RTreeNode child) {
        children.add(child);
        child.setParent(this);
        if (children.size() > 1) {
            expand(child);
        } else {
            setBoundingBox(child);
        }
    }

    public void updateBoundingBox() {
        if (children.isEmpty()) {
            return;
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (RTreeNode child : children) {
            minX = Math.min(minX, child.getMinLon());
            minY = Math.min(minY, child.getMinLat());
            maxX = Math.max(maxX, child.getMaxLon());
            maxY = Math.max(maxY, child.getMaxLat());
        }

        setBoundingBox(minX, minY, maxX, maxY);
    }

    public void setMBR(BoundingBox bbox) {
        this.setBoundingBox(bbox);
    }

    @Override
    public void prepareDrawing(Graphics2D g2d) {}

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {}
}
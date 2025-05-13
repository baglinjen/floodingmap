package dk.itu.data.datastructure.rtree;

import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.db.osm.OsmElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.List;

public class RTreeNode {
    BoundingBox mbr;
    RTreeNode parent;
    List<OsmElement> elements = new ObjectArrayList<>();    // For leaf nodes
    private List<RTreeNode> children = new ReferenceArrayList<>();           // For internal nodes
    public RTreeNode()  {
        this.mbr = null;
    }

    public BoundingBox getMBR() {
        return mbr;
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
        updateMBR(entry.getBoundingBox());
    }

    public List<OsmElement> getElements() {
        return elements;
    }

    public void addChild(RTreeNode child) {
        children.add(child);
        child.setParent(this);
        updateMBR(child.mbr);
    }

    public void updateBoundingBox() {
        if (children.isEmpty()) {
            return;
        }

        // TODO: Check with parallel stream for children (not per value min/max)

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (RTreeNode child : children) {
            minX = Math.min(minX, child.mbr.getMinLon());
            minY = Math.min(minY, child.mbr.getMinLat());
            maxX = Math.max(maxX, child.mbr.getMaxLon());
            maxY = Math.max(maxY, child.mbr.getMaxLat());
        }

        this.mbr = new BoundingBox(minX, minY, maxX, maxY);
    }

    private void updateMBR(BoundingBox bbox) {
        if (mbr == null) {
            mbr = new BoundingBox(bbox.getMinLon(), bbox.getMinLat(), bbox.getMaxLon(), bbox.getMaxLat());
        } else {
            mbr.expand(bbox);
        }
    }

    public void setMBR(BoundingBox bbox) {
        this.mbr = bbox;
    }
}

package dk.itu.data.datastructure.rtree;

import dk.itu.data.models.memory.BoundingBox;
import dk.itu.data.models.memory.OsmElementMemory;

import java.util.List;
import java.util.ArrayList;

public class RTreeNode {
    BoundingBox mbr;
    List<OsmElementMemory> elements = new ArrayList<>();
    List<RTreeNode> children = new ArrayList<>();

    public RTreeNode() {
        this.mbr = null;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public void addEntry(OsmElementMemory entry) {
        elements.add(entry);
        updateMBR(entry.getBoundingBox());
    }

    public List<OsmElementMemory> getElements() {
        return elements;
    }

    public void addChild(RTreeNode child) {
        children.add(child);
        updateMBR(child.mbr);
    }

    public void updateBoundingBox() {
        if (children.isEmpty()) {
            return;
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (RTreeNode child : children) {
            minX = Math.min(minX, child.mbr.getMinX());
            minY = Math.min(minY, child.mbr.getMinY());
            maxX = Math.max(maxX, child.mbr.getMaxX());
            maxY = Math.max(maxY, child.mbr.getMaxY());
        }

        this.mbr = new BoundingBox(minX, minY, maxX, maxY);
    }

    private void updateMBR(BoundingBox bbox) {
        if (mbr == null) {
            mbr = new BoundingBox(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
        } else {
            mbr.expand(bbox);
        }
    }
}

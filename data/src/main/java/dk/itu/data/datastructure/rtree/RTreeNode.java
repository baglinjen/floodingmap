package dk.itu.data.datastructure.rtree;

import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.db.osm.OsmElement;

import java.util.List;
import java.util.ArrayList;

public class RTreeNode {
    BoundingBox mbr;
    List<OsmElement> elements = new ArrayList<>();
    List<RTreeNode> children = new ArrayList<>();

    public RTreeNode() {
        this.mbr = null;
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
        updateMBR(child.mbr);
    }

    public void updateBoundingBox() {
        if (children.isEmpty()) {
            return;
        }

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
}

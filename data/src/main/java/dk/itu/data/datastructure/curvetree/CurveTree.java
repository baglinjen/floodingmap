package dk.itu.data.datastructure.curvetree;

import dk.itu.data.models.parser.ParserGeoJsonElement;

import java.util.ArrayList;
import java.util.List;

public class CurveTree {
    private final List<CurveTreeNode> rootChildren = new ArrayList<>();

    public void put(ParserGeoJsonElement geoJsonElement) {
        put(null, geoJsonElement);
    }

    private void put(CurveTreeNode curveTreeNode, ParserGeoJsonElement geoJsonElement) {
        if (curveTreeNode == null) {
            // Adding to root
            lookupChildren(rootChildren, geoJsonElement);
        } else {
            if (curveTreeNode.getChildren() == null) {
                curveTreeNode.addChild(new CurveTreeNode(geoJsonElement));
            } else {
                lookupChildren(curveTreeNode.getChildren(), geoJsonElement);
            }
        }
    }

    private void lookupChildren(List<CurveTreeNode> children, ParserGeoJsonElement geoJsonElement) {
        for (CurveTreeNode child : children) {
            // If geoJsonElement in child => add + return
            if (child.getGeoJsonElement().contains(geoJsonElement)) {
                // Add to node + return
                put(child, geoJsonElement);
                return;
            }
            // Else => iterate
        }
        children.add(new CurveTreeNode(geoJsonElement));
    }
}

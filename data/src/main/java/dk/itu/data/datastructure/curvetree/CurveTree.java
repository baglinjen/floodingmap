package dk.itu.data.datastructure.curvetree;

import dk.itu.data.models.parser.ParserGeoJsonElement;

import java.util.ArrayList;
import java.util.List;

import static dk.itu.util.PolygonUtils.isPolygonContained;

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
            if (isPolygonContained(child.getGeoJsonElement().getCoordinates(), geoJsonElement.getCoordinates())) {
                // Add to node + return
                put(child, geoJsonElement);
                return;
            }
            // Else => iterate
            System.out.println();
        }
        children.add(new CurveTreeNode(geoJsonElement));
    }
}

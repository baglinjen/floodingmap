package dk.itu.data.datastructure.curvetree;

import dk.itu.data.models.parser.ParserGeoJsonElement;

import java.util.ArrayList;
import java.util.List;

public class CurveTreeNode {
    private final ParserGeoJsonElement geoJsonElement;
    private List<CurveTreeNode> children;

    public CurveTreeNode(ParserGeoJsonElement geoJsonElement) {
        this.geoJsonElement = geoJsonElement;
    }

    public ParserGeoJsonElement getGeoJsonElement() {
        return geoJsonElement;
    }

    public List<CurveTreeNode> getChildren() {
        return children;
    }

    public void addChild(CurveTreeNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }
}

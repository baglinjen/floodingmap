package dk.itu.data.datastructure.heightcurvetree;

import dk.itu.data.models.heightcurve.HeightCurveElement;

import java.util.*;

public class HeightCurveTreeNode {
    private final HeightCurveElement heightCurveElement;
    private final List<HeightCurveTreeNode> children;

    public HeightCurveTreeNode(HeightCurveElement heightCurveElement) {
        this.heightCurveElement = heightCurveElement;
        this.children = new ArrayList<>();
    }

    public List<HeightCurveTreeNode> getChildren() {
        return children;
    }

    public HeightCurveElement getHeightCurveElement() {
        return heightCurveElement;
    }
}
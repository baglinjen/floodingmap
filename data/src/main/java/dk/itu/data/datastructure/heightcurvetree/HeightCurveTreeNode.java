package dk.itu.data.datastructure.heightcurvetree;

import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.util.PolygonUtils;

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

    public double getArea() {
        return heightCurveElement.getArea();
    }

    public boolean contains(HeightCurveElement element) {
        if (
                element.getBounds()[0] >= heightCurveElement.getBounds()[0] &&
                element.getBounds()[1] >= heightCurveElement.getBounds()[1] &&
                element.getBounds()[2] <= heightCurveElement.getBounds()[2] &&
                element.getBounds()[3] <= heightCurveElement.getBounds()[3]
        ) {
            return PolygonUtils.contains(this.heightCurveElement.getCoordinates(), element.getCoordinates());
        } else {
            return false;
        }
    }

    public boolean contains(double lon, double lat) {
        if (
                lon >= heightCurveElement.getBounds()[0] &&
                        lat >= heightCurveElement.getBounds()[1] &&
                        lon <= heightCurveElement.getBounds()[2] &&
                        lat <= heightCurveElement.getBounds()[3]
        ) {
            return PolygonUtils.isPointInPolygon(this.heightCurveElement.getCoordinates(), lon, lat);
        } else {
            return false;
        }
    }
}
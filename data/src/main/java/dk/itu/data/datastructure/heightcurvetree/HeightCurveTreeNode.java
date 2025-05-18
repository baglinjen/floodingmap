package dk.itu.data.datastructure.heightcurvetree;

import dk.itu.data.models.heightcurve.HeightCurveElement;
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

    public boolean contains(HeightCurveElement element) {
        if (heightCurveElement.contains(element)) {
            return PolygonUtils.contains(this.heightCurveElement.getCoordinates(), element.getCoordinates());
        } else {
            return false;
        }
    }

    public boolean contains(double lon, double lat) {
        if (
                lon >= heightCurveElement.getMinLon() &&
                lat >= heightCurveElement.getMinLat() &&
                lon <= heightCurveElement.getMaxLon() &&
                lat <= heightCurveElement.getMaxLat()
        ) {
            return PolygonUtils.isPointInPolygon(this.heightCurveElement.getCoordinates(), lon, lat);
        } else {
            return false;
        }
    }
}
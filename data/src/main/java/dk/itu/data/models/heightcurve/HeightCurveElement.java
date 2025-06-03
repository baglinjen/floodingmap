package dk.itu.data.models.heightcurve;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.Drawable;
import dk.itu.common.models.WithBoundingBox;
import dk.itu.common.models.WithStyle;
import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.PolygonUtils;
import dk.itu.util.shape.HeightCurvePath;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dk.itu.util.PolygonUtils.forceCounterClockwise;


public class HeightCurveElement implements WithBoundingBox, Drawable, WithStyle {
    // Path
    private final float[] outerPolygon;
    private final List<float[]> innerPolygons = new ArrayList<>();
    private HeightCurvePath heightCurvePath; // TODO: Remove the double[] outerPolygon field and only have it on the HeightCurvePath object. OuterPolygon should never change
    // Bounding Box
    private final float minLon, minLat, maxLon, maxLat;
    // Features
    private final float height;
    private byte styleId;
    private boolean isAboveWater = true;

    public HeightCurveElement(float[] outerPolygon, float height) {
        this.outerPolygon = outerPolygon;
        this.height = height;

        // Calculate Bounding Box
        float minLon = Float.MAX_VALUE;
        float minLat = Float.MAX_VALUE;
        float maxLon = Float.MIN_VALUE;
        float maxLat = Float.MIN_VALUE;
        for (int i = 0; i < outerPolygon.length; i+=2) {
            float lon = outerPolygon[i];
            float lat = outerPolygon[i+1];

            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
        }
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;

        setAboveWater();
    }

    public static HeightCurveElement mapToHeightCurveElement(ParserHeightCurveElement parserHeightCurveElement) {
        return new HeightCurveElement(
                forceCounterClockwise(parserHeightCurveElement.getCoordinates()),
                parserHeightCurveElement.getHeight()
        );
    }

    public boolean getIsAboveWater() {
        return isAboveWater;
    }

    public float[] getCoordinates() {
        return outerPolygon;
    }

    public float getHeight() {
        return height;
    }

    // Bounding Box and Path related functions
    public void addInnerPolygon(float[] innerPolygon) {
        innerPolygons.add(innerPolygon);
        heightCurvePath = null;
    }

    public void removeInnerPolygon(float[] innerPolygon) {
        innerPolygons.remove(innerPolygon);
        heightCurvePath = null;
    }

    public void removeAllInnerPolygons() {
        innerPolygons.clear();
        heightCurvePath = null;
    }

    /**
     * Checks whether this Height Curve containers another. It does this by first checking the bounding box, and then does ray-casting.
     * @param other other Height Curve to check if it is contained by this.
     * @return true if this contains the other Height Curve
     */
    public boolean contains(HeightCurveElement other) {
        if (
                minLon <= other.minLon &&
                minLat <= other.minLat &&
                maxLon >= other.maxLon &&
                maxLat >= other.maxLat
        ) {
            return PolygonUtils.contains(this.outerPolygon, other.outerPolygon);
        } else {
            return false;
        }
    }

    /**
     * Checks whether this Height Curve containers a point. It does this by first checking the bounding box, and then does ray-casting.
     * @param lon the longitude of the point.
     * @param lat the latitude of the point.
     * @return true if this contains the other Height Curve
     */
    public boolean containsPoint(float lon, float lat) {
        if (
                minLon <= lon &&
                minLat <= lat &&
                maxLon >= lon &&
                maxLat >= lat
        ) {
            return PolygonUtils.isPointInPolygon(this.outerPolygon, lon, lat);
        } else {
            return false;
        }
    }

    // State-related function
    public void setAboveWater() {
        isAboveWater = true;
        setStyleId((byte) 0);
    }

    public void setBelowWater() {
        isAboveWater = false;
        setStyleId((byte) 1);
    }

    public void setSelected() {
        setStyleId((byte) 2);
    }

    public void setUnselected() {
        setStyleId(isAboveWater ? (byte) 1 : 0);
    }

    @Override
    public float minLon() {
        return this.minLon;
    }

    @Override
    public float minLat() {
        return this.minLat;
    }

    @Override
    public float maxLon() {
        return this.maxLon;
    }

    @Override
    public float maxLat() {
        return this.maxLat;
    }

    @Override
    public byte getStyleId() {
        return this.styleId;
    }

    @Override
    public void setStyleId(byte styleId) {
        this.styleId = styleId;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        if (styleId < 0) setAboveWater();
        if (heightCurvePath == null) {
            this.heightCurvePath = new HeightCurvePath(outerPolygon, innerPolygons);
        }

        g2d.setColor(DrawingConfiguration.getInstance().getColor(styleId));
        if (DrawingConfiguration.getInstance().getStroke(styleId) == null) {
            g2d.fill(heightCurvePath);
        } else {
            g2d.setStroke(new BasicStroke(strokeBaseWidth * DrawingConfiguration.getInstance().getStroke(styleId)));
            g2d.draw(heightCurvePath);
        }
    }
}
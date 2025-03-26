package dk.itu.ui;

import dk.itu.data.services.Services;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static dk.itu.ui.FloodingApp.HEIGHT;

public class State {
    private final List<Consumer<Point2D.Double>> mouseMovedListeners = new ArrayList<>();
    private boolean shouldDrawGeoJson = true;
    private boolean shouldRedraw = true;
    private double mouseX, mouseY;
    private float waterLevel = 0f;
    private final float minWaterLevel, maxWaterLevel;
    private final SuperAffine superAffine = new SuperAffine();

    public State(Services services) {
        this.minWaterLevel = services.getGeoJsonService().getMinWaterLevel();
        this.maxWaterLevel = services.getGeoJsonService().getMaxWaterLevel();
        this.resetWindowBounds();
    }

    // Getter and setter for drawing GeoJson
    public void toggleShouldDrawGeoJson() {
        this.shouldDrawGeoJson = !this.shouldDrawGeoJson;
    }
    public boolean shouldDrawGeoJson() {
        return shouldDrawGeoJson;
    }

    public boolean shouldRedraw(){return shouldRedraw;}

    public void setShouldRedraw(boolean val){
        shouldRedraw = val;
    }

    // Calculates mouse lon/lat
    public Point2D.Double getMouseLonLat() {
        var point = superAffine.inverseTransform(mouseX, mouseY);
        return new Point2D.Double(point.getX()/0.56, -point.getY());
    }

    // Setters for mouse position
    public double getMouseX() {
        return mouseX;
    }
    public double getMouseY() {
        return mouseY;
    }

    // Mouse setter and move event listener
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        // Call event listeners with mouse lon lat
        var mouseLonLat = getMouseLonLat();
        for (var listener : mouseMovedListeners) {
            listener.accept(mouseLonLat);
        }
    }
    public void addOnMouseMovedListener(Consumer<Point2D.Double> listener) {
        mouseMovedListeners.add(listener);
    }

    // Getters for water max/min level
    public float getMinWaterLevel() {
        return minWaterLevel;
    }
    public float getMaxWaterLevel() {
        return maxWaterLevel;
    }

    // Getters and setters for limit of osm elements drawn
    public int getOsmLimit() {
        return 500;
    }

    // Getters and setters for water level
    public float getWaterLevel() {
        return waterLevel;
    }
    public void setWaterLevel(float waterLevel) {
        this.waterLevel = waterLevel;
    }

    // Getter for Affine
    public SuperAffine getSuperAffine() {
        return superAffine;
    }

    // Getter and updater for window bounds
    public double[] getWindowBounds() {
        var min = this.superAffine.inverseTransform(0, 0);
        var max = this.superAffine.inverseTransform(FloodingApp.WIDTH, FloodingApp.HEIGHT);
        return new double[] {min.getX()/0.56, -max.getY(), max.getX()/0.56, -min.getY()};
    }

    public void resetWindowBounds() {
        Services.withServices(this::resetWindowBounds);
    }

    public void resetWindowBounds(Services services) {
        var bounds = services.getOsmService().getBounds();
        double scale = HEIGHT / (bounds.maxLat() - bounds.minLat());
        getSuperAffine()
                .reset()
                .prependTranslation(
                        -0.56 * bounds.minLon(),
                        bounds.maxLat())
                .prependScale(
                        scale,
                        scale
                );
    }
}
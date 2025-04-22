package dk.itu.ui;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.services.Services;
import dk.itu.data.utils.DijkstraConfiguration;
import dk.itu.ui.drawables.NearestNeighbour;
import kotlin.Pair;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static dk.itu.ui.FloodingApp.HEIGHT;

public class State {
    private final List<Consumer<Point2D.Double>> mouseMovedListeners = new ArrayList<>();
    private final List<Consumer<Pair<Float, Float>>> minMaxWaterLevelListeners = new ArrayList<>();
    private boolean shouldDrawGeoJson = true;
    private final DijkstraConfiguration dijkstraConfiguration;
    private double mouseX, mouseY;
    private float waterLevel = 0f;
    private float minWaterLevel, maxWaterLevel;
    private final SuperAffine superAffine = new SuperAffine();
    private boolean showSelectedHeightCurve = false;
    private HeightCurveElement hcSelected = null;
    private NearestNeighbour selectedOsmElement = null;
    private boolean withDb = CommonConfiguration.getInstance().getUseDb();
    private boolean showNearestNeighbour = false;

    public State(Services services) {
        this.dijkstraConfiguration = new DijkstraConfiguration();
        this.minWaterLevel = services.getHeightCurveService().getMinWaterLevel();
        this.maxWaterLevel = services.getHeightCurveService().getMaxWaterLevel();
        this.resetWindowBounds();
    }

    public void setShowSelectedHeightCurve(boolean showSelectedHeightCurve) {
        this.showSelectedHeightCurve = showSelectedHeightCurve;
    }
    public boolean getShowSelectedHeightCurve() {
        return showSelectedHeightCurve;
    }

    public boolean getShowNearestNeighbour() {
        return showNearestNeighbour;
    }
    public void setShowNearestNeighbour(boolean showNearestNeighbour) {
        this.showNearestNeighbour = showNearestNeighbour;
    }

    public HeightCurveElement getHcSelected() {
        return hcSelected;
    }
    public void setHcSelected(HeightCurveElement hcSelected) {
        this.hcSelected = hcSelected;
    }

    public NearestNeighbour getNearestNeighbour() {
        return selectedOsmElement;
    }
    public void setSelectedOsmElement(OsmNode selectedOsmElement) {
        this.selectedOsmElement = selectedOsmElement == null ? null : new NearestNeighbour(selectedOsmElement, getMouseLonLat());
    }

    // Getter and setter for drawing GeoJson
    public void toggleShouldDrawGeoJson() {
        this.shouldDrawGeoJson = !this.shouldDrawGeoJson;
    }
    public boolean shouldDrawGeoJson() {
        return shouldDrawGeoJson;
    }

    // Getter and setter for dijkstra
    public DijkstraConfiguration getDijkstraConfiguration(){
        return dijkstraConfiguration;
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
    public void addMinMaxWaterLevelListener(Consumer<Pair<Float, Float>> listener) {
        minMaxWaterLevelListeners.add(listener);
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
        this.dijkstraConfiguration.setWaterLevel(waterLevel);
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
    public void updateMinMaxWaterLevels(Services services) {
        this.minWaterLevel = services.getHeightCurveService().getMinWaterLevel();
        this.maxWaterLevel = services.getHeightCurveService().getMaxWaterLevel();
        for (var listener : minMaxWaterLevelListeners) {
            listener.accept(new Pair<>(this.minWaterLevel, this.maxWaterLevel));
        }
    }

    public void resetWindowBounds(Services services) {
        var bounds = services.getOsmService(withDb).getBounds();
        double scale = HEIGHT / (bounds.getMaxLat() - bounds.getMinLat());
        getSuperAffine()
                .reset()
                .prependTranslation(
                        -0.56 * bounds.getMinLon(),
                        bounds.getMaxLat())
                .prependScale(
                        scale,
                        scale
                );
    }

    public boolean isWithDb() {
        return withDb;
    }

    public void setWithDb(boolean withDb) {
        // TODO: Use this in component
        this.withDb = withDb;
    }
}

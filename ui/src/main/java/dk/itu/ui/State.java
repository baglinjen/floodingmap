package dk.itu.ui;

public class State {
    private float waterLevel = 0f;
    private final float minWaterLevel, maxWaterLevel;
    private final SuperAffine superAffine = new SuperAffine();

    public State(float minWaterLevel, float maxWaterLevel) {
        this.minWaterLevel = minWaterLevel;
        this.maxWaterLevel = maxWaterLevel;
    }

    public float getMinWaterLevel() {
        return minWaterLevel;
    }
    public float getMaxWaterLevel() {
        return maxWaterLevel;
    }

    public int getOsmLimit() {
        return 500;
    }
    public void setWaterLevel(float waterLevel) {
        this.waterLevel = waterLevel;
    }
    public float getWaterLevel() {
        return waterLevel;
    }
    public SuperAffine getSuperAffine() {
        return superAffine;
    }
    public double[] getWindow() {
        var min = this.superAffine.inverseTransform(0, 0);
        var max = this.superAffine.inverseTransform(FloodingApp.WIDTH, FloodingApp.HEIGHT);
        return new double[] {min.getX()/0.56, -max.getY(), max.getX()/0.56, -min.getY()};
    }
    public float getStrokeBaseWidth() {
        return (float) (1/Math.sqrt(superAffine.getDeterminant()));
    }
}

package dk.itu.ui;

import java.util.function.Consumer;

public class State {
    private int osmLimit = 2000;
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
        return osmLimit;
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
    public float getStrokeBaseWidth() {
        return (float) (1/Math.sqrt(superAffine.getDeterminant()));
    }
    public void adjustOsmLimit(long timeTakenNano) {
        float timeTakenMs = timeTakenNano / 1000000f;
        if (timeTakenMs < 10f) {
            // Too fast
            osmLimit += Math.max(1, (int) (timeTakenMs/10f)*10);
        } else if (timeTakenMs > 20f) {
            // Too slow
            osmLimit -= Math.max(1, (int) (timeTakenMs/20f)*20);
        }
        if (osmLimit < 0) {
            osmLimit = 10;
        }
    }
}

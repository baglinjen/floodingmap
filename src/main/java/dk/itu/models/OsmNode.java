package dk.itu.models;

import org.jetbrains.annotations.NotNull;

public class OsmNode extends OsmElement {
    protected final long id;
    protected final float y, x;

    public OsmNode(long _id, float _y, float _x) {
        id = _id;
        y = _y;
        x = _x;
    }

    public long getId () {
        return id;
    }

    @Override
    public float getMinX() {
        return x;
    }

    @Override
    public float getMaxX() {
        return x;
    }

    @Override
    public float getMinY() {
        return y;
    }

    @Override
    public float getMaxY() {
        return y;
    }
}
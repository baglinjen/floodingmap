package dk.itu.models;

import jakarta.persistence.*;

@Entity
@Table(name="nodes")
public class OsmNode extends OsmElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "lat", nullable = false)
    private float lat;

    @Column(name = "lon", nullable = false)
    private float lon;

    public OsmNode(long _id, float _y, float _x) {
        id = _id;
        lat = _y;
        lon = _x;
    }

    public OsmNode(){}

    public long getId () {
        return id;
    }

    @Override
    public float getMinX() {
        return lon;
    }

    @Override
    public float getMaxX() {
        return lon;
    }

    @Override
    public float getMinY() {
        return lat;
    }

    @Override
    public float getMaxY() {
        return lat;
    }
}
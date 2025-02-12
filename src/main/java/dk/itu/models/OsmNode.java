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
    public float getMinLon() {
        return lon;
    }

    @Override
    public float getMaxLon() {
        return lon;
    }

    public void setLon(float lon) { this.lon = lon; }

    @Override
    public float getMinLat() {
        return lat;
    }

    @Override
    public float getMaxLat() {
        return lat;
    }

    public void setLat(float lat) { this.lat = lat; }
}
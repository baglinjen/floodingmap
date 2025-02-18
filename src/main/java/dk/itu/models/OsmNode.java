package dk.itu.models;

import jakarta.persistence.*;

@Entity
@Table(name="nodes")
public class OsmNode extends OsmElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "lat", nullable = false)
    private double lat;
    @Column(name = "lon", nullable = false)
    private double lon;

    public OsmNode() {}
    public OsmNode(long _id, double _y, double _x) {
        id = _id;
        lat = _y;
        lon = _x;
    }

    @Override
    public long getId () {
        return id;
    }
    @Override
    public double getMinLon() {
        return lon;
    }
    @Override
    public double getMaxLon() {
        return lon;
    }
    @Override
    public double getMinLat() {
        return lat;
    }
    @Override
    public double getMaxLat() {
        return lat;
    }
    @Override
    public double getArea() {
        return 0;
    }

    public void setId(long id) { this.id = id;}
    public void setLat(double lat) { this.lat = lat; }
    public void setLon(double lon) { this.lon = lon; }
}
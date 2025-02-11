package dk.itu.models.dbmodels;

import jakarta.persistence.*;

@Entity
@Table(name="nodes")
public class DbNode
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lon", nullable = false)
    private Double lon;

    public Long getId() { return id; }

    public Double getLat() { return lat; }
    public void setLat(Double lat ) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon ) { this.lon = lon; }
}

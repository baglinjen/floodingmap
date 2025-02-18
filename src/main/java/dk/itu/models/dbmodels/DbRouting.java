package dk.itu.models.dbmodels;

import jakarta.persistence.*;

@Entity
@Table(name="routings")
public class DbRouting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name="fromid")
    private Long fromid;

    @Column(name="toid")
    private Long toid;

    @Column(name="distancemeters")
    private Double distancemeters;

    @Column(name="lowestaltitude")
    private Double lowestaltitude;

    @Transient
    private Double elapsedDistance = 0.0;

    public int getId(){return id;}
    public Long getFromId(){return fromid;}
    public Long getToId(){return toid;}
    public Double getDistanceMeters(){return distancemeters;}
    public Double getLowestAltitude(){
        return (lowestaltitude == null ? 0.0 : lowestaltitude);
    }
    public Double getElapsedDistance(){return elapsedDistance;}
    public void setElapsedDistance(Double dist){elapsedDistance = dist;}
    public Double getTotalDistance(){return elapsedDistance + distancemeters;}
}

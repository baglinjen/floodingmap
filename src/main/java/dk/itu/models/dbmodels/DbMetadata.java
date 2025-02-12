package dk.itu.models.dbmodels;

import jakarta.persistence.*;

@Entity
@Table(name = "metadata")
public class DbMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "minlon", nullable = false)
    private double minlon;

    @Column(name = "maxlon", nullable = false)
    private double maxlon;

    @Column(name = "minlat", nullable = false)
    private double minlat;

    @Column(name = "maxlat", nullable = false)
    private double maxlat;

    public double getMinlat() {
        return minlat;
    }

    public double getMinlon(){
        return minlon;
    }

    public double getMaxlat() {
        return maxlat;
    }

    public double getMaxlon(){
        return maxlon;
    }
}

package dk.itu.models.dbmodels;

import dk.itu.utils.converters.CoordinateConverter;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "lines")
public class DbLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "altitude", nullable = false)
    private double altitude;

    @Column(name = "coords", columnDefinition = "jsonb")
    @Convert(converter = CoordinateConverter.class)
    private List<DbLineCoord> coords;

    public List<DbLineCoord> getCoords() { return coords; }

}


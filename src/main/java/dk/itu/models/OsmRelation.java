package dk.itu.models;

import dk.itu.utils.converters.JsonConverter;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "relations")
public class OsmRelation extends OsmElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "outerways", columnDefinition = "jsonb")
    @Convert(converter = JsonConverter.class)
    private List<Long> outerWaysIds;

    @Column(name = "innerways", columnDefinition = "jsonb")
    @Convert(converter = JsonConverter.class)
    private List<Long> innerWaysIds;

    @Transient
    private OsmWay[] outerWays = new OsmWay[0];

    @Transient
    private OsmWay[] innerWays = new OsmWay[0];

    @Transient
    private double minLon;

    @Transient
    private double maxLon;

    @Transient
    private double minLat;

    @Transient
    private double maxLat;

    // TODO: Consider if tags are necessary

    public long getId() { return id; }

    @Override
    public double getMinLon() {
        return 0;
    }

    public void setMinLon(double lon) { minLon = lon; }

    @Override
    public double getMaxLon() {
        return 0;
    }

    public void setMaxLon(double lon) { maxLon = lon; }

    @Override
    public double getMinLat() {
        return 0;
    }

    public void setMinLat(double lat) { minLat = lat; }

    @Override
    public double getMaxLat() {
        return 0;
    }

    @Override
    public double getArea() {
        return 0;
    }

    public void setMaxLat(double lat) { maxLat = lat; }

    public List<Long> getOuterWaysIds() { return outerWaysIds; }

    public void setOuterWays(List<OsmWay> ways) { outerWays = ways.toArray(new OsmWay[ways.size()]); }

    public List<Long> getInnerWaysIds() { return innerWaysIds; }

    public void setInnerWays(List<OsmWay> ways) { innerWays = ways.toArray(new OsmWay[ways.size()]); }

    public OsmWay[] getOuterWays() { return outerWays; }

    public OsmWay[] getInnerWays() { return innerWays; }
}
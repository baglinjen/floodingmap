package dk.itu.models;

import dk.itu.utils.converters.JsonConverter;
import jakarta.persistence.*;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

@Entity
@Table(name = "relations")
public class OsmRelation {

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

    // TODO: Consider if tags are necessary

    public long getId() { return id; }

    public List<Long> getOuterWaysIds() { return outerWaysIds; }

    public void setOuterWays(List<OsmWay> ways) { outerWays = ways.toArray(new OsmWay[ways.size()]); }

    public List<Long> getInnerWaysIds() { return innerWaysIds; }

    public void setInnerWays(List<OsmWay> ways) { innerWays = ways.toArray(new OsmWay[ways.size()]); }
}

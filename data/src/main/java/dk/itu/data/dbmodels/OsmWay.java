package dk.itu.data.dbmodels;

import dk.itu.common.models.osm.OsmElement;
import jakarta.persistence.*;

import java.awt.*;
import java.util.List;

@Entity
@Table(name = "ways")
public class OsmWay extends OsmElement {

    @ElementCollection
    @CollectionTable(name = "osm_way_nodes", joinColumns = @JoinColumn(name = "way_id"))
    @Column(name = "node_id")
    private List<Long> nodeIds;

    public OsmWay(Long id, List<Long> nodeIds) {
        super(id);
        this.nodeIds = nodeIds;
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(List<Long> nodeIds) {
        this.nodeIds = nodeIds;
    }

    @Override
    public double getArea() {
        return 0;
    }

    @Override
    public Shape getShape() {
        return null;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {

    }

    @Override
    public double[] getBounds() {
        return new double[0];
    }
}

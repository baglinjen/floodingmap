package dk.itu.models.dbmodels;

import dk.itu.utils.converters.JsonConverter;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ways")
public class DbWay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "Nodes", columnDefinition = "jsonb")
    @Convert(converter = JsonConverter.class)
    private List<Long> nodeIds;

    @Transient
    private List<DbNode> nodes;

    public Long getId() { return id; }

    public List<DbNode> getNodes() { return nodes; }
    public void setNodes(List<DbNode> nodes) { this.nodes = nodes; }

    public List<Long> getNodeIds() { return nodeIds; }
    public void setNodeIds(List<Long> nodeIds) { this.nodeIds = nodeIds; }
}

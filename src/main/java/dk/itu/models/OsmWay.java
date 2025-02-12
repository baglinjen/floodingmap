package dk.itu.models;

import dk.itu.utils.converters.JsonConverter;
import jakarta.persistence.*;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.List;

@Entity
@Table(name = "ways")
public class OsmWay implements OsmElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "Nodes", columnDefinition = "jsonb")
    @Convert(converter = JsonConverter.class)
    private List<Long> nodeIds;

    public List<Long> getNodeIds(){
        return nodeIds;
    }

    @Transient
    private OsmNode[] osmNodes = new OsmNode[0];

    public void setNodes(List<OsmNode> nodes){
        osmNodes = nodes.toArray(new OsmNode[nodes.size()]);
    }

    @Transient
public class OsmWay extends OsmElement {
    private final long id;
    private final OsmNode[] osmNodes;
    private float minX, minY, maxX, maxY;

    private Path2D.Float path;
    private final Shape shape;
    private final int color;

    public OsmWay(long _id, List<OsmNode> _osmNodes, int _color) {
        id = _id;
        osmNodes = _osmNodes.toArray(new OsmNode[0]);
        color = _color;

        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;

        for (OsmElement osmNode : _osmNodes) {
            if (osmNode.getMinX() < minX) {
                minX = osmNode.getMinX();
            } else if (osmNode.getMaxX() > maxX) {
                maxX = osmNode.getMaxX();
            }
            if (osmNode.getMinY() < minY) {
                minY = osmNode.getMinY();
            } else if (osmNode.getMaxY() > maxY) {
                maxY = osmNode.getMaxY();
            }
        }
    }

    //For deserialization
    public OsmWay(){

        Path2D.Float path = new Path2D.Float();
    }

    public void GeneratePath(){
        path = new Path2D.Float();

        path.moveTo(0.56* osmNodes[0].getMinX(), -osmNodes[0].getMinY());
        for (int i = 1; i < osmNodes.length; i+=1) {
            path.lineTo(0.56* osmNodes[i].getMinX(), -osmNodes[i].getMinY());
        }

        shape = osmNodes[0].equals(osmNodes[osmNodes.length - 1]) ? new Area(path) : path;
    }

    public OsmNode[] getOsmNodes() {
        return osmNodes;
    }

    @Override
    public long getId() {
        return id;
    }
    @Override
    public float getMinX() {
        return minX;
    }
    @Override
    public float getMaxX() {
        return maxX;
    }
    @Override
    public float getMinY() {
        return minY;
    }
    @Override
    public float getMaxY() {
        return maxY;
    }
    public Shape getShape() {
        return shape;
    }
    public int getColor() {
        return color;
    }
}
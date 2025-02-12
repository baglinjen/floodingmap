package dk.itu.models;

import dk.itu.utils.converters.JsonConverter;
import jakarta.persistence.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.List;

@Entity
@Table(name = "ways")
public class OsmWay extends OsmElement {
    private Path2D.Float path;
    private int color;
    @Transient
    private float minX, minY, maxX, maxY;
    @Transient
    private Shape shape;
    @Transient
    private OsmNode[] osmNodes = new OsmNode[0];

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "Nodes", columnDefinition = "jsonb")
    @Convert(converter = JsonConverter.class)
    private List<Long> nodeIds;

    public List<Long> getNodeIds(){
        return nodeIds;
    }

    public void setNodes(List<OsmNode> nodes){
        osmNodes = nodes.toArray(new OsmNode[nodes.size()]);
    }

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

        path = new Path2D.Float();

        path.moveTo(0.56* osmNodes[0].getMinX(), -osmNodes[0].getMinY());
        for (int i = 1; i < osmNodes.length; i+=1) {
            path.lineTo(0.56* osmNodes[i].getMinX(), -osmNodes[i].getMinY());
        }

        shape = osmNodes[0].equals(osmNodes[osmNodes.length - 1]) ? new Area(path) : path;
    }

    //For deserialization
    public OsmWay(){}

    public void GeneratePath(){

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
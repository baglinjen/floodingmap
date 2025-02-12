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
    @Transient
    private int color;
    @Transient
    private float minLon, minLat, maxLon, maxLat;
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

        minLon = Float.MAX_VALUE;
        minLat = Float.MAX_VALUE;
        maxLon = Float.MIN_VALUE;
        maxLat = Float.MIN_VALUE;

        for (OsmElement osmNode : _osmNodes) {
            if (osmNode.getMinLon() < minLon) {
                minLon = osmNode.getMinLon();
            } else if (osmNode.getMaxLon() > maxLon) {
                maxLon = osmNode.getMaxLon();
            }
            if (osmNode.getMinLat() < minLat) {
                minLat = osmNode.getMinLat();
            } else if (osmNode.getMaxLat() > maxLat) {
                maxLat = osmNode.getMaxLat();
            }
        }

        path = new Path2D.Float();

        path.moveTo(0.56* osmNodes[0].getMinLon(), -osmNodes[0].getMinLat());
        for (int i = 1; i < osmNodes.length; i+=1) {
            path.lineTo(0.56* osmNodes[i].getMinLon(), -osmNodes[i].getMinLat());
        }

        shape = osmNodes[0].equals(osmNodes[osmNodes.length - 1]) ? new Area(path) : path;
    }

    // For deserialization
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
    public float getMinLon() {
        return minLon;
    }
    @Override
    public float getMaxLon() {
        return maxLon;
    }
    @Override
    public float getMinLat() {
        return minLat;
    }
    @Override
    public float getMaxLat() {
        return maxLat;
    }
    public Shape getShape() {
        return shape;
    }
    public int getColor() {
        return color;
    }
}
package dk.itu.models;

import dk.itu.utils.converters.JsonConverter;
import jakarta.persistence.*;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

@Entity
@Table(name = "ways")
public class OsmWay extends OsmElement {
    private Path2D.Double path;
    @Transient
    private int color;
    @Transient
    private double minLon, minLat, maxLon, maxLat;
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

    public OsmWay(long _id, List<OsmNode> _osmNodes, int _color, Boolean shouldFill) {
        id = _id;

        if(id == 3003806L){
            System.out.println("Break here -> FAULTY");
        }

        if(id == 532264315L){
            System.out.println("Break here -> VALID");
        }

        osmNodes = _osmNodes.toArray(new OsmNode[0]);
        color = _color;

        minLon = Double.MAX_VALUE;
        minLat = Double.MAX_VALUE;
        maxLon = Double.MIN_VALUE;
        maxLat = Double.MIN_VALUE;

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

        path = new Path2D.Double();

        path.moveTo(0.56* osmNodes[0].getMinLon(), -osmNodes[0].getMinLat());
        for (int i = 1; i < osmNodes.length; i+=1) {
            path.lineTo(0.56* osmNodes[i].getMinLon(), -osmNodes[i].getMinLat());
        }

        if(shouldFill == null) shape = osmNodes[0].equals(osmNodes[osmNodes.length - 1]) ? new Area(path) : path;
        else shape = shouldFill ? new Area(path) : path;
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
    public double getMinLon() {
        return minLon;
    }
    @Override
    public double getMaxLon() {
        return maxLon;
    }
    @Override
    public double getMinLat() {
        return minLat;
    }
    @Override
    public double getMaxLat() {
        return maxLat;
    }
    public Shape getShape() {
        return shape;
    }

    public int getColor() {
        return color;
    }
}
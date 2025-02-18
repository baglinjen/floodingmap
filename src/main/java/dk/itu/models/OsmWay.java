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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Transient
    private double minLon, minLat, maxLon, maxLat;
    @Transient
    private Shape shape;
    @Transient
    private Color colorObj;
    @Transient
    private Integer stroke;
    @Transient
    private OsmNode[] osmNodes = new OsmNode[0];

    @Column(name = "Nodes", columnDefinition = "jsonb")
    @Convert(converter = JsonConverter.class)
    private List<Long> nodeIds;

    // For deserialization
    public OsmWay(){}
    public OsmWay(long _id, List<OsmNode> _osmNodes, DrawingConfig.Style _style) {
        this(_id, _osmNodes, _style, null);
    }
    public OsmWay(long _id, List<OsmNode> _osmNodes, DrawingConfig.Style _style, Boolean shouldFill) {
        id = _id;

        colorObj = new Color(_style.rgba(), true);
        stroke = _style.stroke();

        setNodes(_osmNodes).generateShape(shouldFill);

        calculateBounds();
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
    @Override
    public double getArea() {
        if (this.shape instanceof Area area) {
            // NOTE: If some elements aren't shown, consider calculating absolute area rather than bounding box
            var bounds = area.getBounds2D();
            return bounds.getWidth()*bounds.getHeight();
        }
        return 0;
    }
    public Shape getShape() {
        if(shape == null) generateShape(null);
        return shape;
    }
    public Color getColorObj() {
        return colorObj;
    }
    public BasicStroke getStrokeWidth(float basicStrokeSize) {
        // TODO: Cache BasicStroke on MapModel
        return new BasicStroke(basicStrokeSize * (stroke == null ? 1 : stroke));
    }
    public List<Long> getNodeIds(){
        return nodeIds;
    }
    public OsmWay setNodes(List<OsmNode> nodes){
        osmNodes = nodes.toArray(new OsmNode[nodes.size()]);
        return this;
    }
    public void generateShape(Boolean shouldFill){
        Path2D path = new Path2D.Double();

        path.moveTo(0.56* osmNodes[0].getMinLon(), -osmNodes[0].getMinLat());
        for (int i = 1; i < osmNodes.length; i++) {
            path.lineTo(0.56* osmNodes[i].getMinLon(), -osmNodes[i].getMinLat());
        }

        boolean isArea = (shouldFill != null) || osmNodes[0].equals(osmNodes[osmNodes.length - 1]);

        shape = isArea ? new Area(path) : path;
    }
    public void calculateBounds() {
        // TODO: Check if bounds are necessary
        minLon = Double.MAX_VALUE;
        minLat = Double.MAX_VALUE;
        maxLon = Double.MIN_VALUE;
        maxLat = Double.MIN_VALUE;

        for (OsmElement osmNode : this.osmNodes) {
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
    }
}
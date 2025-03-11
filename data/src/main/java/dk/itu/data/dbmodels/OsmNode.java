package dk.itu.data.dbmodels;

import dk.itu.common.models.osm.OsmElement;
import jakarta.persistence.*;

import java.awt.*;

@Entity
@Table(name = "nodes")
public class OsmNode extends OsmElement {

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    public OsmNode(Long id, double latitude, double longitude) {
        super(id);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {

    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public double[] getBounds() {
        return new double[0];
    }

    @Override
    public double getArea() {
        return 0;
    }

    @Override
    public Shape getShape() {
        return null;
    }
}

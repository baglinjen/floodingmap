package dk.itu.data.models.parser;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ParserOsmNode extends ParserOsmElement {
    private final double lat, lon;
    private final List<ParserOsmNode> connections = new ArrayList<>();

    public ParserOsmNode(long id, double lat, double lon) {
        super(id);
        this.lat = lat;
        this.lon = lon;
    }

    public void addConnection(ParserOsmNode connection){
        connections.add(connection);
    }

    public List<ParserOsmNode> getConnections(){return connections;}

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public double getArea() {
        return 0;
    }

    @Override
    public double[] getBounds() {
        return new double[]{lon, lat, lon, lat};
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) { /* Nodes are not drawn for now */ }
}
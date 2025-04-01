package dk.itu.data.models.db;
import java.awt.*;
import java.util.Map;

public class DbNode extends DbOsmElement {
    private final double lat, lon;
    private Map<Long, Double> connectionMap;

    public DbNode(long id, double lat, double lon) {
        super(id);
        this.lat = lat;
        this.lon = lon;
    }

    public void setConnectionMap(Map<Long, Double> connectionMap){
        this.connectionMap = connectionMap;
    }

    public Map<Long, Double> getConnectionMap(){
        return connectionMap;
    }

    public double getLat(){return lat;}
    public double getLon(){return lon;}

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {}
}
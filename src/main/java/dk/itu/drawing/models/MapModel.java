package dk.itu.drawing.models;

import dk.itu.drawing.components.BufferedMapComponent;
import dk.itu.drawing.utils.ShapeRasterizer;
import dk.itu.models.OsmElement;
import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

import static dk.itu.drawing.utils.ColorUtils.toARGB;

public abstract class MapModel {
    protected double minLon, minLat, maxLat;
    protected List<OsmElement> elements = new ArrayList<>();
    protected List<OsmElement> areaElements = new ArrayList<>();
    protected List<OsmElement> pathElements = new ArrayList<>();
    protected List<List<OsmElement>> layers = new ArrayList<>();
    private static final int BACKGROUND_COLOR = toARGB(Color.web("#aad3df"));

    public MapModel() {}

    public double getMinLon() {
        return minLon;
    }
    public double getMinLat() {
        return minLat;
    }
    public double getMaxLat() {
        return maxLat;
    }

    public void draw(BufferedMapComponent buffer) {
        buffer.clear(BACKGROUND_COLOR);
        layers.forEach(layer -> {
            layer.parallelStream().forEach(element -> {
                if (element instanceof OsmWay way) {
                    ShapeRasterizer.drawShapeInBuffer(way.getShape(), buffer, way.getColor());
                }
            });
        });
    }

    public void addLayer(List<OsmElement> layer)
    {
        layers.add(layer);
    }

    public void clearLayers() { layers.clear(); }

    public void draw(GraphicsContext gc) {
        layers.forEach(layer -> {
            layer.forEach(element -> {
                switch (element) {
                    case OsmWay way:
                        OsmNode[] osmNodes = way.getOsmNodes();

                        gc.beginPath();
                        gc.moveTo(0.56* osmNodes[0].getMinLon(), -osmNodes[0].getMinLat());

                        for (int i = 1; i < osmNodes.length; i+=1) {
                            gc.lineTo(0.56* osmNodes[i].getMinLon(), -osmNodes[i].getMinLat());
                        }

                        gc.setStroke(Color.BLACK);

                        gc.stroke();

                        break;
                    default:
                        break;
                }
            });
        });
    }
}

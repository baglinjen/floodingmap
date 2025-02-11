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

public class MapModel {
    private final float minX, minY, maxY;
    private final List<List<OsmElement>> layers = new ArrayList<>();
    private final List<OsmElement> ways;
    private static final int BACKGROUND_COLOR = toARGB(Color.web("#aad3df"));

    public MapModel(float minX, float minY, float maxY, List<OsmElement> ways) {
        this.minX = minX;
        this.minY = minY;
        this.maxY = maxY;
        this.ways = ways;
        this.layers.add(ways);
    }

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxY() {
        return maxY;
    }

    public void draw(BufferedMapComponent buffer) {
        buffer.clear(BACKGROUND_COLOR);
        layers.forEach(layer -> {
            layer.forEach(element -> {
                if (element instanceof OsmWay way) {
                    ShapeRasterizer.drawShapeInBuffer(way.getShape(), buffer, way.getColor());
                }
            });
        });
    }

    public void draw(GraphicsContext gc) {
        ways.forEach(element -> {
            switch (element) {
                case OsmWay way:
                    OsmNode[] osmNodes = way.getOsmNodes();

                    gc.beginPath();
                    gc.moveTo(0.56* osmNodes[0].getMinX(), -osmNodes[0].getMinY());

                    for (int i = 1; i < osmNodes.length; i+=1) {
                        gc.lineTo(0.56* osmNodes[i].getMinX(), -osmNodes[i].getMinY());
                    }

                    gc.setStroke(Color.BLACK);

                    gc.stroke();

                    break;
                default:
                    break;
            }
        });
    }
}

package dk.itu.drawing.components;

import dk.itu.drawing.models.MapModel;
import dk.itu.OsmParser;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

public class MapView extends StackPane {
    private final MapModel mapModel;
    private final Canvas canvas = new Canvas(1200, 600);
    private final GraphicsContext gc = canvas.getGraphicsContext2D();
    private final Affine affine = new Affine();
    private double mouseX, mouseY;

    public MapView() {
        super();

        // Get Model
        mapModel = OsmParser.parse("osm/bornholm.osm");

        // Set base zoom
        affine.prependTranslation(-0.56 * mapModel.getMinX(), mapModel.getMaxY());
        affine.prependScale(canvas.getHeight() / (mapModel.getMaxY() - mapModel.getMinY()), canvas.getHeight() / (mapModel.getMaxY() - mapModel.getMinY()));

        // Load events
        loadEventHandlers();

        // Add components to view
        this.getChildren().add(canvas);

        redraw();
    }

    private void loadEventHandlers() {
        canvas.setOnMousePressed(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });
        canvas.setOnMouseDragged(event -> {
            double dx = event.getX() - mouseX, dy = event.getY() - mouseY;
            affine.prependTranslation(dx, dy);
            mouseX = event.getX();
            mouseY = event.getY();

            redraw();
        });
        canvas.setOnScroll(event -> {
            double zoom = event.getDeltaY() > 0 ? 1.1 : 1/1.1;

            affine.prependTranslation(-event.getX(), -event.getY());
            affine.prependScale(zoom, zoom);
            affine.prependTranslation(event.getX(), event.getY());

            redraw();
        });
    }

    private void redraw() {
        gc.setStroke(null);
        gc.setTransform(new Affine());

        gc.setFill(Color.web("#aad3df"));
        gc.fillRect(0,0,canvas.getWidth(), canvas.getHeight());

        gc.setTransform(affine);
        gc.setLineWidth(1/Math.sqrt(affine.determinant()));

        mapModel.draw(gc);

    }
}

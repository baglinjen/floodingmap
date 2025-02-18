package dk.itu.drawing.components;

import dk.itu.drawing.LayerManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import java.awt.geom.Point2D;

public class MouseEventOverlayComponent extends BorderPane {
    private final LayerManager layerManager;
    private double mouseX, mouseY;
    private final Label mouseCoordinatesLabel = new Label();

    public MouseEventOverlayComponent(LayerManager layerManager, Node node) {
        super();
        this.layerManager = layerManager;

        // Set event handlers
        setOnMousePressed(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });
        setOnMouseDragged(event -> {
            double dx = event.getX() - mouseX, dy = event.getY() - mouseY;
            this.layerManager.updateOldAffine();
            this.layerManager.superAffine
                    .prependTranslation(dx, dy);
            mouseX = event.getX();
            mouseY = event.getY();
        });
        setOnScroll(event -> {
            double zoom = event.getDeltaY() > 0 ? 1.05 : 1/1.05;
            this.layerManager.updateOldAffine();
            this.layerManager.superAffine
                    .prependTranslation(-event.getX(), -event.getY())
                    .prependScale(zoom, zoom)
                    .prependTranslation(event.getX(), event.getY());
        });
        setOnMouseMoved(event -> {
            Point2D mousePoint = this.layerManager.superAffine.inverseTransform(event.getX(), event.getY());
            String formattedString = "Y:" + String.format("%.4f", -mousePoint.getY()) + " X:" + String.format("%.4f", mousePoint.getX()/0.56);
            mouseCoordinatesLabel.setText(formattedString);
        });

        // Add Visuals
        BorderPane bp = new BorderPane();
        bp.setRight(mouseCoordinatesLabel);
        bp.setLeft(node);
        bp.setPadding(new Insets(12));
        bp.setStyle("-fx-background-color: white;");
        setBottom(bp);
    }
}
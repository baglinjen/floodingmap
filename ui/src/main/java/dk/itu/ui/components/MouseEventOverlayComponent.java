package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.awt.geom.Point2D;

public class MouseEventOverlayComponent extends BorderPane {
    private double mouseX, mouseY;
    private final Label mouseCoordinatesLabel = new Label();

    public MouseEventOverlayComponent(State state) {
        super();

        // Set event handlers
        setOnMousePressed(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });
        setOnMouseDragged(event -> {
            double dx = event.getX() - mouseX, dy = event.getY() - mouseY;
            state.getSuperAffine().prependTranslation(dx, dy);
            mouseX = event.getX();
            mouseY = event.getY();
        });
        setOnScroll(event -> {
            double zoom = event.getDeltaY() > 0 ? 1.05 : 1/1.05;
            state.getSuperAffine()
                    .prependTranslation(-event.getX(), -event.getY())
                    .prependScale(zoom, zoom)
                    .prependTranslation(event.getX(), event.getY());
        });
        setOnMouseMoved(event -> {
            Point2D mousePoint = state.getSuperAffine().inverseTransform(event.getX(), event.getY());
            String formattedString = "Y:" + String.format("%.4f", -mousePoint.getY()) + " X:" + String.format("%.4f", mousePoint.getX()/0.56);
            mouseCoordinatesLabel.setText(formattedString);
        });

        // Add Visuals
        BorderPane bp = new BorderPane();
        bp.setPadding(new Insets(12));
        bp.setStyle("-fx-background-color: white;");
        bp.setRight(mouseCoordinatesLabel);
        bp.setLeft(new WaterScalerComponent(state));

        setBottom(bp);
    }
}
package dk.itu.drawing.components;

import javafx.scene.layout.BorderPane;
import javafx.scene.transform.Affine;

public class MouseEventOverlayComponent extends BorderPane {
    private final Affine affine;
    private double mouseX, mouseY;
    public MouseEventOverlayComponent(Affine _affine) {
        super();
        this.affine = _affine;
        setOpacity(0);
        setOnMousePressed(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });
        setOnMouseDragged(event -> {
            double dx = event.getX() - mouseX, dy = event.getY() - mouseY;
            affine.prependTranslation(dx, dy);
            mouseX = event.getX();
            mouseY = event.getY();
        });
        setOnScroll(event -> {
            double zoom = event.getDeltaY() > 0 ? 1.1 : 1/1.1;
            affine.prependTranslation(-event.getX(), -event.getY());
            affine.prependScale(zoom, zoom);
            affine.prependTranslation(event.getX(), event.getY());
        });
    }
}

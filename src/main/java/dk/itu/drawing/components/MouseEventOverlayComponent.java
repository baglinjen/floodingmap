package dk.itu.drawing.components;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MouseEventOverlayComponent extends BorderPane {
    private static final Logger logger = LogManager.getLogger();
    private final Affine affine;
    private double mouseX, mouseY;
    private final Label mouseCoordinatesLabel = new Label();

    public MouseEventOverlayComponent(Affine _affine) {
        super();
        this.affine = _affine;
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
        setOnMouseMoved(event -> {
            Point2D mousePoint = new Point2D(mouseX, mouseY);
            try {
                mousePoint = affine.inverseTransform(event.getX(), event.getY());
            } catch (NonInvertibleTransformException ex) {
                logger.fatal(ex);
            }

            String formattedString = "Y:" + String.format("%.4f", -mousePoint.getY()) + " X:" + String.format("%.4f", mousePoint.getX()/0.56);
            mouseCoordinatesLabel.setText(formattedString);
        });
        BorderPane bp = new BorderPane();
        bp.setRight(mouseCoordinatesLabel);
        bp.setPadding(new Insets(12));
        bp.setStyle("-fx-background-color: white;");
        setBottom(bp);
    }
}

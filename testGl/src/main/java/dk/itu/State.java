package dk.itu;

import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class State {
    private final Camera camera;
    // For mouse dragging
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean isDragging = false;

    public State(double[] bounds) {
        this.camera = new Camera(bounds);
    }

    public Camera getCamera() {
        return camera;
    }

    public void handleScroll(ScrollEvent event) {
        float zoomDelta = (float)(event.getDeltaY() * 0.05); // Adjust sensitivity as needed
//        camera.zoom(zoomDelta, (float)event.getX(), (float)event.getY());
        event.consume();
    }
    public void handleMousePressed(MouseEvent event) {
        lastMouseX = event.getX();
        lastMouseY = event.getY();
        isDragging = true;
        event.consume();
    }
    public void handleMouseDragged(MouseEvent event) {
        if (isDragging) {
            // Calculate screen delta
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;

            // Convert screen delta to world coordinates based on current zoom
//            float worldDeltaX = (float)(-deltaX / (getWidth() * camera.getZoomFactor()) * 360);
//            float worldDeltaY = (float)(deltaY / (getHeight() * camera.getZoomFactor()) * 180);

            // Move camera
//            camera.move(worldDeltaX, worldDeltaY);

            // Update last position
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            event.consume();
        }
    }
    public void handleMouseReleased(MouseEvent event) {
        isDragging = false;
        event.consume();
    }
}

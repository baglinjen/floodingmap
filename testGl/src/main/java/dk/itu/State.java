package dk.itu;

public class State {
    private static final float ZOOM_FACTOR = 0.1f;
    private float zoom = 0.0f, translateX = 0.0f, translateY = 0.0f;

    public void zoomOut() {
        zoom -= ZOOM_FACTOR;
    }
    public void zoomIn() {
        zoom += ZOOM_FACTOR;
    }

    public void goUp() {
        translateY += ZOOM_FACTOR;
    }

    public void goDown() {
        translateY -= ZOOM_FACTOR;
    }

    public void goLeft() {
        translateX -= ZOOM_FACTOR;
    }
    public void goRight() {
        translateX += ZOOM_FACTOR;
    }

    public float getZoom() {
        return zoom;
    }
    public float getTranslateX() {
        return translateX;
    }
    public float getTranslateY() {
        return translateY;
    }
}

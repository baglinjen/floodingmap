package dk.itu.ui;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import dk.itu.ui.components.MouseEventOverlayComponent;
import dk.itu.util.LoggerFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.almasb.fxgl.dsl.FXGLForKtKt.*;
import static dk.itu.util.DrawingUtils.bufferedImageToWritableImage;

public class RunningApplication extends GameApplication {
    private static final int WIDTH = 1920, HEIGHT = 920, OSM_LIMIT = 2000;
    private final Logger logger = LoggerFactory.getLogger();
    private Services services;

    private final SuperAffine superAffine = new SuperAffine();
    private BufferedImage image;
    private final ImageView view = new ImageView();

    private void renderLoop() {
        while (true) {
            long start = System.nanoTime();


            var osmElements = services.osmService.getOsmElementsToBeDrawn(OSM_LIMIT);
            var heightCurves = services.geoJsonService.getGeoJsonElementsToBeDrawn();


            float strokeBaseWidth = (float) (1/Math.sqrt(superAffine.getDeterminant()));

            image.flush();

            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

            g2d.setBackground(Color.decode("#a9d3de"));
            g2d.clearRect(0, 0, WIDTH, HEIGHT);
            g2d.setTransform(superAffine);

            osmElements.forEach(element -> element.draw(g2d, strokeBaseWidth));
            heightCurves.forEach(heightCurve -> heightCurve.draw(g2d, strokeBaseWidth));

            g2d.dispose();

            view.setImage(bufferedImageToWritableImage(image));

            logger.debug("Render loop took {} ms", String.format("%.3f", (System.nanoTime() - start) / 1000000f));
        }
    }

    @Override
    protected void initGame() {
        this.image = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
        this.superAffine
                .prependTranslation(-0.56 * services.osmService.getMinLon(), services.osmService.getMaxLat())
                .prependScale(HEIGHT / (services.osmService.getMaxLat() - services.osmService.getMinLat()), HEIGHT / (services.osmService.getMaxLat() - services.osmService.getMinLat()));
        StackPane root = new StackPane(
                view,
                new MouseEventOverlayComponent(this.superAffine)
        );
        addUINode(root);
        getExecutor().startAsync(() -> {
            renderLoop();
            getExecutor().startAsyncFX(() -> getGameController().exit());
        });
    }

    @Override
    protected void initSettings(GameSettings settings) {
        services = new Services();
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setTitle("Flooding Visualisation");
        settings.setVersion("1.0-SNAPSHOT");
        settings.setIntroEnabled(false);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
    }

    @Override
    protected void onUpdate(double tpf) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}

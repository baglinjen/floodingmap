package dk.itu.ui;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import dk.itu.data.models.db.DbRelation;
import dk.itu.data.models.db.DbWay;
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
    public static final int WIDTH = 1920, HEIGHT = 920;
    private final Logger logger = LoggerFactory.getLogger();
    private Services services;

    private State state;

    // Drawing related
    private BufferedImage image;
    private final ImageView view = new ImageView();

    private void renderLoop() throws InterruptedException {
        services.osmService.withOsmServiceConsumer(serviceOperations -> {
            while (true) {
                long start = System.nanoTime();

                var window = state.getWindow();
                var osmElements = serviceOperations.getOsmElementsToBeDrawn(state.getOsmLimit(), window[0], window[1], window[2], window[3]);
//                        .parallelStream().filter(e -> e instanceof DbWay).toList();
                var heightCurves = services.geoJsonService.getGeoJsonElementsToBeDrawn(state.getWaterLevel());

                float strokeBaseWidth = state.getStrokeBaseWidth();

                image.flush();

                Graphics2D g2d = image.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

                g2d.setBackground(Color.decode("#a9d3de"));
                g2d.clearRect(0, 0, WIDTH, HEIGHT);
                g2d.setTransform(state.getSuperAffine());

                osmElements.forEach(element -> element.draw(g2d, strokeBaseWidth));
                heightCurves.forEach(heightCurve -> heightCurve.draw(g2d, strokeBaseWidth));

                g2d.dispose();

                view.setImage(bufferedImageToWritableImage(image));

//            state.adjustOsmLimit(System.nanoTime() - start);

                logger.debug("Render loop took {} ms", String.format("%.3f", (System.nanoTime() - start) / 1000000f));
            }
        });
    }

    @Override
    protected void initGame() {
        this.image = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
        this.state = new State(services.geoJsonService.getMinWaterLevel(), services.geoJsonService.getMaxWaterLevel());
        this.state
                .getSuperAffine()
                .prependTranslation(-0.56 * services.osmService.getMinLon(), services.osmService.getMaxLat())
                .prependScale(HEIGHT / (services.osmService.getMaxLat() - services.osmService.getMinLat()), HEIGHT / (services.osmService.getMaxLat() - services.osmService.getMinLat()));
        StackPane root = new StackPane(
                view,
                new MouseEventOverlayComponent(state) // 997121, 14.9890798 54.9996782, 14.989
        );
        addUINode(root);
        getExecutor().startAsync(() -> {
            try {
                renderLoop();
            } catch (InterruptedException _) {}
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

    public static void main(String[] args) {
        launch(args);
    }
}

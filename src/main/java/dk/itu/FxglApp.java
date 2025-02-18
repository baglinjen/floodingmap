package dk.itu;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import dk.itu.drawing.LayerManager;
import dk.itu.drawing.components.MouseEventOverlayComponent;
import dk.itu.drawing.models.MapModel;
import dk.itu.services.DbService;
import dk.itu.services.modelservices.LineService;
import javafx.scene.Cursor;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.almasb.fxgl.dsl.FXGLForKtKt.*;

public class FxglApp extends GameApplication {
    // Logger
    Logger logger = LogManager.getLogger();
    // Constants
    public static final int W = 1920, H = 1080;
    private static final boolean SHOULD_LAZY_REDRAW = false;
    // Managers
    private LayerManager layerManager;
    // State
    private boolean firstDraw = true;

    private void renderLoop() throws InterruptedException {
        try {
            while (true) {
                if (!layerManager.hasTransformChanged() && !firstDraw) {
                    // No changes and it is not the first render => sleep 1 frame
                    Thread.sleep(16);
                    continue;
                } else {
                    if (firstDraw) {
                        // First draw => render without lazy
                        layerManager
                                .startPreparingLayers()
                                .awaitLayerPreparation()
                                .setLayerImages()
                                .awaitWithTimeout();
                        firstDraw = false;
                    } else if (SHOULD_LAZY_REDRAW) {
                        // Nth draw => render with lazy
                        // TODO: Test if lazy rendering is an optimization
                        layerManager
                                .startPreparingLayers()
                                .startPreparingLazyLayers()
                                .awaitLazyLayerPreparation()
                                .setLazyLayerImages()
                                .awaitLayerPreparation()
                                .setLayerImages();
                    } else {
                        // Nth draw => render without lazy
                        layerManager
                                .startPreparingLayers()
                                .awaitLayerPreparation()
                                .setLayerImages()
                                .awaitWithTimeout();
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.error("Layer preparation interrupted", e);
            throw RuntimeException.class.cast(e);
        }
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(W);
        settings.setHeight(H);
        settings.setTitle("Flooding Visualisation");
        settings.setVersion("");
    }

    @Override
    protected void initGame() {
        // Set cursor
        getGameScene().setCursor(Cursor.DEFAULT);
        // Load Models
        // Models
        DbService dbService = new DbService();
        MapModel mapModel = dbService.GenerateMapModel();
        mapModel.addLayer(LineService.LoadLinesFromDb(0));
//        MapModel mapModel = OsmParser.parse("osm/bornholm.osm", DrawingConfigParser.parse());
        // Create Layer Manager
        layerManager = new LayerManager(mapModel);
        // Create Components
        StackPane root = new StackPane();
        // Add background to root
        BorderPane background = new BorderPane();
        background.setPrefSize(W, H);
        background.setStyle("-fx-background-color: #aad3df");
        root.getChildren().add(background);
        // Add drawing layers to root
        root.getChildren().addAll(layerManager.getLayersAsImageViews());
        // Add event observer to root
        root.getChildren().add(new MouseEventOverlayComponent(layerManager));
        // Add root component to screen
        addUINode(root);

        // Start render loop
        getExecutor().startAsync(() -> {
            try {
                renderLoop();
            } catch (InterruptedException e) {
                logger.error("Render loop interrupted", e);
                throw new RuntimeException(e);
            }
            getExecutor().startAsyncFX(() -> getGameController().exit());
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
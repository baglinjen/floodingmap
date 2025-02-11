package dk.itu;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import dk.itu.drawing.models.MapModel;
import dk.itu.drawing.components.MouseEventOverlayComponent;
import dk.itu.drawing.components.BufferedMapComponent;
import dk.itu.models.dbmodels.DbNode;
import dk.itu.models.dbmodels.DbWay;
import dk.itu.services.DbService;
import dk.itu.services.modelservices.WayService;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Affine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.almasb.fxgl.dsl.FXGLForKtKt.*;

public class FxglApp extends GameApplication {
    // Logger
    private static final Logger logger = LogManager.getLogger();
    // Constants
    private static final int W = 1920, H = 1080;
    private static final int SUPER_SAMPLE_FACTOR = 2, BUFFER_SIZE = 3;
    // Buffers
    private final BlockingQueue<BufferedMapComponent> fullBuffers = new ArrayBlockingQueue<>(BUFFER_SIZE);
    private final BlockingQueue<BufferedMapComponent> emptyBuffers = new ArrayBlockingQueue<>(BUFFER_SIZE);
    private BufferedMapComponent currentBuffer;
    // Models
    private MapModel mapModel;
    // UI Components
    private StackPane root;

    public static void main(String[] args) {
        WayService service = new WayService();
        // List<DbNode> nodes = service.getNodesInWay(4120948);
        DbWay way = service.loadWayWithNodes(4120948L);
        System.out.println();

        launch(args);
    }

    private void renderLoop() {
        try {
            while (true) {
                BufferedMapComponent buffer = emptyBuffers.take();

                mapModel.draw(buffer);

                fullBuffers.add(buffer);
            }
        } catch (Exception e) {
            logger.fatal(e);
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
        mapModel = OsmParser.parse("osm/tuna.osm", DrawingConfigParser.parse());
        // Create Components
        Affine affine = new Affine();
        root = new StackPane(new MouseEventOverlayComponent(affine));
        // Add components
        addUINode(root);

        // Original scaling and translation
        affine.prependTranslation(-0.56 * mapModel.getMinX(), mapModel.getMaxY());
        affine.prependScale(H / (mapModel.getMaxY() - mapModel.getMinY()), H / (mapModel.getMaxY() - mapModel.getMinY()));

        for (int i = 0; i < BUFFER_SIZE; i++) {
            emptyBuffers.add(new BufferedMapComponent(W, H, SUPER_SAMPLE_FACTOR, affine));
        }

        getExecutor().startAsync(() -> {
            renderLoop();
            getExecutor().startAsyncFX(() -> getGameController().exit());
        });
    }

    @Override
    protected void onUpdate(double tpf) {
        try {
            BufferedMapComponent buffer = fullBuffers.take();

            root.getChildren().addFirst(buffer); // Show drawn image

            if (currentBuffer != null) {
                root.getChildren().remove(1); // Remove stale image
                emptyBuffers.add(currentBuffer);
            }

            currentBuffer = buffer.updateBuffer(); // TODO: Check what it does

        } catch (InterruptedException e) {
            logger.fatal(e);
        }
    }
}

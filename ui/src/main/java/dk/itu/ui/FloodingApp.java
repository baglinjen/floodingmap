package dk.itu.ui;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.common.models.GeoJsonElement;
import dk.itu.data.models.parser.ParserGeoJsonElement;
import dk.itu.data.services.Services;
import dk.itu.ui.components.MouseEventOverlayComponent;
import dk.itu.util.LoggerFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGLForKtKt.*;
import static dk.itu.util.DrawingUtils.bufferedImageToWritableImage;

public class FloodingApp extends GameApplication {
    public static final int WIDTH = 1920, HEIGHT = 920;
    private final Logger logger = LoggerFactory.getLogger();

    private volatile State state;
    private volatile boolean simulationRunning;

    // Drawing related
    private BufferedImage image;
    private final ImageView view = new ImageView();

    private void renderLoop() throws InterruptedException {
        Services.withServices(services -> {

            // Temporary whilst using in-memory
//            services.getGeoJsonService().loadGeoJsonData("tuna.geojson");
            services.getOsmService(state.isWithDb()).loadOsmData("bornholm.osm");
            state.resetWindowBounds();

            float registeredWaterLevel = 0.0f;

            while (true) {
                long start = System.nanoTime();

                var window = state.getWindowBounds();
                var osmElements = services
                        .getOsmService(state.isWithDb())
                        .getOsmElementsToBeDrawn(
                                state.getOsmLimit(),
                                window[0],
                                window[1],
                                window[2],
                                window[3]
                        );
                List<GeoJsonElement> heightCurves =
                        state.shouldDrawGeoJson() ?
                                services.getGeoJsonService().getGeoJsonElements()
                                : new ArrayList<>();

                float strokeBaseWidth = state.getSuperAffine().getStrokeBaseWidth();

                image.flush();

                Graphics2D g2d = image.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

                g2d.setBackground(Color.decode("#a9d3de"));
                g2d.clearRect(0, 0, WIDTH, HEIGHT);
                g2d.setTransform(state.getSuperAffine());

                osmElements.parallelStream().forEach(e -> e.prepareDrawing(g2d));
                osmElements.forEach(element -> element.draw(g2d, strokeBaseWidth));

                if(state.getWaterLevel() != registeredWaterLevel){
                    simulationRunning = false;

                    heightCurves.forEach(hc -> hc.setBelowWater(false));

                    simulationRunning = true;

                    Thread simulationThread = new Thread(() -> {
                        try{
                            var x = services.getGeoJsonService().getCurveTree().TraverseFromRoot(state.getWaterLevel());

                            for(List<ParserGeoJsonElement> list : x){
                                if(!simulationRunning) return;
                                Thread.sleep(500);
                                list.forEach(hc -> hc.setBelowWater(state.getWaterLevel() > hc.getHeight()));
                            }
                        } catch (Exception ex){
                            Thread.currentThread().interrupt();
                        }
                    });

                    simulationThread.start();

                    registeredWaterLevel = state.getWaterLevel();
                }

                heightCurves.parallelStream().forEach(e -> e.prepareDrawing(g2d));
                heightCurves.forEach(hc -> hc.draw(g2d, strokeBaseWidth));

                var dijkstraRoute = state.getDijkstraConfiguration().getRoute(state.getWaterLevel());
                if(dijkstraRoute != null){
                    dijkstraRoute.prepareDrawing(g2d);
                    dijkstraRoute.draw(g2d, strokeBaseWidth);
                }

                var startNode = state.getDijkstraConfiguration().getStartNode();
                if (startNode != null) {
                    g2d.setColor(Color.GREEN);
                    g2d.fill(new Ellipse2D.Double(0.56*startNode.getLon() - strokeBaseWidth*8/2, -startNode.getLat() - strokeBaseWidth*8/2, strokeBaseWidth*8, strokeBaseWidth*8));
                }
                var endNode = state.getDijkstraConfiguration().getEndNode();
                if (endNode != null) {
                    g2d.setColor(Color.RED);
                    g2d.fill(new Ellipse2D.Double(0.56*endNode.getLon() - strokeBaseWidth*8/2, -endNode.getLat() - strokeBaseWidth*8/2, strokeBaseWidth*8, strokeBaseWidth*8));
                }

                if (state.getShowNearestNeighbour()) {
                    var nn = state.getNearestNeighbour();
                    if (nn != null) {
                        nn.draw(g2d, strokeBaseWidth);
                    }
                }

                g2d.dispose();

                view.setImage(bufferedImageToWritableImage(image));

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
        Services.withServices(services -> {
            if (CommonConfiguration.getInstance().shouldForceParseOsm()) {
                services.getOsmService(state.isWithDb()).loadOsmData("tuna.osm");
            }
            // TODO: Load in DB using GeoJson service
            // if (CommonConfiguration.getInstance().shouldForceParseGeoJson()) {
            //     services.getGeoJsonService().loadGeoJsonData("modified-tuna.geojson");
            // }

            // Set State
            this.state = new State(services);
        });

        StackPane root = new StackPane(
                view,
                new MouseEventOverlayComponent(state)
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

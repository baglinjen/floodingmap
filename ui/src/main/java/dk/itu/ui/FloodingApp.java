package dk.itu.ui;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import dk.itu.common.models.Drawable;
import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.db.heightcurve.HeightCurveElement;
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
    private static final Logger logger = LoggerFactory.getLogger();

    private volatile State state;

    // Drawing related
    private BufferedImage image;
    private final ImageView view = new ImageView();

    // Simulation thread
    private Thread simulationThread;

    private void renderLoop() throws InterruptedException {
        Services.withServices(services -> {

            // Temporary whilst using in-memory
//            services.getOsmService(state.isWithDb()).loadOsmData("tuna.osm");
            services.getOsmService(state.isWithDb()).loadOsmData("bornholm.osm");
            state.resetWindowBounds();
            state.updateMinMaxWaterLevels(services);
//            var bounds = state.getWindowBounds();
//            services.getHeightCurveService().loadGmlData(bounds[0], bounds[1], bounds[2], bounds[3]);

            float registeredWaterLevel = 0.0f;

            while (true) {
                long start = System.nanoTime();

                var window = state.getWindowBounds();
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

                var osmElements = services
                        .getOsmService(state.isWithDb())
                        .getOsmElementsToBeDrawnScaled(
                                window[0],
                                window[1],
                                window[2],
                                window[3]
                        );
                List<BoundingBox> boundingBoxes = state.shouldDrawBoundingBox() ? services
                        .getOsmService(state.isWithDb())
                        .getBoundingBoxes() : new ArrayList<>();
                List<HeightCurveElement> heightCurves =
                        state.shouldDrawGeoJson() ?
                                services.getHeightCurveService().getElements()
                                : new ArrayList<>();

                if(state.getWaterLevel() != registeredWaterLevel) {
                    if (simulationThread != null && simulationThread.isAlive()) {
                        simulationThread.interrupt();
                    }

                    heightCurves.parallelStream().forEach(HeightCurveElement::setAboveWater);

                    simulationThread = new Thread(() -> {
                        try {
                            var floodingSteps = services.getHeightCurveService().getFloodingSteps(state.getWaterLevel());

                            for (List<HeightCurveElement> floodingStep : floodingSteps) {
                                Thread.sleep(500);
                                floodingStep.parallelStream().forEach(HeightCurveElement::setBelowWater);
                            }
                        } catch (Exception ex){
                            Thread.currentThread().interrupt();
                        }
                    });

                    simulationThread.start();

                    registeredWaterLevel = state.getWaterLevel();
                }

                // Prepare drawable elements
                osmElements.parallelStream().forEach(e -> e.prepareDrawing(g2d));
                heightCurves.parallelStream().forEach(e -> e.prepareDrawing(g2d));
                // Draw elements
                osmElements.stream().filter(Drawable::shouldDraw).forEach(element -> element.draw(g2d, strokeBaseWidth));
                heightCurves.stream().filter(Drawable::shouldDraw).forEach(hc -> hc.draw(g2d, strokeBaseWidth));
                boundingBoxes.forEach(bb -> bb.draw(g2d, strokeBaseWidth));

                // Draw dijkstra route if there is one
                var dijkstraRoute = state.getRoutingConfiguration().getRoute(state.isWithDb(), state.getWaterLevel());
                if (dijkstraRoute != null){
                    dijkstraRoute.prepareDrawing(g2d);
                    dijkstraRoute.draw(g2d, strokeBaseWidth);
                }

                if(state.getRoutingConfiguration().getShouldVisualize()){
                    var nodes = state.getRoutingConfiguration().getTouchedNodes();
                    for(var n : nodes){
                        g2d.setColor(Color.MAGENTA);
                        g2d.fill(new Ellipse2D.Double(0.56*n.getLon() - strokeBaseWidth*8/2, -n.getLat() - strokeBaseWidth*8/2, strokeBaseWidth*8, strokeBaseWidth*8));
                    }
                }

                var startNode = state.getRoutingConfiguration().getStartNode();
                if (startNode != null) {
                    g2d.setColor(Color.GREEN);
                    g2d.fill(new Ellipse2D.Double(0.56*startNode.getLon() - strokeBaseWidth*8/2, -startNode.getLat() - strokeBaseWidth*8/2, strokeBaseWidth*8, strokeBaseWidth*8));
                }
                var endNode = state.getRoutingConfiguration().getEndNode();
                if (endNode != null) {
                    g2d.setColor(Color.RED);
                    g2d.fill(new Ellipse2D.Double(0.56*endNode.getLon() - strokeBaseWidth*8/2, -endNode.getLat() - strokeBaseWidth*8/2, strokeBaseWidth*8, strokeBaseWidth*8));
                }

                // Draw nearest neighbour if there is one
                if (state.getShowNearestNeighbour()) {
                    var nn = state.getNearestNeighbour();
                    if (nn != null) {
                        nn.draw(g2d, strokeBaseWidth);
                    }
                }

                g2d.dispose();

                view.setImage(bufferedImageToWritableImage(image));

//                logger.debug("Render loop took {} ms", String.format("%.3f", (System.nanoTime() - start) / 1000000f));
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

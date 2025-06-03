package dk.itu.ui;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import dk.itu.common.models.Drawable;
import dk.itu.data.datastructure.rtree.RTreeNode;
import dk.itu.data.models.heightcurve.HeightCurveElement;
import dk.itu.data.services.Services;
import dk.itu.ui.components.MouseEventOverlayComponent;
import dk.itu.util.LoggerFactory;
import it.unimi.dsi.fastutil.floats.Float2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceSortedMap;
import it.unimi.dsi.fastutil.objects.*;
import javafx.scene.image.*;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.*;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.almasb.fxgl.dsl.FXGLForKtKt.*;

public class FloodingApp extends GameApplication {
    public static final int WIDTH = 1920, HEIGHT = 920;
    private static final Logger logger = LoggerFactory.getLogger();
    private volatile State state;

    // Drawing related
    private BufferedImagePoolManager bufferedImagePoolManager;
    private final PixelBuffer<IntBuffer> buffer = new PixelBuffer<>(
            WIDTH, HEIGHT,
            IntBuffer.allocate(WIDTH * HEIGHT),
            PixelFormat.getIntArgbPreInstance()
    );
    private final ImageView view = new ImageView();

    // Simulation thread
    private Thread simulationThread;

    private void renderLoop() throws InterruptedException {
        Services.withServices(services -> {

            // Temporary whilst using in-memory
            services.getOsmService(state.isWithDb()).loadOsmData("denmark.osm");

            state.resetWindowBounds();
            state.updateMinMaxWaterLevels(services);
            state.recalculateNodeHeight();

            float registeredWaterLevel = 0.0f;

            CompletableFuture<Void>[] dataFetchFutures = new CompletableFuture[3];

            List<Drawable> drawables = new ArrayList<>();

            Float2ReferenceSortedMap<Drawable> osmElements = new Float2ReferenceRBTreeMap<>();
            List<RTreeNode> spatialNodes = new ReferenceArrayList<>();
            List<HeightCurveElement> heightCurves = new ReferenceArrayList<>();

            try (ExecutorService executor = Executors.newCachedThreadPool()) {
                while (true) {
                    long start = System.nanoTime();

                    var window = state.getWindowBounds();

                    // Adding OSM Elements
                    dataFetchFutures[0] = CompletableFuture.runAsync(() -> {
                        osmElements.clear();
                        services
                                .getOsmService(state.isWithDb())
                                .getOsmElementsToBeDrawnScaled(
                                        window[0],
                                        window[1],
                                        window[2],
                                        window[3],
                                        osmElements
                                );
                    }, executor);

                    // Adding Bounding Boxes
                    dataFetchFutures[1] = CompletableFuture.runAsync(() -> {
                        spatialNodes.clear();
                        if (state.shouldDrawBoundingBox()) {
                            spatialNodes.addAll(
                                    services
                                            .getOsmService(state.isWithDb())
                                            .getSpatialNodes()
                            );
                        }
                    }, executor);

                    // Adding Height Curves
                    dataFetchFutures[2] = CompletableFuture.runAsync(() -> {
                        heightCurves.clear();
                        if (state.shouldDrawGeoJson()) {
                            heightCurves.addAll(
                                    services
                                            .getHeightCurveService()
                                            .getElements()
                            );
                        }
                    }, executor);

                    CompletableFuture.allOf(dataFetchFutures).join();

                    //Simulate water flooding animation
                    if (state.getWaterLevel() != registeredWaterLevel) {
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
                                state.setActualWaterLevel(state.getActualWaterLevel() + 2.5f);
                            }
                        } catch (Exception ex){
                            Thread.currentThread().interrupt();
                        }
                    });

                        simulationThread.start();

                        registeredWaterLevel = state.getWaterLevel();
                    }

                    // Add all to final drawables in order
                    drawables.clear();
                    drawables.addAll(osmElements.values());
                    drawables.addAll(spatialNodes);
                    drawables.addAll(heightCurves);

                    // Add routing if there is one
                    var dijkstraRoute = state.getRoutingConfiguration().getRoute(state.getActualWaterLevel());
                    if (dijkstraRoute != null){
                        drawables.add(dijkstraRoute);
                    }

                    // Add visited nodes from route search
                    if (state.getRoutingConfiguration().getShouldVisualize()) {
                        var nodes = state.getRoutingConfiguration().getTouchedNodes();
                        for (var n : nodes) {
                            drawables.add((g2d, strokeBaseWidth) -> {
                                g2d.setColor(Color.MAGENTA);
                                g2d.fill(new Ellipse2D.Double(0.56 * n.getLon() - strokeBaseWidth * 8 / 2, -n.getLat() - strokeBaseWidth * 8 / 2, strokeBaseWidth * 8, strokeBaseWidth * 8));
                            });
                        }
                    }

                    // Add start/end nodes from routing
                    var startNode = state.getRoutingConfiguration().getStartNode();
                    if (startNode != null) {
                        drawables.add((g2d, strokeBaseWidth) -> {
                            g2d.setColor(Color.GREEN);
                            g2d.fill(new Ellipse2D.Double(0.56 * startNode.getLon() - strokeBaseWidth * 8 / 2, -startNode.getLat() - strokeBaseWidth * 8 / 2, strokeBaseWidth * 8, strokeBaseWidth * 8));
                        });
                    }
                    var endNode = state.getRoutingConfiguration().getEndNode();
                    if (endNode != null) {
                        drawables.add((g2d, strokeBaseWidth) -> {
                            g2d.setColor(Color.RED);
                            g2d.fill(new Ellipse2D.Double(0.56 * endNode.getLon() - strokeBaseWidth * 8 / 2, -endNode.getLat() - strokeBaseWidth * 8 / 2, strokeBaseWidth * 8, strokeBaseWidth * 8));
                        });
                    }

                    // Add nearest neighbour if there is one
                    if (state.getShowNearestNeighbour()) {
                        var nn = state.getNearestNeighbour();
                        if (nn != null) {
                            drawables.add(nn);
                        }
                    }

                    // Draw all drawables in parallel
                    try {
                        bufferedImagePoolManager.drawElements(drawables);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }

                    // Transfer BufferedImage pixels to WritableImage
                    int[] pixels = ((DataBufferInt) bufferedImagePoolManager.getBufferedImage().getRaster().getDataBuffer()).getData();
                    System.arraycopy(pixels, 0, buffer.getBuffer().array(), 0, pixels.length);
                    view.setImage(new WritableImage(buffer));

//                    logger.debug("Render loop took {} ms", String.format("%.3f", (System.nanoTime() - start) / 1000000f));
                }
            }
        });
    }

    @Override
    protected void initGame() {
        Services.withServices(services -> {
            this.state = new State(services);
        });

        this.bufferedImagePoolManager = new BufferedImagePoolManager(this.state);

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
        settings.setTitle("Flooding visualisation tool for BSc project");
        settings.setVersion("1.0-SNAPSHOT");
        settings.setIntroEnabled(false);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
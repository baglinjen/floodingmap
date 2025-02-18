package dk.itu.drawing;

import com.almasb.fxgl.core.concurrent.AsyncTask;
import dk.itu.FxglApp;
import dk.itu.drawing.models.MapModel;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.almasb.fxgl.dsl.FXGLForKtKt.getExecutor;
import static dk.itu.utils.DrawingUtils.bufferedImageToWritableImage;

public class LayerManager {
    // Logger
    Logger logger = LogManager.getLogger();
    // Constants
    private static final int H = FxglApp.H, W = FxglApp.W;
    // Fields
    private final Map<Integer, LayerImageView> layers = new HashMap<>();
    private final MapModel mapModel;
    public final SuperAffine superAffine, oldSuperAffine;

    public LayerManager(MapModel mapModel) {
        this.mapModel = mapModel;

        // Create layers
        for (int i = 0; i < mapModel.layerCount(); i++) {
            ImageView img = new ImageView();
            img.setFitWidth(W);
            img.setFitHeight(H);
            img.setPreserveRatio(false);
            img.setSmooth(true);
            layers.put(i, new LayerManager.LayerImageView(null, img));
        }

        // Original scaling and translation
        this.superAffine = new SuperAffine()
                .prependTranslation(-0.56 * mapModel.getMinLon(), mapModel.getMaxLat())
                .prependScale(H / (mapModel.getMaxLat() - mapModel.getMinLat()), H / (mapModel.getMaxLat() - mapModel.getMinLat()));
        this.oldSuperAffine = new SuperAffine(this.superAffine);

        // Set image preparation lists
        preparedImages = new ArrayList<>(this.layers.size());
        lazyPreparedImages = new ArrayList<>(this.layers.size());
        // Reset images
        for (int i = 0; i < this.layers.size(); i++) {
            preparedImages.add(null);
            lazyPreparedImages.add(null);
        }
    }

    // Normal Layer Drawing functions
    private CountDownLatch prepareLatch;
    private ExecutorService executor;
    private Boolean hasLayerPreparationStarted = false;
    private final List<LayerWritableImage> preparedImages;
    public LayerManager startPreparingLayers() throws InterruptedException {
        int layerAmount = layers.size();

        // Latch to pre-render layers in parallel
        prepareLatch = new CountDownLatch(layerAmount);
        hasLayerPreparationStarted = true;

        // Prepare all layers concurrently
        for (int i = 0; i < layerAmount; i++) {
            final int layerIndex = i;
            getExecutor().startAsync(() -> {
                BufferedImage layerImage = mapModel.prepareLayer(layerIndex, superAffine);
                synchronized (preparedImages) {
                    preparedImages.set(layerIndex, new LayerWritableImage(layerImage, bufferedImageToWritableImage(layerImage)));
                }
                prepareLatch.countDown();
            });
        }

        return this;
    }
    public LayerManager awaitLayerPreparation() throws InterruptedException {
        if (!hasLayerPreparationStarted) {
            throw new IllegalStateException("Layer preparation has not started yet");
        }
        prepareLatch.await();
        return this;
    }
    public LayerManager setLayerImages() {
        executor = Executors.newFixedThreadPool(layers.size());
        for (int i = 0; i < layers.size(); i++) {
            int finalI = i;
            AsyncTask<?> redraw = getExecutor().startAsync(() -> {
                LayerWritableImage layerWritableImage = preparedImages.get(finalI);
                layers.get(finalI).imageView.setImage(layerWritableImage.writableImage);
                layers.get(finalI).setBufferedImage(layerWritableImage.bufferedImage);
            });
            executor.submit(redraw::await);
        }
        return this;
    }
    public void awaitWithTimeout() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(16, TimeUnit.MILLISECONDS)) {
            executor.shutdownNow();
            logger.warn("Some redraw operations did not complete within 16 seconds timeout");
        }
    }

    // Lazy Layer Drawing functions
    private CountDownLatch lazyPrepareLatch;
    private ExecutorService lazyExecutor;
    private Boolean hasLazyLayerPreparationStarted = false;
    private final List<LayerWritableImage> lazyPreparedImages;
    public LayerManager startPreparingLazyLayers() throws InterruptedException {
        int layerAmount = layers.size();

        // Latch to pre-render layers in parallel
        lazyPrepareLatch = new CountDownLatch(layerAmount);
        hasLazyLayerPreparationStarted = true;

        // Prepare all layers concurrently
        for (int i = 0; i < layerAmount; i++) {
            final int layerIndex = i;
            getExecutor().startAsync(() -> {
                BufferedImage layerImage = mapModel.prepareLazyLayer(layers.get(layerIndex).bufferedImage, oldSuperAffine, superAffine);
                synchronized (lazyPreparedImages) {
                    lazyPreparedImages.set(layerIndex, new LayerWritableImage(layerImage, bufferedImageToWritableImage(layerImage)));
                }
                lazyPrepareLatch.countDown();
            });
        }

        return this;
    }
    public LayerManager awaitLazyLayerPreparation() throws InterruptedException {
        if (!hasLazyLayerPreparationStarted) {
            throw new IllegalStateException("Layer preparation has not started yet");
        }
        lazyPrepareLatch.await();
        return this;
    }
    public LayerManager setLazyLayerImages() {
        if (lazyExecutor != null) {
            lazyExecutor.shutdownNow();
        }
        lazyExecutor = Executors.newFixedThreadPool(layers.size());
        for (int i = 0; i < layers.size(); i++) {
            int finalI = i;
            AsyncTask<?> redraw = getExecutor().startAsync(() -> {
                LayerWritableImage layerWritableImage = lazyPreparedImages.get(finalI);
                layers.get(finalI).imageView.setImage(layerWritableImage.writableImage);
                layers.get(finalI).setBufferedImage(layerWritableImage.bufferedImage);
            });
            lazyExecutor.submit(redraw::await);
        }
        return this;
    }

    // Utility functions
    public boolean hasTransformChanged() {
        return !oldSuperAffine.equals(superAffine);
    }
    public void updateOldAffine() {
        oldSuperAffine.setTransform(superAffine);
    }
    public List<ImageView> getLayersAsImageViews() {
        return layers.values().stream().map(v -> v.imageView).toList();
    }

    // Utility classes
    public record LayerWritableImage(BufferedImage bufferedImage, WritableImage writableImage) {}
    public static class LayerImageView {
        private BufferedImage bufferedImage;
        private final ImageView imageView;
        public LayerImageView(BufferedImage _bufferedImage, ImageView _imageView) {
            bufferedImage = _bufferedImage;
            imageView = _imageView;
        }
        public void setBufferedImage(BufferedImage newBufferedImage) {
            bufferedImage = newBufferedImage;
        }
    }
}
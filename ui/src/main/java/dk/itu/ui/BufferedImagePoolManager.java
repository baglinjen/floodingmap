package dk.itu.ui;

import com.google.common.collect.Lists;
import dk.itu.common.models.Drawable;
import dk.itu.data.models.heightcurve.HeightCurveElement;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static dk.itu.ui.FloodingApp.HEIGHT;
import static dk.itu.ui.FloodingApp.WIDTH;

public class BufferedImagePoolManager {
    private static final int ELEMENTS_PER_BUFFER = 1_000, ELEMENTS_BEFORE_SPLIT = ELEMENTS_PER_BUFFER * 2, THREADS = Runtime.getRuntime().availableProcessors();
    private final State state;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final BufferedImage finalImageToDrawTo = createBufferedImage();
    private final List<BufferedImage> bufferedImages = new LinkedList<>();

    public BufferedImagePoolManager(State state) {
        this.state = state;
    }

    public BufferedImage getBufferedImage() {
        return finalImageToDrawTo;
    }

    public <T extends Drawable> void drawElements(List<T> elements) throws ExecutionException, InterruptedException {
        final SuperAffine capturedTransform = new SuperAffine(state.getSuperAffine());

        // 60FPS is reached when 10_000 elements are drawn
        if (elements.size() > ELEMENTS_BEFORE_SPLIT) {
            drawElementsMultiBuffer(elements, capturedTransform);
        } else {
            drawElementsSingleBuffer(elements, capturedTransform);
        }
    }

    public <T extends Drawable> void drawElementsSingleBuffer(List<T> elements, SuperAffine capturedTransform) {
        float strokeBaseWidth = capturedTransform.getStrokeBaseWidth();

        var g2d = createGraphicsWaterBackgroundSingleBuffer(finalImageToDrawTo, capturedTransform);

        for (T element : elements) {
            element.draw(g2d, strokeBaseWidth);
        }
    }

    public <T extends Drawable> void drawElementsMultiBuffer(List<T> elements, SuperAffine capturedTransform) throws ExecutionException, InterruptedException {
        float strokeBaseWidth = capturedTransform.getStrokeBaseWidth();

        Graphics2D finalG2d = createGraphicsWaterBackground(finalImageToDrawTo);

        List<List<T>> partitions = Lists.partition(elements, elements.size() / THREADS);

        CompletableFuture<BufferedImage>[] bufferedImageFutures = new CompletableFuture[partitions.size()];

        for (int i = 0; i < partitions.size(); i++) {
            int finalI = i;
            bufferedImageFutures[i] = CompletableFuture.supplyAsync(() -> {
                BufferedImage img;
                synchronized (bufferedImages) {
                    img = pollBufferedImage();
                }
                Graphics2D g2d = createGraphicsTransparent(img, capturedTransform);

                for (T element : partitions.get(finalI)) {
                    element.draw(g2d, strokeBaseWidth);
                }

                g2d.dispose();
                return img;
            }, executor);
        }

        CompletableFuture.allOf(bufferedImageFutures).join();

        finalG2d.setComposite(AlphaComposite.SrcOver);

        for (CompletableFuture<BufferedImage> future : bufferedImageFutures) {
            BufferedImage img = future.get();
            finalG2d.drawImage(img, 0, 0, null);
            releaseBufferedImage(img);
        }

        finalG2d.dispose();
    }

    private synchronized BufferedImage pollBufferedImage() {
        if (bufferedImages.isEmpty()) {
            return createBufferedImage();
        }
        return bufferedImages.removeFirst();
    }
    private synchronized void releaseBufferedImage(BufferedImage img) {
        bufferedImages.add(img);
    }

    private BufferedImage createBufferedImage() {
        return GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
    }

    private Graphics2D createGraphicsWaterBackgroundSingleBuffer(BufferedImage image, SuperAffine capturedTransform) {
        Graphics2D g2d = createGraphicsWaterBackground(image);
        g2d.setTransform(capturedTransform);
        return g2d;
    }

    private Graphics2D createGraphicsWaterBackground(BufferedImage image) {
        Graphics2D g2d = image.createGraphics();
        applyQualitySettings(g2d);

        g2d.setBackground(Color.decode("#a9d3de"));
        g2d.clearRect(0, 0, WIDTH, HEIGHT);

        return g2d;
    }

    private Graphics2D createGraphicsTransparent(BufferedImage image, SuperAffine capturedTransform) {
        Graphics2D g2d = image.createGraphics();

        // Single, reliable clear operation
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Reset to normal drawing mode
        g2d.setComposite(AlphaComposite.SrcOver);
        applyQualitySettings(g2d);
        g2d.setTransform(capturedTransform);

        return g2d;
    }

    private void applyQualitySettings(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
    }
}
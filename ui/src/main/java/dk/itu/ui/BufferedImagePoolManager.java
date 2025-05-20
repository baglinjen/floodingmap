package dk.itu.ui;

import com.google.common.collect.Lists;
import dk.itu.common.models.Colored;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static dk.itu.ui.FloodingApp.HEIGHT;
import static dk.itu.ui.FloodingApp.WIDTH;

public class BufferedImagePoolManager {
    private static final int ELEMENTS_PER_BUFFER = 2_000, ELEMENTS_BEFORE_SPLIT = ELEMENTS_PER_BUFFER * 2;
    private final State state;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final BufferedImage finalImageToDrawTo = createBufferedImage();
    private final List<BufferedImage> bufferedImages = new LinkedList<>();

    public BufferedImagePoolManager(State state) {
        // Default 3 buffered images
        for (int i = 0; i < 3; i++) {
            bufferedImages.add(createBufferedImage());
        }
        this.state = state;
    }

    public BufferedImage getBufferedImage() {
        return finalImageToDrawTo;
    }

    public <T extends Colored> void drawElements(List<T> elements) throws ExecutionException, InterruptedException {
        if (elements.size() > ELEMENTS_BEFORE_SPLIT) {
            drawElementsMultiBuffer(elements);
        } else {
            drawElementsSingleBuffer(elements);
        }
    }

    public <T extends Colored> void drawElementsSingleBuffer(List<T> elements) {
        float strokeBaseWidth = state.getSuperAffine().getStrokeBaseWidth();

        var g2d = createGraphicsWaterBackgroundSingleBuffer(finalImageToDrawTo);

        for (T element : elements) {
            element.draw(g2d, strokeBaseWidth);
        }
    }

    public <T extends Colored> void drawElementsMultiBuffer(List<T> elements) throws ExecutionException, InterruptedException {
        float strokeBaseWidth = state.getSuperAffine().getStrokeBaseWidth();

        Graphics2D finalG2d = createGraphicsWaterBackground(finalImageToDrawTo);

        // Split as evenly as possibly with max of 1000
        var splits = elements.size() / ELEMENTS_PER_BUFFER;
        List<List<T>> partitions = Lists.partition(elements, elements.size() / splits);

        CompletableFuture<BufferedImage>[] bufferedImageFutures = new CompletableFuture[partitions.size()];

        for (int i = 0; i < partitions.size(); i++) {
            int finalI = i;
            bufferedImageFutures[i] = CompletableFuture.supplyAsync(() -> {
                List<T> partition = partitions.get(finalI);
                BufferedImage img;
                synchronized (bufferedImages) {
                    img = pollBufferedImage();
                }
                Graphics2D g2d = createGraphicsTransparent(img);

                for (T element : partition) {
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
    private void releaseBufferedImage(BufferedImage img) {
        bufferedImages.add(img);
    }

    private BufferedImage createBufferedImage() {
        return GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
    }

    private Graphics2D createGraphicsWaterBackgroundSingleBuffer(BufferedImage image) {
        Graphics2D g2d = image.createGraphics();
        applyQualitySettings(g2d);

        g2d.setBackground(Color.decode("#a9d3de"));
        g2d.clearRect(0, 0, WIDTH, HEIGHT);
        g2d.setTransform(state.getSuperAffine());

        return g2d;
    }

    private Graphics2D createGraphicsWaterBackground(BufferedImage image) {
        Graphics2D g2d = image.createGraphics();
        applyQualitySettings(g2d);

        g2d.setBackground(Color.decode("#a9d3de"));
        g2d.clearRect(0, 0, WIDTH, HEIGHT);

        return g2d;
    }

    private Graphics2D createGraphicsTransparent(BufferedImage image) {
        image.flush();

        Graphics2D g2d = image.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setComposite(AlphaComposite.SrcOver);
        applyQualitySettings(g2d);
        g2d.setTransform(state.getSuperAffine());

        return g2d;
    }

    private void applyQualitySettings(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
    }
}
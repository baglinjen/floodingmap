package dk.itu.drawing.models;

import dk.itu.FxglApp;
import dk.itu.drawing.SuperAffine;
import dk.itu.models.OsmElement;
import dk.itu.models.OsmWay;
import dk.itu.utils.TimeUtils;
import kotlin.Pair;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static dk.itu.drawing.SuperAffine.combineWithInverse;
import static dk.itu.utils.GeneralUtils.splitList;

public abstract class MapModel {
    protected static final int AREA_LAYERS = 8;
    protected double minLon, minLat, maxLat, maxLon;
    protected List<List<OsmElement>> layers = new ArrayList<>();
    private final GraphicsConfiguration gfxConfig = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration();

    public MapModel() {}

    public double getMinLon() {
        return minLon;
    }
    public double getMinLat() {
        return minLat;
    }
    public double getMaxLat() {
        return maxLat;
    }
    public int layerCount() {
        return layers.size();
    }
    public void addLayer(List<OsmElement> layer)
    {
        layers.add(layer);
    }
    public void removeTopLayer()
    {
        layers.removeLast();
    }

    public void sortAndSplitLayers(List<OsmElement> areaElements, List<OsmElement> pathElements) {
        AtomicReference<List<OsmElement>> atomicSortedAreaElements = new AtomicReference<>(new ArrayList<>());

        TimeUtils.timeFunction("Splitting layers", () -> {
            atomicSortedAreaElements.updateAndGet(list -> {
                list = areaElements.stream().parallel().sorted(Comparator.comparing(OsmElement::getArea).reversed()).collect(Collectors.toList());
                list.addAll(pathElements);
                return list;
            });
        });

        this.layers = splitList(atomicSortedAreaElements.get(), AREA_LAYERS);
    }

    public BufferedImage prepareLayer(int layerIndex, SuperAffine transform) {
        List<OsmElement> shapes = layers.get(layerIndex);
        var baseStrokeSize = (float) (1/Math.sqrt(transform.getDeterminant()));

        Pair<BufferedImage, Graphics2D> preparedLayerGraphics = prepareLayerGraphics(transform);
        BufferedImage image = preparedLayerGraphics.getFirst();
        Graphics2D g2d = preparedLayerGraphics.getSecond();

        // Draw each shape with its color
        for (OsmElement cs : shapes) {
            if (cs instanceof OsmWay way) {
                switch (way.getShape()) {
                    case Area _:
                        g2d.setColor(way.getColorObj());
                        g2d.fill(way.getShape());
                        break;
                    case Path2D _:
                        g2d.setColor(way.getColorObj());
                        g2d.setStroke(way.getStrokeWidth(baseStrokeSize));
                        g2d.draw(way.getShape());
                        break;
                    default:
                        break;
                }
            }
        }

        g2d.dispose();
        return image;
    }

    public BufferedImage prepareLazyLayer(BufferedImage oldBufferedImage, SuperAffine oldSuperAffine, SuperAffine superAffine) {
        Pair<BufferedImage, Graphics2D> preparedLayerGraphics = prepareLayerGraphics(combineWithInverse(oldSuperAffine, superAffine));
        BufferedImage image = preparedLayerGraphics.getFirst();
        Graphics2D g2d = preparedLayerGraphics.getSecond();

        g2d.drawImage(oldBufferedImage, 0, 0, null);

        g2d.dispose();

        oldSuperAffine.setTransform(superAffine);

        return image;
    }

    private Pair<BufferedImage, Graphics2D> prepareLayerGraphics(SuperAffine superAffine) {
        BufferedImage image = gfxConfig.createCompatibleImage(FxglApp.W, FxglApp.H, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

        g2d.setTransform(superAffine);

        return new Pair<>(image, g2d);
    }
}

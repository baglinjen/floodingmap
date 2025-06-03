package dk.itu.util.shape;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.itu.util.DrawingUtils.DRAWING_TOLERANCE;
import static dk.itu.util.DrawingUtils.calculateDistance;

public class RelationPath implements Shape {
    private static final AffineTransform projectionTransform = AffineTransform.getScaleInstance(0.56, -1);
    private final List<double[]> outerCoordinates, innerCoordinates;
    private final byte[] pointTypes;
    private final PathIterator pathIteratorNodeSkip;

    private int pathIteratorPointer = 0;
    private AffineTransform transform = new AffineTransform();

    public RelationPath(List<double[]> outerCoordinates, List<double[]> innerCoordinates) {
        this.outerCoordinates = outerCoordinates;
        if (this.outerCoordinates instanceof ArrayList<?>) ((ArrayList<?>) this.outerCoordinates).trimToSize();
        this.innerCoordinates = innerCoordinates;
        if (this.innerCoordinates instanceof ArrayList<?>) ((ArrayList<?>) this.innerCoordinates).trimToSize();

        int outerCoordinatesLength = outerCoordinates.stream().mapToInt(x -> x.length).sum();
        int innerCoordinatesLength = innerCoordinates.stream().mapToInt(x -> x.length).sum();

        // Assemble coordinates
        double[] coordinates = new double[outerCoordinatesLength + innerCoordinatesLength];
        this.pointTypes = new byte[coordinates.length / 2];
        int lastEmptyIndex = 0;
        for (double[] outerCoordinate : outerCoordinates) {
            System.arraycopy(outerCoordinate, 0, coordinates, lastEmptyIndex, outerCoordinate.length); // Copy coordinates to projected list
            pointTypes[lastEmptyIndex / 2] = (byte) 0; // moveTo
            Arrays.fill(pointTypes, lastEmptyIndex / 2 + 1, (lastEmptyIndex + outerCoordinate.length) / 2 - 1, (byte) 1); // lineTo
            pointTypes[(lastEmptyIndex + outerCoordinate.length) / 2 - 1] = (byte) 4; // closePath
            lastEmptyIndex += outerCoordinate.length;
        }
        for (double[] innerCoordinate : innerCoordinates) {
            System.arraycopy(innerCoordinate, 0, coordinates, lastEmptyIndex, innerCoordinate.length); // Copy coordinates to projected list
            pointTypes[lastEmptyIndex / 2] = (byte) 0; // moveTo
            Arrays.fill(pointTypes, lastEmptyIndex / 2 + 1, (lastEmptyIndex + innerCoordinate.length) / 2 - 1, (byte) 1); // lineTo
            pointTypes[(lastEmptyIndex + innerCoordinate.length) / 2 - 1] = (byte) 4; // closePath
            lastEmptyIndex += innerCoordinate.length;
        }

        double[] coordinatesProjected = new double[coordinates.length];
        projectionTransform.transform(coordinates, 0, coordinatesProjected, 0, coordinates.length / 2);

        // Create path iterator with node skipping
        this.pathIteratorNodeSkip = new PathIterator() {
            double lastCoordinateX, lastCoordinateY;

            @Override
            public int getWindingRule() {
                return Path2D.WIND_EVEN_ODD;
            }

            @Override
            public boolean isDone() {
                return pathIteratorPointer == pointTypes.length;
            }

            @Override
            public void next() {
                pathIteratorPointer++;
            }

            @Override
            public int currentSegment(double[] coords) {
                byte type = pointTypes[pathIteratorPointer];
                if (pathIteratorPointer == 0) {
                    // First point
                    transform.transform(coordinatesProjected, 0, coords, 0, 1);
                    lastCoordinateX = coords[0];
                    lastCoordinateY = coords[1];
                } else {
                    // Nth point
                    transform.transform(coordinatesProjected, pathIteratorPointer*2, coords, 0, 1);
                    var distanceToLastPoint = calculateDistance(lastCoordinateX, lastCoordinateY, coords[0], coords[1]);

                    while (distanceToLastPoint < DRAWING_TOLERANCE && type == 1) {
                        // Point not found and has not reached end of current polygon yet
                        pathIteratorPointer++;
                        type = pointTypes[pathIteratorPointer];
                        transform.transform(coordinatesProjected, pathIteratorPointer * 2, coords, 0, 1);
                        distanceToLastPoint = calculateDistance(lastCoordinateX, lastCoordinateY, coords[0], coords[1]);
                    }

                    if (distanceToLastPoint >= DRAWING_TOLERANCE) {
                        // Point was found and it should be tracked
                        lastCoordinateX = coords[0];
                        lastCoordinateY = coords[1];
                    }
                }
                return type;
            }

            @Override
            public int currentSegment(float[] coords) {
                throw new UnsupportedOperationException("Only double coords are supported.");
            }
        };
    }

    public List<double[]> getOuterPolygons() {
        return this.outerCoordinates;
    }
    public List<double[]> getInnerPolygons() {
        return this.innerCoordinates;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        this.pathIteratorPointer = 0;

        transform.setToScale(0.56, -1);
        this.transform = at;

        return pathIteratorNodeSkip;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        // This path iterator is never used
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Rectangle getBounds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Rectangle2D getBounds2D() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean contains(double x, double y) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean contains(Point2D p) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean contains(Rectangle2D r) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
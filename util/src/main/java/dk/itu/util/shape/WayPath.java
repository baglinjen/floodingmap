package dk.itu.util.shape;

import java.awt.*;
import java.awt.geom.*;
import java.util.Arrays;

import static dk.itu.util.DrawingUtils.DRAWING_TOLERANCE;
import static dk.itu.util.DrawingUtils.calculateDistance;
import static dk.itu.util.PolygonUtils.isClosed;

public class WayPath implements Shape {
    private static final AffineTransform transform = new AffineTransform();
    private final byte[] pointTypes;
    private final float[] outerCoordinates;
    private final PathIterator pathIteratorNodeSkip;

    private short pathIteratorPointer = 0;

    public WayPath(float[] coordinates) {
        this.outerCoordinates = coordinates;
        // Set point types
        this.pointTypes = new byte[coordinates.length / 2];
        Arrays.fill(pointTypes, (byte) 1); // 1 is lineTo
        this.pointTypes[0] = (byte) 0; // 0 is moveTo
        if (isClosed(coordinates)) this.pointTypes[this.pointTypes.length - 1] = (byte) 4; // 4 is closePath

        // Create path iterator with node skipping
        this.pathIteratorNodeSkip = new PathIterator() {
            float lastCoordinateX , lastCoordinateY; // Last transformed points that should be drawn

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
                    transform.transform(outerCoordinates, 0, coords, 0, 1);
                    lastCoordinateX = (float) coords[0];
                    lastCoordinateY = (float) coords[1];
                } else {
                    // Nth point
                    transform.transform(outerCoordinates, pathIteratorPointer*2, coords, 0, 1);
                    var distanceToLastPoint = calculateDistance(lastCoordinateX, lastCoordinateY, coords[0], coords[1]);

                    while (distanceToLastPoint < DRAWING_TOLERANCE && type == 1 && pathIteratorPointer < pointTypes.length-1) {
                        // Point not found and has not reached end of current polygon yet
                        pathIteratorPointer++;
                        type = pointTypes[pathIteratorPointer];
                        transform.transform(outerCoordinates, pathIteratorPointer*2, coords, 0, 1);
                        distanceToLastPoint = calculateDistance(lastCoordinateX, lastCoordinateY, coords[0], coords[1]);
                    }

                    if (distanceToLastPoint >= DRAWING_TOLERANCE) {
                        // Point was found and it should be tracked
                        lastCoordinateX = (float) coords[0];
                        lastCoordinateY = (float) coords[1];
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

    public float[] getOuterCoordinates() {
        return outerCoordinates;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        this.pathIteratorPointer = 0;

        transform.setToScale(0.56, -1);
        transform.preConcatenate(at);

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
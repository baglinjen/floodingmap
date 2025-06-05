package dk.itu.util.shape;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.itu.util.DrawingUtils.*;

public class HeightCurvePath implements Shape {
    private static final AffineTransform projectionTransform = AffineTransform.getScaleInstance(0.56, -1);
    private final PathIterator pathIteratorNodeSkip;

    private int pathIteratorPointer = 0;
    private AffineTransform transform = new AffineTransform();

    private final float[] coordinates;
    private int indexPolygon = -1, indexCoordinate = 0;
    private final List<float[]> innerPolygons = new ArrayList<>();
    private byte[] pointTypes;
    private final int outerPolygonLength;

    public HeightCurvePath(float[] outerPolygon) {
        // Create transformed inverted polygon
        this.outerPolygonLength = outerPolygon.length;
        this.coordinates = new float[outerPolygonLength];
        projectionTransform.transform(outerPolygon, 0, this.coordinates, 0, outerPolygonLength / 2);

        this.pointTypes = new byte[outerPolygonLength / 2];

        // Fill in outer polygon
        Arrays.fill(pointTypes, (byte) 1);
        pointTypes[0] = (byte) 0;
        pointTypes[pointTypes.length - 1] = (byte) 4;

        this.pathIteratorNodeSkip = new PathIterator() {
            float lastCoordinateX, lastCoordinateY;

            @Override
            public int getWindingRule() {
                return Path2D.WIND_EVEN_ODD;
            }

            @Override
            public boolean isDone() {
                return pathIteratorPointer >= pointTypes.length;
            }

            @Override
            public void next() {
                pathIteratorPointer++;

                // Stop here if end of shape has been reached
                if (pathIteratorPointer >= pointTypes.length) {
                    return;
                }

                if (indexPolygon >= 0) {
                    // If iterating through inner polygons => increase index coordinate by 2
                    indexCoordinate += 2;

                    // Bounds check: ensure we don't go out of bounds on current inner polygon
                    if (indexPolygon < innerPolygons.size() && indexCoordinate >= innerPolygons.get(indexPolygon).length) {
                        indexCoordinate = 0;
                        indexPolygon++;

                        // Check if we've run out of inner polygons
                        if (indexPolygon >= innerPolygons.size()) {
                            // We've finished all inner polygons, mark as done
                            pathIteratorPointer = pointTypes.length;
                            return;
                        }
                    } else if (indexPolygon >= innerPolygons.size()) {
                        // Safety check: if indexPolygon is invalid, mark as done
                        pathIteratorPointer = pointTypes.length;
                    }
                } else {
                    // Still in outer polygon
                    if (pathIteratorPointer * 2 >= coordinates.length) {
                        // Finished outer polygon, move to inner polygons
                        indexPolygon = 0;
                        indexCoordinate = 0;

                        // Check if there are actually inner polygons
                        if (innerPolygons.isEmpty()) {
                            // No inner polygons, we're done
                            pathIteratorPointer = pointTypes.length;
                        }
                    }
                }
            }

            @Override
            public int currentSegment(double[] coords) {
                byte type = pointTypes[pathIteratorPointer];

                if (pathIteratorPointer == 0) {
                    // First point
                    if (coordinates.length >= 2) {
                        transform.transform(coordinates, 0, coords, 0, 1);
                        lastCoordinateX = (float) coords[0];
                        lastCoordinateY = (float) coords[1];
                    }
                } else {
                    // Nth point
                    updateCoordinates(coords);

                    // Only skip points if we're in the middle of a polygon (type == 1)
                    // NEVER skip polygon start (0) or end (4) markers
                    if (type == 1) {
                        var distanceToLastPoint = calculateDistance(lastCoordinateX, lastCoordinateY, coords[0], coords[1]);

                        // While the next point is too close and still in middle of polygon
                        while (distanceToLastPoint < DRAWING_TOLERANCE_HC && type == 1) {

                            // Check what the next point type will be before advancing
                            byte nextType = pointTypes[pathIteratorPointer + 1];

                            // Don't skip if next point is start/end of polygon
                            if (nextType == 0 || nextType == 4) {
                                type = nextType;
                                break;
                            }

                            next();

                            // Bounds check after next()
                            if (pathIteratorPointer >= pointTypes.length) {
                                break;
                            }

                            type = pointTypes[pathIteratorPointer];
                            updateCoordinates(coords);
                            distanceToLastPoint = calculateDistance(lastCoordinateX, lastCoordinateY, coords[0], coords[1]);
                        }

                        // Update last coordinate only if we found a valid point
                        if (distanceToLastPoint >= DRAWING_TOLERANCE_HC || type != 1) {
                            lastCoordinateX = (float) coords[0];
                            lastCoordinateY = (float) coords[1];
                        }
                    } else {
                        // For polygon start/end markers, always update coordinates
                        lastCoordinateX = (float) coords[0];
                        lastCoordinateY = (float) coords[1];
                    }
                }

                return type;
            }

            private void updateCoordinates(double[] coords) {
                if (indexPolygon < 0) {
                    // Outer polygon
                    if (pathIteratorPointer * 2 + 1 < coordinates.length) {
                        transform.transform(coordinates, pathIteratorPointer*2, coords, 0, 1);
                    }
                } else {
                    // Inner polygon - add comprehensive bounds checking
                    if (indexPolygon < innerPolygons.size() && indexCoordinate + 1 < innerPolygons.get(indexPolygon).length) {
                        projectionTransform.transform(innerPolygons.get(indexPolygon), indexCoordinate, coords, 0, 1);
                        transform.transform(coords, 0, coords, 0, 1);
                    }
                }
            }

            @Override
            public int currentSegment(float[] coords) {
                throw new UnsupportedOperationException("Only double coords are supported.");
            }
        };
    }

    public float[] getOuterPolygon() {
        try {
            float[] outerToReturn = new float[outerPolygonLength];
            projectionTransform.createInverse().transform(coordinates, 0, outerToReturn, 0, outerPolygonLength / 2);
            return outerToReturn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addInnerPolygon(float[] innerPolygon) {
        // Reset iterator state when modifying structure
        indexPolygon = -1;
        indexCoordinate = 0;
        pathIteratorPointer = 0;

        innerPolygons.add(innerPolygon);

        // Create copy of point types with space for new inner polygon
        int oldPointTypesLength = pointTypes.length;
        int pointTypesForInnerPolygonCount = innerPolygon.length / 2;
        this.pointTypes = Arrays.copyOf(this.pointTypes, oldPointTypesLength + pointTypesForInnerPolygonCount);

        Arrays.fill(pointTypes, oldPointTypesLength, oldPointTypesLength + pointTypesForInnerPolygonCount, (byte) 1);
        this.pointTypes[oldPointTypesLength] = (byte) 0; // Start
        this.pointTypes[oldPointTypesLength + pointTypesForInnerPolygonCount - 1] = (byte) 4; // End
    }

    public void removeInnerPolygon(float[] innerPolygon) {
        // Reset iterator state when modifying structure
        indexPolygon = -1;
        indexCoordinate = 0;
        pathIteratorPointer = 0;

        int indexOfInnerPolygonToRemove = -1;
        for (int i = 0; i < innerPolygons.size(); i++) {
            if (Arrays.equals(innerPolygons.get(i), innerPolygon)) {
                indexOfInnerPolygonToRemove = i;
                break;
            }
        }

        if (indexOfInnerPolygonToRemove < 0) {
            return; // Polygon attempted to be removed wasn't present
        }

        // Calculate coordinate index BEFORE removing from the list
        int coordinateIndexOfPolygon = outerPolygonLength;
        for (int i = 0; i < indexOfInnerPolygonToRemove; i++) {
            coordinateIndexOfPolygon += innerPolygons.get(i).length;
        }

        // NOW remove the inner polygon
        innerPolygons.remove(indexOfInnerPolygonToRemove);

        // Remove it from the current pointTypes
        int startIndexToRemove = coordinateIndexOfPolygon / 2;
        int startAfterRemoval = startIndexToRemove + (innerPolygon.length / 2);
        byte[] newPointTypes = new byte[this.pointTypes.length - (innerPolygon.length / 2)];
        System.arraycopy(this.pointTypes, 0, newPointTypes, 0, startIndexToRemove);
        System.arraycopy(this.pointTypes, startAfterRemoval, newPointTypes, startIndexToRemove, this.pointTypes.length - startAfterRemoval);

        this.pointTypes = newPointTypes;
    }

    public void removeAllInnerPolygons() {
        indexPolygon = -1;
        indexCoordinate = 0;
        pathIteratorPointer = 0;

        innerPolygons.clear();
        this.pointTypes = Arrays.copyOf(this.pointTypes, this.outerPolygonLength / 2);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        this.pathIteratorPointer = 0;
        this.indexPolygon = -1;
        this.indexCoordinate = 0;
        this.transform = at != null ? at : new AffineTransform();
        return pathIteratorNodeSkip;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
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
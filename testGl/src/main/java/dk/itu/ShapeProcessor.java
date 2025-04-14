package dk.itu;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision;

public class ShapeProcessor {

    public static class VertexAndIndexData {
        private float[] vertices;
        private int[] polygonIndices;
        private int[] lineIndices;

        public VertexAndIndexData(float[] vertices, int[] polygonIndices, int[] lineIndices) {
            this.vertices = vertices;
            this.polygonIndices = polygonIndices;
            this.lineIndices = lineIndices;
        }

        public float[] getVertices() {
            return vertices;
        }

        public int[] getPolygonIndices() {
            return polygonIndices;
        }

        public int[] getLineIndices() {
            return lineIndices;
        }
    }

    /**
     * Process a list of drawable elements into vertices and indices
     */
    public static VertexAndIndexData processDrawableElements(List<DrawableElement> elements) {
        List<Float> allVertices = new ArrayList<>();
        List<Integer> polygonIndices = new ArrayList<>();
        List<Integer> lineIndices = new ArrayList<>();

        int vertexOffset = 0;

        // Process each drawable element
        for (DrawableElement element : elements) {
            if (element.isLine()) {
                // Process as a line
                vertexOffset = processLineElement(element, allVertices, lineIndices, vertexOffset);
            } else {
                // Process as a polygon
                List<float[]> outerPolygons = element.getCoordinates();
                float[] color = element.getColor();
                List<float[]> innerPolygons = element.getInnerPolygons();

                // Process each outer polygon in the element
                for (float[] outerCoords : outerPolygons) {
                    // Process the polygon and get its vertices and indices
                    VertexAndIndexData polygonData = processPolygonElement(outerCoords, innerPolygons, color);
                    float[] polygonVertices = polygonData.getVertices();
                    int[] polygonElementIndices = polygonData.getPolygonIndices();

                    // Add vertices to the all vertices list
                    for (float v : polygonVertices) {
                        allVertices.add(v);
                    }

                    // Add polygon indices with the appropriate offset
                    for (int index : polygonElementIndices) {
                        polygonIndices.add(index + vertexOffset);
                    }

                    // Update vertex offset for the next polygon
                    vertexOffset += polygonVertices.length / 6; // Each vertex has 6 values
                }
            }
        }

        // Convert list to array
        float[] vertices = new float[allVertices.size()];
        for (int i = 0; i < allVertices.size(); i++) {
            vertices[i] = allVertices.get(i);
        }

        int[] polygonIndicesArray = new int[polygonIndices.size()];
        for (int i = 0; i < polygonIndices.size(); i++) {
            polygonIndicesArray[i] = polygonIndices.get(i);
        }

        int[] lineIndicesArray = new int[lineIndices.size()];
        for (int i = 0; i < lineIndices.size(); i++) {
            lineIndicesArray[i] = lineIndices.get(i);
        }

        return new VertexAndIndexData(vertices, polygonIndicesArray, lineIndicesArray);
    }

    /**
     * Process a line element and add its vertices and indices
     * Returns the updated vertex offset
     */
    private static int processLineElement(DrawableElement element, List<Float> vertices, List<Integer> lineIndices, int vertexOffset) {
        List<float[]> coordsList = element.getCoordinates();
        float[] color = element.getColor();
        int currentOffset = vertexOffset;

        // Process each line segment in the element
        for (float[] coords : coordsList) {
            // For each vertex in the line
            for (int i = 0; i < coords.length; i += 2) {
                // Add vertex coordinates (x, y, z)
                vertices.add(coords[i]);       // x
                vertices.add(coords[i + 1]);   // y
                vertices.add(0.0f);            // z (assuming 2D shapes)

                // Add color (r, g, b)
                vertices.add(color[0]);
                vertices.add(color[1]);
                vertices.add(color[2]);
            }

            // Create line segments connecting consecutive vertices
            for (int i = 0; i < coords.length / 2 - 1; i++) {
                lineIndices.add(currentOffset + i);        // Start of segment
                lineIndices.add(currentOffset + i + 1);    // End of segment
            }

            // Update the current offset for the next line
            currentOffset += coords.length / 2;
        }

        return currentOffset;
    }

    /**
     * Process a polygon element
     */
    private static VertexAndIndexData processPolygonElement(float[] outerCoords, List<float[]> innerPolygons, float[] color) {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Create a JTS Geometry from the coordinates
        GeometryFactory gf = new GeometryFactory();

        // Convert outer polygon to JTS Coordinate array
        Coordinate[] outerCoordinates = convertToJTSCoordinates(outerCoords);

        // Create a linear ring for the outer polygon
        LinearRing outerRing = gf.createLinearRing(outerCoordinates);

        // Convert inner polygons to JTS LinearRings
        LinearRing[] holes = new LinearRing[innerPolygons.size()];
        for (int i = 0; i < innerPolygons.size(); i++) {
            Coordinate[] innerCoordinates = convertToJTSCoordinates(innerPolygons.get(i));
            holes[i] = gf.createLinearRing(innerCoordinates);
        }

        // Create a polygon with holes
        Polygon polygon = gf.createPolygon(outerRing, holes);

        // Use JTS triangulation to triangulate the polygon
        DelaunayTriangulationBuilder triangulator = new DelaunayTriangulationBuilder();
        triangulator.setSites(polygon);

        QuadEdgeSubdivision subdivision = triangulator.getSubdivision();
        GeometryCollection triangles = (GeometryCollection) subdivision.getTriangles(gf);

        // Process each triangle in the triangulation that is within the polygon
        for (int i = 0; i < triangles.getNumGeometries(); i++) {
            Geometry triangle = triangles.getGeometryN(i);

            // Check if the triangle is inside the polygon
            if (polygon.contains(triangle.getInteriorPoint())) {
                Coordinate[] triangleCoords = triangle.getCoordinates();

                // Add the three vertices of the triangle
                int vertexIndexBase = vertices.size() / 6;

                for (int j = 0; j < 3; j++) {
                    // Add vertex coordinates (x, y, z)
                    vertices.add((float) triangleCoords[j].x);
                    vertices.add((float) triangleCoords[j].y);
                    vertices.add(0.0f); // z-coordinate (assuming 2D shapes)

                    // Add color (r, g, b)
                    vertices.add(color[0]);
                    vertices.add(color[1]);
                    vertices.add(color[2]);

                    // Add index
                    indices.add(vertexIndexBase + j);
                }
            }
        }

        // Convert lists to arrays
        float[] verticesArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = vertices.get(i);
        }

        int[] indicesArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }

        return new VertexAndIndexData(verticesArray, indicesArray, new int[0]);
    }

    /**
     * Convert [lon1, lat1, lon2, lat2, ...] to JTS Coordinates
     */
    private static Coordinate[] convertToJTSCoordinates(float[] coords) {
        Coordinate[] jtsCoords = new Coordinate[coords.length / 2];

        for (int i = 0; i < coords.length; i += 2) {
            jtsCoords[i / 2] = new Coordinate(coords[i], coords[i + 1]);
        }

        return jtsCoords;
    }

    /**
     * Alternative implementation for simple polygons using ear-clipping
     */
    public static VertexAndIndexData processElementsSimple(List<DrawableElement> elements) {
        List<Float> allVertices = new ArrayList<>();
        List<Integer> polygonIndices = new ArrayList<>();
        List<Integer> lineIndices = new ArrayList<>();

        int vertexOffset = 0;

        for (DrawableElement element : elements) {
            List<float[]> coordsList = element.getCoordinates();
            float[] color = element.getColor();

            if (element.isLine()) {
                // Process as a line
                for (float[] coords : coordsList) {
                    // For each vertex in the line
                    for (int i = 0; i < coords.length; i += 2) {
                        // Add vertex coordinates (x, y, z)
                        allVertices.add(coords[i]);       // x
                        allVertices.add(coords[i + 1]);   // y
                        allVertices.add(0.0f);            // z (assuming 2D shapes)

                        // Add color (r, g, b)
                        allVertices.add(color[0]);
                        allVertices.add(color[1]);
                        allVertices.add(color[2]);
                    }

                    // Create line segments connecting consecutive vertices
                    for (int i = 0; i < coords.length / 2 - 1; i++) {
                        lineIndices.add(vertexOffset + i);        // Start of segment
                        lineIndices.add(vertexOffset + i + 1);    // End of segment
                    }

                    vertexOffset += coords.length / 2;
                }
            } else {
                // Process as a polygon
                for (float[] coords : coordsList) {
                    // For each coordinate pair in the outer polygon
                    for (int i = 0; i < coords.length; i += 2) {
                        // Add vertex coordinates (x, y, z)
                        allVertices.add(coords[i]);       // x
                        allVertices.add(coords[i + 1]);   // y
                        allVertices.add(0.0f);            // z (assuming 2D shapes)

                        // Add color (r, g, b)
                        allVertices.add(color[0]);
                        allVertices.add(color[1]);
                        allVertices.add(color[2]);
                    }

                    // Simple triangulation for convex polygons
                    // This works only for convex polygons
                    int vertexCount = coords.length / 2;
                    for (int i = 1; i < vertexCount - 1; i++) {
                        polygonIndices.add(vertexOffset);
                        polygonIndices.add(vertexOffset + i);
                        polygonIndices.add(vertexOffset + i + 1);
                    }

                    vertexOffset += vertexCount;
                }
            }
        }

        // Convert lists to arrays
        float[] vertices = new float[allVertices.size()];
        for (int i = 0; i < allVertices.size(); i++) {
            vertices[i] = allVertices.get(i);
        }

        int[] polygonIndicesArray = new int[polygonIndices.size()];
        for (int i = 0; i < polygonIndices.size(); i++) {
            polygonIndicesArray[i] = polygonIndices.get(i);
        }

        int[] lineIndicesArray = new int[lineIndices.size()];
        for (int i = 0; i < lineIndices.size(); i++) {
            lineIndicesArray[i] = lineIndices.get(i);
        }

        return new VertexAndIndexData(vertices, polygonIndicesArray, lineIndicesArray);
    }

    // For testing purposes
    public static void main(String[] args) {
        // Example usage would go here
    }
}
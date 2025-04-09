package dk.itu;

import kotlin.Pair;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

import java.util.*;

public class PolygonTriangulator {

    public static Pair<float[], int[]> triangulatePolygon(double[] coordinates, float[] color) {
        if (coordinates.length < 6 || coordinates.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid coordinates array");
        }

        // Convert the flat array to JTS Coordinates
        List<Coordinate> coordinateList = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) {
            coordinateList.add(new Coordinate(coordinates[i], coordinates[i + 1]));
        }

        // Create a GeometryFactory to build geometries
        GeometryFactory geometryFactory = new GeometryFactory();

        // Create the points for triangulation
        DelaunayTriangulationBuilder triangulationBuilder = new DelaunayTriangulationBuilder();
        triangulationBuilder.setSites(coordinateList);

        // Get the triangulation as a GeometryCollection
        Geometry triangles = triangulationBuilder.getTriangles(geometryFactory);

        // Extract vertices and indices
        Map<Coordinate, Integer> vertexIndices = new HashMap<>();
        List<Coordinate> vertices = new ArrayList<>();
        List<int[]> triangleIndices = new ArrayList<>();

        int index = 0;

        // Process each triangle in the geometry collection
        for (int i = 0; i < triangles.getNumGeometries(); i++) {
            Geometry triangle = triangles.getGeometryN(i);
            Coordinate[] triangleCoords = triangle.getCoordinates();
            int[] indices = new int[3];

            // We only need the first 3 coordinates as the 4th is the same as the 1st for a closed polygon
            for (int j = 0; j < 3; j++) {
                Coordinate coord = triangleCoords[j];
                if (!vertexIndices.containsKey(coord)) {
                    vertexIndices.put(coord, index);
                    vertices.add(coord);
                    indices[j] = index;
                    index++;
                } else {
                    indices[j] = vertexIndices.get(coord);
                }
            }

            triangleIndices.add(indices);
        }

        float[] verticesArray = new float[vertices.size()*6];

        for (int i = 0; i < vertices.size(); i++) {
            var vertex = vertices.get(i);
            // X
            verticesArray[i*6] = (float) vertex.x;
            // Y
            verticesArray[i*6 + 1] = (float) vertex.y;
            // Z
            verticesArray[i*6 + 2] = 0.0f;
            // R
            verticesArray[i*6 + 3] = color[0];
            // G
            verticesArray[i*6 + 4] = color[1];
            // B
            verticesArray[i*6 + 5] = color[2];
        }

        int[] triangleIndicesArray = new int[triangleIndices.size()*3];
        for (int i = 0; i < triangleIndices.size(); i++) {
            var triangle = triangleIndices.get(i);
            triangleIndicesArray[i*3] = triangle[0];
            triangleIndicesArray[i*3 + 1] = triangle[1];
            triangleIndicesArray[i*3 + 2] = triangle[2];
        }

        return new Pair<>(verticesArray, triangleIndicesArray);
    }
}
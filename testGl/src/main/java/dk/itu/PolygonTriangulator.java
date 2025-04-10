package dk.itu;

import kotlin.Pair;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

public class PolygonTriangulator {

    public static Pair<float[], int[]> triangulatePolygon(double[] coordinates, float[] color) {
        List<Coordinate> coordinateList = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) {
            coordinateList.add(new Coordinate(coordinates[i], coordinates[i + 1]));
        }

        // Create the points for triangulation
        DelaunayTriangulationBuilder triangulationBuilder = new DelaunayTriangulationBuilder();
        triangulationBuilder.setSites(coordinateList);

        // Get the triangulation as a GeometryCollection
        Geometry triangles = triangulationBuilder.getTriangles(new GeometryFactory());

        Map<Coordinate, Integer> m = new ConcurrentHashMap<>();
        Queue<Integer> q = new ConcurrentLinkedQueue<>();

        IntStream.range(0, triangles.getNumGeometries()).parallel().forEach(i -> {
            Coordinate[] triangleCoords = triangles.getGeometryN(i).getCoordinates();

            List<Integer> indexes = new ArrayList<>(3);
            m.putIfAbsent(triangleCoords[0], i*3);
            indexes.add(m.get(triangleCoords[0]));
            m.putIfAbsent(triangleCoords[1], i*3 + 1);
            indexes.add(m.get(triangleCoords[1]));
            m.putIfAbsent(triangleCoords[2], i*3 + 2);
            indexes.add(m.get(triangleCoords[2]));

            q.addAll(indexes);
        });

        var mList = m.keySet().parallelStream().toList();

        float[] verticessss = new float[mList.size()*6];

        IntStream.range(0, mList.size()).parallel().forEach(i -> {
            var c = mList.get(i);
            verticessss[i*6] = (float) c.x;
            verticessss[i*6 + 1] = (float) c.y;
            verticessss[i*6 + 2] = 0.0f;
            verticessss[i*6 + 3] = color[0];
            verticessss[i*6 + 4] = color[1];
            verticessss[i*6 + 5] = color[2];

        });

//        var finalVertices = m.keySet()
//                .parallelStream()
//                .map(coordinate -> new double[] {
//                        coordinate.x,
//                        coordinate.y,
//                        0.0f,
//                        color[0],
//                        color[1],
//                        color[2]
//                })
//                .flatMapToDouble(Arrays::stream)
//                .mapToObj(d -> (float) d)
//                .collect(Collectors.toList());


        return new Pair<>(verticessss, q.parallelStream().mapToInt(i -> i).toArray());



        // Extract vertices and indices
//        Map<Coordinate, Integer> vertexIndices = new HashMap<>();
//        List<Coordinate> vertices = new ArrayList<>();
//        List<int[]> triangleIndices = new ArrayList<>();
//
//        int index = 0;
//
//        // Process each triangle in the geometry collection
//        for (int i = 0; i < triangles.getNumGeometries(); i++) {
//            Geometry triangle = triangles.getGeometryN(i);
//            Coordinate[] triangleCoords = triangle.getCoordinates();
//            int[] indices = new int[3];
//
//            // We only need the first 3 coordinates as the 4th is the same as the 1st for a closed polygon
//            for (int j = 0; j < 3; j++) {
//                Coordinate coord = triangleCoords[j];
//                if (!vertexIndices.containsKey(coord)) {
//                    vertexIndices.put(coord, index);
//                    vertices.add(coord);
//                    indices[j] = index;
//                    index++;
//                } else {
//                    indices[j] = vertexIndices.get(coord);
//                }
//            }
//
//            triangleIndices.add(indices);
//        }
//
//        float[] verticesArray = new float[vertices.size()*6];
//
//        for (int i = 0; i < vertices.size(); i++) {
//            var vertex = vertices.get(i);
//            // X
//            verticesArray[i*6] = (float) vertex.x;
//            // Y
//            verticesArray[i*6 + 1] = (float) vertex.y;
//            // Z
//            verticesArray[i*6 + 2] = 0.0f;
//            // R
//            verticesArray[i*6 + 3] = color[0];
//            // G
//            verticesArray[i*6 + 4] = color[1];
//            // B
//            verticesArray[i*6 + 5] = color[2];
//        }
//
//        int[] triangleIndicesArray = new int[triangleIndices.size()*3];
//        for (int i = 0; i < triangleIndices.size(); i++) {
//            var triangle = triangleIndices.get(i);
//            triangleIndicesArray[i*3] = triangle[0];
//            triangleIndicesArray[i*3 + 1] = triangle[1];
//            triangleIndicesArray[i*3 + 2] = triangle[2];
//        }
//
//        return new Pair<>(verticesArray, triangleIndicesArray);
    }
}
package datastructures;

import dk.itu.data.datastructure.heightcurvetree.HeightCurveTree;
import dk.itu.data.dto.HeightCurveParserResult;
import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.parsers.GmlParser;
import dk.itu.data.repositories.HeightCurveRepositoryMemory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dk.itu.data.services.HeightCurveService.splitBoundingBoxInUTMQuadrants;

public class HeightCurveBenchmarking {


//    \item Performance metrics with using shoelace before ray casting
//    \item Performance metrics bounding box before ray casting
//    \item Insertion time of 10-15000 elements
//    \item Insertion time for polygons with 10-10000 points in same tree

    private static List<HeightCurveElement> heightCurveElements;
    private static HeightCurveTree heightCurveTree;

    @BeforeAll
    public static void init() {
//        var quadrants = splitBoundingBoxInUTMQuadrants(14.660547, 55.092908, 15.287148, 55.320550); // Northern bornholm
//        var quadrants = splitBoundingBoxInUTMQuadrants(10.988758, 55.857587, 11.200883, 55.903806); // Sejerø
        var quadrants = splitBoundingBoxInUTMQuadrants(9.748950, 54.837057, 10.934551, 55.614826); // Fyn => 79_156

        HeightCurveRepositoryMemory heightCurveRepositoryMemory = HeightCurveRepositoryMemory.getInstance();

        HeightCurveParserResult heightCurveParserResult = new HeightCurveParserResult(heightCurveRepositoryMemory);
        heightCurveTree = heightCurveRepositoryMemory.getHeightCurveTree();

        GmlParser.parse(quadrants, heightCurveParserResult);

        heightCurveParserResult.sanitize();

        heightCurveElements = heightCurveParserResult.getElements().parallelStream().map(HeightCurveElement::mapToHeightCurveElement).toList();
    }

//    @ParameterizedTest
//    @ValueSource(ints = {50, 100, 250, 500, 1000, 3000, 7000, 15_000, 25_000, 40_000, 60_000})
//    public void benchmark_insertion(int curves) {
//        for (int i = 0; i < 1; i++) {
//            long elapsedTime = runBenchmarkInsertion(curves);
//            System.out.println((String.format("%.3f", elapsedTime / 1000000f) + "\t\t\t" + String.format("%.3f", (elapsedTime / 1000000f) / curves)).replace(".", ","));
//        }
//    }
//    private long runBenchmarkInsertion(int curves) {
//        List<HeightCurveElement> heightCurvesToTest = heightCurveElements.subList(0, curves);
//
//        long startTime = System.nanoTime();
//        for (HeightCurveElement heightCurveElement : heightCurvesToTest) {
//            heightCurveTree.put(heightCurveElement);
//        }
//        long elapsedTime = System.nanoTime() - startTime;
//
//        heightCurveTree.clear();
//
//        return elapsedTime;
//    }

    @ParameterizedTest
    @ValueSource(ints = {50, 100, 250, 500, 1000, 3000, 7000, 15_000, 25_000, 40_000, 60_000})
    public void benchmark_ray_casting_without_shoelace(int curves) {
        for (int i = 0; i < 1; i++) {
            long elapsedTime = runBenchmarkShoelace(curves);
            System.out.println((String.format("%.3f", elapsedTime / 1000000f) + "\t\t\t" + String.format("%.3f", (elapsedTime / 1000000f) / curves)).replace(".", ","));
        }
    }
    private long runBenchmarkShoelace(int curves) {
        List<HeightCurveElement> heightCurvesToTest = heightCurveElements.subList(0, curves);

        long startTime = System.nanoTime();
        for (HeightCurveElement heightCurveElement : heightCurvesToTest) {
            heightCurveTree.put(heightCurveElement);
        }
        long elapsedTime = System.nanoTime() - startTime;

        heightCurveTree.clear();

        return elapsedTime;
    }

//    @ParameterizedTest
//    @ValueSource(ints = {})
//    public void benchmark_ray_casting_with_and_without_bounding_box(int curves) {
//
//    }


}
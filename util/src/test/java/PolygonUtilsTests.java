import kotlin.Pair;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class PolygonUtilsTests {
    @Test
    public void testContains() {
        double[] p1 = new double[]{1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 8, 0, 1, 1};
        double[] p2 = new double[]{3, 2, 4, 3, 4, 2, 3, 2};

        var r1 = testTimeNanoWithResult(() -> contains(p1, p2));
        var r2 = testTimeNanoWithResult(() -> contains2(p1, p2));
        var r3 = testTimeNanoWithResult(() -> contains3(p1, p2));
        var r4 = testTimeNanoWithResult(() -> contains4(p1, p2));

        System.out.println(
                String.format("r1: %.0f \t\t nano\n", r1.component1()) +
                String.format("r2: %.0f \t nano\n", r2.component1()) +
                String.format("r3: %.0f \t\t nano\n", r3.component1()) +
                String.format("r4: %.0f \t\t nano\n", r4.component1())
        );

        System.out.println();
    }

    private boolean contains4(double[] p1, double[] p2) {



//        Arrays
//                .stream(p2)
//                .parallel()
//                .allMatch(contains4())

        // All p2 points should be in p1
        var isInside = true;
        var i = 0;
        while (isInside && i < p2.length) {
            // Test if p2 is in
            isInside = isPointInPolygon4(p1, p2[i], p2[i+1]);
            i+=2;
        }
        return isInside;
    }
    private boolean isPointInPolygon4(double[] polygon, double x, double y) {
        boolean inside = false;

        double x1 = polygon[polygon.length - 2];
        double y1 = polygon[polygon.length - 1];
        double x2, y2;

        for (int i = 0; i < polygon.length; i+=2) {
            x2 = polygon[i];
            y2 = polygon[i + 1];

            if (
                    ((y1 > y) != (y2 > y)) && // Condition 1 within y bounds
                            (x < (x2 - x1) * (y - y1) / (y2 - y1) + x1)  // Condition 2 within x bounds
            ) inside = !inside;

            x1 = x2;
            y1 = y2;
        }

        return inside;
    }

    private boolean contains3(double[] p1, double[] p2) {
        // All p2 points should be in p1
        var isInside = true;
        var i = 0;
        while (isInside && i < p2.length) {
            // Test if p2 is in
            isInside = isPointInPolygon3(p1, p2[i], p2[i+1]);
            i+=2;
        }
        return isInside;
    }
    private boolean isPointInPolygon3(double[] polygon, double x, double y) {
        boolean inside = false;

        double x1 = polygon[polygon.length - 2];
        double y1 = polygon[polygon.length - 1];
        double x2, y2;

        for (int i = 0; i < polygon.length; i+=2) {
            x2 = polygon[i];
            y2 = polygon[i + 1];

            if (
                ((y1 > y) != (y2 > y)) && // Condition 1 within y bounds
                (x < (x2 - x1) * (y - y1) / (y2 - y1) + x1)  // Condition 2 within x bounds
            ) inside = !inside;

            x1 = x2;
            y1 = y2;
        }

        return inside;
    }

    private boolean contains2(double[] p1, double[] p2) {
        // All p2 points should be in p1
        var isInside = true;
        var i = 0;
        while (isInside && i < p2.length) {
            // Test if p2 is in
            isInside = isPointInPolygon2(p1, p2[i], p2[i+1]);
            i+=2;
        }
        return isInside;
    }
    private boolean isPointInPolygon2(double[] polygon, double lon, double lat) {
        AtomicInteger counter = new AtomicInteger(0);

        IntStream
                .range(0, (polygon.length - 1)/2)
                .parallel()
                .forEach(i -> {
                    var ii = i*2;
                    double lon1 = polygon[ii];
                    double lat1 = polygon[(ii + 1) % polygon.length];
                    double lon2 = polygon[(ii + 2) % polygon.length];
                    double lat2 = polygon[(ii + 3) % polygon.length];

                    if (
                            lat < lat1 != lat < lat2 && // Condition 1 between y-axis
                                    lon < lon1 + (lat - lat1) / (lat2 - lat1) * (lon2 - lon1) // Condition 2 between x-axis
                    ) {
                        counter.getAndIncrement();
                    }
                });

        return counter.get() % 2 == 1;
    }

    private Pair<Double, Boolean> testTimeNanoWithResult(Supplier<Boolean> runnable) {
        long start = System.nanoTime();
        var r = runnable.get();
        long end = System.nanoTime() - start;
        return new Pair<>((double) end, r);
    }

    private boolean contains(double[] p1, double[] p2) {
        // All p2 points should be in p1
        var isInside = true;
        var i = 0;
        while (isInside && i < p2.length) {
            // Test if p2 is in
            isInside = isPointInPolygon(p1, p2[i], p2[i+1]);
            i+=2;
        }
        return isInside;
    }
    private boolean isPointInPolygon(double[] polygon, double x, double y) {
        boolean inside = false;
        int numVertices = polygon.length / 2;

        for (int i = 0, j = numVertices - 1; i < numVertices; j = i++) {
            double xi = polygon[2*i];
            double yi = polygon[2*i + 1];
            double xj = polygon[2*j];
            double yj = polygon[2*j + 1];

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }

        return inside;
    }
}
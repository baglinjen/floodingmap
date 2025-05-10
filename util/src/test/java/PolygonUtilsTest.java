import dk.itu.util.PolygonUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

public class PolygonUtilsTest {

    @Test
    public void testContains() {

        // TODO: Fix test
        try (var polygonUtils = Mockito.mockStatic(PolygonUtils.class)) {

            double[] polygonBigger = new double[]{
                    0, 0,
                    10, 10,
                    10, 0,
                    0, 0
            };

            double[] polygonSmallerWithOnePointOutside = new double[]{
                    1, 1,
                    9, 11,
                    9, 1,
                    1, 1
            };

            polygonUtils.verify(
                    () -> PolygonUtils.isPointInPolygon(any(), any(), any()),
                    times(1)
            );

            assertThat(PolygonUtils.contains(polygonBigger, polygonSmallerWithOnePointOutside)).isFalse();

        }
    }
}

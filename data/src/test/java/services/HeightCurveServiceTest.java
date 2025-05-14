package services;

import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.services.Services;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class HeightCurveServiceTest {
    @ParameterizedTest
    @CsvSource({
            "'myfile.gml', X"
    })
    void serviceCanLoadGmlDataFromFile(String filename, int expectedCurveAmount){
        Services.withServices(s -> {
            //Arrange
            List<HeightCurveElement> curves = null;

            //Act
            s.getHeightCurveService().loadGmlFileData(filename);
            curves = s.getHeightCurveService().getElements();

            //Assert
            assertThat(curves.size()).isEqualTo(expectedCurveAmount);
        });
    }
}

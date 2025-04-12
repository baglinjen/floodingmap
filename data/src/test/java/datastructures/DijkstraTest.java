package datastructures;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.itu.data.models.db.OsmNode;
import dk.itu.data.models.db.OsmWay;
import dk.itu.data.services.Services;
import dk.itu.data.utils.DijkstraConfiguration;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DijkstraTest {
    private static final Logger logger = LoggerFactory.getLogger();
    private static DijkstraConfiguration testConfiguration;
    private static List<OsmNode> nodes;

    @BeforeEach
    void setupSuite(){
        Services.withServices(services -> {
            services.getOsmService(false).loadOsmData("tuna.osm");
            services.getGeoJsonService().loadGeoJsonData("tuna-dijkstra.geojson");
            nodes = services.getOsmService(false).getTraversableOsmNodes();
        });

        testConfiguration = new DijkstraConfiguration();
        testConfiguration.setWaterLevel(0.0);
    }

    @ParameterizedTest
    @CsvSource({
            "9342037677, 4289093536, '9342037677-4289093536-route.json'"
            //TODO: Add more cases
    })
    void DijkstraCanFindShortestPath(long startNodeId, long endNodeId, String filename){
        //Arrange
        testConfiguration.setStartNodeId(startNodeId);
        testConfiguration.setEndNodeId(endNodeId);
        var expectedRouteCoordinates = extractCoordinates(filename);

        //Act
        testConfiguration.calculateRoute(false);
        var route = testConfiguration.getRoute(false, 0.0);
        var routeCoordinates = ((OsmWay)route).getOuterCoordinates();

        //Assert
        assertThat(route).isNotNull();
        assertThat(routeCoordinates).isNotNull();
        assertThat(routeCoordinates).isEqualTo(expectedRouteCoordinates);
    }

    @ParameterizedTest
    @CsvSource({
            "1078669419, 4545580716, 0.0, '1078669419-4545580716-route.json'",
            "1078669419, 4545580716, 4.0, '1078669419-4545580716-route-flooded.json'"
            //TODO: Add more cases
    })
    void DijkstraWillAccountForRisingWater(long startNodeId, long endNodeId, float waterLevel, String filename){
        //Arrange
        testConfiguration.setStartNodeId(startNodeId);
        testConfiguration.setEndNodeId(endNodeId);
        testConfiguration.setWaterLevel(waterLevel);
        var expectedCoords = extractCoordinates(filename);

        //Act
        testConfiguration.calculateRoute(false);
        var route = testConfiguration.getRoute(false, waterLevel);
        var routeCoordinates = ((OsmWay)route).getOuterCoordinates();

        //Assert
        assertThat(route).isNotNull();
        assertThat(routeCoordinates).isNotNull();
        assertThat(routeCoordinates).isEqualTo(expectedCoords);
    }


    @Disabled("Not implemented yet")
    @Test
    public void PathCanBeConstructedFromNodes(){
        //TODO: Implement
    }

    //Load test data with list of id's
    private List<Long> loadRouteData(String filename){
        try{
            var mapper = new ObjectMapper();
            return mapper.readValue(
                    new File(DijkstraTest.class.getClassLoader().getResource("dijkstraRouteData/"+filename).toURI()),
                    new TypeReference<List<Long>>(){}
            );
        } catch(Exception e){
            logger.error("An exception occurred processing route data: {}", e.getMessage());

            fail();
            return List.of();
        }
    }

    private double[] extractCoordinates(List<OsmNode> nodes){
        var res = new ArrayList<Double>();

        for(var n : nodes){
            res.add(n.getLon());
            res.add(n.getLat());
        }

        var resArray = new double[res.size()];
        for(int i = 0; i < res.size(); i++){
            resArray[i] = res.get(i);
        }

        return resArray;
    }

    private double[] extractCoordinates(String filename){
        var returnNodes = new ArrayList<OsmNode>();

        for(var id : loadRouteData(filename)){
            returnNodes.add(nodes.stream().filter(o -> o.getId() == id).findFirst().get());
        }

        return extractCoordinates(returnNodes);
    }
}
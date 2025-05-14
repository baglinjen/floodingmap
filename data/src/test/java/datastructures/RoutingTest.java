package datastructures;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.itu.data.enums.RoutingType;
import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.models.db.osm.OsmWay;
import dk.itu.data.services.Services;
import dk.itu.data.services.RoutingService;
import dk.itu.data.utils.RoutingUtils;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RoutingTest {
    private static final Logger logger = LoggerFactory.getLogger();
    private static RoutingService testConfiguration;
    private static Map<Long, OsmNode> nodes;

    @BeforeAll
    void setupGeneral(){
        Services.withServices(services -> {
            services.getOsmService(false).loadOsmData("tuna.osm");
            services.getOsmService(false).loadOsmData("bornholm.osm");
            services.getHeightCurveService().loadGmlFileData("tuna-dijkstra.gml");
            nodes = services.getOsmService(false).getTraversableOsmNodes();
        });
    }

    @BeforeEach
    void setupIndividual(){
        testConfiguration = new RoutingService();
        testConfiguration.setWaterLevel(0.0);

        resetFlooding();
    }

    @ParameterizedTest
    @CsvSource({
            "1078669546, 1078669405, 450",
            "1079130218, 1078685441, 3166",
            "1078669228, 12568363317, 9"
    })
    void euclideanDistanceIsCorrect(long startNodeId, long endNodeId, double expectedDistance){
        var startNode = nodes.get(startNodeId);
        var endNode = nodes.get(endNodeId);

        var distance = RoutingUtils.distanceMeters(startNode.getLat(), startNode.getLon(), endNode.getLat(), endNode.getLon());

        assertThat(Math.floor(distance)).isEqualTo(expectedDistance);
    }

    @ParameterizedTest
    @CsvSource({
            "9342037677, 4289093536, '9342037677-4289093536-route.json'",
            "3344638963, 11975103676, '3344638963-11975103676-route.json'"
            //TODO: Consider adding more cases
    })
    void routingCanFindShortestPath(long startNodeId, long endNodeId, String filename) throws InterruptedException {
        //Arrange
        testConfiguration.setStartNode(nodes.get(startNodeId));
        testConfiguration.setEndNode(nodes.get(endNodeId));

        var expectedRouteCoordinates = extractCoordinates(filename);

        //Act
        var thread = testConfiguration.calculateRoute(false);
        thread.join();

        var route = testConfiguration.getRoute(false, 0.0);
        var routeCoordinates = ((OsmWay)route).getOuterCoordinates();

        //Assert
        assertThat(route).isNotNull();
        assertThat(routeCoordinates).isNotNull();
        assertThat(routeCoordinates).isEqualTo(expectedRouteCoordinates);

        assertThat(testConfiguration.getTouchedNodes().size()).isPositive();

        assertThat(testConfiguration.getStartNode()).isEqualTo(nodes.get(startNodeId));
        assertThat(testConfiguration.getEndNode()).isEqualTo(nodes.get(endNodeId));
    }

    @ParameterizedTest
    @CsvSource({
            "Dijkstra, 1078669419, 4545580716, 0.0, 'flooded/1078669419-4545580716-route.json'",
            "Dijkstra, 1078669419, 4545580716, 4.0, 'flooded/1078669419-4545580716-route-flooded.json'",
            "AStar, 1078669419, 4545580716, 0.0, 'flooded/1078669419-4545580716-route.json'",
            "AStar, 1078669419, 4545580716, 4.0, 'flooded/1078669419-4545580716-route-flooded.json'",
            "AStarBidirectional, 1078669419, 4545580716, 0.0, 'flooded/1078669419-4545580716-route.json'",
            "AStarBidirectional, 1078669419, 4545580716, 4.0, 'flooded/1078669419-4545580716-route-flooded.json'"
    })
    void routingWillAccountForRisingWater(String routingConfig, long startNodeId, long endNodeId, float waterLevel, String filename) throws InterruptedException {
        //Arrange
        testConfiguration.setStartNode(nodes.get(startNodeId));
        testConfiguration.setEndNode(nodes.get(endNodeId));
        testConfiguration.setRoutingMethod(Enum.valueOf(RoutingType.class, routingConfig));

        simulateFlooding(waterLevel);

        testConfiguration.setWaterLevel(waterLevel);
        var expectedCoords = extractCoordinates(filename);

        //Act
        var thread = testConfiguration.calculateRoute(false);
        thread.join();

        var route = testConfiguration.getRoute(false, waterLevel);
        var routeCoordinates = ((OsmWay)route).getOuterCoordinates();

        //Assert
        assertThat(route).isNotNull();
        assertThat(routeCoordinates).isNotNull();
        assertThat(routeCoordinates).isEqualTo(expectedCoords);
    }

    @ParameterizedTest
    @CsvSource({
            "9342037677, 4289093536, '9342037677-4289093536-route.json'",
            "3344638963, 11975103676, '3344638963-11975103676-route.json'",
            "1078669419, 4545580716, 'flooded/1078669419-4545580716-route.json'"
    })
    void dijkstraAndAStarFindsEquallyCorrectRoute(long startNodeId, long endNodeId, String filename) throws InterruptedException{
        //Arrange
        testConfiguration.setStartNode(nodes.get(startNodeId));
        testConfiguration.setEndNode(nodes.get(endNodeId));

        var expectedRouteCoordinates = extractCoordinates(filename);

        //Act
        testConfiguration.setRoutingMethod(RoutingType.Dijkstra);
        assertThat(testConfiguration.getRoutingMethod()).isEqualTo(RoutingType.Dijkstra);

        var dijkstraThread = testConfiguration.calculateRoute(false);
        dijkstraThread.join();

        var dijkstraRoute = testConfiguration.getRoute(false, 0.0);
        var dijkstraCoordinates = ((OsmWay)dijkstraRoute).getOuterCoordinates();

        testConfiguration.setRoutingMethod(RoutingType.AStar);
        assertThat(testConfiguration.getRoutingMethod()).isEqualTo(RoutingType.AStar);

        var aStarThread = testConfiguration.calculateRoute(false);
        aStarThread.join();

        var aStarRoute = testConfiguration.getRoute(false, 0.0);
        var aStarCoordinates = ((OsmWay)aStarRoute).getOuterCoordinates();

        testConfiguration.setRoutingMethod(RoutingType.AStarBidirectional);
        assertThat(testConfiguration.getRoutingMethod()).isEqualTo(RoutingType.AStarBidirectional);

        var aStarBidirectionalThread = testConfiguration.calculateRoute(false);
        aStarBidirectionalThread.join();

        var aStarBidirectionalRoute = testConfiguration.getRoute(false, 0.0);
        var aStarBidirectionalCoordinates = ((OsmWay)aStarBidirectionalRoute).getOuterCoordinates();

        //Assert
        assertThat(dijkstraRoute).isNotNull();
        assertThat(dijkstraCoordinates).isNotNull();
        assertThat(dijkstraCoordinates).isEqualTo(expectedRouteCoordinates);

        assertThat(aStarRoute).isNotNull();
        assertThat(aStarCoordinates).isNotNull();

        assertThat(aStarCoordinates).isEqualTo(dijkstraCoordinates);
        assertThat(aStarBidirectionalCoordinates).isEqualTo(dijkstraCoordinates);
    }

    @Test
    void routingConfigurationCanSwitchVisualization(){
        assertThat(testConfiguration.getShouldVisualize()).isEqualTo(false);

        testConfiguration.toggleShouldVisualize();
        assertThat(testConfiguration.getShouldVisualize()).isEqualTo(true);

        testConfiguration.toggleShouldVisualize();
        assertThat(testConfiguration.getShouldVisualize()).isEqualTo(false);
    }

    @ParameterizedTest
    @CsvSource({
            //Start node is on Tunø, end node on Bornholm -> not physically connected
            "1078669498, 8202351169, Dijkstra",
            "1078669498, 8202351169, AStar",
            "1078669498, 8202351169, AStarBidirectional"
    })
    void routingCanHandleNotFindingARoute(long startNodeId, long endNodeId, String routingEnum){
        var routingType = Enum.valueOf(RoutingType.class, routingEnum);

        testConfiguration.setRoutingMethod(routingType);
        testConfiguration.setStartNode(nodes.get(startNodeId));
        testConfiguration.setEndNode(nodes.get(endNodeId));

        testConfiguration.calculateRoute(false);

        var route = testConfiguration.getRoute(false, 0.0);

        assertThat(route).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "Dijkstra, 1078669419, 4545580716",
            "AStar, 1078669419, 4545580716",
            "AStarBidirectional, 1078669419, 4545580716"
    })
    void routingWillInvalidate(String routingEnum, long startNodeId, long endNodeId) throws InterruptedException{
        //Arrange
        var routingType = Enum.valueOf(RoutingType.class, routingEnum);
        testConfiguration.setRoutingMethod(routingType);

        testConfiguration.setStartNode(nodes.get(startNodeId));
        testConfiguration.setEndNode(nodes.get(endNodeId));

        //Act
        var initialCalculationThread = testConfiguration.calculateRoute(false);
        initialCalculationThread.join();

        var routeBeforeFlood = testConfiguration.getRoute(false, 0.0);

        testConfiguration.setWaterLevel(10);
        simulateFlooding(10);

        var routeAfterFlood = testConfiguration.getRoute(false, 10);

        //Assert
        assertThat(routeBeforeFlood).isNotNull();
        assertThat(routeAfterFlood).isNull();
        assertThat(routeBeforeFlood).isNotEqualTo(routeAfterFlood);
    }

    @Test
    void routingWillCalculateThenFailThenRecalculate() throws InterruptedException{
        //Arrange
        testConfiguration.setStartNode(nodes.get(12260042387L));//Rønne
        testConfiguration.setEndNode(nodes.get(10267226718L));//Gudhjem
        testConfiguration.setRoutingMethod(RoutingType.AStarBidirectional);

        //Act
        var initialCalculation = testConfiguration.calculateRoute(false);
        initialCalculation.join();

        var initialRoute = testConfiguration.getRoute(false, 0.0);
        assertThat(initialRoute).isNotNull();

            //Simulate flooding
            testConfiguration.setWaterLevel(10);//Invalidate route
            simulateFlooding(10);

        var floodedRoute = testConfiguration.getRoute(false, 10.0);
        assertThat(floodedRoute).isNull();

            //Remove flooding
            testConfiguration.setWaterLevel(0);
            resetFlooding();

        testConfiguration.setStartNode(nodes.get(5817871235L));//Rønne
        testConfiguration.setEndNode(nodes.get(4823363926L));//Nexø

        var newCalculation = testConfiguration.calculateRoute(false);
        newCalculation.join();

        var newRoute = testConfiguration.getRoute(false, 0.0);
        assertThat(newRoute).isNotNull();
        assertThat(newRoute).isNotEqualTo(initialRoute);
    }

    @Test
    void routingCanBeCancelledAndThenCalledAgain() throws InterruptedException{
        //Arrange
        testConfiguration.setStartNode(nodes.get(12260042387L));//Rønne
        testConfiguration.setEndNode(nodes.get(10267226718L));//Gudhjem
        testConfiguration.setRoutingMethod(RoutingType.AStarBidirectional);

        //Act
        testConfiguration.calculateRoute(false);
        testConfiguration.cancelRouteCalculation();

        var cancelledRoute = testConfiguration.getRoute(false, 0.0);

        Thread.sleep(500);//Provide time for the calculation thread to die

        var newCalculation = testConfiguration.calculateRoute(false);
        newCalculation.join();

        var newRoute = testConfiguration.getRoute(false, 0.0);

        //Assert
        assertThat(cancelledRoute).isNull();
        assertThat(newRoute).isNotNull();
    }

    /// Auxiliary method to load contents of a file. File should be in .JSON format containing a list of node IDs which a route should pass through.
    private List<Long> loadRouteData(String filename){
        try{
            var mapper = new ObjectMapper();
            return mapper.readValue(
                    new File(RoutingTest.class.getClassLoader().getResource("testdata/routing/"+filename).toURI()),
                    new TypeReference<>() {
                    }
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
            returnNodes.add(nodes.get(id));
        }

        return extractCoordinates(returnNodes);
    }

    /// Method to simulate the flooding which would otherwise occur in the timeline-effect in FloodingApp
    private void simulateFlooding(float waterLevel){
        Services.withServices(s -> {
            var floodingSteps = s.getHeightCurveService().getFloodingSteps(waterLevel);

            for (List<HeightCurveElement> floodingStep : floodingSteps) {
                floodingStep.parallelStream().forEach(HeightCurveElement::setBelowWater);
            }
        });

    }

    private void resetFlooding(){
        //Ensure flooded curves are reset upon each run
        Services.withServices(s -> {
            s.getHeightCurveService().getElements().parallelStream().forEach(HeightCurveElement::setAboveWater);
        });
    }
}
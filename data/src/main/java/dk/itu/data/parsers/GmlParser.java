package dk.itu.data.parsers;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.dto.HeightCurveElementBuilder;
import dk.itu.data.dto.HeightCurveParserResult;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static dk.itu.util.CoordinateUtils.convertLatLonToUTM;
import static org.apache.commons.collections4.ListUtils.partition;

public class GmlParser {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final int TIMEOUT_SECONDS = 30, THREADS_POOL_SIZE = 64;
    private static final HttpClient httpClient = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    public static void parse(List<double[]> quadrants, HeightCurveParserResult result) {
        logger.info("Starting preparing futures");
        long startTime = System.nanoTime();

        var futures = quadrants
                .parallelStream()
                .map(quadrant -> {
                            try {
                                var token = CommonConfiguration.getInstance().getDataForsyningenToken();
                                if (token == null) {
                                    logger.error("No dataforsyningen token found in env");
                                    return null;
                                }

                                // Converting EPSG4326 lat/lon bounds to UTM
                                var bl = convertLatLonToUTM(quadrant[1], quadrant[0]);
                                var br = convertLatLonToUTM(quadrant[1], quadrant[2]);
                                var tl = convertLatLonToUTM(quadrant[3], quadrant[0]);
                                var tr = convertLatLonToUTM(quadrant[3], quadrant[2]);


                                var minBoundsOld = convertLatLonToUTM(quadrant[1], quadrant[0]);
                                var maxBoundsOld = convertLatLonToUTM(quadrant[3], quadrant[2]);
                                var minBounds = new double[]{Math.min(bl[0], tl[0]), Math.min(bl[1], br[1])};
                                var maxBounds = new double[]{Math.min(br[0], tr[0]), Math.min(tl[1], tr[1])};

                                // Building request
                                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(
                                                URI.create(
                                                        "https://api.dataforsyningen.dk/DHMhoejdekurver_GML3_DAF?" +
                                                                "token=" + token +
                                                                "&service=WFS" +
                                                                "&request=GetFeature" +
                                                                "&version=2.0.0" +
                                                                "&typenames=Formkurve2_5" +
                                                                "&SRSNAME=EPSG:25832" +
                                                                "&BBOX=" + minBounds[0] + "," + minBounds[1] + "," + maxBounds[0] + "," + maxBounds[1]
                                                )
                                        )
                                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                                        .GET()
                                        .build();

                                var boundsId = Arrays.hashCode(quadrant);

                                return (Supplier<CompletableFuture<Void>>) () -> CompletableFuture
                                        .runAsync(() -> {
                                            try {
                                                processCompletableFutureInputStream(httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream()).body(), result);
                                            } catch (IOException | InterruptedException e) {
                                                logger.warn("Error occurred sending request for bounds with ID #{}", boundsId, e);
                                            }
                                        });
                            } catch (Exception e) {
                                logger.error("Encountered some error trying to connect to dataforsyningen", e);
                                return null;
                            }
                        }
                )
                .filter(Objects::nonNull)
                .toList();

        var batches = partition(futures, THREADS_POOL_SIZE);

        logger.info("Split {} futures in {} batches of futures", futures.size(), batches.size());

        for (int i = 0; i < batches.size(); i++) {
//            logger.debug("Starting batch {}", i);
//            long start = System.nanoTime();
            CompletableFuture
                    .allOf(batches.get(i).parallelStream().map(Supplier::get).toArray(CompletableFuture[]::new))
                    .orTimeout(TIMEOUT_SECONDS*3, TimeUnit.SECONDS)
                    .join();
//            logger.debug("Finished batch {} in {}ms", i, String.format("%.3f", (System.nanoTime() - start) / 1000000f));
        }

        logger.info("Finished futures in {}ms", String.format("%.3f", (System.nanoTime() - startTime) / 1000000f));
    }

    public static void parse(String gmlFileName, HeightCurveParserResult result) {
        processCompletableFutureInputStream(CommonConfiguration.class.getClassLoader().getResourceAsStream("gml/" + gmlFileName), result);
    }

    private static void processCompletableFutureInputStream(InputStream inputStream, HeightCurveParserResult result) {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        HeightCurveElementBuilder elementBuilder = new HeightCurveElementBuilder(result);

        try (inputStream) {
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(inputStream);

            while (reader.hasNext()) {
                XMLEvent nextEvent = reader.nextEvent();

                if (nextEvent.isStartElement()) {
                    StartElement startElement = nextEvent.asStartElement();

                    switch (startElement.getName().getLocalPart()) {
                        case "Formkurve2_5":
                            startElement
                                    .getAttributes()
                                    .forEachRemaining(attribute -> {
                                        if (attribute.getName().getLocalPart().equals("id"))
                                            elementBuilder.withGmlId(Long.parseLong(attribute.getValue()));
                                    });
                            break;
                        case "posList":
                            nextEvent = reader.nextEvent();
                            elementBuilder.withEPSG25832Coords(nextEvent.asCharacters().getData());
                            break;
                    }
                }

                if (nextEvent.isEndElement()) {
                    EndElement endElement = nextEvent.asEndElement();
                    if (endElement.getName().getLocalPart().equals("featureMember")) {
                        elementBuilder.buildAndAddElement();
                    }
                }
            }

        } catch (IOException | XMLStreamException e) {
            logger.error("Error occurred processing input stream", e);
            throw new RuntimeException(e);
        }
    }
}
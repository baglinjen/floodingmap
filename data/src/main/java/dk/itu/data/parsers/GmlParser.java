package dk.itu.data.parsers;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.dto.HeightCurveElementBuilder;
import dk.itu.data.dto.HeightCurveParserResult;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
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
import java.util.stream.IntStream;

public class GmlParser {
    private static final Set<String> quadrantsProcessed = new HashSet<>();
    private static final Logger logger = LoggerFactory.getLogger();
    private static final int TIMEOUT_SECONDS = 60, MAX_RETRIES = 64, MAX_IO_RETRIES = 32; // Max 64 retries - 32 io retries counts for 1 retry
    private static final double IO_MIN_DELAY_MS = 50, IO_DELAY_VARIANCE_MS = 500;
    private static final HttpClient httpClient = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    public static void parse(List<double[]> quadrants, HeightCurveParserResult result) {
        logger.info("Starting preparing futures");
        long startTime = System.nanoTime();

        // Thread pool executor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var quadrantsFiltered = quadrants.parallelStream().filter(q -> !quadrantsProcessed.contains(Arrays.toString(q))).toList();

            // Futures to process
            CompletableFuture<Void>[] futures = new CompletableFuture[quadrantsFiltered.size()];

            IntStream.range(0, quadrantsFiltered.size()).forEach(i -> {
                futures[i] =
                        CompletableFuture.runAsync(() -> {
                            try {
                                processQuadrant(quadrantsFiltered.get(i), result);
                            } catch (Exception e) {
                                logger.error("Error processing quadrant", e);
                            }
                        }, executor);
            });

            logger.info("Prepared and starting {} futures", futures.length);

            // Wait for all tasks to complete
            CompletableFuture.allOf(futures)
                    .orTimeout(TIMEOUT_SECONDS * 5, TimeUnit.SECONDS)
                    .join();

            // Shutdown the executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        logger.info("Finished futures in {}ms", String.format("%.3f", (System.nanoTime() - startTime) / 1000000f));
    }

    private static void processQuadrant(double[] quadrant, HeightCurveParserResult result) {
        try {
            var token = CommonConfiguration.getInstance().getDataForsyningenToken();
            if (token == null) {
                logger.error("No dataforsyningen token found in env");
                return;
            }

            // Building request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.dataforsyningen.dk/DHMhoejdekurver_GML3_DAF?" +
                                    "token=" + token +
                                    "&service=WFS" +
                                    "&request=GetFeature" +
                                    "&version=2.0.0" +
                                    "&typenames=Formkurve2_5" +
                                    "&SRSNAME=EPSG:25832" +
                                    "&BBOX=" + quadrant[0] + "," + quadrant[1] + "," + quadrant[2] + "," + quadrant[3]
                    ))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            // Retry logic
            int count = 0, ioRetryCount = 0;
            while (count < MAX_RETRIES) {
                try {
                    processCompletableFutureInputStream(httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream()).body(), result);
                    quadrantsProcessed.add(Arrays.toString(quadrant));
                    return;
                } catch (IOException e) {
                    ioRetryCount++;
                    Thread.sleep(Math.round(IO_MIN_DELAY_MS + (Math.random() * IO_DELAY_VARIANCE_MS)));
                    if (ioRetryCount >= MAX_IO_RETRIES) {
                        count++;
                        ioRetryCount = 0;
                    }
                } catch (InterruptedException | XMLStreamException e) {
                    logger.warn("Error occurred sending request at attempt {} with {} io retries for bounds {}", count, ioRetryCount, Arrays.toString(quadrant));
                    count++;
                    ioRetryCount = 0;
                }
            }
            logger.error("Failed sending request after {} attempts and {} io retries for bounds {}", count, ioRetryCount, Arrays.toString(quadrant));
        } catch (Exception e) {
            logger.error("Encountered some error trying to connect to dataforsyningen", e);
        }
    }

    public static void parse(String gmlFileName, HeightCurveParserResult result) {
        try {
            processCompletableFutureInputStream(CommonConfiguration.class.getClassLoader().getResourceAsStream("gml/" + gmlFileName), result);
        } catch (IOException | XMLStreamException e) {
            logger.error("Error occurred parsing gml file {}", gmlFileName, e);
        }
    }

    private static void processCompletableFutureInputStream(InputStream inputStream, HeightCurveParserResult result) throws IOException, XMLStreamException {
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
                        case "hoejde":
                            nextEvent = reader.nextEvent();
                            elementBuilder.withHeight(Float.parseFloat(nextEvent.asCharacters().getData()));
                    }
                }

                if (nextEvent.isEndElement()) {
                    EndElement endElement = nextEvent.asEndElement();
                    if (endElement.getName().getLocalPart().equals("featureMember")) {
                        elementBuilder.buildAndAddElement();
                    }
                }
            }
        } catch (IOException e) {
            throw new IOException(e);
        } catch (XMLStreamException e) {
            throw new XMLStreamException(e);
        }
    }
}
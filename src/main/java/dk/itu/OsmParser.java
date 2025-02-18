package dk.itu;

import dk.itu.drawing.models.MapModel;
import dk.itu.drawing.models.MapModelOsmFile;
import dk.itu.models.DrawingConfig;
import dk.itu.models.OsmElement;
import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import dk.itu.utils.Search;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmParser {
    private static final Logger logger = LogManager.getLogger();
    public static MapModel parse(String fileSource, DrawingConfig drawingConfig) {
        // Logger Details
        logger.info("Parsing file: {}", fileSource);
        long start = System.nanoTime();
        // Parsing start
        try (InputStream is = FxglApp.class.getClassLoader().getResourceAsStream(fileSource)) {
            // Reading utils
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);

            // Final result
            double minY = -181, maxY = 181, minX = -181, maxX = 181;
            List<OsmNode> allNodes = new ArrayList<>();
            List<OsmWay> allWays = new ArrayList<>();
            List<OsmElement> allAreaElements = new ArrayList<>();
            List<OsmElement> allPathElements = new ArrayList<>();

            // Temporary values
            long currentId = -1L;
            double currentX = 0f, currentY = 0f;
            boolean invalidWay = false;
            Map<String, String> currentTags = new HashMap<>();
            List<OsmNode> currentNodesList = new ArrayList<>();

            while (reader.hasNext()) {
                reader.next();
                if (reader.isCharacters()) {
                    continue;
                }

                if (reader.isStartElement()) {
                    switch (reader.getLocalName()) {
                        case "bounds" -> {
                            minY = Double.parseDouble(reader.getAttributeValue(null, "minlat"));
                            maxY = Double.parseDouble(reader.getAttributeValue(null, "maxlat"));
                            minX = Double.parseDouble(reader.getAttributeValue(null, "minlon"));
                            maxX = Double.parseDouble(reader.getAttributeValue(null, "maxlon"));
                            continue;
                        }
                        case "node" -> {
                            currentId = Long.parseUnsignedLong(reader.getAttributeValue(null, "id"));
                            currentX = Double.parseDouble(reader.getAttributeValue(null, "lon"));
                            currentY = Double.parseDouble(reader.getAttributeValue(null, "lat"));
                            continue;
                        }
                        case "way", "relation" -> {
                            currentId = Long.parseUnsignedLong(reader.getAttributeValue(null, "id"));
                            invalidWay = false;
                            continue;
                        }
                        case "nd" -> {
                            int target = Search.fibSearch(allNodes, Long.parseUnsignedLong(reader.getAttributeValue(null, "ref")));
                            if(target != -1){
                                currentNodesList.add(allNodes.get(target));
                            } else {
                                invalidWay = true;
                            }
                            continue;
                        }
                        case "tag" -> {
                            String tagKey = reader.getAttributeValue(null, "k").intern();
                            currentTags.put(tagKey, reader.getAttributeValue(null, "v").intern());
                            continue;
                        }
                    }
                }
                if (reader.isEndElement()) {
                    switch (reader.getLocalName()) {
                        case "node" -> {
                            allNodes.add(new OsmNode(currentId, currentY, currentX));
                            currentTags.clear();
                        }
                        case "way" -> {
                            if(currentNodesList.isEmpty() || currentId < 0 || invalidWay) {
                                currentNodesList.clear();
                                currentTags.clear();
                                continue;
                            }

                            var color = drawingConfig.getStyle(currentTags);
                            if (color != null) {
                                OsmWay way = new OsmWay(currentId, currentNodesList, color, null);
                                switch (way.getShape()) {
                                    case Area _:
                                        allAreaElements.add(way);
                                        break;
                                    case Path2D _:
                                        allPathElements.add(way);
                                        break;
                                    default:
                                        break;
                                }
                                allWays.add(way);
                            }
                            currentNodesList.clear();
                            currentTags.clear();
                        }
                        case "relation" -> currentTags.clear();
                    }
                }
            }
            reader.close();

            logger.info("Parsed {} nodes and {} ways in {}ms", allNodes.size(), allWays.size(), String.format("%.3f", (System.nanoTime() - start) / 1000000f));

            return new MapModelOsmFile(minX, minY, maxY, maxX, allAreaElements, allPathElements);
        } catch (IOException | XMLStreamException e) {
            logger.error("Failed to parse file", e);
            throw new UnsupportedOperationException("Failed to parse file");
        }
    }
}
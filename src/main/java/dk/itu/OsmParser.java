package dk.itu;

import dk.itu.drawing.models.MapModel;
import dk.itu.drawing.models.MapModelOsmFile;
import dk.itu.models.DrawingConfig;
import dk.itu.models.OsmElement;
import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import dk.itu.services.modelservices.WayService;
import dk.itu.utils.Search;
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
    public static MapModel parse(String fileSource, DrawingConfig drawingConfig) {
        try (InputStream is = JavaFxApp.class.getClassLoader().getResourceAsStream(fileSource)) {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);

            float minY = -181, maxY = 181, minX = -181;
            List<OsmNode> allNodes = new ArrayList<>();
            List<OsmWay> allWays = new ArrayList<>();
            List<OsmElement> allAreaElements = new ArrayList<>();
            List<OsmElement> allPathElements = new ArrayList<>();

            long currentId = -1L;
            float currentX = 0f, currentY = 0f;
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
                            minY = Float.parseFloat(reader.getAttributeValue(null, "minlat"));
                            maxY = Float.parseFloat(reader.getAttributeValue(null, "maxlat"));
                            minX = Float.parseFloat(reader.getAttributeValue(null, "minlon"));
                            continue;
                        }
                        case "node" -> {
                            currentId = Long.parseUnsignedLong(reader.getAttributeValue(null, "id"));
                            currentX = Float.parseFloat(reader.getAttributeValue(null, "lon"));
                            currentY = Float.parseFloat(reader.getAttributeValue(null, "lat"));
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

                            var pair = drawingConfig.getColor(currentTags);
                            if (pair != null) {
                                OsmWay way = new OsmWay(currentId, currentNodesList, pair.component1());
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

            return new MapModelOsmFile(minX, minY, maxY, allWays, allAreaElements, allPathElements);
        } catch (IOException | XMLStreamException e) {
            throw new UnsupportedOperationException("Failed to parse custom file");
        }
    }

    public static MapModel parseDB(DrawingConfig drawingConfig){
        WayService wayService = new WayService();

        List<OsmWay> ways = wayService.GetAllWays();

        for(OsmWay way : ways){
            //TODO: Implement color of way
        }

        return null;//TODO: REMOVE


    }
}

package dk.itu;

import dk.itu.drawing.models.MapModel;
import dk.itu.models.DrawingConfig;
import dk.itu.models.OsmElement;
import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import dk.itu.utils.Search;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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
            List<List<OsmElement>> levels = new ArrayList<>(drawingConfig.getLevelsCount());
            for (int i = 0; i < drawingConfig.getLevelsCount(); i++) {
                levels.add(new ArrayList<>());
            }

            long currentId = -1L;
            float currentX = 0f, currentY = 0f;
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
                            continue;
                        }
                        case "nd" -> {
                            int target = Search.fibSearch(allNodes, Long.parseUnsignedLong(reader.getAttributeValue(null, "ref")));
                            if(target != -1){
                                currentNodesList.add(allNodes.get(target));
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
                            if(currentNodesList.isEmpty() || currentId < 0) {
                                currentNodesList.clear();
                                currentTags.clear();
                                continue;
                            }

                            var pair = drawingConfig.getColor(currentTags);

                            if (pair != null) {
                                OsmWay way = new OsmWay(currentId, currentNodesList, pair.component1());
                                levels.get(pair.component2()).add(way);
                            }
                            currentNodesList.clear();
                            currentTags.clear();
                        }
                        case "relation" -> currentTags.clear();
                    }
                }
            }
            reader.close();

            return new MapModel(minX, minY, maxY, levels);
        } catch (IOException | XMLStreamException e) {
            throw new UnsupportedOperationException("Failed to parse custom file");
        }
    }
}

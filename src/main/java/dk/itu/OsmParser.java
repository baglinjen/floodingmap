package dk.itu;

import dk.itu.drawing.models.MapModel;
import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import dk.itu.utils.Search;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OsmParser {
    public static MapModel parse(String fileSource) {
        try (InputStream is = JavaFxApp.class.getClassLoader().getResourceAsStream(fileSource)) {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);

            float minY = -181, maxY = 181, minX = -181;
            List<OsmNode> allNodes = new ArrayList<>();
            List<OsmWay> allWays = new ArrayList<>();

            long currentId = -1L;
            float currentX = 0f, currentY = 0f;
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
                            continue;
                        }
                    }
                }
                if (reader.isEndElement()) {
                    switch (reader.getLocalName()) {
                        case "node" -> allNodes.add(new OsmNode(currentId, currentY, currentX));
                        case "way" -> {
                            if(currentNodesList.isEmpty() || currentId < 0) continue;

                            allWays.add(new OsmWay(currentId, currentNodesList));

                            currentNodesList.clear();
                        }
                        case "relation" -> {
                        }
                    }
                }
            }
            reader.close();

            return new MapModel(minX, minY, maxY, new ArrayList<>(allWays));
        } catch (IOException | XMLStreamException e) {
            throw new UnsupportedOperationException("Failed to parse custom file");
        }
    }
}

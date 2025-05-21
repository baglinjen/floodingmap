package dk.itu.data.parsers;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.dto.OsmElementBuilder;
import dk.itu.data.dto.OsmParserResult;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;
import org.codehaus.stax2.XMLInputFactory2;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;

public class OsmParser {
    private static final Logger logger = LoggerFactory.getLogger();

    public static void parse(String fileName, OsmParserResult osmParserResult) {
        logger.info("Starting parsing file {}", fileName);
        long startTime = System.nanoTime();

        OsmElementBuilder elementBuilder = new OsmElementBuilder(osmParserResult);

        try (InputStream is = CommonConfiguration.class.getClassLoader().getResourceAsStream("osm/"+fileName)) {
            // Reading utils
            XMLInputFactory xmlInputFactory = XMLInputFactory2.newInstance();
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);

            while (reader.hasNext()) {
                reader.next();
                if (reader.isCharacters()) {
                    continue;
                }

                if (reader.isStartElement()) {
                    switch (reader.getLocalName()) {
                        case "tag" -> elementBuilder.withTag(
                                reader.getAttributeValue(null, "k").intern(),
                                reader.getAttributeValue(null, "v").intern()
                        );
                        case "node" -> elementBuilder
                                .withType(OsmElementBuilder.OsmElementType.NODE)
                                .withCoordinates(
                                        Double.parseDouble(reader.getAttributeValue(null, "lat")),
                                        Double.parseDouble(reader.getAttributeValue(null, "lon"))
                                )
                                .withId(Long.parseLong(reader.getAttributeValue(null, "id")));
                        case "way" -> elementBuilder
                                .withType(OsmElementBuilder.OsmElementType.WAY)
                                .withId(Long.parseLong(reader.getAttributeValue(null, "id")));
                        case "relation" -> elementBuilder
                                .withType(OsmElementBuilder.OsmElementType.RELATION)
                                .withId(Long.parseLong(reader.getAttributeValue(null, "id")));
                        case "nd" -> elementBuilder.withNodeReference(Long.parseLong(reader.getAttributeValue(null, "ref")));
                        case "member" -> elementBuilder.withMemberReference(
                                Long.parseLong(reader.getAttributeValue(null, "ref")),
                                OsmElementBuilder.OsmElementType.fromString(reader.getAttributeValue(null, "type")),
                                ParserOsmRelation.OsmRelationMemberType.fromString(reader.getAttributeValue(null, "role").intern())
                        );
                    }
                    continue;
                }

                if (reader.isEndElement()) {
                    String name = reader.getLocalName();
                    if (name.equals("node") || name.equals("way") || name.equals("relation")) {
                        elementBuilder.buildAndAddElement();
                    }
                }
            }

            elementBuilder.clear();
            logger.info("Finished parsing file {} in {}ms", fileName, String.format("%.3f", (System.nanoTime() - startTime) / 1_000_000d));

        } catch (IOException | XMLStreamException e) {
            logger.error("Failed to parse file", e);
            throw new UnsupportedOperationException("Failed to parse file");
        }
    }
}
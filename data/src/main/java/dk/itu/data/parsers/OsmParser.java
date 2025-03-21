package dk.itu.data.parsers;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.dto.OsmElementBuilder;
import dk.itu.data.dto.OsmParserResult;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;

public class OsmParser {
    private static final Logger logger = LoggerFactory.getLogger();

    public static void parse(String fileName, OsmParserResult osmParserResult) {
        logger.info("Starting parsing file {}", fileName);

        OsmElementBuilder elementBuilder = new OsmElementBuilder(osmParserResult);

        try (InputStream is = CommonConfiguration.class.getClassLoader().getResourceAsStream("osm/"+fileName)) {
            // Reading utils
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
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
                        case "bounds" -> osmParserResult.setBounds(
                                Double.parseDouble(reader.getAttributeValue(null, "minlon")),
                                Double.parseDouble(reader.getAttributeValue(null, "minlat")),
                                Double.parseDouble(reader.getAttributeValue(null, "maxlon")),
                                Double.parseDouble(reader.getAttributeValue(null, "maxlat"))
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

        } catch (IOException | XMLStreamException e) {
            logger.error("Failed to parse file", e);
            throw new UnsupportedOperationException("Failed to parse file");
        }
    }
}
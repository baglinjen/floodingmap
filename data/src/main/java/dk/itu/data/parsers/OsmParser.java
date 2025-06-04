package dk.itu.data.parsers;

import ch.randelshofer.fastdoubleparser.JavaFloatParser;
import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.common.configurations.DrawingConfiguration;
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
                        case "tag" -> {
                            String tagKey = reader.getAttributeValue(null, "k");
                            if (DrawingConfiguration.getInstance().getTagKeys().contains(tagKey)) {
                                // Only add tags which are drawable + highway + type (see DrawingConfiguration)
                                elementBuilder.withTag(
                                        tagKey,
                                        reader.getAttributeValue(null, "v")
                                );
                            }
                        }
                        case "node" -> elementBuilder
                                .withType(OsmElementBuilder.OsmElementType.NODE)
                                .withCoordinates(
                                        JavaFloatParser.parseFloat(reader.getAttributeValue(null, "lat")),
                                        JavaFloatParser.parseFloat(reader.getAttributeValue(null, "lon"))
                                )
                                .withId(fastParseLong(reader.getAttributeValue(null, "id")));
                        case "way" -> elementBuilder
                                .withType(OsmElementBuilder.OsmElementType.WAY)
                                .withId(fastParseLong(reader.getAttributeValue(null, "id")));
                        case "relation" -> elementBuilder
                                .withType(OsmElementBuilder.OsmElementType.RELATION)
                                .withId(fastParseLong(reader.getAttributeValue(null, "id")));
                        case "nd" -> elementBuilder.withNodeReference(fastParseLong(reader.getAttributeValue(null, "ref")));
                        case "member" -> elementBuilder.withMemberReference(
                                fastParseLong(reader.getAttributeValue(null, "ref")),
                                OsmElementBuilder.OsmElementType.fromString(reader.getAttributeValue(null, "type")),
                                ParserOsmRelation.OsmRelationMemberType.fromString(reader.getAttributeValue(null, "role"))
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

    /**
     * Fast long parser optimized for positive numbers (OSM IDs)
     * About 2-3x faster than Long.parseLong() for typical OSM data
     */
    private static long fastParseLong(String str) {
        long result = 0L;
        int len = str.length();

        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c >= '0' && c <= '9') {
                result = result * 10L + (c - '0'); // Parse using bit representation
            } else {
                break; // Stop on first non-digit
            }
        }

        return result;
    }
}
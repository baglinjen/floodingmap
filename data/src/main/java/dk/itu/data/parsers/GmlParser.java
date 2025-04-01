package dk.itu.data.parsers;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.dto.GeoJsonParserResult;
import dk.itu.data.dto.GmlElementBuilder;
import dk.itu.data.dto.GmlParserResult;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;

import static dk.itu.util.CoordinateConverter.convertLatLonToUTM;

public class GmlParser {
    public static void parse(double minLon, double minLat, double maxLon, double maxLat, GmlParserResult result) {
        parse(() -> {
            try {
                var token = CommonConfiguration.getInstance().getDataForsyningenToken();
                if (token == null) {
                    // Logger
                    return null;
                }
                var minBounds = convertLatLonToUTM(minLat, minLon);
                var maxBounds = convertLatLonToUTM(maxLat, maxLon);
                StringBuilder sb = new StringBuilder("https://api.dataforsyningen.dk/DHMhoejdekurver_GML3_DAF?");
                sb
                        .append("token=").append(token)
                        .append("&service=WFS")
                        .append("&request=GetFeature")
                        .append("&version=2.0.0")
                        .append("&typenames=Formkurve2_5")
                        .append("&SRSNAME=EPSG:25832")
                        .append("&BBOX=").append(minBounds[0]).append(",").append(minBounds[1]).append(",").append(maxBounds[0]).append(",").append(maxBounds[1]);
                URL url = new URL(sb.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                if (connection.getResponseCode() != 200) {
                    // Logger
                    return null;
                }
                return connection.getInputStream();
            } catch (Exception e) {
                // Logger
                return null;
            }
        }, result);
    }

    public static void parse(String gmlFileName, GmlParserResult result) {
        parse(() -> CommonConfiguration.class.getClassLoader().getResourceAsStream("gml/" + gmlFileName), result);
    }

    private static void parse(Supplier<InputStream> inputStreamSupplier, GmlParserResult result) {

        GmlElementBuilder elementBuilder = new GmlElementBuilder(result);

        try (InputStream is = inputStreamSupplier.get()) {
            if (is == null) {
                return;
            }
            // Reading utils
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);

            reader.next();
            while (reader.hasNext()) {
                if (reader.isStartElement()) {
                    switch (reader.getLocalName()) {
                        case "Formkurve2_5":
                            elementBuilder.withId(reader.getAttributeValue(null, "id"));
                            reader.next();
                            break;
                        case "posList":
                            reader.next();
                            StringBuilder sb = new StringBuilder();
                            while (reader.hasText()) {
                                sb.append(reader.getText());
                                reader.next();
                            }
                            elementBuilder.withCoords(sb.toString());
                            break;
                        default:
                            reader.next();
                            break;
                    }
                    continue;
                }

                if (reader.isEndElement() && reader.getLocalName().equals("Formkurve2_5")) {
                    elementBuilder.buildAndAddElement();
                    reader.next();
                    continue;
                }

                reader.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        result.buildGeoJsonElements();
    }
}
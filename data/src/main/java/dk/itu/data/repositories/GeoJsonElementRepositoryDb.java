package dk.itu.data.repositories;

import dk.itu.common.models.GeoJsonElement;
import dk.itu.data.models.parser.ParserGeoJsonElement;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class GeoJsonElementRepositoryDb implements GeoJsonElementRepository {
    private final DSLContext ctx;

    public GeoJsonElementRepositoryDb(Connection connection) {
        throw new UnsupportedOperationException("GeoJsonElementRepositoryDb is not implemented yet");
//        ctx = DSL.using(connection, SQLDialect.POSTGRES);
    }

    @Override
    public void add(List<ParserGeoJsonElement> geoJsonElements) {

    }

    @Override
    public List<GeoJsonElement> getGeoJsonElements() {
        return new ArrayList<>();
    }

    @Override
    public float getMinWaterLevel() {
        return 0;
    }

    @Override
    public float getMaxWaterLevel() {
        return 10;
    }
}

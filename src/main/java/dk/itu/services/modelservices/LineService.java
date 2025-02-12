package dk.itu.services.modelservices;
import dk.itu.drawing.utils.ColorUtils;
import dk.itu.models.OsmElement;
import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import dk.itu.models.dbmodels.DbLine;
import dk.itu.models.dbmodels.DbLineCoord;
import dk.itu.utils.HibernateUtil;
import javafx.scene.paint.Color;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class LineService {
    private static Session session;

    public static List<OsmElement> LoadLinesFromDb() throws Exception {
        List<OsmElement> ways = new ArrayList<>();
        List<DbLine> lines;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            lines = session.createQuery("FROM DbLine", DbLine.class).getResultList();
            if (lines == null) throw new Exception("The ways could not be extracted from the database");

            int waterLevel = Integer.parseInt(System.getenv("WL"));
            int blackColorCode = ColorUtils.toARGB(Color.BLACK);
            int blueColorCode = ColorUtils.toARGB(Color.BLUE);

            for(DbLine line : lines)
            {
                boolean isUnderwater = waterLevel > line.getAltitude();
                List<OsmNode> nodes = new ArrayList<>();

                //for(DbLineCoord coord : line.getCoords())
                for(int i = 0; i < line.getCoords().size(); i++)
                {
                    DbLineCoord coord = line.getCoords().get(i);
                    OsmNode node = new OsmNode();
                    node.setId(i);
                    node.setLat(BigDecimal.valueOf(coord.getLatitude()).setScale(7, RoundingMode.HALF_EVEN).doubleValue());
                    node.setLon(BigDecimal.valueOf(coord.getLongitude()).setScale(7, RoundingMode.HALF_EVEN).doubleValue());
                    nodes.add(node);
                }

                //nodes.add(nodes.getFirst());

                OsmWay way = new OsmWay(Long.parseLong(line.getId()), nodes, (isUnderwater ? blueColorCode : blackColorCode), false);
                ways.add(way);
            }

            return ways;
        } catch (Exception e){
            System.out.println(e);
            return ways;
        } finally {
            session.close();
        }
    }
}

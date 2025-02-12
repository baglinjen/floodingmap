package dk.itu.services.modelservices;
import dk.itu.drawing.utils.ColorUtils;
import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import dk.itu.models.dbmodels.DbLine;
import dk.itu.models.dbmodels.DbLineCoord;
import dk.itu.utils.HibernateUtil;
import javafx.scene.paint.Color;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class LineService {
    private static Session session;

    public static List<OsmWay> LoadLinesFromDb() throws Exception {
        List<OsmWay> ways = new ArrayList<>();
        List<DbLine> lines;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            lines = session.createQuery("FROM DbLine", DbLine.class).getResultList();
            if (lines == null) throw new Exception("The ways could not be extracted from the database");

            int colorCode = ColorUtils.toARGB(Color.BLACK);

            for(DbLine line : lines)
            {
                List<OsmNode> nodes = new ArrayList<>();
                for(DbLineCoord coord : line.getCoords())
                {
                    OsmNode node = new OsmNode();
                    node.setLat((float)coord.getLatitude());
                    node.setLon((float)coord.getLongitude());
                    nodes.add(node);
                }
                OsmWay way = new OsmWay(0L, nodes, colorCode);
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

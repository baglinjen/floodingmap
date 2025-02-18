package dk.itu.services;

import dk.itu.drawing.models.MapModel;
import dk.itu.drawing.models.MapModelDb;
import dk.itu.models.OsmElement;
import dk.itu.models.OsmWay;
import dk.itu.models.dbmodels.DbMetadata;
import dk.itu.services.modelservices.LineService;
import dk.itu.services.modelservices.WayService;
import dk.itu.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class DbService {
    private Session session = null;
    private Transaction transaction = null;
    private WayService wayService = null;
    private LineService lineService;

    public DbService()
    {
        session = HibernateUtil.getSessionFactory().openSession();
        transaction = session.beginTransaction();
        wayService = new WayService();
        lineService = new LineService();
    }

    public <T> T getObject(Class<T> object, long id)
    {
        return session.get(object, id);
    }

    public MapModel GenerateMapModel()
    {
        try
        {
            DbMetadata dbMetadata = getMetadata();

            // List<OsmRelation> relations = RelationService.LoadRelationsFromDb();
            List<OsmWay> allWays = WayService.loadWaysFromDb();
            List<OsmElement> allAreaElements = new ArrayList<>();
            List<OsmElement> allPathElements = new ArrayList<>();

            for (OsmWay way : allWays) {
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
            }

            double minLon = dbMetadata.getMinlon();
            double minLat = dbMetadata.getMinlat();
            double maxLat = dbMetadata.getMaxlat();
            double maxLon = dbMetadata.getMaxlon();

            return new MapModelDb(minLon, minLat, maxLat, maxLon, allAreaElements, allPathElements);
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public DbMetadata getMetadata(){
        return session.createQuery("FROM DbMetadata", DbMetadata.class).getResultList().getFirst();
    }
}

package dk.itu.services;

import dk.itu.drawing.models.MapModel;
import dk.itu.drawing.models.MapModelDb;
import dk.itu.models.OsmWay;
import dk.itu.models.dbmodels.DbLine;
import dk.itu.models.dbmodels.DbMetadata;
import dk.itu.services.modelservices.LineService;
import dk.itu.services.modelservices.WayService;
import dk.itu.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
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
            List<OsmWay> dbLines = lineService.LoadLinesFromDb();
            System.out.println(dbLines.size());
            MapModelDb mapModelDb = new MapModelDb(dbMetadata.getMinlon(), dbMetadata.getMinlat(), dbMetadata.getMaxlat(), null);
        } catch (Exception e) { System.out.println(e); }
//        try {
//            List<OsmWay> ways = wayService.LoadWaysFromDb();
//            System.out.println(ways.size());
//        } catch (Exception e)
//        {
//            return null;
//        }
//
        return null;
    }

    public DbMetadata getMetadata(){
        return session.createQuery("FROM DbMetadata", DbMetadata.class).getResultList().getFirst();
    }
}

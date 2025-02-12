package dk.itu.services;

import dk.itu.drawing.models.MapModel;
import dk.itu.models.OsmWay;
import dk.itu.models.dbmodels.DbMetadata;
import dk.itu.services.modelservices.WayService;
import dk.itu.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;

public class DbService {
    private Session session = null;
    private Transaction transaction = null;
    private WayService wayService = null;

    public DbService()
    {
        session = HibernateUtil.getSessionFactory().openSession();
        transaction = session.beginTransaction();
        wayService = new WayService();
    }

    public <T> T getObject(Class<T> object, long id)
    {
        return session.get(object, id);
    }

    public MapModel GenerateMapModel()
    {
        // DbMetadata dbMetadata = getMetadata();
        try {
            List<OsmWay> ways = wayService.LoadWaysFromDb();
            System.out.println(ways.size());
        } catch (Exception e)
        {
            return null;
        }

        return null;
    }

    public DbMetadata getMetadata(){
        return session.createQuery("FROM DbMetadata", DbMetadata.class).getResultList().getFirst();
    }
}

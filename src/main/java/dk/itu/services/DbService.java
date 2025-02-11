package dk.itu.services;

import dk.itu.drawing.models.MapModel;
import dk.itu.models.OsmElement;
import dk.itu.models.dbmodels.DbNode;
import dk.itu.models.dbmodels.DbWay;
import dk.itu.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

public class DbService {
    private Session session = null;
    private Transaction transaction = null;

    public DbService()
    {
        session = HibernateUtil.getSessionFactory().openSession();
        transaction = session.beginTransaction();
    }

    public <T> T getObject(Class<T> object, long id)
    {
        return session.get(object, id);
    }

    public MapModel GenerateMapModel()
    {
        return null;
    }
}

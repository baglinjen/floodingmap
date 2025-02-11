package dk.itu.services;

import dk.itu.drawing.models.MapModel;
import dk.itu.models.dbmodels.DbNode;
import dk.itu.models.dbmodels.DbWay;
import dk.itu.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class DbService {
    private Session session = null;
    private Transaction transaction = null;

    public DbService()
    {
        session = HibernateUtil.getSessionFactory().openSession();
        transaction = session.beginTransaction();
    }

//    public DbNode getNode(long id)
//    {
//        return session.get(DbNode.class, id);
//    }
//
//    public DbWay getWay(long id)
//    {
//        return session.get(DbWay.class, id);
//    }

    public <T> T getObject(Class<T> object, long id)
    {
        return session.get(object, id);
    }

    public List<DbNode> getNodesInWay(long id)
    {
        DbWay way = getObject(DbWay.class, id);
//        String json = session.createQuery(
//                "select nodes " +
//                        "from ways " +
//                        "where id = :id", String.class)
//                .setParameter("id", id)
//                .uniqueResult();

        return null;
    }

    public MapModel GenerateMapModel()
    {
        return null;
    }
}

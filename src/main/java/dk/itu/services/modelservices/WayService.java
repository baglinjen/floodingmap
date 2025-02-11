package dk.itu.services.modelservices;

import dk.itu.models.dbmodels.DbNode;
import dk.itu.models.dbmodels.DbWay;
import dk.itu.utils.HibernateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WayService {

    private Session session;
    private Transaction transaction;
    @PersistenceContext
    private EntityManager entityManager;

    public WayService(){
        session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
    }

    public DbWay loadWayWithNodes(Long wayId) {
        DbWay way = session.find(DbWay.class, wayId);

        // Fetch associated nodes based on the nodeIds
        if (way != null && way.getNodeIds() != null && !way.getNodeIds().isEmpty()) {
            List<DbNode> nodes = session.createQuery(
                            "SELECT n FROM DbNode n WHERE n.id IN :ids", DbNode.class)
                    .setParameter("ids", way.getNodeIds())
                    .getResultList();
            way.setNodes(nodes);
        }

        return way;
    }

    public List<DbWay> loadAllWays(){
        List<DbWay> ways = new ArrayList<>();
        List x = session.createQuery("SELECT id FROM DbWay").getResultList();
        for(Object l : x){
            ways.add(loadWayWithNodes((Long)l));
        }

        return ways;
    }
}

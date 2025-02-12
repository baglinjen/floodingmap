package dk.itu.services.modelservices;

import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import dk.itu.utils.HibernateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public OsmWay LoadWayFromDb(Long wayId) throws Exception {
        OsmWay way = session.find(OsmWay.class, wayId);
        if(way == null) throw new Exception("The way could not be extracted from the database");

        //Retrive all nodes related to way
        var query = "SELECT n FROM OsmNode n WHERE n.id IN :ids ORDER BY CASE n.id ";
        List<Long> nodeIds = way.getNodeIds();
        for (int i = 0; i < nodeIds.size(); i++) {
            query += "WHEN " + nodeIds.get(i) + " THEN " + i + " ";
        }
        query += "END";

        List<OsmNode> nodes = session.createQuery(query, OsmNode.class)
                .setParameter("ids", nodeIds)
                .getResultList();

        //Ensure way "close" if needed
        if(Objects.equals(way.getNodeIds().getFirst(), way.getNodeIds().getLast())) nodes.add(nodes.getFirst());

        way.setNodes(nodes);
        way.GeneratePath();

        return way;
    }

    public List<OsmWay> GetAllWays(){
        List<OsmWay> ways = new ArrayList<>();
        List x = session.createQuery("SELECT id FROM OsmWay").getResultList();
        for(Object l : x){
            try{
                ways.add(LoadWayFromDb((Long)l));
            } catch(Exception e){
                System.out.println("A way was not loaded");
            }
        }

        return ways;
    }
}

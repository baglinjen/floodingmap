package dk.itu.services.modelservices;

import dk.itu.models.OsmRelation;
import dk.itu.models.OsmWay;
import dk.itu.utils.HibernateUtil;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RelationService {
    private static Session session;

    public static List<OsmRelation> loadRelationsFromDb() throws Exception {
        List<OsmRelation> relations = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            relations = session.createQuery("FROM OsmRelation", OsmRelation.class).getResultList();
            if (relations == null) throw new Exception("The relations could not be extracted from the database");

            // Retrieve all nodes related to way
            for(OsmRelation relation : relations)
            {
                List<Long> outerWaysIds = relation.getOuterWaysIds();
                List<Long> innerWaysIds = relation.getInnerWaysIds();

                List<OsmWay> outerWays = session.createNativeQuery(
                                "SELECT * FROM ways w WHERE w.id = ANY(:ids) ORDER BY array_position(:ids, w.id)", OsmWay.class)
                        .setParameter("ids", outerWaysIds.toArray(new Long[0]))
                        .getResultList();
                List<OsmWay> innerWays = session.createNativeQuery(
                                "SELECT * FROM ways w WHERE w.id = ANY(:ids) ORDER BY array_position(:ids, w.id)", OsmWay.class)
                        .setParameter("ids", innerWaysIds.toArray(new Long[0]))
                        .getResultList();

                // Ensure way "close" if needed
//                if (Objects.equals(nodeIds.getFirst(), nodeIds.getLast())) nodes.add(nodes.getFirst());
                // TODO: Consider calculating max/min lat/lon in the data tool instead to improve performance

                relation.setInnerWays(innerWays);
                relation.setOuterWays(outerWays);
            }

            return relations;
        } catch (Exception e){
            System.out.println(e);
            return relations;
        } finally {
            session.close();
        }
    }
}
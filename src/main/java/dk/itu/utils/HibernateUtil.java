package dk.itu.utils;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration conf = new Configuration();
            conf.configure("hibernate.cfg.xml");

            conf.setProperty("hibernate.connection.url", String.format("jdbc:postgresql://localhost:5432/%s", System.getenv("DBNAME")));
            conf.setProperty("hibernate.connection.username", System.getenv("DBUSER"));
            conf.setProperty("hibernate.connection.password", System.getenv("DBPASSWORD"));

            return conf.buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}

package dk.itu.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeUtils {
    private static final Logger logger = LogManager.getLogger();
    public static void timeFunction(String name, Runnable function) {
        long start = System.nanoTime();

        function.run();

        logger.info("{} took {}ms\n", name, String.format("%.3f", (System.nanoTime() - start) / 1000000f));
    }
}
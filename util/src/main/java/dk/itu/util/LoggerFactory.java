package dk.itu.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.StackLocatorUtil;

import java.util.HashMap;

public class LoggerFactory {
    private static final HashMap<Class<?>, Logger> loggers = new HashMap<>();

    public static Logger getLogger() {
        return getLogger(StackLocatorUtil.getCallerClass(2));
    }

    private static Logger getLogger(Class<?> clazz) {
        return loggers.computeIfAbsent(clazz, k -> LogManager.getLogger(k.getName()));
    }
}

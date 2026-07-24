package utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SystemLogger {
    
    public static Logger setupLogger(String className, String appRootPath) {
        Logger logger = Logger.getLogger(className);
        
        try {
            if (logger.getHandlers().length == 0) {

                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setFormatter(new SimpleFormatter());

                logger.addHandler(consoleHandler);

                logger.setUseParentHandlers(false);

                logger.setLevel(Level.INFO);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return logger;
    }
}

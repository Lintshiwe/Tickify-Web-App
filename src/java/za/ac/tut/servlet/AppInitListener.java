

package za.ac.tut.servlet;


import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import za.ac.tut.databaseConnection.DatabaseInitializer;
 

@WebListener
public class AppInitListener implements ServletContextListener {
    
       @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Tickify: Initializing database...");
        DatabaseInitializer.initialize();
        System.out.println("Tickify: Database ready.");
    }
 
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ClassLoader appClassLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver != null && driver.getClass().getClassLoader() == appClassLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                    System.out.println("Tickify: Deregistered JDBC driver " + driver.getClass().getName());
                } catch (SQLException ex) {
                    System.err.println("Tickify: Failed to deregister JDBC driver "
                            + driver.getClass().getName() + ": " + ex.getMessage());
                }
            }
        }
        System.out.println("Tickify: Application shutting down.");
    }
}

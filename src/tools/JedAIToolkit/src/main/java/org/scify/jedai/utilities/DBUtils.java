package org.scify.jedai.utilities;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Database static utility methods.
 */
public final class DBUtils {
    /** 
     * Creates and returns and database connection. This method parses the specified DB URL and 
     * attempts to infer a database dialect from that URL.
     * <p>The only supported databases are MySQL and PostgreSQL.
     * <p>All checked exceptions are translated into runtime exceptions.
     * 
     * @param dbURL portion of the JDBC connection string
     * @return the non-null database connection. This must be closed by the caller.
     * @throws IllegalStateException if the specified {@code dbURL} string does not identify a 
     *    supported database dialect.
     * @throws RuntimeException if any other exception occurs
     */
    public static Connection getDBConnection(String dbURL, String dbUser, String dbPassword, boolean ssl) 
        throws IllegalStateException, RuntimeException {
      
        requireNonNull(dbURL, "dbURL cannot be null");
        requireNonNull(dbUser, "dbUser cannot be null");
        requireNonNull(dbPassword, "dbPassword cannot be null");
        try {
            if (dbURL.startsWith("mysql")) {
                Class.forName("com.mysql.jdbc.Driver");
                return DriverManager.getConnection("jdbc:" + dbURL + "?user=" + dbUser + "&password=" + dbPassword);
            } else if (dbURL.startsWith("postgresql")) {
                final Properties props = new Properties();
                props.setProperty("user", dbUser);
                props.setProperty("password", dbPassword);
                if (ssl) {
                    props.setProperty("ssl", "true");
                }
                return DriverManager.getConnection("jdbc:" + dbURL, props);
            } else {
                throw new IllegalStateException("Only MySQL and PostgreSQL are supported for the time being.");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
    }
}

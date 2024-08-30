package ru.yandex.practicum;

import org.apache.commons.dbcp2.BasicDataSourceFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionPoolConfig {

    private DataSource dataSource;

    private static String host = "127.0.0.1";
    private static String user = "myuser";
    private static String password = "mypassword";
    private static String dbName = "mydatabase";
    private static Integer port = 5432;
    private static String schema = "public";

    public ConnectionPoolConfig(Integer batchSize) throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);

        Properties props = new Properties();
        props.setProperty("url", url);
        props.setProperty("username", user);
        props.setProperty("password", password);
        props.setProperty("initialSize", String.valueOf(batchSize));
        props.setProperty("schema", schema);
        props.setProperty("minIdle", "10");
        props.setProperty("maxTotal", "100");

        dataSource = BasicDataSourceFactory.createDataSource(props);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

}

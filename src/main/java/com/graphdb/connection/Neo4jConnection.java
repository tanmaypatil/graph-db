package com.graphdb.connection;

import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.AuthTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Neo4jConnection {
    private static final Logger logger = LoggerFactory.getLogger(Neo4jConnection.class);
    private static Neo4jConnection instance;
    private Driver driver;

    private Neo4jConnection() {
        Properties props = loadProperties();
        String uri = props.getProperty("neo4j.uri", "bolt://localhost:7687");
        String username = props.getProperty("neo4j.username", "neo4j");
        String password = props.getProperty("neo4j.password", "password");

        logger.info("Connecting to Neo4j at {}", uri);
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }

    public static synchronized Neo4jConnection getInstance() {
        if (instance == null) {
            instance = new Neo4jConnection();
        }
        return instance;
    }

    public Driver getDriver() {
        return driver;
    }

    public void close() {
        if (driver != null) {
            logger.info("Closing Neo4j driver");
            driver.close();
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
                logger.info("Loaded properties from application.properties");
            } else {
                logger.warn("application.properties not found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Error loading properties: {}", e.getMessage());
        }
        return props;
    }
}

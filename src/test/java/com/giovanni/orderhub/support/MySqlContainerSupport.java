package com.giovanni.orderhub.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

/**
 * A single MySQL for all integration tests (singleton container).
 * One container per class would slow the suite down and break Spring's context cache.
 */
public abstract class MySqlContainerSupport {

    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }
}

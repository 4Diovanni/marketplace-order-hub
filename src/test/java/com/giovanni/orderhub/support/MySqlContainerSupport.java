package com.giovanni.orderhub.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

/**
 * Um único MySQL para todos os testes de integração (singleton container).
 * Subir um container por classe deixaria a suíte lenta e quebraria o cache de contexto do Spring.
 */
public abstract class MySqlContainerSupport {

    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }
}

package com.jackis.jsonintegration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackis.jsonintegration.product.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = JsonIntegrationApplicationTests.DatabaseInitializer.class)
class JsonIntegrationApplicationTests {

    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER;

    static {
        POSTGRE_SQL_CONTAINER = new PostgreSQLContainer();
        POSTGRE_SQL_CONTAINER.start();
    }

    /**
     * Initializes the Spring Boot application under test with the required runtime information from the started
     * Postgres Container.
     */
    static class DatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        public DatabaseInitializer() {
        }

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.username=" + POSTGRE_SQL_CONTAINER.getUsername(),
                    "spring.datasource.password=" + POSTGRE_SQL_CONTAINER.getPassword(),
                    "spring.datasource.url=" + POSTGRE_SQL_CONTAINER.getJdbcUrl())
                    .applyTo(configurableApplicationContext);
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper jacksonObjectMapper;

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() throws URISyntaxException {

        jdbcTemplate.update("INSERT INTO product (sku, attributes) VALUES (?, ?::JSONB)",
                UUID.randomUUID(), "{ \"color\": \"green\" }");
        jdbcTemplate.update("INSERT INTO product (sku, attributes) VALUES (?, ?::JSONB)",
                UUID.randomUUID(), "{ \"color\": \"blue\" }");

        final List<Product> products = this.restTemplate
                .execute(new URI("http://localhost:" + port + "/products?attribute=color&value=green"),
                        HttpMethod.GET,
                        request -> {
                            request.getHeaders().add("Content-Type", "application/json");
                        },
                        response -> jacksonObjectMapper
                                .readValue(response.getStatusText(), new TypeReference<>() {
                                }));

        assertThat(products).isNotEmpty();

    }


}

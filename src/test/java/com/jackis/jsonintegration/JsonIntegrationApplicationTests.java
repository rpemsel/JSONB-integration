package com.jackis.jsonintegration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackis.jsonintegration.product.rest.Product;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.ResponseExtractor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = JsonIntegrationApplicationTests.DatabaseInitializer.class)
class JsonIntegrationApplicationTests {

  private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER;

  static {
    POSTGRE_SQL_CONTAINER = new PostgreSQLContainer("postgres:10.11");
    POSTGRE_SQL_CONTAINER.start();
  }

  /**
   * Initializes the Spring Boot application under test with the required runtime information from
   * the started Postgres Container.
   */
  static class DatabaseInitializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {

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

  @BeforeEach
  void addTestData() {
    insertProduct(new Product(UUID.randomUUID().toString(),
        jacksonObjectMapper.createObjectNode().put("color", "green")));

    IntStream.range(0, 10000).forEach(idx -> insertProduct(new Product(UUID.randomUUID().toString(),
        jacksonObjectMapper.createObjectNode().put("color", UUID.randomUUID().toString()))));
  }

  @AfterEach
  void purgeTable() {
    jdbcTemplate.update("TRUNCATE TABLE product");
  }

  @Test
  void searchProduct() throws URISyntaxException {

    final URI uri = new URI("http://localhost:" + port
        + "/products?attributeSearchParameter=" + URLEncoder.encode("{\"color\":\"green\"}",
        StandardCharsets.UTF_8));

    final List<Product> products = searchProduct(uri, response -> {
      final String responseBody = IOUtils
          .toString(response.getBody(), StandardCharsets.UTF_8.name());
      return jacksonObjectMapper.readValue(responseBody, new TypeReference<>() {
      });
    });

    assertThat(products).hasSize(1);
    assertThat(products.get(0)).matches(product ->
        "green".equals(product.getAttributes().get("color").asText())
    );

  }

  @Test
  void verifyUsageOfIndex() {
    final List<String> result = jdbcTemplate.queryForList(
        "EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM product "
            + "WHERE attributes @> CAST('{ \"color\": \"green\" }' AS JSONB)",
        String.class);

    final String concatenatedResult = String.join(" ", result);

    final List<String> indexesResult = jdbcTemplate
        .queryForList("SELECT indexdef FROM pg_indexes WHERE tablename = 'product'", String.class);

    indexesResult.forEach(index -> System.out.println("indexes result: " + index));

    System.out.println("Analyze result: " + concatenatedResult);
    assertThat(concatenatedResult).containsIgnoringCase("index");
  }

  private HttpStatus insertProduct(final Product product) {
    try {
      return this.restTemplate.execute(new URI("http://localhost:" + port
              + "/products"), HttpMethod.POST, request -> {
            request.getHeaders().add("Content-Type", "application/json");
            request.getBody().write(jacksonObjectMapper.writeValueAsBytes(product));
          }, ClientHttpResponse::getStatusCode
      );
    } catch (URISyntaxException exp) {
      throw new RuntimeException(exp);
    }
  }

  private <T> T searchProduct(final URI uri, final ResponseExtractor<T> responseExtractor) {
    return this.restTemplate
        .execute(uri, HttpMethod.GET,
            request -> {
              request.getHeaders().add("Content-Type", "application/json");
            }, responseExtractor);
  }


}

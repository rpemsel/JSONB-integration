package com.jackis.jsonintegration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jackis.jsonintegration.product.rest.Price;
import com.jackis.jsonintegration.product.rest.Product;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

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

    final ObjectNode firstProduct = jacksonObjectMapper.createObjectNode();
    firstProduct.put("brand", "LuxuryBrand");
    firstProduct.set("colors", jacksonObjectMapper.createArrayNode().add("green").add("black"));
    firstProduct.set("weight",
        jacksonObjectMapper.createObjectNode().put("unit", "g").put("value", 43));

    insertProduct(new Product(UUID.randomUUID().toString(), new Price(new BigDecimal(10), "EUR"),
        firstProduct));

    final ObjectNode secondProduct = jacksonObjectMapper.createObjectNode();
    secondProduct.put("brand", "NormalBrand");
    secondProduct.set("colors", jacksonObjectMapper.createArrayNode().add("blue").add("black"));
    secondProduct.set("weight",
        jacksonObjectMapper.createObjectNode().put("unit", "g").put("value", 42));

    insertProduct(new Product(UUID.randomUUID().toString(), new Price(new BigDecimal(0.99f), "EUR"),
        secondProduct));
  }

  @AfterEach
  void purgeTable() {
    jdbcTemplate.update("TRUNCATE TABLE product");
  }

  @Test
  void searchProductBrand() throws URISyntaxException {

    final URI uri = new URI("http://localhost:" + port
        + "/products?attributeSearchParameter=" + URLEncoder.encode("{\"brand\":\"NormalBrand\"}",
        StandardCharsets.UTF_8));

    final List<Product> products = searchProduct(uri, response -> {
      final String responseBody = IOUtils
          .toString(response.getBody(), StandardCharsets.UTF_8.name());
      return jacksonObjectMapper.readValue(responseBody, new TypeReference<>() {
      });
    });

    assertThat(products).hasSize(1);
    assertThat(products.get(0)).matches(product ->
        "NormalBrand".equals(product.getAttributes().get("brand").asText())
    );

  }

  @Test
  void searchProductColors() throws URISyntaxException {

    final URI uri = new URI("http://localhost:" + port
        + "/products?attributeSearchParameter=" + URLEncoder.encode("{\"colors\":[\"green\"]}",
        StandardCharsets.UTF_8));

    final List<Product> products = searchProduct(uri, response -> {
      final String responseBody = IOUtils
          .toString(response.getBody(), StandardCharsets.UTF_8.name());
      return jacksonObjectMapper.readValue(responseBody, new TypeReference<>() {
      });
    });

    assertThat(products).hasSize(1);
    assertThat(products.get(0)).matches(product -> {
          for (JsonNode color : product.getAttributes().get("colors")) {
            if ("green".equals(color.asText())) {
              return true;
            }
          }

          return false;
        }
    );

  }

  @Test
  void verifyUsageOfIndex() throws InterruptedException {

    /* to see the GIN index being used there must be a few entries in the database. Also using
     *  plain JDBC here for entering the rows into the database because this is much faster than
     *  using the REST interface. */
    insertHighNumberOfProducts();

    /* Force updating statistics so query planner knows that there are a lot of rows in the
       product table the index is really used. */
    jdbcTemplate.update("ANALYZE product;");

    /* Wait to be sure to have updated table statistics */
    //    Thread.sleep(5000L);

    final List<String> result = jdbcTemplate.queryForList(
        "EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM product "
            + "WHERE attributes @> CAST('{ \"colors\": [\"green\"] }' AS JSONB)",
        String.class);

    assertThat(String.join(" ", result)).containsIgnoringCase("index");
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
            request -> request.getHeaders().add("Content-Type", "application/json"),
            responseExtractor);
  }

  private void insertHighNumberOfProducts() {
    final Random random = new Random();

    List<Object[]> parameters = IntStream.range(0, 50_000)
        .boxed()
        .map(idx -> {
              final JsonNode jsonNode = jacksonObjectMapper.createObjectNode()
                  .put("color", RandomStringUtils.random(10, true, true))
                  .put("brand", RandomStringUtils.random(10, true, true))
                  .set("weight", jacksonObjectMapper.createObjectNode().put("unit", "g")
                      .put("value", random.nextInt(10000)));

              try {
                return new Object[]{UUID.randomUUID().toString(),
                    new BigDecimal(random.nextFloat()),
                    jacksonObjectMapper.writeValueAsString(jsonNode)};
              } catch (JsonProcessingException exp) {
                throw new RuntimeException(exp);
              }


            }
        ).collect(Collectors.toList());

    jdbcTemplate.batchUpdate("INSERT INTO product (sku, price, currency, attributes) "
            + "VALUES (?, ?, 'EUR', CAST(? AS JSONB))", parameters,
        new int[]{Types.VARCHAR, Types.NUMERIC, Types.CLOB});
  }


}

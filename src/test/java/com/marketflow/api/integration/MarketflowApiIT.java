package com.marketflow.api.integration;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full application against real PostgreSQL, Redis, Kafka and Elasticsearch (Testcontainers):
 * validates Flyway migrations against the JPA mappings, security, problem details and the OpenAPI document.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = "marketflow.rate-limit.enabled=false")
class MarketflowApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @Container
    @ServiceConnection
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.2")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m");

    @Autowired
    private MockMvc mvc;

    @Test
    void exposesOpenApiDocumentation() throws Exception {
        mvc.perform(get("/v3/api-docs/0-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("MarketFlow E-commerce API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/checkout'].post.operationId").value("checkout"));
    }

    @Test
    void registersAndLogsInCustomer() throws Exception {
        String body = """
                {"email":"it-customer@marketflow.dev","password":"S3cretPass1"}""";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRouteAnswersProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/orders").header("X-Correlation-ID", "it-trace-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-ID", "it-trace-1"))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.correlationId").value("it-trace-1"));
    }

    @Test
    void checkoutRequiresIdempotencyKey() throws Exception {
        String token = com.jayway.jsonpath.JsonPath.read(mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"it-idem@marketflow.dev\",\"password\":\"S3cretPass1\"}"))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(post("/api/v1/checkout").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REQUIRED"));
    }
}

package com.marketflow.api.infrastructure.config;

import com.marketflow.api.infrastructure.web.CorrelationIdFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Central Swagger / OpenAPI 3.0 configuration.
 *
 * <ul>
 *   <li>Swagger UI: {@code /swagger-ui.html} — OpenAPI JSON: {@code /v3/api-docs}</li>
 *   <li>Global {@code bearerAuth} (JWT) security scheme; public operations opt out with an empty
 *       {@code @SecurityRequirements}.</li>
 *   <li>Every operation documents the optional {@code X-Correlation-ID} header.</li>
 *   <li>Grouped specs per audience: storefront, customer, seller, admin, integrations.</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI marketflowOpenApi(@Value("${server.port:8080}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("MarketFlow E-commerce API")
                        .version("v1")
                        .description("""
                                Multi-vendor marketplace backend.

                                **Authentication** — call `POST /api/v1/auth/login`, then click *Authorize* and paste the
                                `accessToken` (JWT). Roles: `CUSTOMER`, `SELLER`, `ADMIN`.

                                **Conventions**
                                - Errors follow RFC 7807 (`application/problem+json`) with `errorCode` and `correlationId`.
                                - Send `X-Correlation-ID` to trace a request across logs and Kafka events.
                                - Checkout and order mutations require an `Idempotency-Key` header; retries replay the
                                  first response (`Idempotent-Replayed: true`).
                                - Requests are rate limited per IP (`X-RateLimit-*` headers, `429` + `Retry-After`).
                                - Monetary values are decimals in the order currency.
                                """)
                        .contact(new Contact().name("MarketFlow Engineering").email("engineering@marketflow.dev"))
                        .license(new License().name("Proprietary")))
                .externalDocs(new ExternalDocumentation()
                        .description("Order lifecycle: PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → DELIVERED")
                        .url("https://docs.marketflow.dev"))
                .servers(List.of(new Server().url("http://localhost:" + port).description("Local")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token obtained from /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .tags(List.of(
                        new Tag().name("Authentication").description("Registration, login and logout"),
                        new Tag().name("Catalog").description("Public storefront: full-text search and product pages"),
                        new Tag().name("Taxonomy").description("Categories and brands"),
                        new Tag().name("Products").description("Seller catalog management"),
                        new Tag().name("Inventory").description("Stock levels with optimistic locking"),
                        new Tag().name("Cart").description("Shopping cart cached in Redis"),
                        new Tag().name("Checkout").description("Quote and place orders, reserve stock, create payment intents"),
                        new Tag().name("Orders").description("Order queries and lifecycle (State pattern)"),
                        new Tag().name("Sellers").description("Seller onboarding and verification"),
                        new Tag().name("Coupons").description("Discount coupons (Strategy pattern)"),
                        new Tag().name("Notifications").description("In-app notifications"),
                        new Tag().name("Webhooks").description("Payment provider callbacks")));
    }

    /** Documents the tracing header on every operation. */
    @Bean
    public OperationCustomizer correlationIdHeaderCustomizer() {
        return (operation, handlerMethod) -> operation.addParametersItem(new HeaderParameter()
                .name(CorrelationIdFilter.HEADER)
                .required(false)
                .description("Optional trace id echoed in the response, logs (MDC) and Kafka headers")
                .schema(new StringSchema().example("3f1c2a9e-7b6d-4c1e-9a55-2d8f0e4b7c10")));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder().group("0-all").displayName("All endpoints").pathsToMatch("/api/**").build();
    }

    @Bean
    public GroupedOpenApi storefrontApi() {
        return GroupedOpenApi.builder().group("1-storefront").displayName("Storefront & customer")
                .pathsToMatch("/api/v1/auth/**", "/api/v1/catalog/**", "/api/v1/categories/**", "/api/v1/brands/**",
                        "/api/v1/cart/**", "/api/v1/checkout/**", "/api/v1/orders/**", "/api/v1/notifications/**")
                .build();
    }

    @Bean
    public GroupedOpenApi sellerApi() {
        return GroupedOpenApi.builder().group("2-seller").displayName("Seller back-office")
                .pathsToMatch("/api/v1/sellers/**", "/api/v1/products/**", "/api/v1/inventory/**", "/api/v1/orders/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder().group("3-admin").displayName("Administration")
                .pathsToMatch("/api/v1/admin/**", "/api/v1/coupons/**", "/api/v1/categories/**", "/api/v1/brands/**")
                .build();
    }

    @Bean
    public GroupedOpenApi integrationsApi() {
        return GroupedOpenApi.builder().group("4-integrations").displayName("Integrations (webhooks)")
                .pathsToMatch("/api/v1/webhooks/**")
                .build();
    }
}

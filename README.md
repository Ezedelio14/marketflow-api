# MarketFlow E-commerce API

Backend de uma plataforma de comércio electrónico **multi-vendor** (marketplace), construído com Java 21 e Spring Boot 3.5.

A API está documentada com **Swagger / OpenAPI 3.0** (springdoc-openapi). Todos os controllers, endpoints e DTOs têm
anotações explícitas (`@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, `@RequestBody`, `@Schema`).

---

## Stack tecnológica

| Área | Tecnologia |
|---|---|
| Linguagem / framework | Java 21, Spring Boot 3.5 |
| Segurança | Spring Security, JWT stateless (jjwt), RBAC (`CUSTOMER`, `SELLER`, `ADMIN`) |
| Persistência | Spring Data JPA, Hibernate, PostgreSQL 16, Flyway |
| Cache | Redis: carrinho temporário, deny-list de tokens, idempotência, rate limiting |
| Mensageria | Apache Kafka (Spring Kafka) com retentativas e Dead Letter Topics |
| Pesquisa | Elasticsearch 8 (Spring Data Elasticsearch) |
| Pagamentos | Stripe Java SDK (PaymentIntents, webhooks, reembolsos) |
| Mapeamento | MapStruct, Lombok |
| Documentação | springdoc-openapi-starter-webmvc-ui (Swagger UI) |
| Testes | JUnit 5, Mockito, AssertJ, Testcontainers |

---

## Como executar

### Pré-requisitos

- Java 21+
- Docker Desktop (para PostgreSQL, Redis, Kafka e Elasticsearch)

### 1. Subir a infra-estrutura

```bash
docker compose up -d
```

Se já tiver um PostgreSQL local a usar a porta 5432, use outra porta:

```bash
POSTGRES_PORT=5433 docker compose up -d
export DB_URL=jdbc:postgresql://localhost:5433/marketflow
```

### 2. Arrancar a aplicação

```bash
./mvnw spring-boot:run
```

As migrations Flyway criam o schema automaticamente no arranque.

### 3. Aceder

| Recurso | URL |
|---|---|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Grupos OpenAPI | `/v3/api-docs/0-all`, `1-storefront`, `2-seller`, `3-admin`, `4-integrations` |
| Health check | http://localhost:8080/actuator/health |

No primeiro arranque é criado um administrador (`admin@marketflow.dev` / `Admin@123456`, configurável).
No Swagger UI: chame `POST /api/v1/auth/login`, clique em **Authorize** e cole o `accessToken`.

### Variáveis de ambiente

| Variável | Predefinição |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/marketflow` / `marketflow` / `marketflow` |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `ELASTICSEARCH_URIS` | `http://localhost:9200` |
| `JWT_SECRET` | chave Base64 de desenvolvimento (**altere em produção**, mínimo 256 bits) |
| `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` | placeholders (`sk_test_...` / `whsec_...`) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@marketflow.dev` / `Admin@123456` |

Para receber webhooks do Stripe localmente:

```bash
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

### Testes

```bash
./mvnw test      # testes unitários (JUnit 5 + Mockito)
./mvnw verify    # + testes de integração com Testcontainers (requer Docker)
```

---

## Fluxo típico na API

1. `POST /api/v1/auth/register` cria um cliente.
2. `POST /api/v1/sellers` faz o pedido para ser vendedor, e um admin aprova-o em `POST /api/v1/admin/sellers/{id}/approve`.
3. O admin cria a taxonomia: `POST /api/v1/categories` e `POST /api/v1/brands`.
4. O vendedor cria o produto e as variantes (`POST /api/v1/products`) e publica-o (`POST /api/v1/products/{id}/publish`).
   O produto é indexado no Elasticsearch via Kafka.
5. O cliente pesquisa em `GET /api/v1/catalog/search?q=...` e adiciona ao carrinho (`POST /api/v1/cart/items`).
6. `POST /api/v1/checkout` (com o header `Idempotency-Key`) cria o pedido, reserva o stock e devolve o `clientSecret` do Stripe.
7. O webhook `payment_intent.succeeded` passa o pedido a `PAID`.
8. O vendedor executa `processing`, depois `ship` e por fim `deliver`.

---

## Arquitectura

Arquitectura em camadas com inspiração hexagonal. O domínio não conhece HTTP, Kafka, Stripe nem Elasticsearch. A camada de
aplicação comunica com o exterior através de **portas** (`PaymentGateway`, `EventPublisher`, `ProductSearchPort`,
`ProductIndexer`, `CartCache`, `TokenProvider`, ...), implementadas por **adapters** na infra-estrutura.

```
src/main/java/com/marketflow/api
├── config/                      MarketflowProperties (configuração tipada marketflow.*)
├── domain/                      Entidades ricas, value objects, enums, repositórios, specifications
│   ├── common/                  BaseEntity (PK UUID), AuditableEntity, excepções de domínio
│   ├── user/ seller/ catalog/   User, Seller, Category, Brand, Product, ProductVariant, ProductImage
│   ├── inventory/               Inventory (@Version), InventoryReservation (TTL), locks PESSIMISTIC_WRITE
│   ├── cart/ coupon/ review/ wishlist/ shipment/ notification/
│   ├── payment/                 Payment, ProcessedEvent (idempotência de webhooks)
│   └── order/                   Order (Builder), OrderItem, OrderStatusHistory, ShippingAddress (JSONB)
│       └── state/               State pattern: OrderState + 8 estados + OrderStateMachine
├── application/                 Casos de uso, DTOs (records com @Schema), mappers MapStruct, portas
│   ├── checkout/                CheckoutFacade (Facade), CheckoutPricing, CheckoutQuote (Builder)
│   ├── pricing/                 Strategy: frete (STANDARD/EXPRESS/PICKUP), descontos (PERCENTAGE/FIXED), impostos
│   ├── order/ payment/ inventory/ cart/ catalog/ seller/ coupon/ auth/
│   ├── notification/            NotificationFactory (Factory), NotificationService
│   └── event/                   Eventos de integração, DomainEventFactory (Factory), KafkaTopics
├── infrastructure/
│   ├── config/                  OpenApiConfig, SecurityConfig, KafkaConfig (tópicos, retries, DLT), WebMvcConfig
│   ├── security/                JWT (emissão + filtro), deny-list Redis, respostas 401/403 em Problem Details
│   ├── web/                     X-Correlation-ID (MDC), rate limiting Redis, interceptor Idempotency-Key
│   ├── payment/stripe/          StripePaymentAdapter, StripeWebhookAdapter (Webhook.constructEvent)
│   ├── search/                  ProductDocument, ElasticsearchSyncAdapter, ElasticsearchProductSearchAdapter
│   ├── messaging/               KafkaEventPublisher (after-commit) e consumers
│   ├── cache/                   RedisCartCache
│   └── scheduling/ bootstrap/   Job de expiração de reservas, criação do admin inicial
└── presentation/
    ├── rest/                    Controllers REST documentados com Swagger
    └── error/                   GlobalExceptionHandler (RFC 7807 Problem Details)
src/main/resources/db/migration  Migrations Flyway
```

### Padrões de projecto

| Padrão | Onde |
|---|---|
| **State** | `domain/order/state`. Uma transição inválida lança `InvalidStateTransitionException` (HTTP 409); o histórico fica em `order_status_history`. |
| **Strategy** | `ShippingStrategy` + `ShippingCalculator`, `DiscountStrategy` + `DiscountCalculator` |
| **Factory** | `DomainEventFactory` (eventos), `NotificationFactory` (notificações) |
| **Adapter** | `StripePaymentAdapter`, `StripeWebhookAdapter`, `ElasticsearchSyncAdapter`, `ElasticsearchProductSearchAdapter` |
| **Specification** | `ProductSpecifications`, `OrderSpecifications` (JPA Criteria com sub-queries EXISTS) |
| **Builder / Facade** | `Order.Builder`, `CheckoutQuote`, `CheckoutFacade` |

### Ciclo de vida do pedido

```
PENDING_PAYMENT ─PAY→ PAID ─START_PROCESSING→ PROCESSING ─SHIP→ SHIPPED ─DELIVER→ DELIVERED
       │                │
       └─CANCEL→ CANCELLED └─REQUEST_REFUND→ REFUND_REQUESTED ─REFUND→ REFUNDED
```

### Checkout, stock e concorrência

1. O `CheckoutFacade` valida e precifica o carrinho, cria o pedido em `PENDING_PAYMENT` e reserva o stock com TTL
   (`marketflow.checkout.reservation-ttl`, 15 min por omissão), tudo numa única transacção.
2. A reserva usa primeiro **optimistic locking** (`@Version`). Quando há conflitos com checkouts simultâneos, repete com
   **`PESSIMISTIC_WRITE`** (`SELECT … FOR UPDATE`, com as linhas bloqueadas por ordem de id para evitar deadlocks).
   Constraints na base de dados garantem `available = quantity - reserved ≥ 0`, o que torna o overselling impossível.
3. O PaymentIntent do Stripe é criado **depois do commit**, para não manter locks durante a chamada de rede. Usa a chave
   de idempotência `checkout-<orderId>`. Se o Stripe falhar, há compensação: o pedido é cancelado, o stock libertado e o
   carrinho reaberto.
4. Os webhooks são validados com `Webhook.constructEvent` e processados uma única vez graças à tabela `processed_events`.
5. O `ReservationExpirationJob` cancela os pedidos não pagos quando o TTL expira. Um pagamento que chegue depois disso é
   reembolsado automaticamente.

### Kafka

| Tópico | Produzido quando |
|---|---|
| `order.created` / `order.paid` / `order.cancelled` / `order.shipped` | O pedido muda de estado |
| `payment.succeeded` / `payment.failed` | Chega um webhook de pagamento |
| `inventory.reserved` / `inventory.released` | O stock é reservado ou libertado |
| `product.indexed` | O catálogo muda (sincronização com o Elasticsearch) |
| `notification.send` | É criada uma notificação, para entrega por e-mail ou push |

Cada tópico tem um `<tópico>.DLT`. Os consumers fazem 3 retentativas com back-off exponencial (1s, 2s, 4s) antes de
enviar a mensagem para o DLT. Os eventos são publicados após o commit da transacção.

### Middlewares

- **JWT Bearer** stateless, com logout via deny-list no Redis.
- **`X-Correlation-ID`**: propagado no MDC (logs), na resposta e nos headers Kafka.
- **`Idempotency-Key`**: obrigatório no checkout e nas mutações de pedidos. A primeira resposta fica guardada no Redis
  (24h) e é devolvida nas repetições com o header `Idempotent-Replayed: true`.
- **Rate limiting** com Redis (janela fixa por IP; limites mais apertados em auth e checkout). Responde `429` com
  `Retry-After`.
- **Erros** em RFC 7807 (`application/problem+json`) com `errorCode`, `timestamp` e `correlationId`.

---

## Limitações conhecidas

- O job de expiração de reservas não tem lock distribuído. Com várias instâncias, adicione ShedLock.
- Os reembolsos parciais do Stripe são reconhecidos mas não modelados.
- Reviews e wishlists têm entidades, repositórios e tabelas, mas ainda não têm endpoints REST.
- Para entrega garantida de eventos mesmo em caso de crash entre o commit e o envio, implemente um transactional outbox
  no `KafkaEventPublisher`.

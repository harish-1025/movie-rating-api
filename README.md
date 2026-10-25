# Circuit Breaker, Fallback, and Timeout in Spring Boot Microservices

## 1. Overview

In a microservices architecture, one service frequently depends on other services to complete a request. If a downstream service becomes unavailable or responds slowly, the calling service may experience failures, increased response times, and resource exhaustion.

To handle these situations, we can use three important resilience mechanisms:

- **Circuit Breaker** — prevents repeated calls to an unhealthy downstream service.
- **Fallback** — provides an alternative response when the protected operation fails or is rejected.
- **Timeout** — limits how long the calling service waits for a connection or response.

These mechanisms solve different problems and can work together to improve the reliability of a distributed system.

### Example architecture

In our Movie Catalog application:

- **Movie Catalog Service** retrieves a user's movie ratings.
- **Rating Data Service** returns the user's ratings and movie IDs.
- **Movie Info Service** provides movie details.

The Catalog Service calls the other services to construct the final catalog response.

If Movie Info Service becomes unavailable or slow, the Catalog Service must handle the failure gracefully.

---

## 2. Circuit Breaker Pattern

### What is a Circuit Breaker?

The Circuit Breaker pattern prevents an application from repeatedly calling a downstream service that is experiencing failures.

It monitors the outcomes of calls and changes its state according to a configured policy.

### Why do we need it?

Suppose Movie Info Service is unavailable, but Catalog Service continues sending requests to it.

Repeated failures can waste processing time and resources. If the problem continues, it can contribute to cascading failures across the system.

A Circuit Breaker helps prevent this by temporarily stopping calls when the failure rate exceeds a configured threshold.

### Three Circuit Breaker states

#### 1. CLOSED

- Requests are allowed to reach the downstream service.
- The Circuit Breaker records successful and failed calls.
- When enough calls have been recorded, it evaluates the failure rate.
- If the configured failure threshold is reached, the breaker transitions to OPEN.

**Example:** Five calls are recorded, and three fail. The failure rate is 60%. With a 50% threshold, the breaker can transition to OPEN once the minimum-call requirement is met.

#### 2. OPEN

- New calls through the protected Circuit Breaker are rejected without invoking the protected operation.
- A fallback can handle the resulting exception.
- The breaker waits for the configured open-state duration before permitting recovery checks.

**Important:** Opening the Circuit Breaker does not automatically cancel requests that were already in progress.

#### 3. HALF_OPEN

- After the configured waiting period, the breaker allows a limited number of test calls.
- If the results satisfy the success criteria, it transitions to CLOSED.
- If the results satisfy the failure criteria, it transitions back to OPEN.

For example, with two permitted calls and a 50% failure threshold, one success and one failure produce a 50% failure rate. Under this configuration, that meets the failure threshold.

### Important Circuit Breaker configuration properties

| Property | Purpose |
|---|---|
| `slidingWindowType` | Defines how call outcomes are measured, such as by count or time. |
| `slidingWindowSize` | Defines the number of calls or time-window size used for evaluation, depending on the window type. |
| `minimumNumberOfCalls` | Minimum number of recorded calls required before calculating the failure rate. |
| `failureRateThreshold` | Failure percentage at which the breaker opens. |
| `waitDurationInOpenState` | How long the breaker remains OPEN before recovery checks. |
| `permittedNumberOfCallsInHalfOpenState` | Maximum number of test calls permitted in HALF_OPEN. |
| `automaticTransitionFromOpenToHalfOpenEnabled` | Enables automatic transition after the configured open-state duration. |

**Remember:** `slidingWindowSize` and `minimumNumberOfCalls` have different purposes. A window size of five does not, by itself, mean that the breaker can evaluate failures before its minimum-call requirement is satisfied.

---

## 3. Fallback Mechanism

### What is a fallback?

A fallback is an alternative response or recovery action used when an operation cannot complete normally.

In our application, the Catalog Service can return a predefined response when the protected operation fails.

Fallback is not itself a Circuit Breaker state or a Circuit Breaker pattern. It is a separate failure-handling mechanism that can be used alongside the Circuit Breaker.

### Example using Resilience4j

```java
@GetMapping("/{userId}")
@CircuitBreaker(
    name = "MovieCatalogService",
    fallbackMethod = "catalogFallback"
)
public List<CatalogItem> getCatalog(
        @PathVariable int userId) {

    UserRating ratings = restTemplate.getForObject(
        "http://rating-data-service/ratings/user/" + userId,
        UserRating.class
    );

    return ratings.getRatings().stream()
        .map(rating -> {
            Movie movie = restTemplate.getForObject(
                "http://movie-info-service/movies/"
                    + rating.getMovieId(),
                Movie.class
            );

            return new CatalogItem(
                movie.getName(),
                movie.getDescription(),
                rating.getRating()
            );
        })
        .collect(Collectors.toList());
}

public List<CatalogItem> catalogFallback(
        int userId, Throwable exception) {

    System.out.println(
        "Catalog request failed: " + exception.getMessage()
    );

    return List.of(
        new CatalogItem(
            "fallback",
            "Movie catalog is temporarily unavailable",
            0
        )
    );
}
```

*Note: This is an illustrative example. It assumes the application's DTOs, imports, and RestTemplate configuration are already in place.*

### How does the fallback work?

1. A request reaches `getCatalog()`.
2. The protected operation executes.
3. A downstream call fails, a timeout occurs, or the Circuit Breaker rejects the call because it is OPEN.
4. Resilience4j invokes the configured fallback for the applicable exception.
5. The fallback returns the alternative response.

The fallback method must have a compatible return type and parameters corresponding to the original method, followed optionally by a compatible exception parameter.

### Important considerations

- A fallback should not silently hide every programming error.
- A fallback that throws an exception can still result in a failed request.
- Returning a placeholder response is useful for demonstrations, but production APIs should define an appropriate response contract.
- If partial results are acceptable, the application may return available data instead of replacing the entire response.

---

## 4. Timeout Mechanism

### What is a timeout?

A timeout limits how long the calling service waits for a particular operation.

Without suitable timeouts, a slow or unresponsive downstream service can keep client resources occupied for too long.

For synchronous HTTP calls made using RestTemplate, two important timeout settings are connection timeout and read timeout.

### 1. Connection timeout

The connection timeout limits how long the client waits to establish a connection to the target service.

**Example:** Catalog Service attempts to connect to Movie Info Service, but the connection cannot be established within the configured two-second limit.

The connection attempt may fail with a timeout-related exception.

### 2. Read timeout

The read timeout limits how long the client waits for response data during a read operation, according to the configured HTTP client's behavior.

**Example:** Movie Info Service accepts the request but takes too long to send a response. The client may time out after its configured read-timeout period.

### Example configuration

For a compatible Spring Boot `RestTemplateBuilder` configuration:

```java
import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(
            RestTemplateBuilder builder) {

        return builder
            .connectTimeout(Duration.ofSeconds(2))
            .readTimeout(Duration.ofSeconds(3))
            .build();
    }
}
```

This example configures a two-second connection timeout and a three-second read timeout.

**Note:** Verify the builder API and timeout behavior against the HTTP request factory used by your Spring Boot version. These are not necessarily strict end-to-end deadlines for the entire Catalog operation.

### Connection timeout vs. read timeout

| Connection timeout | Read timeout |
|---|---|
| Limits the wait to establish a connection. | Limits waiting for response data during a read. |
| Applies while establishing the connection. | Applies after the connection has been established, when reading the response. |
| Helps handle connection-establishment delays. | Helps handle slow or stalled responses. |

### Does a client timeout stop the server's work?

Not necessarily. A client-side timeout limits how long the client waits. It does not guarantee that the downstream server immediately cancels processing.

---

## 5. How Circuit Breaker, Fallback, and Timeout Work Together

Consider a request to retrieve a user's movie catalog.

### Scenario A: Downstream service is slow

1. Catalog Service calls Movie Info Service.
2. The connection is established, but the response is too slow.
3. The configured read timeout is exceeded.
4. The HTTP client throws an exception.
5. The Circuit Breaker can record the failed protected operation.
6. The fallback returns an alternative response, provided the exception is handled by the configured fallback.

If the failure rate reaches the configured threshold after the minimum-call requirement is met, the Circuit Breaker can transition to OPEN.

### Scenario B: Circuit Breaker is already OPEN

1. A new request reaches Catalog Service.
2. The Circuit Breaker rejects the protected operation.
3. The downstream HTTP request is not made by that protected invocation.
4. The fallback can return the alternative response.

The HTTP timeout does not need to expire in this scenario because the protected downstream call is not attempted.

### Scenario C: Downstream service recovers

1. The breaker remains OPEN for the configured waiting period.
2. It transitions to HALF_OPEN when recovery checks are permitted.
3. A limited number of test calls are allowed.
4. Successful results can close the breaker; sufficiently high failure rates can reopen it.

### Important limitation

A timeout and a Circuit Breaker do not automatically limit every kind of resource consumption. For example, already-running requests may continue, and multiple sequential downstream calls can make the total Catalog operation take longer than the timeout configured for any individual HTTP call.

---

## 6. Circuit Breaker Configuration Example

The following is an example `application.yml` configuration for Resilience4j:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      MovieCatalogService:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 5
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 2
        automaticTransitionFromOpenToHalfOpenEnabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,circuitbreakers,circuitbreakerevents
  endpoint:
    health:
      show-details: always
```

### How to interpret these settings

- The sliding window evaluates up to five calls.
- At least five calls must be recorded before the failure-rate calculation can trigger opening under this configuration.
- A failure rate of 50% or greater meets the configured threshold.
- The OPEN state lasts for ten seconds before recovery checks are permitted.
- In HALF_OPEN, up to two test calls are permitted.

This configuration defines Circuit Breaker behavior. It does **not** configure the HTTP connection or read timeouts; those belong to the HTTP client configuration.

---

## 7. Common Misconceptions

### Does opening the Circuit Breaker cancel in-flight requests?

No. It prevents new protected calls from proceeding through the open breaker. Already-running requests are not automatically cancelled.

### Does a timeout automatically open the Circuit Breaker?

No. A timeout can cause a failure, and the breaker can record that failure. The breaker opens only when its configured state-transition criteria are met.

### Does fallback mean the Circuit Breaker is OPEN?

No. Fallback may execute when an exception occurs while the breaker is CLOSED or HALF_OPEN, as well as when the breaker rejects a call in OPEN.

### Is fallback a replacement for timeout?

No. A timeout limits waiting. Fallback defines what alternative response to return when an applicable failure occurs.

### Does a read timeout cancel downstream processing?

Not necessarily. It limits client-side waiting and does not guarantee server-side cancellation.

### Does the Circuit Breaker protect every call in a method automatically?

No. Resilience4j applies to the operation intercepted by the annotation. In this example, the annotation protects the `getCatalog()` method as a whole, including its downstream calls. Individual service calls can require separate policies if different behavior is needed.

---


## 9. Summary

| Mechanism | Main responsibility |
|---|---|
| Circuit Breaker | Prevents repeated calls to an unhealthy dependency. |
| Fallback | Supplies an alternative response or recovery action. |
| Connection timeout | Limits waiting to establish a connection. |
| Read timeout | Limits waiting for response data during a read operation. |

**Key takeaway:** Circuit Breaker, fallback, and timeout complement one another, but they are separate mechanisms. The Circuit Breaker controls whether calls are permitted, timeouts limit client-side waiting, and fallback defines how an applicable failure is handled.

**Next learning topics:** Retry pattern, Bulkhead pattern, and how to combine these mechanisms safely in production microservices.

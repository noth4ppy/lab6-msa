package io.hexlet.spring.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import io.hexlet.spring.config.FeignClientConfig;
import io.hexlet.spring.dto.Order;

/**
 * Declarative Feign client of the Order-Service (LR6).
 *
 * <p>Spring Cloud OpenFeign creates a proxy for this interface at startup
 * ({@code @EnableFeignClients} on {@link io.hexlet.spring.Lab1Application}); every method call is turned
 * into an HTTP request to {@code ${clients.order-service.url}} + the path of the method mapping.
 * <ul>
 *   <li>{@code name = "order-service"} - logical name of the target service (the methodology uses
 *       {@code "Order-Service"}); it is also the key of the per-client settings
 *       {@code spring.cloud.openfeign.client.config.order-service.*} (timeouts);</li>
 *   <li>{@code url} - the address is set explicitly because there is no service registry (Eureka);</li>
 *   <li>{@code configuration} - {@link FeignClientConfig}: one-line logger {@link OneLineLogger}, log level,
 *       retryer, request interceptor;</li>
 *   <li>{@code fallback} - {@link OrderClientFallback}: returned when the Order-Service is unavailable
 *       (requires {@code spring.cloud.openfeign.circuitbreaker.enabled=true}).</li>
 * </ul>
 * Response format of both methods: JSON array of {@link Order}
 * ({@code [{"id":1,"userId":"1","bookId":1,"bookTitle":"...","quantity":1,"status":"NEW"}]}).
 */
@FeignClient(
        name = "order-service",
        url = "${clients.order-service.url}",
        configuration = FeignClientConfig.class,
        fallback = OrderClientFallback.class)
public interface OrderClient {

    /**
     * Orders of a user: {@code GET {url}/orders/{userId}}.
     *
     * @param userId user identifier, substituted into the path variable {@code {userId}}
     * @return orders of the user; an empty list if the user has no orders or the service is unavailable
     */
    @GetMapping("/orders/{userId}")
    List<Order> getOrdersByUserId(@PathVariable("userId") String userId);

    /**
     * Orders in a status: {@code GET {url}/orders?status={orderStatus}}.
     * The Java parameter name differs from the HTTP one - {@code @RequestParam("status")} sets the name
     * of the query parameter explicitly.
     *
     * @param orderStatus order status ({@code NEW}, {@code PAID}, {@code SHIPPED}, {@code DELIVERED}),
     *                    sent as the query parameter {@code status}
     * @return orders in this status; an empty list if there are none or the service is unavailable
     */
    @GetMapping("/orders")
    List<Order> getOrdersByStatus(@RequestParam("status") String orderStatus);
}

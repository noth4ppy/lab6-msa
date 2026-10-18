package io.hexlet.spring.client;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import io.hexlet.spring.dto.Order;
import lombok.extern.slf4j.Slf4j;

/**
 * Fallback of {@link OrderClient} (LR6, "Handling failures"): used by Spring Cloud CircuitBreaker
 * (Resilience4j, a replacement of Hystrix from the methodology) when a call fails - the Order-Service is
 * down, a timeout occurred or it answered with an error. The calling endpoint gets an empty list
 * instead of HTTP 500.
 */
@Slf4j
@Component
public class OrderClientFallback implements OrderClient {

    @Override
    public List<Order> getOrdersByUserId(String userId) {
        log.warn("Order-Service is unavailable: fallback for getOrdersByUserId({}) -> []", userId);
        return Collections.emptyList();
    }

    @Override
    public List<Order> getOrdersByStatus(String orderStatus) {
        log.warn("Order-Service is unavailable: fallback for getOrdersByStatus({}) -> []", orderStatus);
        return Collections.emptyList();
    }
}

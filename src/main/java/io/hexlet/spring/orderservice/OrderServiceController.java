package io.hexlet.spring.orderservice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.hexlet.spring.dto.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Emulated remote "Order-Service" from the methodology (LR6). It lives in the same application
 * under the prefix {@code /order-service} so that the demo works offline; the Feign client
 * {@link io.hexlet.spring.client.OrderClient} calls it over real HTTP by the URL from the property
 * {@code clients.order-service.url}.
 */
@Slf4j
@RestController
@RequestMapping("/order-service")
@RequiredArgsConstructor
@Tag(name = "order-service-stub", description = "Заглушка удалённого Order-Service (ЛР6), вызывается через Feign")
public class OrderServiceController {

    private final OrderStore orderStore;

    @GetMapping("/orders/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Order-Service: заказы пользователя (пустой список, если заказов нет)")
    public List<Order> getOrdersByUserId(
            @Parameter(description = "Идентификатор пользователя", example = "1") @PathVariable("userId") String userId,
            @RequestHeader(value = "X-Request-Source", required = false) String source) {
        log.info("Order-Service: GET /orders/{} (X-Request-Source: {})", userId, source);
        return orderStore.findByUserId(userId);
    }

    @GetMapping("/orders")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Order-Service: заказы по статусу")
    public List<Order> getOrdersByStatus(
            @Parameter(description = "Статус заказа", example = "NEW") @RequestParam("status") String status,
            @RequestHeader(value = "X-Request-Source", required = false) String source) {
        log.info("Order-Service: GET /orders?status={} (X-Request-Source: {})", status, source);
        return orderStore.findByStatus(status);
    }
}

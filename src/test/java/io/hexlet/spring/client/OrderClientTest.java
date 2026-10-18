package io.hexlet.spring.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import io.hexlet.spring.config.FeignClientConfig;
import io.hexlet.spring.dto.Order;

/**
 * Feign client OrderClient against the Order-Service stub (LR6): the proxy makes a real HTTP request,
 * passes {@code @PathVariable("userId")} and {@code @RequestParam("status")} and decodes JSON into
 * {@link Order}; OneLineLogger writes one line per exchange.
 */
@ExtendWith(OutputCaptureExtension.class)
public class OrderClientTest extends AbstractFeignIntegrationTest {

    @Autowired
    private OrderClient orderClient;

    @Test
    public void testClientIsFeignProxy() {
        assertThat(orderClient).isNotInstanceOf(OrderClientFallback.class);
        assertThat(java.lang.reflect.Proxy.isProxyClass(orderClient.getClass())).isTrue();
    }

    @Test
    public void testGetOrdersByUserId() {
        var orders = orderClient.getOrdersByUserId("1");

        assertThat(orders).hasSize(3);
        assertThat(orders).extracting(Order::getUserId).containsOnly("1");
        assertThat(orders).extracting(Order::getId).containsExactly(1L, 2L, 3L);
        assertThat(orders.get(0).getBookTitle()).isEqualTo("Crime and Punishment");
        assertThat(orders.get(0).getStatus()).isEqualTo("NEW");
    }

    @Test
    public void testGetOrdersByUserIdWithoutOrders() {
        assertThat(orderClient.getOrdersByUserId("999")).isEmpty();
    }

    @Test
    public void testGetOrdersByStatus() {
        var orders = orderClient.getOrdersByStatus("NEW");

        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getStatus).containsOnly("NEW");
        assertThat(orders).extracting(Order::getUserId).containsExactly("1", "2");
    }

    @Test
    public void testOneLineLogsAndRequestHeader(CapturedOutput output) {
        orderClient.getOrdersByStatus("PAID");

        var url = "GET http://localhost:" + PORT + "/order-service/orders?status=PAID -> 200";
        var line = output.getOut().lines()
                .filter(l -> l.contains("OrderClient#getOrdersByStatus(String) - " + url))
                .findFirst();
        assertThat(line).isPresent();
        assertThat(line.get())
                .contains("ms)")
                .contains("X-Request-Source=[" + FeignClientConfig.REQUEST_SOURCE + "]")
                .contains("\"status\":\"PAID\"");
        // the stub received the header added by the RequestInterceptor
        assertThat(output.getOut())
                .contains("Order-Service: GET /orders?status=PAID (X-Request-Source: "
                        + FeignClientConfig.REQUEST_SOURCE + ")");
    }
}

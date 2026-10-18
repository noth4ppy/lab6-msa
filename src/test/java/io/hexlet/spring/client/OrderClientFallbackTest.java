package io.hexlet.spring.client;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.hexlet.spring.config.TestConfig;

/**
 * Fallback (LR6): the Order-Service is unavailable (closed port 1), the Feign call fails with a
 * connection error, and Spring Cloud CircuitBreaker (Resilience4j) returns the result of
 * OrderClientFallback - an empty list instead of an exception / HTTP 500.
 */
@SpringBootTest(properties = "clients.order-service.url=http://localhost:1/order-service")
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
public class OrderClientFallbackTest {

    @Autowired
    private OrderClient orderClient;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testClientReturnsEmptyListWhenServiceIsDown(CapturedOutput output) {
        assertThat(orderClient.getOrdersByUserId("1")).isEmpty();
        assertThat(orderClient.getOrdersByStatus("NEW")).isEmpty();

        assertThat(output.getOut())
                .contains("fallback for getOrdersByUserId(1) -> []")
                .contains("fallback for getOrdersByStatus(NEW) -> []")
                // the connection error and the retries are logged in one line each
                .contains("OrderClient#getOrdersByUserId(String) - ERROR ")
                .contains("OrderClient#getOrdersByUserId(String) - RETRYING");
    }

    @Test
    public void testEndpointReturnsEmptyListInsteadOf500() throws Exception {
        var body = mockMvc.perform(get("/users/1/orders"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThatJson(body).isArray().isEmpty();

        var byStatus = mockMvc.perform(get("/users/orders/by-status").param("status", "NEW"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThatJson(byStatus).isArray().isEmpty();
    }
}

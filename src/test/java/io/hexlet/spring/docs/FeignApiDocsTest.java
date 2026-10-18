package io.hexlet.spring.docs;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.hexlet.spring.config.TestConfig;

/** Swagger / OpenAPI: the LR6 endpoints are documented. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class FeignApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testFeignEndpointsAreDocumented() throws Exception {
        var body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).node("paths").isObject()
                .containsKeys("/users/{id}/orders", "/users/orders/by-status",
                        "/order-service/orders/{userId}", "/order-service/orders");
        assertThatJson(body).node("paths./users/{id}/orders.get.summary").isString().contains("Feign");
        assertThatJson(body).node("components.schemas").isObject().containsKey("Order");
    }
}

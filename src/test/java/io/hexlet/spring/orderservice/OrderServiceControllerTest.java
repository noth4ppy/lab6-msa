package io.hexlet.spring.orderservice;

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

/** Order-Service stub (LR6): the target endpoints of the Feign client, called directly. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class OrderServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testOrdersByUserId() throws Exception {
        var body = mockMvc.perform(get("/order-service/orders/2"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isArray().hasSize(2);
        assertThatJson(body).inPath("$[*].userId").isEqualTo("[\"2\",\"2\"]");
    }

    @Test
    public void testOrdersOfUnknownUserIsEmptyListNot404() throws Exception {
        var body = mockMvc.perform(get("/order-service/orders/unknown"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isArray().isEmpty();
    }

    @Test
    public void testOrdersByStatusIgnoresCase() throws Exception {
        var body = mockMvc.perform(get("/order-service/orders").param("status", "delivered"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isArray().hasSize(1);
        assertThatJson(body).node("[0].bookTitle").isEqualTo("The Idiot");
    }
}

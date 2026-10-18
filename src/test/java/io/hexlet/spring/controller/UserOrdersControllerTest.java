package io.hexlet.spring.controller;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.hexlet.spring.client.AbstractFeignIntegrationTest;

/**
 * Endpoints of UserController that use the Feign client (LR6): GET /users/{id}/orders and
 * GET /users/orders/by-status?status=... return the orders received from the Order-Service over HTTP.
 */
public class UserOrdersControllerTest extends AbstractFeignIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetUserOrders() throws Exception {
        var body = mockMvc.perform(get("/users/1/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isArray().hasSize(3);
        assertThatJson(body).node("[0]").isEqualTo("""
                {"id":1,"userId":"1","bookId":1,"bookTitle":"Crime and Punishment","quantity":1,"status":"NEW"}
                """);
        assertThatJson(body).inPath("$[*].userId").isEqualTo("[\"1\",\"1\",\"1\"]");
    }

    @Test
    public void testGetUserOrdersOfSecondUser() throws Exception {
        var body = mockMvc.perform(get("/users/2/orders"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).inPath("$[*].id").isArray().containsExactly(4, 5);
    }

    @Test
    public void testGetUserOrdersEmpty() throws Exception {
        var body = mockMvc.perform(get("/users/999/orders"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isArray().isEmpty();
    }

    @Test
    public void testGetOrdersByStatus() throws Exception {
        var body = mockMvc.perform(get("/users/orders/by-status").param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isArray().hasSize(1);
        assertThatJson(body).node("[0].id").isEqualTo(4);
        assertThatJson(body).node("[0].status").isEqualTo("SHIPPED");
    }

    @Test
    public void testGetOrdersByStatusWithoutParam() throws Exception {
        mockMvc.perform(get("/users/orders/by-status"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testExistingUserEndpointsStillWork() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isOk());
        mockMvc.perform(get("/users/999999")).andExpect(status().isNotFound());
    }
}

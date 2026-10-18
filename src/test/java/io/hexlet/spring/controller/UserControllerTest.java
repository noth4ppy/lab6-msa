package io.hexlet.spring.controller;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.hexlet.spring.config.TestConfig;
import io.hexlet.spring.dto.UserRequest;
import io.hexlet.spring.dto.UserResponse;
import net.datafaker.Faker;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private Faker faker;

    private UserResponse createUser(String name, String email) throws Exception {
        var request = post("/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(new UserRequest(name, email)));
        var body = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readValue(body, UserResponse.class);
    }

    @Test
    public void testCreate() throws Exception {
        var name = faker.name().fullName();
        var email = faker.internet().emailAddress();

        var request = post("/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(new UserRequest(name, email)));
        var body = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("id").isString().isNotEmpty(),
                v -> v.node("name").isEqualTo(name),
                v -> v.node("email").isEqualTo(email));
    }

    @Test
    public void testShow() throws Exception {
        var created = createUser(faker.name().fullName(), faker.internet().emailAddress());

        var body = mockMvc.perform(get("/users/" + created.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("id").isString().isEqualTo(created.getId()),
                v -> v.node("name").isEqualTo(created.getName()),
                v -> v.node("email").isEqualTo(created.getEmail()));
    }

    @Test
    public void testShowNotFound() throws Exception {
        var body = mockMvc.perform(get("/users/no-such-user"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("no-such-user");
    }

    @Test
    public void testIndex() throws Exception {
        var created = createUser(faker.name().fullName(), faker.internet().emailAddress());

        var body = mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isArray().isNotEmpty();
        assertThat(body).contains(created.getEmail());
    }

    @Test
    public void testCreateInvalid() throws Exception {
        var request = post("/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("name", "", "email", "not-an-email")));

        var body = mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isObject().containsKeys("name", "email");
    }
}

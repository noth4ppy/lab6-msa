package io.hexlet.spring.docs;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.hexlet.spring.config.TestConfig;

/** Swagger / OpenAPI documentation (LR5): springdoc paths and generated description. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testApiDocsJson() throws Exception {
        var body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).node("openapi").isString().startsWith("3.");
        assertThatJson(body).node("info.title").isEqualTo("Book catalog API");
        assertThatJson(body).node("paths").isObject()
                .containsKeys("/users", "/users/create", "/users/{userId}",
                        "/api/books", "/api/books/{id}");
        assertThatJson(body).node("paths./users/create.post").isPresent();
        assertThatJson(body).node("paths./users/{userId}.get").isPresent();
        assertThatJson(body).node("paths./api/books/{id}").isObject()
                .containsKeys("get", "put", "patch", "delete");
        assertThatJson(body).node("components.schemas").isObject()
                .containsKeys("UserRequest", "UserResponse", "BookCreateDTO", "BookDTO", "BookUpdateDTO");
    }

    @Test
    public void testBookUpdateSchemaUnwrapsJsonNullable() throws Exception {
        var body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // JsonNullable<String> must be documented as a plain string, not as {present: boolean}
        assertThatJson(body).node("components.schemas.BookUpdateDTO.properties").isObject()
                .containsKeys("authorId", "slug", "title", "description", "publishedYear");
        assertThatJson(body).node("components.schemas.BookUpdateDTO.properties.title.type")
                .isEqualTo("string");
        assertThat(body).doesNotContain("JsonNullable");
    }

    @Test
    public void testApiDocsYaml() throws Exception {
        var body = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).startsWith("openapi: 3.")
                .contains("/users/create:")
                .contains("/users/{userId}:")
                .contains("/api/books:");
    }

    @Test
    public void testSwaggerUiRedirect() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    public void testSwaggerUiIndex() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}

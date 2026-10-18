package io.hexlet.spring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.hexlet.spring.dto.UserRequest;
import io.hexlet.spring.exception.ResourceNotFoundException;
import io.hexlet.spring.service.impl.UserServiceImpl;

public class UserServiceImplTest {

    private UserService userService;

    @BeforeEach
    public void setUp() {
        userService = new UserServiceImpl();
    }

    @Test
    public void testCreateGeneratesSequentialIds() {
        var first = userService.create(new UserRequest("Ivan", "ivan@example.com"));
        var second = userService.create(new UserRequest("Anna", "anna@example.com"));

        assertThat(first.getId()).isEqualTo("1");
        assertThat(second.getId()).isEqualTo("2");
        assertThat(first.getName()).isEqualTo("Ivan");
        assertThat(first.getEmail()).isEqualTo("ivan@example.com");
    }

    @Test
    public void testFindById() {
        var created = userService.create(new UserRequest("Ivan", "ivan@example.com"));

        var found = userService.findById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getEmail()).isEqualTo("ivan@example.com");
    }

    @Test
    public void testFindByIdNotFound() {
        assertThatThrownBy(() -> userService.findById("42"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    public void testGetAll() {
        assertThat(userService.getAll()).isEmpty();
        userService.create(new UserRequest("Ivan", "ivan@example.com"));
        userService.create(new UserRequest("Anna", "anna@example.com"));

        assertThat(userService.getAll())
                .extracting("email")
                .containsExactly("ivan@example.com", "anna@example.com");
    }
}

package io.hexlet.spring.orderservice;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import io.hexlet.spring.dto.Order;

/**
 * In-memory storage of the emulated Order-Service (LR6). Demo orders of books by users "1" and "2"
 * in different statuses; the database and Flyway are not involved.
 */
@Component
public class OrderStore {

    private final List<Order> orders = new CopyOnWriteArrayList<>(List.of(
            new Order(1L, "1", 1L, "Crime and Punishment", 1, "NEW"),
            new Order(2L, "1", 2L, "War and Peace", 2, "PAID"),
            new Order(3L, "1", 3L, "The Idiot", 1, "DELIVERED"),
            new Order(4L, "2", 1L, "Crime and Punishment", 1, "SHIPPED"),
            new Order(5L, "2", 4L, "Anna Karenina", 1, "NEW")));

    /** Orders of the user; an empty list (not 404) for a user without orders. */
    public List<Order> findByUserId(String userId) {
        return orders.stream().filter(order -> order.getUserId().equals(userId)).toList();
    }

    /** Orders in the given status (case-insensitive); an empty list if there are none. */
    public List<Order> findByStatus(String status) {
        return orders.stream().filter(order -> order.getStatus().equalsIgnoreCase(status)).toList();
    }
}

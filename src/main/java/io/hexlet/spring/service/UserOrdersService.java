package io.hexlet.spring.service;

import java.util.List;

import io.hexlet.spring.dto.Order;

/** Orders of users received from the Order-Service through the Feign client (LR6). */
public interface UserOrdersService {

    /** Orders of the user; an empty list if there are none or the Order-Service is unavailable. */
    List<Order> getUserOrders(String userId);

    /** Orders in the given status; an empty list if there are none or the Order-Service is unavailable. */
    List<Order> getOrdersByStatus(String status);
}

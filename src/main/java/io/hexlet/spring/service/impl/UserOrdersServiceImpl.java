package io.hexlet.spring.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import io.hexlet.spring.client.OrderClient;
import io.hexlet.spring.dto.Order;
import io.hexlet.spring.service.UserOrdersService;
import lombok.RequiredArgsConstructor;

/**
 * The Feign client {@link OrderClient} is injected through the constructor (equivalent of the
 * {@code @Autowired} field from the methodology); the calls are delegated to it, each of them is an HTTP
 * request to the Order-Service.
 */
@Service
@RequiredArgsConstructor
public class UserOrdersServiceImpl implements UserOrdersService {

    private final OrderClient orderClient;

    @Override
    public List<Order> getUserOrders(String userId) {
        return orderClient.getOrdersByUserId(userId);
    }

    @Override
    public List<Order> getOrdersByStatus(String status) {
        return orderClient.getOrdersByStatus(status);
    }
}

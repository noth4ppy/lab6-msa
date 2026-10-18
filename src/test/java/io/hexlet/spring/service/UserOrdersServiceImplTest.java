package io.hexlet.spring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.hexlet.spring.client.OrderClient;
import io.hexlet.spring.dto.Order;
import io.hexlet.spring.service.impl.UserOrdersServiceImpl;

/** UserOrdersServiceImpl (LR6): calls are delegated to the Feign client OrderClient. */
@ExtendWith(MockitoExtension.class)
public class UserOrdersServiceImplTest {

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private UserOrdersServiceImpl userOrdersService;

    @Test
    public void testGetUserOrdersDelegatesToClient() {
        List<Order> orders = Instancio.ofList(Order.class).size(2).create();
        when(orderClient.getOrdersByUserId("7")).thenReturn(orders);

        assertThat(userOrdersService.getUserOrders("7")).isSameAs(orders);
        verify(orderClient).getOrdersByUserId("7");
        verifyNoMoreInteractions(orderClient);
    }

    @Test
    public void testGetUserOrdersEmpty() {
        when(orderClient.getOrdersByUserId("1")).thenReturn(List.of());

        assertThat(userOrdersService.getUserOrders("1")).isEmpty();
    }

    @Test
    public void testGetOrdersByStatusDelegatesToClient() {
        List<Order> orders = Instancio.ofList(Order.class).size(3).create();
        when(orderClient.getOrdersByStatus("NEW")).thenReturn(orders);

        assertThat(userOrdersService.getOrdersByStatus("NEW")).isSameAs(orders);
        verify(orderClient).getOrdersByStatus("NEW");
        verifyNoMoreInteractions(orderClient);
    }
}

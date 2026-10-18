package io.hexlet.spring.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.hexlet.spring.dto.Order;
import io.hexlet.spring.dto.UserRequest;
import io.hexlet.spring.dto.UserResponse;
import io.hexlet.spring.service.UserOrdersService;
import io.hexlet.spring.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "users-controller", description = "Пользователи (ЛР5, хранение в памяти) и их заказы через Feign (ЛР6)")
public class UserController {

    private final UserService userService;

    /** LR6: orders are received from the Order-Service through the Feign client OrderClient. */
    private final UserOrdersService userOrdersService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Список пользователей")
    public List<UserResponse> index() {
        return userService.getAll();
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Пользователь создан"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации полей",
                content = @Content(mediaType = "application/json"))
    })
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получить пользователя по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Пользователь найден"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    public UserResponse getUser(@Parameter(description = "Идентификатор пользователя", example = "1")
                                @PathVariable String userId) {
        return userService.findById(userId);
    }

    /**
     * LR6: orders of the user from the Order-Service. The call goes through the Feign client
     * ({@code OrderClient.getOrdersByUserId}); if the Order-Service is unavailable, the fallback returns
     * an empty list (200 and {@code []}, not 500).
     */
    @GetMapping("/{id}/orders")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Заказы пользователя из Order-Service (через @FeignClient OrderClient)")
    @ApiResponse(responseCode = "200",
            description = "Список заказов (пустой, если заказов нет или Order-Service недоступен)")
    public List<Order> getUserOrders(@Parameter(description = "Идентификатор пользователя", example = "1")
                                     @PathVariable("id") String id) {
        return userOrdersService.getUserOrders(id);
    }

    /**
     * LR6: orders in a status from the Order-Service; demonstrates {@code @RequestParam("status")}
     * of the Feign client ({@code OrderClient.getOrdersByStatus}).
     */
    @GetMapping("/orders/by-status")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Заказы по статусу из Order-Service (через @FeignClient, параметр запроса status)")
    @ApiResponse(responseCode = "200",
            description = "Список заказов (пустой, если заказов нет или Order-Service недоступен)")
    public List<Order> getOrdersByStatus(@Parameter(description = "Статус заказа", example = "NEW")
                                         @RequestParam("status") String status) {
        return userOrdersService.getOrdersByStatus(status);
    }
}

package io.hexlet.spring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Order of a book (LR6). The same JSON is produced by the Order-Service stub
 * ({@link io.hexlet.spring.orderservice.OrderServiceController}) and read by the Feign client
 * ({@link io.hexlet.spring.client.OrderClient}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Заказ книги (Order-Service)")
public class Order {

    @Schema(description = "Идентификатор заказа", example = "1")
    private Long id;

    @Schema(description = "Идентификатор пользователя", example = "1")
    private String userId;

    @Schema(description = "Идентификатор книги", example = "1")
    private Long bookId;

    @Schema(description = "Название книги", example = "Crime and Punishment")
    private String bookTitle;

    @Schema(description = "Количество экземпляров", example = "1")
    private Integer quantity;

    @Schema(description = "Статус заказа: NEW, PAID, SHIPPED, DELIVERED", example = "NEW")
    private String status;
}

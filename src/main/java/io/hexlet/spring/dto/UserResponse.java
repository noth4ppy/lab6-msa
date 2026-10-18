package io.hexlet.spring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response with user data (LR5). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Пользователь")
public class UserResponse {

    @Schema(description = "Идентификатор пользователя", example = "1")
    private String id;

    @Schema(description = "Имя пользователя", example = "Ivan Petrov")
    private String name;

    @Schema(description = "Адрес электронной почты", example = "ivan.petrov@example.com")
    private String email;

    /** Constructor from the methodology example: new UserResponse(userId, email). */
    public UserResponse(String id, String email) {
        this.id = id;
        this.email = email;
    }
}

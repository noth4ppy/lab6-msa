package io.hexlet.spring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Input data for POST /users/create (LR5). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные для создания пользователя")
public class UserRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Имя пользователя", example = "Ivan Petrov")
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    @Schema(description = "Адрес электронной почты", example = "ivan.petrov@example.com")
    private String email;
}

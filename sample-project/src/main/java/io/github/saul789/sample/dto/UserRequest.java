package io.github.saul789.sample.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "user.name.required") @Size(min = 2, max = 50, message = "user.name.size")
        String name,
    @NotBlank(message = "user.email.required") @Email(message = "user.email.invalid")
        String email) {}

package com.ecommerce.ecommerce_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private String firstName;

    private String lastName;

    @Email(message = "Please provide a valid email address")
    private String email;

    private String phoneNumber;

    private String shippingAddress;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}

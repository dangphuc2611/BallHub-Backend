package com.ballhub.ballhub_backend.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private Integer addressId;
    private String fullAddress;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}

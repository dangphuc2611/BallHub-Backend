package com.ballhub.ballhub_backend.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private Boolean status;
    private LocalDateTime createdAt;
}

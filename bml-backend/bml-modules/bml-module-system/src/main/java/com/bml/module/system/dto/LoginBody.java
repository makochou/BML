package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;

/**
 * Login request body.
 */
@Data
@Schema(description = "Login request body")
public class LoginBody {

    @Schema(description = "Username", requiredMode = RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Password", requiredMode = RequiredMode.REQUIRED)
    private String password;
}

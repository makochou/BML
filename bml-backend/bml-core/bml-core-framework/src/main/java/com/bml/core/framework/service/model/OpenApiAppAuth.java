package com.bml.core.framework.service.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpenApiAppAuth {

    private Long accountId;

    private String secretKey;

    private String signVersion;

    private String ipWhitelist;
}

package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "API 凭证一次性展示对象")
public class ApiCredentialVO {

    @Schema(description = "账号ID")
    private Long id;

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "AccessKey")
    private String accessKey;

    @Schema(description = "SecretKey，仅在创建或重置时返回一次")
    private String secretKey;
}

package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 账号详情视图对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "API账号详情视图对象")
public class SysApiAccountDetailVO extends SysApiAccountVO {
}

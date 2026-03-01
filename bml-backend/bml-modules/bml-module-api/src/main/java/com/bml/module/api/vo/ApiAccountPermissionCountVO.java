package com.bml.module.api.vo;

import lombok.Data;

/**
 * API账号授权数量统计中间对象。
 */
@Data
public class ApiAccountPermissionCountVO {

    private Long accountId;

    private Long authorizedApiCount;
}

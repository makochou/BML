package com.bml.module.api.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysApiPermissionMapper {

    @Select("""
            SELECT COUNT(1)
            FROM sys_api_permission
            WHERE account_id = #{accountId}
              AND api_id = #{apiId}
            """)
    long countPermission(@Param("accountId") Long accountId, @Param("apiId") Long apiId);
}

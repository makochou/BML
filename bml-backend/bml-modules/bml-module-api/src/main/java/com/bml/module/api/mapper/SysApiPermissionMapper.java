package com.bml.module.api.mapper;

import com.bml.module.api.vo.ApiAccountPermissionCountVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * API账号授权关系数据访问层。
 */
@Mapper
public interface SysApiPermissionMapper {

    @Select("""
            SELECT COUNT(1)
            FROM sys_api_permission
            WHERE account_id = #{accountId}
              AND api_id = #{apiId}
            """)
    long countPermission(@Param("accountId") Long accountId, @Param("apiId") Long apiId);

    @Select("""
            SELECT api_id
            FROM sys_api_permission
            WHERE account_id = #{accountId}
            ORDER BY api_id ASC
            """)
    List<Long> selectApiIdsByAccountId(@Param("accountId") Long accountId);

    @Delete("""
            DELETE FROM sys_api_permission
            WHERE account_id = #{accountId}
            """)
    int deleteByAccountId(@Param("accountId") Long accountId);

    @Insert("""
            <script>
            INSERT INTO sys_api_permission (account_id, api_id)
            VALUES
            <foreach collection="apiIds" item="apiId" separator=",">
                (#{accountId}, #{apiId})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("accountId") Long accountId, @Param("apiIds") List<Long> apiIds);

    @Select("""
            <script>
            SELECT account_id AS accountId, COUNT(1) AS authorizedApiCount
            FROM sys_api_permission
            WHERE account_id IN
            <foreach collection="accountIds" item="accountId" open="(" separator="," close=")">
                #{accountId}
            </foreach>
            GROUP BY account_id
            </script>
            """)
    List<ApiAccountPermissionCountVO> selectAuthorizedCountList(@Param("accountIds") Collection<Long> accountIds);

    @Select("""
            <script>
            SELECT p.account_id AS accountId, COUNT(1) AS authorizedApiCount
            FROM sys_api_permission p
            INNER JOIN sys_api_registry r ON r.id = p.api_id
            WHERE r.status = 1
              AND p.account_id IN
            <foreach collection="accountIds" item="accountId" open="(" separator="," close=")">
                #{accountId}
            </foreach>
            GROUP BY p.account_id
            </script>
            """)
    List<ApiAccountPermissionCountVO> selectEnabledAuthorizedCountList(@Param("accountIds") Collection<Long> accountIds);
}

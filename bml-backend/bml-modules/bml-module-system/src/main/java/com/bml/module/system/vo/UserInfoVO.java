package com.bml.module.system.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 当前登录用户信息响应对象
 * <p>
 * 用于 {@code GET /auth/info} 接口的响应，包含当前登录用户的完整前端所需数据：
 * </p>
 * <ul>
 * <li><b>user</b> — 用户基本信息（用户名、昵称、头像等）</li>
 * <li><b>roles</b> — 角色标识列表（如 {@code ["admin", "common"]}），用于前端角色判断</li>
 * <li><b>permissions</b> — 权限标识列表（如
 * {@code ["system:user:list", "*:*:*"]}），用于按钮级权限控制</li>
 * </ul>
 *
 * <h3>前端使用示例：</h3>
 * 
 * <pre>
 * // 获取用户信息
 * const { user, roles, permissions } = response.data;
 *
 * // 角色判断
 * if (roles.includes('admin')) { ... }
 *
 * // 按钮权限控制
 * v-hasPermi="['system:user:add']"
 * </pre>
 *
 * @author BML Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前登录用户信息响应对象")
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户基本信息
     * <p>
     * 包含用户ID、用户名、真实姓名、头像、状态等字段。
     * 前端可据此渲染用户资料卡片、头像显示等。
     * </p>
     */
    @Schema(description = "用户基本信息")
    private SysUserVO user;

    /**
     * 角色标识集合
     * <p>
     * 用户所拥有的角色编码列表，如 {@code ["admin", "editor"]}。
     * 前端可据此控制页面级别的访问权限。
     * </p>
     */
    @Schema(description = "角色标识集合", example = "[\"admin\", \"common\"]")
    private Set<String> roles;

    /**
     * 权限标识集合
     * <p>
     * 用户所拥有的菜单权限标识列表，如 {@code ["system:user:list", "system:user:add"]}。
     * 超级管理员拥有 {@code "*:*:*"} 的全部权限。
     * 前端可据此控制按钮级别的显示/隐藏。
     * </p>
     */
    @Schema(description = "权限标识集合", example = "[\"system:user:list\", \"system:user:add\"]")
    private Set<String> permissions;
}

package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 前端路由 Meta 信息。
 * <p>
 * 该对象用于 /auth/routers 接口，前端会直接读取此对象驱动：
 * 菜单标题、图标、缓存策略等渲染行为。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "前端路由 Meta 信息")
public class RouterMetaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单标题")
    private String title;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "是否缓存页面")
    private Boolean noCache;
}

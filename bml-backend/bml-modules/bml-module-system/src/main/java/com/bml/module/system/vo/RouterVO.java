package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 前端动态路由对象。
 * <p>
 * 该对象是后端输出给前端路由引擎的统一契约，字段命名与前端路由模型保持一致，
 * 便于前端直接转换为 Vue Router 配置。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "前端动态路由对象")
public class RouterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "路由名称")
    private String name;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径（Layout 或视图组件）")
    private String component;

    @Schema(description = "重定向地址")
    private String redirect;

    @Schema(description = "是否隐藏菜单")
    private Boolean hidden;

    @Schema(description = "是否总是显示根菜单")
    private Boolean alwaysShow;

    @Schema(description = "路由元信息")
    private RouterMetaVO meta;

    @Schema(description = "子路由")
    private List<RouterVO> children = new ArrayList<>();
}

package com.bml.module.api.vo;

import com.bml.module.api.support.ApiCatalogDisplayNameSupport;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * API 接口目录树节点 VO。
 * <p>
 * 用于「API 接口列表」页的树形展示，支持三层结构：
 * </p>
 * <ul>
 * <li><b>MODULE</b> — 模块（一级），如：系统管理、API 管理</li>
 * <li><b>RESOURCE</b> — 业务/资源（二级），如：用户管理、角色管理</li>
 * <li><b>API</b> — 具体接口（叶子），如：用户列表、新增用户</li>
 * </ul>
 * <p>
 * 仅当 {@code type == API} 时，{@code apiId}、{@code httpMethod}、{@code apiUrl}、{@code description}、{@code status} 有值；
 * 否则以 {@code children} 表示子节点。
 * </p>
 *
 * @author BML Team
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 接口目录树节点")
public class ApiCatalogTreeNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点唯一标识：模块/资源层为 moduleName 或 controllerKey，接口层为 apiId 的字符串形式")
    private String id;

    @Schema(description = "展示名称（中文）")
    private String label;

    @Schema(description = "节点类型：MODULE-模块，RESOURCE-业务资源，API-具体接口")
    private String type;

    @Schema(description = "子节点列表（仅 MODULE、RESOURCE 类型存在）")
    private List<ApiCatalogTreeNodeVO> children;

    // ---------- 以下字段仅 type=API 时有效 ----------

    @Schema(description = "接口主键 ID（仅 API 类型）")
    private Long apiId;

    @Schema(description = "HTTP 方法（仅 API 类型），如 GET、POST、PUT、DELETE")
    private String httpMethod;

    @Schema(description = "接口路径（仅 API 类型），如 /system/user/list")
    private String apiUrl;

    @Schema(description = "接口描述（仅 API 类型）")
    private String description;

    @Schema(description = "状态：1 启用，0 停用（仅 API 类型）")
    private Integer status;

    /**
     * 创建模块节点。
     */
    public static ApiCatalogTreeNodeVO module(String moduleName, String label) {
        ApiCatalogTreeNodeVO node = new ApiCatalogTreeNodeVO();
        node.setId("module:" + (moduleName != null ? moduleName : ""));
        node.setLabel(label != null ? label : moduleName);
        node.setType(ApiCatalogDisplayNameSupport.TYPE_MODULE);
        node.setChildren(new ArrayList<>());
        return node;
    }

    /**
     * 创建资源/业务节点。
     */
    public static ApiCatalogTreeNodeVO resource(String controllerName, String label) {
        ApiCatalogTreeNodeVO node = new ApiCatalogTreeNodeVO();
        node.setId("resource:" + (controllerName != null ? controllerName : ""));
        node.setLabel(label != null ? label : controllerName);
        node.setType(ApiCatalogDisplayNameSupport.TYPE_RESOURCE);
        node.setChildren(new ArrayList<>());
        return node;
    }

    /**
     * 创建接口叶子节点。
     */
    public static ApiCatalogTreeNodeVO api(Long apiId, String label, String httpMethod,
                                           String apiUrl, String description, Integer status) {
        ApiCatalogTreeNodeVO node = new ApiCatalogTreeNodeVO();
        node.setId(apiId != null ? "api:" + apiId : null);
        node.setLabel(label);
        node.setType(ApiCatalogDisplayNameSupport.TYPE_API);
        node.setApiId(apiId);
        node.setHttpMethod(httpMethod);
        node.setApiUrl(apiUrl);
        node.setDescription(description);
        node.setStatus(status);
        return node;
    }
}

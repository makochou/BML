package com.bml.module.api.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bml.core.common.support.ApiRegistryPathSupport;
import com.bml.module.api.dto.OpenApiRegistryTreeQuery;
import com.bml.module.api.entity.SysApiRegistry;
import com.bml.module.api.mapper.SysApiRegistryMapper;
import com.bml.module.api.support.ApiCatalogDisplayNameSupport;
import com.bml.module.api.vo.ApiCatalogTreeNodeVO;
import com.bml.module.api.vo.OpenApiControllerGroupVO;
import com.bml.module.api.vo.OpenApiGroupVO;
import com.bml.module.api.vo.OpenApiRegistryItemVO;
import com.bml.module.api.vo.OpenApiRegistrySyncResultVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 项目接口目录服务。
 * <p>
 * 与早期仅收口 `/open/api/**` 的实现不同，当前版本会自动纳管项目内全部业务接口：
 * 1. 只要控制器属于 `com.bml.*` 包；
 * 2. 只要路径不属于登录、健康检查、Swagger、错误页等基础设施白名单；
 * 3. 启动时和手动点击“同步接口目录”时都会自动刷新。
 * </p>
 * <p>
 * 这样后续新增控制器方法后，无需手工维护目录，即可自动进入 API 账号授权工作台。
 * </p>
 */
@Slf4j
@Service
public class SysOpenApiRegistryService extends ServiceImpl<SysApiRegistryMapper, SysApiRegistry> {

    private final List<RequestMappingInfoHandlerMapping> handlerMappings;
    private final List<WebEndpointsSupplier> webEndpointsSuppliers;

    public SysOpenApiRegistryService(List<RequestMappingInfoHandlerMapping> handlerMappings,
                                     List<WebEndpointsSupplier> webEndpointsSuppliers) {
        this.handlerMappings = handlerMappings;
        this.webEndpointsSuppliers = webEndpointsSuppliers;
    }

    /**
     * 查询接口目录树。
     *
     * @param query 查询条件
     * @return 按模块、控制器分组后的接口树
     */
    public List<OpenApiGroupVO> listRegistryTree(OpenApiRegistryTreeQuery query) {
        List<SysApiRegistry> registries = this.list(buildRegistryQueryWrapper(query));
        if (registries.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, OpenApiGroupVO> moduleMap = new LinkedHashMap<>();
        for (SysApiRegistry registry : registries) {
            OpenApiGroupVO moduleGroup = moduleMap.computeIfAbsent(
                    StrUtil.blankToDefault(registry.getModuleName(), "common"),
                    moduleName -> {
                        OpenApiGroupVO groupVO = new OpenApiGroupVO();
                        groupVO.setModuleName(moduleName);
                        return groupVO;
                    });

            OpenApiControllerGroupVO controllerGroup = moduleGroup.getControllers().stream()
                    .filter(item -> Objects.equals(item.getControllerName(), registry.getControllerName()))
                    .findFirst()
                    .orElseGet(() -> {
                        OpenApiControllerGroupVO groupVO = new OpenApiControllerGroupVO();
                        groupVO.setControllerName(registry.getControllerName());
                        moduleGroup.getControllers().add(groupVO);
                        return groupVO;
                    });

            controllerGroup.getApis().add(toRegistryItem(registry));
        }
        return new ArrayList<>(moduleMap.values());
    }

    /**
     * 查询 API 接口目录树（用于「API 接口列表」页展示）。
     * <p>
     * 返回三层结构：模块（如系统管理）→ 业务资源（如用户管理）→ 具体接口（如用户列表、新增用户）。
     * 模块与控制器名称会通过 {@link ApiCatalogDisplayNameSupport} 解析为中文展示名。
     * </p>
     *
     * @param query 查询条件（关键词、方法、状态、模块名等），可为空
     * @return 树形节点列表，每个一级节点为模块，其 children 为资源，资源的 children 为接口叶子节点
     */
    public List<ApiCatalogTreeNodeVO> listApiCatalogTree(OpenApiRegistryTreeQuery query) {
        List<SysApiRegistry> registries = this.list(buildRegistryQueryWrapper(query));
        if (registries.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, ApiCatalogTreeNodeVO> moduleMap = new LinkedHashMap<>();
        for (SysApiRegistry registry : registries) {
            String moduleName = StrUtil.blankToDefault(registry.getModuleName(), "common");
            String controllerName = StrUtil.blankToDefault(registry.getControllerName(), "");

            ApiCatalogTreeNodeVO moduleNode = moduleMap.computeIfAbsent(moduleName, mn -> {
                ApiCatalogTreeNodeVO node = ApiCatalogTreeNodeVO.module(mn,
                        ApiCatalogDisplayNameSupport.getModuleDisplayName(mn));
                return node;
            });

            ApiCatalogTreeNodeVO resourceNode = moduleNode.getChildren().stream()
                    .filter(n -> controllerName.equals(extractControllerNameFromId(n.getId())))
                    .findFirst()
                    .orElseGet(() -> {
                        ApiCatalogTreeNodeVO node = ApiCatalogTreeNodeVO.resource(controllerName,
                                ApiCatalogDisplayNameSupport.getControllerDisplayName(controllerName));
                        moduleNode.getChildren().add(node);
                        return node;
                    });

            ApiCatalogTreeNodeVO apiNode = ApiCatalogTreeNodeVO.api(
                    registry.getId(),
                    StrUtil.blankToDefault(registry.getApiName(), registry.getMethodName()),
                    registry.getHttpMethod(),
                    registry.getApiUrl(),
                    registry.getDescription(),
                    registry.getStatus());
            resourceNode.getChildren().add(apiNode);
        }

        return new ArrayList<>(moduleMap.values());
    }

    private static String extractControllerNameFromId(String nodeId) {
        if (StrUtil.isBlank(nodeId) || !nodeId.startsWith("resource:")) {
            return "";
        }
        return nodeId.substring("resource:".length());
    }

    /**
     * 统计纳管接口数量。
     *
     * @param status 接口状态；为空时统计全部
     * @return 纳管接口数量
     */
    public long countOpenApi(Integer status) {
        return this.count(new LambdaQueryWrapper<SysApiRegistry>()
                .eq(status != null, SysApiRegistry::getStatus, status));
    }

    /**
     * 根据接口 ID 列表查询当前启用的目录项。
     *
     * @param ids 接口 ID 列表
     * @return 启用状态的接口目录
     */
    public List<SysApiRegistry> listEnabledByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return this.list(new LambdaQueryWrapper<SysApiRegistry>()
                .in(SysApiRegistry::getId, ids)
                .eq(SysApiRegistry::getStatus, 1));
    }

    /**
     * 扫描控制器映射并同步到统一接口目录。
     *
     * @return 同步结果
     */
    @Transactional(rollbackFor = Exception.class)
    public OpenApiRegistrySyncResultVO syncRegistry() {
        Map<String, OpenApiMappingDescriptor> discoveredMap = new LinkedHashMap<>();
        long skippedCount = 0L;

        // 1. 标准路由映射扫描
        for (RequestMappingInfoHandlerMapping mapping : handlerMappings) {
            log.debug("Scanning mapping bean: {}", mapping.getClass().getSimpleName());
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping.getHandlerMethods().entrySet()) {
                List<OpenApiMappingDescriptor> descriptors = extractDescriptors(entry.getKey(), entry.getValue());
                if (descriptors.isEmpty()) {
                    skippedCount++;
                    continue;
                }
                for (OpenApiMappingDescriptor descriptor : descriptors) {
                    discoveredMap.put(descriptor.uniqueKey(), descriptor);
                }
            }
        }

        // 2. Actuator 端点扫描 (处理 management.server.port 隔离情况)
        for (WebEndpointsSupplier supplier : webEndpointsSuppliers) {
            log.debug("Scanning Actuator endpoints from supplier: {}", supplier.getClass().getSimpleName());
            for (ExposableWebEndpoint endpoint : supplier.getEndpoints()) {
                for (WebOperation operation : endpoint.getOperations()) {
                    OpenApiMappingDescriptor descriptor = mapActuatorEndpoint(endpoint, operation);
                    if (descriptor != null) {
                        discoveredMap.put(descriptor.uniqueKey(), descriptor);
                    }
                }
            }
        }

        log.info("API Sync: discovered {} unique APIs, skipped {} methods", discoveredMap.size(), skippedCount);

        List<SysApiRegistry> existingRegistries = this.list();
        Map<String, SysApiRegistry> existingMap = new LinkedHashMap<>();
        List<SysApiRegistry> redundantToDisable = new ArrayList<>();

        for (SysApiRegistry registry : existingRegistries) {
            String key = buildUniqueKey(registry.getApiUrl(), registry.getHttpMethod());
            SysApiRegistry duplicate = existingMap.put(key, registry);
            if (duplicate != null) {
                // 如果发现多个 active 记录对应同一个 key，保留当前的，禁用之前的那个
                if (Objects.equals(duplicate.getStatus(), 1)) {
                    duplicate.setStatus(0);
                    redundantToDisable.add(duplicate);
                    log.warn("API Sync: found redundant active record for key {}, disabling previous one (ID: {})", 
                            key, duplicate.getId());
                } else if (Objects.equals(registry.getStatus(), 0)) {
                    // 如果当前是 0，之前也是 0 或 1，为了确保 existingMap 里的 key 尽可能对应一个能用的或者最新的记录，
                    // 我们可以根据 ID 或时间戳进一步微调，但这里核心是清理 redundant active。
                }
            }
        }

        List<SysApiRegistry> toInsert = new ArrayList<>();
        List<SysApiRegistry> toUpdate = new ArrayList<>(redundantToDisable);
        long disabledCount = (long) redundantToDisable.size();

        for (OpenApiMappingDescriptor descriptor : discoveredMap.values()) {
            SysApiRegistry existing = existingMap.remove(descriptor.uniqueKey());
            if (existing == null) {
                toInsert.add(descriptor.toEntity());
                continue;
            }
            if (applyDescriptor(existing, descriptor)) {
                toUpdate.add(existing);
            }
        }

        for (SysApiRegistry staleRegistry : existingMap.values()) {
            if (Objects.equals(staleRegistry.getStatus(), 0)) {
                continue;
            }
            staleRegistry.setStatus(0);
            toUpdate.add(staleRegistry);
            disabledCount++;
        }

        if (!toInsert.isEmpty()) {
            this.saveBatch(toInsert);
        }
        for (SysApiRegistry registry : toUpdate) {
            this.updateById(registry);
        }

        OpenApiRegistrySyncResultVO resultVO = new OpenApiRegistrySyncResultVO();
        resultVO.setTotalDiscovered(discoveredMap.size());
        resultVO.setInsertedCount(toInsert.size());
        resultVO.setUpdatedCount(toUpdate.size() - (int) disabledCount);
        resultVO.setDisabledCount(disabledCount);
        resultVO.setSkippedCount(skippedCount);
        return resultVO;
    }

    private LambdaQueryWrapper<SysApiRegistry> buildRegistryQueryWrapper(OpenApiRegistryTreeQuery query) {
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        String moduleName = query == null ? null : StrUtil.trim(query.getModuleName());
        String method = query == null ? null : normalizeMethod(query.getMethod());
        Integer status = query == null ? null : query.getStatus();

        LambdaQueryWrapper<SysApiRegistry> wrapper = new LambdaQueryWrapper<SysApiRegistry>()
                .eq(status != null, SysApiRegistry::getStatus, status)
                .eq(StrUtil.isNotBlank(method), SysApiRegistry::getHttpMethod, method)
                .eq(StrUtil.isNotBlank(moduleName), SysApiRegistry::getModuleName, moduleName)
                .orderByAsc(SysApiRegistry::getModuleName, SysApiRegistry::getControllerName,
                        SysApiRegistry::getHttpMethod, SysApiRegistry::getApiUrl);

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(item -> item
                    .like(SysApiRegistry::getApiName, keyword)
                    .or()
                    .like(SysApiRegistry::getApiUrl, keyword)
                    .or()
                    .like(SysApiRegistry::getDescription, keyword)
                    .or()
                    .like(SysApiRegistry::getControllerName, keyword)
                    .or()
                    .like(SysApiRegistry::getMethodName, keyword));
        }
        return wrapper;
    }

    private OpenApiRegistryItemVO toRegistryItem(SysApiRegistry registry) {
        OpenApiRegistryItemVO itemVO = new OpenApiRegistryItemVO();
        itemVO.setId(registry.getId());
        itemVO.setApiName(registry.getApiName());
        itemVO.setApiUrl(registry.getApiUrl());
        itemVO.setHttpMethod(registry.getHttpMethod());
        itemVO.setDescription(registry.getDescription());
        itemVO.setStatus(registry.getStatus());
        return itemVO;
    }

    private List<OpenApiMappingDescriptor> extractDescriptors(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        Class<?> beanType = ClassUtils.getUserClass(handlerMethod.getBeanType());
        if (beanType.isAnonymousClass() || beanType.isSynthetic()) {
            return Collections.emptyList();
        }
        String packageName = beanType.getPackageName();
        if (!ApiRegistryPathSupport.isProjectControllerPackage(packageName)) {
            log.trace("Skipping handler {}.{} in package {} (not in managed package range)", 
                    beanType.getSimpleName(), handlerMethod.getMethod().getName(), packageName);
            return Collections.emptyList();
        }

        Set<String> patterns = extractPatterns(mappingInfo);
        if (patterns.isEmpty()) {
            return Collections.emptyList();
        }

        Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
        List<String> httpMethods = methods.isEmpty() 
                ? List.of("ANY") 
                : methods.stream().map(Enum::name).toList();

        List<OpenApiMappingDescriptor> descriptors = new ArrayList<>();
        for (String httpMethod : httpMethods) {
            for (String pattern : patterns.stream().sorted().toList()) {
                String normalizedPath = normalizePath(pattern);
                if (!ApiRegistryPathSupport.isManagedApiPath(normalizedPath)) {
                    log.trace("Skipping path {} (explicitly excluded by path support)", normalizedPath);
                    continue;
                }
                descriptors.add(new OpenApiMappingDescriptor(
                        resolveApiName(handlerMethod),
                        normalizedPath,
                        httpMethod,
                        resolveModuleName(beanType),
                        beanType.getSimpleName(),
                        handlerMethod.getMethod().getName(),
                        resolveDescription(handlerMethod)));
            }
        }
        return descriptors;
    }

    private Set<String> extractPatterns(RequestMappingInfo mappingInfo) {
        Set<String> patterns = new LinkedHashSet<>(mappingInfo.getPatternValues());
        if (!patterns.isEmpty()) {
            return patterns;
        }
        if (mappingInfo.getPathPatternsCondition() != null) {
            mappingInfo.getPathPatternsCondition().getPatterns()
                    .forEach(pattern -> patterns.add(pattern.getPatternString()));
        }
        if (!patterns.isEmpty()) {
            return patterns;
        }
        if (mappingInfo.getPatternsCondition() != null) {
            patterns.addAll(mappingInfo.getPatternsCondition().getPatterns());
        }
        return patterns;
    }

    private String normalizePath(String path) {
        return ApiRegistryPathSupport.normalizePath(path);
    }

    private String resolveApiName(HandlerMethod handlerMethod) {
        // 优先使用 Swagger @Operation(summary = "...") 注解作为接口名
        Operation operation = handlerMethod.getMethodAnnotation(Operation.class);
        if (operation != null && StrUtil.isNotBlank(operation.summary())) {
            return operation.summary();
        }
        
        // 兜底逻辑：将 camelCase 格式的方法名（例：listItems）转换为更易读的格式（例：List Items）
        String methodName = handlerMethod.getMethod().getName();
        return StrUtil.upperFirst(StrUtil.toSymbolCase(methodName, ' '));
    }

    private String resolveDescription(HandlerMethod handlerMethod) {
        Operation operation = handlerMethod.getMethodAnnotation(Operation.class);
        if (operation != null && StrUtil.isNotBlank(operation.description())) {
            return operation.description();
        }
        String apiName = resolveApiName(handlerMethod);
        return StrUtil.blankToDefault(apiName,
                handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName());
    }

    private String resolveModuleName(Class<?> beanType) {
        String packageName = beanType.getPackageName();
        
        // 1. 业务模块：符合 com.bml.module.[moduleName].xxx 规范
        if (StrUtil.contains(packageName, ".module.")) {
            String afterModule = StrUtil.subAfter(packageName, ".module.", false);
            String moduleSegment = StrUtil.subBefore(afterModule, ".", false);
            if (StrUtil.isNotBlank(moduleSegment)) {
                return moduleSegment;
            }
        }
        
        // 2. 基础设施：针对 Actuator, SpringDoc 等知名框架进行归类映射
        if (packageName.startsWith("org.springframework.boot.actuate")) {
            return "actuate";
        }
        if (packageName.startsWith("org.springdoc") || packageName.contains("swagger")) {
            return "springdoc";
        }
        if (packageName.contains(".web.servlet.error")) {
            return "error";
        }
        if (packageName.startsWith("org.springframework.")) {
            return "system"; // 其他 Spring 内部接口归类到系统
        }

        // 3. 兜底策略：取包名最后一段
        String lastSegment = StrUtil.subAfter(packageName, ".", true);
        return StrUtil.blankToDefault(lastSegment, "common");
    }

    private boolean applyDescriptor(SysApiRegistry target, OpenApiMappingDescriptor descriptor) {
        boolean changed = false;
        changed |= updateIfChanged(target.getApiName(), descriptor.apiName(), target::setApiName);
        changed |= updateIfChanged(target.getApiUrl(), descriptor.apiUrl(), target::setApiUrl);
        changed |= updateIfChanged(target.getHttpMethod(), descriptor.httpMethod(), target::setHttpMethod);
        changed |= updateIfChanged(target.getModuleName(), descriptor.moduleName(), target::setModuleName);
        changed |= updateIfChanged(target.getControllerName(), descriptor.controllerName(), target::setControllerName);
        changed |= updateIfChanged(target.getMethodName(), descriptor.methodName(), target::setMethodName);
        changed |= updateIfChanged(target.getDescription(), descriptor.description(), target::setDescription);
        if (!Objects.equals(target.getStatus(), 1)) {
            target.setStatus(1);
            changed = true;
        }
        return changed;
    }

    private boolean updateIfChanged(String oldValue, String newValue, java.util.function.Consumer<String> consumer) {
        if (Objects.equals(StrUtil.nullToEmpty(oldValue), StrUtil.nullToEmpty(newValue))) {
            return false;
        }
        consumer.accept(newValue);
        return true;
    }

    private OpenApiMappingDescriptor mapActuatorEndpoint(ExposableWebEndpoint endpoint, WebOperation operation) {
        String endpointId = endpoint.getEndpointId().toString();
        String path = "/actuator/" + endpointId;
        // Actuator 端点通常由 Spring Boot 自动生成名称。
        String apiName = StrUtil.upperFirst(endpointId);
        
        return new OpenApiMappingDescriptor(
                apiName,
                path,
                operation.getRequestPredicate().getHttpMethod().name(),
                "actuate",
                StrUtil.toCamelCase(endpointId) + "Endpoint",
                endpointId + "_" + operation.getType().name(),
                "Spring Boot Actuator " + endpointId + " endpoint"
        );
    }

    private String normalizeMethod(String method) {
        return StrUtil.isBlank(method) ? null : StrUtil.trim(method).toUpperCase();
    }

    private String buildUniqueKey(String apiUrl, String httpMethod) {
        return normalizePath(apiUrl) + "#" + normalizeMethod(httpMethod);
    }

    /**
     * 自动发现到的控制器映射描述。
     */
    private record OpenApiMappingDescriptor(
            String apiName,
            String apiUrl,
            String httpMethod,
            String moduleName,
            String controllerName,
            String methodName,
            String description) {

        private String uniqueKey() {
            return apiUrl + "#" + httpMethod;
        }

        private SysApiRegistry toEntity() {
            SysApiRegistry entity = new SysApiRegistry();
            entity.setApiName(apiName);
            entity.setApiUrl(apiUrl);
            entity.setHttpMethod(httpMethod);
            entity.setModuleName(moduleName);
            entity.setControllerName(controllerName);
            entity.setMethodName(methodName);
            entity.setDescription(description);
            entity.setStatus(1);
            return entity;
        }
    }
}

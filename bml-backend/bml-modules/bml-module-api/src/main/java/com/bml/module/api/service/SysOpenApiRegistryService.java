package com.bml.module.api.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bml.core.common.support.ApiRegistryPathSupport;
import com.bml.module.api.dto.OpenApiRegistryTreeQuery;
import com.bml.module.api.entity.SysApiRegistry;
import com.bml.module.api.mapper.SysApiRegistryMapper;
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
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
@Service
public class SysOpenApiRegistryService extends ServiceImpl<SysApiRegistryMapper, SysApiRegistry> {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public SysOpenApiRegistryService(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
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

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
            List<OpenApiMappingDescriptor> descriptors = extractDescriptors(entry.getKey(), entry.getValue());
            if (descriptors.isEmpty()) {
                skippedCount++;
                continue;
            }
            for (OpenApiMappingDescriptor descriptor : descriptors) {
                discoveredMap.put(descriptor.uniqueKey(), descriptor);
            }
        }

        List<SysApiRegistry> existingRegistries = this.list();
        Map<String, SysApiRegistry> existingMap = existingRegistries.stream()
                .collect(LinkedHashMap::new,
                        (map, item) -> map.put(buildUniqueKey(item.getApiUrl(), item.getHttpMethod()), item),
                        Map::putAll);

        List<SysApiRegistry> toInsert = new ArrayList<>();
        List<SysApiRegistry> toUpdate = new ArrayList<>();
        long disabledCount = 0L;

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
        resultVO.setUpdatedCount(toUpdate.size() - disabledCount);
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
        if (!ApiRegistryPathSupport.isProjectControllerPackage(beanType.getPackageName())) {
            return Collections.emptyList();
        }

        Set<String> patterns = extractPatterns(mappingInfo);
        if (patterns.isEmpty()) {
            return Collections.emptyList();
        }

        Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
        if (methods.size() != 1) {
            return Collections.emptyList();
        }
        String httpMethod = methods.iterator().next().name();

        List<OpenApiMappingDescriptor> descriptors = new ArrayList<>();
        for (String pattern : patterns.stream().sorted().toList()) {
            String normalizedPath = normalizePath(pattern);
            if (!ApiRegistryPathSupport.isManagedApiPath(normalizedPath)) {
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
        Operation operation = handlerMethod.getMethodAnnotation(Operation.class);
        if (operation != null && StrUtil.isNotBlank(operation.summary())) {
            return operation.summary();
        }
        return handlerMethod.getMethod().getName();
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
        if (StrUtil.contains(packageName, ".module.")) {
            String afterModule = StrUtil.subAfter(packageName, ".module.", false);
            String moduleSegment = StrUtil.subBefore(afterModule, ".", false);
            if (StrUtil.isNotBlank(moduleSegment)) {
                return moduleSegment;
            }
        }
        String fallback = StrUtil.subAfter(packageName, ".", true);
        return StrUtil.blankToDefault(StrUtil.subSuf(packageName, packageName.lastIndexOf('.') + 1),
                StrUtil.blankToDefault(fallback, "common"));
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

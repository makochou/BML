package com.bml.module.api.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.module.api.entity.ApiGroup;
import com.bml.module.api.entity.ApiInfo;
import com.bml.module.api.service.ApiGroupService;
import com.bml.module.api.service.ApiInfoService;
import com.bml.module.api.service.ApiSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.Set;

/**
 * API 同步服务实现
 *
 * @author BML Team
 */
@Slf4j
@Service
public class ApiSyncServiceImpl implements ApiSyncService {

    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Resource
    private ApiGroupService apiGroupService;

    @Resource
    private ApiInfoService apiInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String syncAll() {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        int groupCount = 0;
        int apiCount = 0;

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // 1. 获取 Controller 信息 (分组)
            Class<?> beanType = handlerMethod.getBeanType();
            String controllerName = beanType.getSimpleName();

            // 排除 Spring 自带的 Controller (如 error)
            if (controllerName.startsWith("BasicError")) {
                continue;
            }

            Tag tag = beanType.getAnnotation(Tag.class);
            String groupName = (tag != null && StrUtil.isNotBlank(tag.name())) ? tag.name() : controllerName;

            // 获取或创建分组
            Long groupId = getOrCreateGroup(groupName, controllerName);
            if (groupId != null) {
                groupCount++;
            }

            // 2. 获取 Method 信息 (API)
            if (mappingInfo.getPathPatternsCondition() == null && mappingInfo.getPatternsCondition() == null)
                continue;

            Set<String> patterns;
            if (mappingInfo.getPathPatternsCondition() != null) {
                patterns = mappingInfo.getPathPatternsCondition().getPatternValues();
            } else {
                patterns = mappingInfo.getPatternsCondition().getPatterns();
            }

            if (CollUtil.isEmpty(patterns))
                continue;
            String path = patterns.iterator().next();

            Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
            String requestMethod = CollUtil.isEmpty(methods) ? "ALL" : methods.iterator().next().name();

            Operation operation = handlerMethod.getMethodAnnotation(Operation.class);
            String apiName = (operation != null && StrUtil.isNotBlank(operation.summary())) ? operation.summary()
                    : handlerMethod.getMethod().getName();

            // 权限标识 (自动生成规则: controller:method)
            String permFlag = StrUtil.format("{}:{}", controllerName.replace("Controller", "").toLowerCase(),
                    handlerMethod.getMethod().getName());

            saveOrUpdateApi(groupId, apiName, path, requestMethod, controllerName, permFlag);
            apiCount++;
        }

        log.info("API Sync completed. Groups processed: {}, APIs processed: {}", groupCount, apiCount);
        return StrUtil.format("同步完成。处理分组: {}, 处理API: {}", groupCount, apiCount);
    }

    private Long getOrCreateGroup(String groupName, String controllerName) {
        // 先按控制器类名查
        ApiGroup group = apiGroupService.getOne(new LambdaQueryWrapper<ApiGroup>()
                .eq(ApiGroup::getName, groupName)); // 这里简化处理，按名称查

        if (group == null) {
            group = new ApiGroup();
            group.setName(groupName);
            group.setSort(0);
            group.setStatus(1); // 启用
            apiGroupService.save(group);
        }
        return group.getId();
    }

    private void saveOrUpdateApi(Long groupId, String name, String path, String method, String controller,
            String permFlag) {
        ApiInfo api = apiInfoService.getOne(new LambdaQueryWrapper<ApiInfo>()
                .eq(ApiInfo::getPath, path)
                .eq(ApiInfo::getMethod, method));

        if (api == null) {
            api = new ApiInfo();
            api.setGroupId(groupId);
            api.setPath(path);
            api.setMethod(method);
            api.setName(name);
            api.setController(controller);
            api.setPermFlag(permFlag);
            api.setStatus(1); // 发布
            api.setAuthType("NONE"); // 默认公开，需手动调整
            apiInfoService.save(api);
        } else {
            // 更新元数据
            api.setGroupId(groupId);
            api.setName(name);
            api.setController(controller);
            // api.setPermFlag(permFlag); // 权限标识手动维护后可能不想被覆盖，暂不更新
            apiInfoService.updateById(api);
        }
    }
}

package com.bml.module.api.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.bml.module.api.dto.OpenApiRegistryTreeQuery;
import com.bml.module.api.entity.SysApiRegistry;
import com.bml.module.api.vo.OpenApiGroupVO;
import com.bml.module.api.vo.OpenApiRegistrySyncResultVO;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysOpenApiRegistryServiceTest {

    @Test
    void shouldGroupRegistryTreeByModuleAndController() {
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        SysOpenApiRegistryService service = org.mockito.Mockito.spy(new SysOpenApiRegistryService(handlerMapping));

        SysApiRegistry apiOne = buildRegistry(1L, "system", "MockSystemController", "查询用户列表", "/system/user/list", "GET", 1);
        SysApiRegistry apiTwo = buildRegistry(2L, "system", "MockSystemController", "新增用户", "/system/user", "POST", 1);
        SysApiRegistry apiThree = buildRegistry(3L, "enterprise", "MockEnterpriseController", "查询企业概览", "/open/api/enterprise/dashboard/summary", "GET", 1);
        doReturn(List.of(apiThree, apiOne, apiTwo)).when(service).list(anyWrapper());

        OpenApiRegistryTreeQuery query = new OpenApiRegistryTreeQuery();
        query.setStatus(1);
        List<OpenApiGroupVO> groups = service.listRegistryTree(query);

        assertEquals(2, groups.size());
        assertEquals("enterprise", groups.get(0).getModuleName());
        assertEquals("system", groups.get(1).getModuleName());
        assertEquals(2, groups.get(1).getControllers().get(0).getApis().size());
    }

    @Test
    void shouldSyncAllManagedProjectApisAndDisableStaleApis() throws Exception {
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = new LinkedHashMap<>();
        handlerMethods.put(
                RequestMappingInfo.paths("/system/mock/items").methods(RequestMethod.GET).build(),
                buildHandlerMethod("listItems"));
        handlerMethods.put(
                RequestMappingInfo.paths("/auth/login").methods(RequestMethod.POST).build(),
                buildHandlerMethod("ignoredLogin"));
        handlerMethods.put(
                RequestMappingInfo.paths("/open/api/test/ignore").methods(RequestMethod.GET).build(),
                buildHandlerMethod("ignoredOpenApiTest"));
        when(handlerMapping.getHandlerMethods()).thenReturn(handlerMethods);

        SysOpenApiRegistryService service = org.mockito.Mockito.spy(new SysOpenApiRegistryService(handlerMapping));
        SysApiRegistry existing = buildRegistry(10L, "system", "MockProjectController", "旧名称", "/system/mock/items", "GET", 0);
        SysApiRegistry stale = buildRegistry(11L, "system", "LegacyController", "过期接口", "/system/stale", "POST", 1);
        doReturn(List.of(existing, stale)).when(service).list(anyWrapper());
        doReturn(true).when(service).saveBatch(any());
        doReturn(true).when(service).updateById(any(SysApiRegistry.class));

        OpenApiRegistrySyncResultVO result = service.syncRegistry();

        assertEquals(1L, result.getTotalDiscovered());
        assertEquals(0L, result.getInsertedCount());
        assertEquals(1L, result.getUpdatedCount());
        assertEquals(1L, result.getDisabledCount());
        assertEquals(2L, result.getSkippedCount());
        assertEquals("List Mock Items", existing.getApiName());
        assertEquals(1, existing.getStatus());
        assertEquals(0, stale.getStatus());
        verify(service).updateById(existing);
        verify(service).updateById(stale);
    }

    private SysApiRegistry buildRegistry(Long id, String moduleName, String controllerName,
            String apiName, String apiUrl, String method, Integer status) {
        SysApiRegistry registry = new SysApiRegistry();
        registry.setId(id);
        registry.setModuleName(moduleName);
        registry.setControllerName(controllerName);
        registry.setApiName(apiName);
        registry.setApiUrl(apiUrl);
        registry.setHttpMethod(method);
        registry.setMethodName("mock");
        registry.setDescription(apiName);
        registry.setStatus(status);
        return registry;
    }

    private HandlerMethod buildHandlerMethod(String methodName) throws Exception {
        Method method = MockProjectController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new MockProjectController(), method);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<SysApiRegistry> anyWrapper() {
        return any(Wrapper.class);
    }

    @RequestMapping
    static class MockProjectController {

        @Operation(summary = "List Mock Items", description = "List mock items for registry sync")
        @GetMapping("/system/mock/items")
        public void listItems() {
        }

        @PostMapping("/auth/login")
        public void ignoredLogin() {
        }

        @Operation(summary = "Ignored Test Api", description = "Should be skipped")
        @GetMapping("/open/api/test/ignore")
        public void ignoredOpenApiTest() {
        }
    }
}

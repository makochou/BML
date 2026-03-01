package com.bml.module.api.service;

import java.util.Map;

/**
 * API 回调 HTTP 客户端抽象。
 * <p>
 * 抽象出网络发送能力后，回调日志服务和自动重试任务都可以复用同一套调用入口，
 * 单元测试也可以直接 mock 该接口，避免把真实网络调用耦合进业务逻辑。
 * </p>
 */
public interface ApiCallbackHttpClient {

    ApiCallbackHttpResponse postJson(String url, Map<String, String> headers, String requestBody);
}

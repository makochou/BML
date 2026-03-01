package com.bml.module.api.service;

/**
 * API 回调 HTTP 响应抽象。
 *
 * @param statusCode   HTTP 状态码
 * @param responseBody 响应体
 */
public record ApiCallbackHttpResponse(int statusCode, String responseBody) {
}

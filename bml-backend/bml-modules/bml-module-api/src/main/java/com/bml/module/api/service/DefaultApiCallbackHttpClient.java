package com.bml.module.api.service;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 默认 API 回调 HTTP 客户端实现。
 */
@Component
public class DefaultApiCallbackHttpClient implements ApiCallbackHttpClient {

    private final RestClient restClient;

    public DefaultApiCallbackHttpClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public ApiCallbackHttpResponse postJson(String url, Map<String, String> headers, String requestBody) {
        RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON);
        if (headers != null) {
            headers.forEach(requestSpec::header);
        }
        ResponseEntity<String> responseEntity = requestSpec
                .body(requestBody == null ? "" : requestBody)
                .retrieve()
                .toEntity(String.class);
        return new ApiCallbackHttpResponse(responseEntity.getStatusCode().value(), responseEntity.getBody());
    }
}

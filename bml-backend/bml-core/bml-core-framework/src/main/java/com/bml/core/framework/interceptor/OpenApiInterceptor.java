package com.bml.core.framework.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.json.JSONUtil;
import com.bml.core.framework.service.OpenApiAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 开放平台 API 签名拦截器
 *
 * @author BML Team
 */
@Slf4j
@Component
public class OpenApiInterceptor implements HandlerInterceptor {

    @Resource
    private OpenApiAuthService openApiAuthService;

    private static final long EXPIRE_TIME = 5 * 60 * 1000; // 5分钟

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 1. 获取 Header 参数
        String appKey = request.getHeader("X-Bml-App-Key");
        String timestamp = request.getHeader("X-Bml-Timestamp");
        String nonce = request.getHeader("X-Bml-Nonce");
        String sign = request.getHeader("X-Bml-Sign");

        if (StrUtil.hasBlank(appKey, timestamp, nonce, sign)) {
            writeError(response, "缺少必要Header参数");
            return false;
        }

        // 2. 校验时间戳 (防重放)
        long clientTime;
        try {
            clientTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            writeError(response, "时间戳格式错误");
            return false;
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - clientTime) > EXPIRE_TIME) {
            writeError(response, "请求已过期");
            return false;
        }

        // 3. 校验 AppKey (通过接口调用实现类查询)
        String secret = openApiAuthService.getAppSecret(appKey);
        if (secret == null) {
            writeError(response, "无效的AppKey或应用已停用");
            return false;
        }

        // 4. 校验签名
        String params = request.getQueryString();
        if (params == null)
            params = "";

        String raw = StrUtil.format("appKey={}&timestamp={}&nonce={}&params={}",
                appKey, timestamp, nonce, params);

        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        String calcSign = hMac.digestHex(raw);

        if (!StrUtil.equals(sign, calcSign)) {
            log.warn("签名校验失败. Server: {}, Client: {}", calcSign, sign);
            writeError(response, "签名校验失败");
            return false;
        }

        return true;
    }

    private void writeError(HttpServletResponse response, String msg) {
        response.setStatus(403);
        response.setContentType("application/json;charset=utf-8");
        try {
            response.getWriter().write(JSONUtil.toJsonStr(
                    Map.of("code", 403, "msg", msg, "success", false)));
        } catch (Exception e) {
            log.error("Write error response failed", e);
        }
    }
}

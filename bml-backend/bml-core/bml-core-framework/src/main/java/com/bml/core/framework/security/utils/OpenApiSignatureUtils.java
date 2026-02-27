package com.bml.core.framework.security.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class OpenApiSignatureUtils {

    private OpenApiSignatureUtils() {
    }

    public static String buildCanonicalQuery(Map<String, String[]> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return "";
        }
        Map<String, String[]> sorted = new TreeMap<>(parameterMap);
        return sorted.entrySet().stream()
                .flatMap(entry -> Arrays.stream(entry.getValue() == null ? new String[] { "" } : entry.getValue())
                        .sorted()
                        .map(value -> encode(entry.getKey()) + "=" + encode(value)))
                .collect(Collectors.joining("&"));
    }

    public static String sha256Hex(byte[] bodyBytes) {
        byte[] payload = bodyBytes == null ? new byte[0] : bodyBytes;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate SHA-256", ex);
        }
    }

    public static String sha256Hex(String value) {
        return sha256Hex(value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8));
    }

    public static String buildSignPayload(String appKey, String timestamp, String nonce, String method, String path,
            String canonicalQuery, String bodySha256) {
        return String.join("\n",
                StrUtil.nullToEmpty(appKey),
                StrUtil.nullToEmpty(timestamp),
                StrUtil.nullToEmpty(nonce),
                StrUtil.nullToEmpty(method).toUpperCase(),
                StrUtil.nullToEmpty(path),
                StrUtil.nullToEmpty(canonicalQuery),
                StrUtil.nullToEmpty(bodySha256));
    }

    public static String sign(String payload, String secret) {
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        return hMac.digestHex(payload);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

package com.bml.core.common.support;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.exception.BusinessException;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * API 账号 IP 白名单支持工具。
 */
public final class ApiIpWhitelistSupport {

    private ApiIpWhitelistSupport() {
    }

    public static List<String> normalizeEntries(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalizedEntries = new LinkedHashSet<>();
        for (String entry : entries) {
            String normalized = normalizeEntry(entry);
            if (normalized != null) {
                normalizedEntries.add(normalized);
            }
        }
        return new ArrayList<>(normalizedEntries);
    }

    public static String serializeEntries(List<String> entries) {
        List<String> normalizedEntries = normalizeEntries(entries);
        return normalizedEntries.isEmpty() ? null : String.join(",", normalizedEntries);
    }

    public static List<String> deserializeEntries(String serializedEntries) {
        if (StrUtil.isBlank(serializedEntries)) {
            return Collections.emptyList();
        }
        return normalizeEntries(StrUtil.splitTrim(serializedEntries, ','));
    }

    public static boolean isAllowed(String clientIp, String serializedWhitelist) {
        return isAllowed(clientIp, deserializeEntries(serializedWhitelist));
    }

    public static boolean isAllowed(String clientIp, List<String> whitelistEntries) {
        if (whitelistEntries == null || whitelistEntries.isEmpty()) {
            return true;
        }
        String normalizedClientIp = StrUtil.trimToNull(clientIp);
        if (normalizedClientIp == null) {
            return false;
        }
        for (String entry : whitelistEntries) {
            if (matchesEntry(normalizedClientIp, entry)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeEntry(String entry) {
        String normalized = StrUtil.trimToNull(entry);
        if (normalized == null) {
            return null;
        }
        if (normalized.contains("/")) {
            return normalizeCidr(normalized);
        }
        return normalizeSingleIp(normalized).getHostAddress();
    }

    private static boolean matchesEntry(String clientIp, String entry) {
        if (entry.contains("/")) {
            return matchesCidr(clientIp, entry);
        }
        return normalizeSingleIp(clientIp).getHostAddress().equals(normalizeSingleIp(entry).getHostAddress());
    }

    private static boolean matchesCidr(String clientIp, String cidr) {
        String[] parts = cidr.split("/", 2);
        InetAddress baseAddress = normalizeSingleIp(parts[0]);
        InetAddress clientAddress = normalizeSingleIp(clientIp);
        int prefixLength = parsePrefixLength(parts[1], baseAddress.getAddress().length * 8);
        if (baseAddress.getAddress().length != clientAddress.getAddress().length) {
            return false;
        }

        BigInteger base = new BigInteger(1, baseAddress.getAddress());
        BigInteger client = new BigInteger(1, clientAddress.getAddress());
        int totalBits = baseAddress.getAddress().length * 8;
        BigInteger fullMask = BigInteger.ONE.shiftLeft(totalBits).subtract(BigInteger.ONE);
        BigInteger hostMask = prefixLength == totalBits
                ? BigInteger.ZERO
                : BigInteger.ONE.shiftLeft(totalBits - prefixLength).subtract(BigInteger.ONE);
        BigInteger networkMask = fullMask.xor(hostMask);
        return base.and(networkMask).equals(client.and(networkMask));
    }

    private static String normalizeCidr(String cidr) {
        String[] parts = cidr.split("/", 2);
        if (parts.length != 2) {
            throw new BusinessException("IP 白名单 CIDR 格式不正确: " + cidr);
        }
        InetAddress address = normalizeSingleIp(parts[0]);
        int maxPrefix = address.getAddress().length * 8;
        int prefixLength = parsePrefixLength(parts[1], maxPrefix);
        return address.getHostAddress() + "/" + prefixLength;
    }

    private static int parsePrefixLength(String prefix, int maxPrefix) {
        try {
            int prefixLength = Integer.parseInt(prefix);
            if (prefixLength < 0 || prefixLength > maxPrefix) {
                throw new BusinessException("IP 白名单掩码范围不合法: " + prefix);
            }
            return prefixLength;
        } catch (NumberFormatException exception) {
            throw new BusinessException("IP 白名单掩码格式不正确: " + prefix);
        }
    }

    private static InetAddress normalizeSingleIp(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException exception) {
            throw new BusinessException("IP 白名单存在非法地址: " + ip);
        }
    }
}

package com.bml.module.system.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计日志 CSV 导出工具类。
 * <p>
 * 导出能力属于审计模块的通用基础能力。当前采用 CSV 格式，避免引入额外 Excel 依赖，兼容 Excel、WPS、Numbers 和常见 SIEM/BI 工具。
 * 后续如需要 XLSX，可在本工具类内扩展实现，业务控制器无需感知底层格式变化。
 * </p>
 *
 * @author BML Team
 */
public final class AuditCsvExportUtils {

    private AuditCsvExportUtils() {
    }

    /**
     * 写出 CSV 下载响应。
     *
     * @param response HTTP 响应对象
     * @param filename 下载文件名
     * @param headers  表头列
     * @param rows     数据行
     * @throws IOException 写出失败时抛出，由全局异常处理器统一返回错误
     */
    public static void writeCsv(HttpServletResponse response, String filename, List<String> headers, List<List<?>> rows)
            throws IOException {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        StringBuilder builder = new StringBuilder();
        builder.append('\ufeff');
        builder.append(toCsvLine(headers)).append('\n');
        for (List<?> row : rows) {
            builder.append(toCsvLine(row)).append('\n');
        }
        response.getWriter().write(builder.toString());
        response.getWriter().flush();
    }

    private static String toCsvLine(List<?> values) {
        return values.stream().map(AuditCsvExportUtils::escapeCsvValue).collect(Collectors.joining(","));
    }

    private static String escapeCsvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\r", " ").replace("\n", " ");
        if (text.contains(",") || text.contains("\"") || text.contains(" ")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}

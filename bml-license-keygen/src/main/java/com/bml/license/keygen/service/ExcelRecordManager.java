package com.bml.license.keygen.service;

import com.bml.license.keygen.model.LicensePayload;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 许可证签发记录 Excel 管理器。
 * <p>
 * 每次签发许可证时，将记录追加到 Excel 文件中，便于供应商追溯管理。
 * Excel 文件采用精心设计的样式，包含标题行、表头、数据行、日期格式化等。
 * </p>
 *
 * @author BML Team
 */
public class ExcelRecordManager {

    private static final String SHEET_NAME = "许可证签发记录";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 列标题 */
    private static final String[] HEADERS = {
            "序号", "许可证ID", "客户名称", "客户编码", "产品版本",
            "授权模块", "最大API账号数", "单账号最大用户数", "全局最大用户数",
            "签发日期", "到期日期", "签发时间", "备注"
    };

    /** 列宽（字符数 × 256） */
    private static final int[] COLUMN_WIDTHS = {
            2400, 7200, 6400, 5600, 4000,
            8000, 5600, 5600, 5600,
            4800, 4800, 6400, 8000
    };

    private final Path excelPath;

    public ExcelRecordManager(Path excelPath) {
        this.excelPath = excelPath;
    }

    /**
     * 追加一条签发记录到 Excel。
     * 若文件不存在则自动创建并初始化表头。
     *
     * @param payload 许可证载荷
     * @throws IOException IO 异常
     */
    public void appendRecord(LicensePayload payload) throws IOException {
        Workbook workbook;
        boolean isNew = !Files.exists(excelPath);

        if (isNew) {
            workbook = createWorkbook();
        } else {
            try (FileInputStream fis = new FileInputStream(excelPath.toFile())) {
                workbook = new XSSFWorkbook(fis);
            }
        }

        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) {
            sheet = createSheet(workbook);
        }

        int rowNum = sheet.getLastRowNum() + 1;
        int seq = rowNum - 2; // 减去标题行和表头行

        Row row = sheet.createRow(rowNum);
        row.setHeightInPoints(28);

        CellStyle dataStyle = createDataCellStyle(workbook, rowNum % 2 == 0);
        CellStyle dateStyle = createDateCellStyle(workbook, rowNum % 2 == 0);

        int col = 0;
        createCell(row, col++, seq, dataStyle);
        createCell(row, col++, payload.getLicenseId(), dataStyle);
        createCell(row, col++, payload.getCustomerName(), dataStyle);
        createCell(row, col++, payload.getCustomerCode(), dataStyle);
        createCell(row, col++, payload.getProductVersion(), dataStyle);
        createCell(row, col++, formatFeatures(payload.getFeatures()), dataStyle);
        createCell(row, col++, payload.getMaxApiAccounts(), dataStyle);
        createCell(row, col++, payload.getMaxUsersPerAccount(), dataStyle);
        createCell(row, col++, payload.getMaxTotalUsers(), dataStyle);
        createCell(row, col++, formatDate(payload.getIssueDate()), dateStyle);
        createCell(row, col++, formatDate(payload.getExpireDate()), dateStyle);
        createCell(row, col++, LocalDateTime.now().format(DATETIME_FMT), dateStyle);
        createCell(row, col, payload.getRemark() != null ? payload.getRemark() : "", dataStyle);

        Files.createDirectories(excelPath.getParent());
        try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
            workbook.write(fos);
        }
        workbook.close();
    }

    /**
     * 创建新的工作簿并初始化样式。
     */
    private Workbook createWorkbook() {
        Workbook workbook = new XSSFWorkbook();
        createSheet(workbook);
        return workbook;
    }

    /**
     * 创建并初始化工作表（标题行 + 表头行）。
     */
    private Sheet createSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(SHEET_NAME);

        // 设置列宽
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            sheet.setColumnWidth(i, COLUMN_WIDTHS[i]);
        }

        // ── 标题行（合并单元格） ──
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(48);
        CellStyle titleStyle = createTitleStyle(workbook);

        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BML 许可证签发记录");
        titleCell.setCellStyle(titleStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));
        // 合并区域内的其他单元格也需要设置样式
        for (int i = 1; i < HEADERS.length; i++) {
            Cell cell = titleRow.createCell(i);
            cell.setCellStyle(titleStyle);
        }

        // ── 副标题行 ──
        Row subtitleRow = sheet.createRow(1);
        subtitleRow.setHeightInPoints(24);
        CellStyle subtitleStyle = createSubtitleStyle(workbook);

        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("由 BML License Keygen 工具自动生成 | 请妥善保管，切勿泄露私钥信息");
        subtitleCell.setCellStyle(subtitleStyle);

        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, HEADERS.length - 1));
        for (int i = 1; i < HEADERS.length; i++) {
            Cell cell = subtitleRow.createCell(i);
            cell.setCellStyle(subtitleStyle);
        }

        // ── 表头行 ──
        Row headerRow = sheet.createRow(2);
        headerRow.setHeightInPoints(36);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // 冻结表头
        sheet.createFreezePane(0, 3);

        // 设置筛选
        sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, HEADERS.length - 1));

        return sheet;
    }

    // ── 样式工厂方法 ──

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Microsoft YaHei");
        font.setFontHeightInPoints((short) 18);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 22, (byte) 93, (byte) 255}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createSubtitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Microsoft YaHei");
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 240, (byte) 244, (byte) 255}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Microsoft YaHei");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 64, (byte) 128, (byte) 255}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.WHITE.getIndex());
        return style;
    }

    private CellStyle createDataCellStyle(Workbook wb, boolean even) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Microsoft YaHei");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        if (even) {
            style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 245, (byte) 247, (byte) 255}, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setBorderBottom(BorderStyle.THIN);
        if (style instanceof XSSFCellStyle xssfStyle) {
            xssfStyle.setBottomBorderColor(new XSSFColor(new byte[]{(byte) 220, (byte) 225, (byte) 240}, null));
        }
        return style;
    }

    private CellStyle createDateCellStyle(Workbook wb, boolean even) {
        CellStyle style = createDataCellStyle(wb, even);
        DataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("yyyy-mm-dd"));
        return style;
    }

    // ── 辅助方法 ──

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String formatFeatures(List<String> features) {
        if (features == null || features.isEmpty()) {
            return "";
        }
        return String.join(", ", features);
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "";
    }
}

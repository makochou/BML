package com.bml.license.keygen.ui;

import com.bml.license.keygen.BmlLicenseKeygenApp;
import com.bml.license.keygen.model.LicensePayload;
import com.bml.license.keygen.service.ExcelRecordManager;
import com.bml.license.keygen.service.LicenseFileGenerator;
import com.bml.license.keygen.service.LicenseFileParser;
import com.bml.license.keygen.service.RsaKeyPairManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 许可证签发面板。
 * <p>
 * 提供客户信息表单、配额设置、功能模块勾选，一键签发生成 .lic 文件并追加 Excel 记录。
 * 支持加载已有的 .lic 文件回填表单，方便在原有授权信息上进行更新续签。
 * </p>
 *
 * @author BML Team
 */
public class IssueLicensePanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 可选授权模块定义。
     * <p>
     * 许可证授权控制的是前台业务端模块（非中台管理），
     * 前台业务模块尚未开发，待开发后在此补充模块定义。
     * 格式：{模块标识码, 中文显示名}
     * </p>
     */
    private static final String[][] FEATURE_DEFS = {};

    // ── 表单字段 ──
    private final JTextField customerNameField = createTextField("输入客户名称");
    private final JTextField customerCodeField = createTextField("如 CUST-001");
    private final JTextField productVersionField = createTextField("2.0.0");
    private final JSpinner maxApiAccountsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
    private final JSpinner maxUsersPerAccountSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
    private final JSpinner maxTotalUsersSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
    private final JTextField expireDateField = createTextField(LocalDate.now().plusYears(1).format(DATE_FMT));
    private final JTextArea remarkArea = new JTextArea(2, 30);
    private final JCheckBox[] featureChecks;

    // ── 加载状态 ──

    /** 当前加载的许可证来源文件路径（未加载时为 null） */
    private Path loadedLicenseFile;

    /** 加载状态提示标签 */
    private final JLabel loadStatusLabel = new JLabel(" ");

    // ── 结果区域 ──
    private final JTextArea resultArea = new JTextArea(6, 50);

    private final RsaKeyPairManager keyManager;
    private final LicenseFileGenerator generator;
    private final ExcelRecordManager excelManager;
    private final LicenseFileParser parser;

    public IssueLicensePanel(RsaKeyPairManager keyManager, LicenseFileGenerator generator,
                             ExcelRecordManager excelManager) {
        this.keyManager = keyManager;
        this.generator = generator;
        this.excelManager = excelManager;
        this.parser = new LicenseFileParser();

        // 初始化功能模块复选框
        featureChecks = new JCheckBox[FEATURE_DEFS.length];
        for (int i = 0; i < FEATURE_DEFS.length; i++) {
            featureChecks[i] = new JCheckBox(FEATURE_DEFS[i][1], true);
            featureChecks[i].setFont(new Font("微软雅黑", Font.PLAIN, 13));
        }

        productVersionField.setText("2.0.0");
        initUI();
    }

    private void initUI() {
        setLayout(new MigLayout("fill, insets 16", "[grow]", "[]8[]12[]12[]"));
        setBackground(new Color(250, 251, 254));

        // ── 工具栏：加载已有许可证 ──
        JPanel toolBar = createToolBar();
        add(toolBar, "growx, wrap");

        // ── 客户信息卡片 ──
        JPanel customerCard = createCard("客户信息");
        customerCard.setLayout(new MigLayout("insets 12 16 12 16, gap 8 6", "[][180!]24[][180!]", "[][]"));

        customerCard.add(createLabel("客户名称 *"), "right");
        customerCard.add(customerNameField, "growx");
        customerCard.add(createLabel("客户编码 *"), "right");
        customerCard.add(customerCodeField, "growx, wrap");

        customerCard.add(createLabel("产品版本"), "right");
        customerCard.add(productVersionField, "growx");
        customerCard.add(createLabel("到期日期"), "right");
        customerCard.add(expireDateField, "growx, wrap");

        add(customerCard, "growx, wrap");

        // ── 配额 & 模块卡片 ──
        JPanel middlePanel = new JPanel(new MigLayout("fill, insets 0, gap 12", "[grow 1][grow 1]", "[grow]"));
        middlePanel.setOpaque(false);

        // 配额卡片
        JPanel quotaCard = createCard("配额设置");
        quotaCard.setLayout(new MigLayout("insets 12 16 12 16, gap 8 8", "[][120!]", "[][][]"));

        /* 排列顺序与前端配额面板保持一致：业务用户上限 → 最大 API 账号 → 允许 API 账号调用新增用户 */
        quotaCard.add(createLabel("业务用户上限"), "right");
        quotaCard.add(maxTotalUsersSpinner, "growx, wrap");
        quotaCard.add(createLabel("最大 API 账号"), "right");
        quotaCard.add(maxApiAccountsSpinner, "growx, wrap");
        quotaCard.add(createLabel("允许API账号调用新增用户"), "right");
        quotaCard.add(maxUsersPerAccountSpinner, "growx, wrap");

        JLabel quotaHint = new JLabel("* 0 = 不限制");
        quotaHint.setFont(new Font("微软雅黑", Font.ITALIC, 11));
        quotaHint.setForeground(new Color(140, 150, 165));
        quotaCard.add(quotaHint, "span 2, center");

        middlePanel.add(quotaCard, "grow");

        // 模块卡片
        JPanel moduleCard = createCard("授权功能模块");
        moduleCard.setLayout(new MigLayout("insets 12 16 12 16, wrap 1, gap 4"));
        for (JCheckBox cb : featureChecks) {
            moduleCard.add(cb);
        }

        JPanel moduleButtonPanel = new JPanel(new MigLayout("insets 4 0 0 0, gap 6"));
        moduleButtonPanel.setOpaque(false);
        JButton selectAll = createSmallButton("全选");
        selectAll.addActionListener(e -> { for (JCheckBox cb : featureChecks) cb.setSelected(true); });
        JButton deselectAll = createSmallButton("全不选");
        deselectAll.addActionListener(e -> { for (JCheckBox cb : featureChecks) cb.setSelected(false); });
        moduleButtonPanel.add(selectAll);
        moduleButtonPanel.add(deselectAll);
        moduleCard.add(moduleButtonPanel);

        middlePanel.add(moduleCard, "grow");

        add(middlePanel, "growx, wrap");

        // ── 备注 + 操作 ──
        JPanel bottomPanel = new JPanel(new MigLayout("fill, insets 0, gap 12", "[grow 1][grow 1]", "[grow]"));
        bottomPanel.setOpaque(false);

        // 备注
        JPanel remarkCard = createCard("备注");
        remarkCard.setLayout(new MigLayout("fill, insets 8 12 8 12"));
        remarkArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        remarkArea.setLineWrap(true);
        remarkArea.setWrapStyleWord(true);
        remarkCard.add(new JScrollPane(remarkArea), "grow");
        bottomPanel.add(remarkCard, "grow");

        // 操作 & 结果
        JPanel actionCard = createCard("签发操作");
        actionCard.setLayout(new MigLayout("fill, insets 12 16 12 16", "[grow]", "[]8[grow]"));

        JButton issueBtn = new JButton("签发许可证");
        issueBtn.setFont(new Font("微软雅黑", Font.BOLD, 15));
        issueBtn.setForeground(Color.WHITE);
        issueBtn.setBackground(BmlLicenseKeygenApp.BRAND_PRIMARY);
        issueBtn.setPreferredSize(new Dimension(0, 42));
        issueBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        issueBtn.addActionListener(this::handleIssue);
        actionCard.add(issueBtn, "growx, wrap");

        resultArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(248, 249, 252));
        resultArea.setText("签发结果将显示在此处...");
        resultArea.setForeground(new Color(140, 150, 165));
        actionCard.add(new JScrollPane(resultArea), "grow");

        bottomPanel.add(actionCard, "grow");

        add(bottomPanel, "grow");
    }

    /**
     * 创建顶部工具栏面板。
     * <p>
     * 包含「加载已有许可证」按钮和加载状态标签，
     * 用于从已签发的 .lic 文件回填表单信息。
     * </p>
     */
    private JPanel createToolBar() {
        JPanel bar = new JPanel(new MigLayout("insets 4 0 4 0, gap 8", "[][][]push", "[]"));
        bar.setOpaque(false);

        // 加载许可证按钮
        JButton loadBtn = new JButton("加载已有许可证");
        loadBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        loadBtn.setForeground(BmlLicenseKeygenApp.BRAND_PRIMARY);
        loadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadBtn.setToolTipText("选择之前签发的 .lic 文件，自动回填表单信息用于更新续签");
        loadBtn.addActionListener(this::handleLoadLicense);

        // 清空表单按钮（放在加载按钮旁边）
        JButton clearBtn = new JButton("清空表单（新建）");
        clearBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        clearBtn.setForeground(new Color(75, 85, 100));
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.setToolTipText("清空所有表单字段，恢复为初始空白状态");
        clearBtn.addActionListener(e -> resetForm());

        // 加载状态标签
        loadStatusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        loadStatusLabel.setForeground(new Color(140, 150, 165));

        bar.add(loadBtn);
        bar.add(clearBtn);
        bar.add(loadStatusLabel);

        return bar;
    }

    /**
     * 签发按钮点击处理。
     */
    private void handleIssue(ActionEvent e) {
        // 校验必填项
        String customerName = customerNameField.getText().trim();
        String customerCode = customerCodeField.getText().trim();
        if (customerName.isEmpty() || customerCode.isEmpty()) {
            showResult("[!] \u8bf7\u586b\u5199\u5ba2\u6237\u540d\u79f0\u548c\u5ba2\u6237\u7f16\u7801\uff01", true);
            return;
        }

        if (!keyManager.isReady()) {
            showResult("[!] RSA \u5bc6\u94a5\u5bf9\u672a\u5c31\u7eea\uff0c\u8bf7\u5148\u5728\u300c\u5bc6\u94a5\u7ba1\u7406\u300d\u4e2d\u751f\u6210\u5bc6\u94a5\u5bf9", true);
            return;
        }

        // 构建 payload
        LicensePayload payload = new LicensePayload();
        String licenseId = "LIC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payload.setLicenseId(licenseId);
        payload.setCustomerName(customerName);
        payload.setCustomerCode(customerCode);
        payload.setProductVersion(productVersionField.getText().trim().isEmpty()
                ? "2.0.0" : productVersionField.getText().trim());

        // 功能模块
        List<String> features = new java.util.ArrayList<>();
        for (int i = 0; i < featureChecks.length; i++) {
            if (featureChecks[i].isSelected()) {
                features.add(FEATURE_DEFS[i][0]);
            }
        }
        payload.setFeatures(features);

        // 配额
        payload.setMaxApiAccounts((int) maxApiAccountsSpinner.getValue());
        payload.setMaxUsersPerAccount((int) maxUsersPerAccountSpinner.getValue());
        payload.setMaxTotalUsers((int) maxTotalUsersSpinner.getValue());

        // 日期
        payload.setIssueDate(LocalDate.now());
        try {
            payload.setExpireDate(LocalDate.parse(expireDateField.getText().trim(), DATE_FMT));
        } catch (DateTimeParseException ex) {
            payload.setExpireDate(LocalDate.now().plusYears(1));
        }

        // 备注
        String remark = remarkArea.getText().trim();
        if (!remark.isEmpty()) {
            payload.setRemark(remark);
        }

        // 确认对话框
        int confirm = JOptionPane.showConfirmDialog(this,
                buildConfirmMessage(payload),
                "\u786e\u8ba4\u7b7e\u53d1", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // 生成文件
        try {
            String fileName = String.format("BML-LIC-%s-%s.lic",
                    payload.getCustomerCode(),
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            Path outputPath = BmlLicenseKeygenApp.OUTPUT_DIR.resolve(fileName);

            generator.generate(payload, outputPath);
            excelManager.appendRecord(payload);

            StringBuilder sb = new StringBuilder();
            sb.append("[OK] \u8bb8\u53ef\u8bc1\u7b7e\u53d1\u6210\u529f\uff01\n\n");
            sb.append(">> \u8bb8\u53ef\u8bc1ID: ").append(licenseId).append("\n");
            sb.append(">> \u6587\u4ef6\u8def\u5f84: ").append(outputPath.toAbsolutePath()).append("\n");
            sb.append(">> Excel\u8bb0\u5f55: ").append(BmlLicenseKeygenApp.EXCEL_PATH.toAbsolutePath()).append("\n\n");
            sb.append("\u4e0b\u4e00\u6b65\uff1a\u5c06 .lic \u6587\u4ef6\u4ea4\u4ed8\u7ed9\u5ba2\u6237\uff0c");
            sb.append("\u5ba2\u6237\u5728\u4e2d\u53f0\u7ba1\u7406\u4e0a\u4f20\u5373\u53ef\u6fc0\u6d3b\u3002");
            showResult(sb.toString(), false);

            // 打开输出目录
            Desktop.getDesktop().open(BmlLicenseKeygenApp.OUTPUT_DIR.toFile());

        } catch (Exception ex) {
            showResult("[!] \u7b7e\u53d1\u5931\u8d25: " + ex.getMessage(), true);
        }
    }

    /**
     * 构建确认弹窗消息。
     */
    private String buildConfirmMessage(LicensePayload p) {
        /* 配额展示顺序与前端配额面板保持一致：业务用户上限 → 最大 API 账号 → 允许 API 账号调用新增用户 */
        return String.format("""
                \u8bf7\u786e\u8ba4\u4ee5\u4e0b\u8bb8\u53ef\u8bc1\u4fe1\u606f\uff1a

                \u5ba2\u6237\u540d\u79f0:  %s
                \u5ba2\u6237\u7f16\u7801:  %s
                \u4ea7\u54c1\u7248\u672c:  %s
                \u6388\u6743\u6a21\u5757:  %s
                \u4e1a\u52a1\u7528\u6237\u4e0a\u9650: %s
                \u6700\u5927API\u8d26\u53f7: %s
                \u5141\u8bb8API\u8d26\u53f7\u8c03\u7528\u65b0\u589e\u7528\u6237: %s
                \u5230\u671f\u65e5\u671f:  %s
                """,
                p.getCustomerName(),
                p.getCustomerCode(),
                p.getProductVersion(),
                p.getFeatures() != null ? String.join(", ", p.getFeatures()) : "\u65e0",
                p.getMaxTotalUsers() == 0 ? "\u4e0d\u9650" : p.getMaxTotalUsers(),
                p.getMaxApiAccounts() == 0 ? "\u4e0d\u9650" : p.getMaxApiAccounts(),
                p.getMaxUsersPerAccount() == 0 ? "\u4e0d\u9650" : p.getMaxUsersPerAccount(),
                p.getExpireDate());
    }

    // ── 加载已有许可证 ──

    /**
     * 处理「加载已有许可证」按钮点击事件。
     * <p>
     * 弹出文件选择器，用户选择 .lic 文件后解析其内容并回填到表单中，
     * 方便在原有授权信息的基础上修改后重新签发（续签/升级/降级）。
     * </p>
     */
    private void handleLoadLicense(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择已有许可证文件");
        chooser.setFileFilter(new FileNameExtensionFilter("BML 许可证文件 (*.lic)", "lic"));

        // 默认打开 output 目录（签发文件输出目录）
        File outputDir = BmlLicenseKeygenApp.OUTPUT_DIR.toFile();
        if (outputDir.exists()) {
            chooser.setCurrentDirectory(outputDir);
        }

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        try {
            LicensePayload payload = parser.parse(selectedFile.toPath());
            populateForm(payload);
            loadedLicenseFile = selectedFile.toPath();

            // 更新加载状态标签
            loadStatusLabel.setText("已加载: " + selectedFile.getName());
            loadStatusLabel.setForeground(BmlLicenseKeygenApp.COLOR_SUCCESS);

            // 在结果区域显示加载的许可证信息摘要
            showLoadedSummary(payload, selectedFile);

        } catch (LicenseFileParser.LicenseParseException ex) {
            loadedLicenseFile = null;
            loadStatusLabel.setText("加载失败");
            loadStatusLabel.setForeground(BmlLicenseKeygenApp.COLOR_DANGER);
            showResult("[!] 许可证文件解析失败:\n" + ex.getMessage(), true);
        }
    }

    /**
     * 将许可证载荷数据回填到表单各字段。
     * <p>
     * 此方法是通用的表单填充方法，接受任意 {@link LicensePayload} 对象，
     * 将其字段值映射到对应的表单组件中。
     * </p>
     *
     * @param payload 许可证载荷对象
     */
    private void populateForm(LicensePayload payload) {
        // 客户信息
        customerNameField.setText(nullSafe(payload.getCustomerName()));
        customerCodeField.setText(nullSafe(payload.getCustomerCode()));
        productVersionField.setText(
                payload.getProductVersion() != null && !payload.getProductVersion().isEmpty()
                        ? payload.getProductVersion() : "2.0.0");

        // 到期日期
        if (payload.getExpireDate() != null) {
            expireDateField.setText(payload.getExpireDate().format(DATE_FMT));
        }

        // 配额
        maxApiAccountsSpinner.setValue(payload.getMaxApiAccounts());
        maxUsersPerAccountSpinner.setValue(payload.getMaxUsersPerAccount());
        maxTotalUsersSpinner.setValue(payload.getMaxTotalUsers());

        // 功能模块复选框
        if (FEATURE_DEFS.length > 0 && payload.getFeatures() != null) {
            Set<String> enabledFeatures = new HashSet<>(payload.getFeatures());
            for (int i = 0; i < FEATURE_DEFS.length; i++) {
                featureChecks[i].setSelected(enabledFeatures.contains(FEATURE_DEFS[i][0]));
            }
        }

        // 备注
        remarkArea.setText(nullSafe(payload.getRemark()));
    }

    /**
     * 在结果区域显示已加载许可证的信息摘要。
     *
     * @param payload 许可证载荷
     * @param file    许可证文件
     */
    private void showLoadedSummary(LicensePayload payload, File file) {
        StringBuilder sb = new StringBuilder();
        sb.append("[已加载] 许可证信息如下，可在表单中修改后重新签发\n");
        sb.append("====================================\n\n");
        sb.append(">> 文件路径: ").append(file.getAbsolutePath()).append("\n");
        sb.append(">> 许可证ID: ").append(nullSafe(payload.getLicenseId())).append("\n");
        sb.append(">> 客户名称: ").append(nullSafe(payload.getCustomerName())).append("\n");
        sb.append(">> 客户编码: ").append(nullSafe(payload.getCustomerCode())).append("\n");
        sb.append(">> 产品版本: ").append(nullSafe(payload.getProductVersion())).append("\n");
        sb.append(">> 签发日期: ").append(payload.getIssueDate() != null ? payload.getIssueDate().format(DATE_FMT) : "-").append("\n");
        sb.append(">> 到期日期: ").append(payload.getExpireDate() != null ? payload.getExpireDate().format(DATE_FMT) : "-").append("\n");
        sb.append(">> 配额: 业务用户上限=").append(formatQuota(payload.getMaxTotalUsers()));
        sb.append(", API账号=").append(formatQuota(payload.getMaxApiAccounts()));
        sb.append(", 允许API账号调用新增用户=").append(formatQuota(payload.getMaxUsersPerAccount())).append("\n");

        if (payload.getFeatures() != null && !payload.getFeatures().isEmpty()) {
            sb.append(">> 授权模块: ").append(String.join(", ", payload.getFeatures())).append("\n");
        }

        if (payload.getRemark() != null && !payload.getRemark().isEmpty()) {
            sb.append(">> 备注: ").append(payload.getRemark()).append("\n");
        }

        sb.append("\n[提示] 修改需要更新的字段后点击「签发许可证」即可生成新的许可证文件。");
        sb.append("\n       签发时会自动生成新的许可证ID和签发日期。");
        showResult(sb.toString(), false);
    }

    /**
     * 重置表单为初始空白状态。
     * <p>
     * 清空所有表单字段并恢复默认值，同时清除加载状态。
     * </p>
     */
    private void resetForm() {
        customerNameField.setText("");
        customerCodeField.setText("");
        productVersionField.setText("2.0.0");
        expireDateField.setText(LocalDate.now().plusYears(1).format(DATE_FMT));
        maxApiAccountsSpinner.setValue(0);
        maxUsersPerAccountSpinner.setValue(0);
        maxTotalUsersSpinner.setValue(0);
        remarkArea.setText("");

        // 复选框全选
        for (JCheckBox cb : featureChecks) {
            cb.setSelected(true);
        }

        // 清除加载状态
        loadedLicenseFile = null;
        loadStatusLabel.setText(" ");

        showResult("签发结果将显示在此处...", false);
        resultArea.setForeground(new Color(140, 150, 165));
    }

    // ── 辅助方法 ──

    /**
     * 格式化配额显示（0 表示不限）。
     */
    private static String formatQuota(int value) {
        return value == 0 ? "不限" : String.valueOf(value);
    }

    /**
     * null 安全字符串转换。
     */
    private static String nullSafe(String value) {
        return value != null ? value : "";
    }

    private void showResult(String text, boolean isError) {
        resultArea.setForeground(isError ? BmlLicenseKeygenApp.COLOR_DANGER : new Color(30, 40, 60));
        resultArea.setText(text);
        resultArea.setCaretPosition(0);
    }

    // ── UI 工具方法 ──

    private static JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(228, 232, 240), 1, true),
                "  " + title + "  ");
        tb.setTitleFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 13));
        tb.setTitleColor(BmlLicenseKeygenApp.BRAND_PRIMARY);
        card.setBorder(tb);
        return card;
    }

    private static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 13));
        label.setForeground(new Color(75, 85, 100));
        return label;
    }

    private static JTextField createTextField(String placeholder) {
        JTextField field = new JTextField(20);
        field.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 13));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        return field;
    }

    private static JButton createSmallButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 11));
        btn.setMargin(new Insets(2, 8, 2, 8));
        return btn;
    }
}

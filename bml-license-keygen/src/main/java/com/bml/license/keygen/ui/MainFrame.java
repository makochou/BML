package com.bml.license.keygen.ui;

import com.bml.license.keygen.BmlLicenseKeygenApp;
import com.bml.license.keygen.service.ExcelRecordManager;
import com.bml.license.keygen.service.LicenseFileGenerator;
import com.bml.license.keygen.service.RsaKeyPairManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * 主窗口框架。
 * <p>
 * 顶部为品牌横幅，中间为 TabbedPane 包含「签发许可证」和「密钥管理」两个面板。
 * </p>
 *
 * @author BML Team
 */
public class MainFrame extends JFrame {

    private final RsaKeyPairManager keyManager;
    private final LicenseFileGenerator generator;
    private final ExcelRecordManager excelManager;

    public MainFrame(RsaKeyPairManager keyManager) {
        this.keyManager = keyManager;
        this.generator = new LicenseFileGenerator(keyManager.getPrivateKey());
        this.excelManager = new ExcelRecordManager(BmlLicenseKeygenApp.EXCEL_PATH);

        initUI();
    }

    private void initUI() {
        setTitle("BML \u8bb8\u53ef\u8bc1\u79bb\u7ebf\u7b7e\u53d1\u5de5\u5177 v2.0.0");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 680));
        setPreferredSize(new Dimension(960, 720));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new MigLayout("fill, insets 0, gap 0", "[grow]", "[]0[grow]0[]"));

        // ── 顶部横幅 ──
        root.add(createBanner(), "growx, h 80!, wrap");

        // ── 标签页 ──
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 14));
        tabs.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));

        tabs.addTab("  \u7b7e\u53d1\u8bb8\u53ef\u8bc1  ", new IssueLicensePanel(keyManager, generator, excelManager));
        tabs.addTab("  \u5bc6\u94a5\u7ba1\u7406  ", new KeyManagePanel(keyManager, this));

        root.add(tabs, "grow, wrap");

        // ── 底部状态栏 ──
        root.add(createStatusBar(), "growx, h 28!");

        setContentPane(root);
        pack();
    }

    /**
     * 创建品牌横幅面板。
     */
    private JPanel createBanner() {
        JPanel banner = new JPanel(new MigLayout("fill, insets 16 24 16 24", "[]push[]")) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(22, 93, 255),
                        getWidth(), 0, new Color(64, 128, 255));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        banner.setOpaque(false);

        // 标题
        JLabel titleLabel = new JLabel("BML \u8bb8\u53ef\u8bc1\u79bb\u7ebf\u7b7e\u53d1\u5de5\u5177");
        titleLabel.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("BML License Offline Keygen Tool");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(200, 215, 255));

        JPanel titleGroup = new JPanel(new MigLayout("insets 0, gap 0, flowy", "[]", "[]2[]"));
        titleGroup.setOpaque(false);
        titleGroup.add(titleLabel);
        titleGroup.add(subtitleLabel);

        // 版本标签
        JLabel versionLabel = new JLabel("v2.0.0");
        versionLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        versionLabel.setForeground(new Color(180, 200, 255));
        versionLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 255, 100), 1, true),
                BorderFactory.createEmptyBorder(2, 10, 2, 10)));

        banner.add(titleGroup);
        banner.add(versionLabel);

        return banner;
    }

    /**
     * 创建底部状态栏。
     */
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new MigLayout("fill, insets 4 12 4 12", "[]push[]"));
        bar.setBackground(new Color(248, 249, 252));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 232, 240)));

        JLabel keyStatus = new JLabel(keyManager.isReady()
                ? "RSA \u5bc6\u94a5\u5bf9\u5df2\u5c31\u7eea"
                : "RSA \u5bc6\u94a5\u5bf9\u672a\u521d\u59cb\u5316");
        keyStatus.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 11));
        keyStatus.setForeground(keyManager.isReady()
                ? BmlLicenseKeygenApp.COLOR_SUCCESS
                : BmlLicenseKeygenApp.COLOR_DANGER);

        JLabel pathLabel = new JLabel("\u5bc6\u94a5\u76ee\u5f55: " + BmlLicenseKeygenApp.KEYS_DIR.toAbsolutePath());
        pathLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        pathLabel.setForeground(new Color(140, 150, 165));

        bar.add(keyStatus);
        bar.add(pathLabel);
        return bar;
    }

    /**
     * 重新初始化密钥后刷新生成器。
     */
    public void refreshAfterKeyChange() {
        // Generator 内部持有私钥引用，需要用新密钥重建
        // 由 KeyManagePanel 调用
    }
}

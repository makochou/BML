package com.bml.license.keygen;

import com.bml.license.keygen.service.RsaKeyPairManager;
import com.bml.license.keygen.ui.MainFrame;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/**
 * BML 许可证离线签发工具 — GUI 主入口。
 * <p>
 * 基于 FlatLaf 现代化 Swing 主题构建的图形界面工具，提供：
 * <ol>
 *     <li>许可证签发 — 表单填写客户信息，一键生成签名 .lic 文件并记录到 Excel</li>
 *     <li>密钥管理 — 查看/复制 RSA 公钥，重新生成密钥对</li>
 * </ol>
 * </p>
 *
 * @author BML Team
 */
public class BmlLicenseKeygenApp {

    /** 品牌主色 */
    public static final Color BRAND_PRIMARY = new Color(22, 93, 255);
    /** 品牌浅色背景 */
    public static final Color BRAND_LIGHT = new Color(240, 244, 255);
    /** 成功色 */
    public static final Color COLOR_SUCCESS = new Color(0, 180, 42);
    /** 警告色 */
    public static final Color COLOR_WARNING = new Color(255, 125, 0);
    /** 错误色 */
    public static final Color COLOR_DANGER = new Color(245, 63, 63);

    public static final Path BASE_DIR = Path.of(".");
    public static final Path KEYS_DIR = BASE_DIR.resolve("keys");
    public static final Path OUTPUT_DIR = BASE_DIR.resolve("output");
    public static final Path EXCEL_PATH = OUTPUT_DIR.resolve("BML\u8bb8\u53ef\u8bc1\u7b7e\u53d1\u8bb0\u5f55.xlsx");

    public static void main(String[] args) {
        // 配置 FlatLaf 全局属性
        FlatLaf.registerCustomDefaultsSource("com.bml.license.keygen");
        System.setProperty("flatlaf.useWindowDecorations", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                // 设置 FlatLaf IntelliJ 亮色主题
                FlatIntelliJLaf.setup();

                // 全局 UI 微调
                UIManager.put("Component.arc", 10);
                UIManager.put("Button.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("Component.focusWidth", 1);
                UIManager.put("ScrollBar.trackArc", 999);
                UIManager.put("ScrollBar.thumbArc", 999);
                UIManager.put("TabbedPane.selectedBackground", BRAND_LIGHT);
                UIManager.put("TabbedPane.underlineColor", BRAND_PRIMARY);

                // 设置默认字体
                Font font = new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 13);
                UIManager.put("defaultFont", font);

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // 初始化密钥管理器
            RsaKeyPairManager keyManager = new RsaKeyPairManager(KEYS_DIR);
            try {
                keyManager.init();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "RSA \u5bc6\u94a5\u5bf9\u521d\u59cb\u5316\u5931\u8d25: " + ex.getMessage(),
                        "\u521d\u59cb\u5316\u9519\u8bef", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            // 启动主窗口
            MainFrame frame = new MainFrame(keyManager);
            frame.setVisible(true);
        });
    }
}

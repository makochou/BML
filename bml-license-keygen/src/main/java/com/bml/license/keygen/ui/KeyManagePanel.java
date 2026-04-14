package com.bml.license.keygen.ui;

import com.bml.license.keygen.BmlLicenseKeygenApp;
import com.bml.license.keygen.service.RsaKeyPairManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 密钥管理面板。
 * <p>
 * 提供 RSA 公钥查看/复制、密钥对重新生成等功能。
 * </p>
 *
 * @author BML Team
 */
public class KeyManagePanel extends JPanel {

    private final RsaKeyPairManager keyManager;
    private final MainFrame mainFrame;
    private final JTextArea publicKeyArea;
    private final JLabel statusLabel;

    public KeyManagePanel(RsaKeyPairManager keyManager, MainFrame mainFrame) {
        this.keyManager = keyManager;
        this.mainFrame = mainFrame;
        this.publicKeyArea = new JTextArea(4, 60);
        this.statusLabel = new JLabel();

        initUI();
        refreshKeyDisplay();
    }

    private void initUI() {
        setLayout(new MigLayout("fill, insets 16", "[grow]", "[]16[]16[]"));
        setBackground(new Color(250, 251, 254));

        // ── 公钥展示卡片 ──
        JPanel keyCard = createCard("\u2709  RSA \u516c\u94a5\uff08Base64 \u7f16\u7801\uff09");
        keyCard.setLayout(new MigLayout("fill, insets 16", "[grow]", "[]8[]8[]"));

        JLabel hint = new JLabel("\u8bf7\u5c06\u4ee5\u4e0b\u516c\u94a5\u914d\u7f6e\u5230\u540e\u7aef application.yml \u6216\u73af\u5883\u53d8\u91cf BML_LICENSE_PUBLIC_KEY");
        hint.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 12));
        hint.setForeground(new Color(100, 110, 130));
        keyCard.add(hint, "wrap");

        publicKeyArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        publicKeyArea.setEditable(false);
        publicKeyArea.setLineWrap(true);
        publicKeyArea.setWrapStyleWord(false);
        publicKeyArea.setBackground(new Color(248, 249, 252));
        publicKeyArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane keyScroll = new JScrollPane(publicKeyArea);
        keyScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 240)));
        keyCard.add(keyScroll, "grow, wrap");

        JPanel btnPanel = new JPanel(new MigLayout("insets 0, gap 8"));
        btnPanel.setOpaque(false);

        JButton copyBtn = createActionButton("\u590d\u5236\u516c\u94a5", BmlLicenseKeygenApp.BRAND_PRIMARY);
        copyBtn.addActionListener(e -> copyPublicKey());

        JButton copyYamlBtn = createActionButton("\u590d\u5236 YAML \u914d\u7f6e", new Color(64, 128, 255));
        copyYamlBtn.addActionListener(e -> copyYamlConfig());

        btnPanel.add(copyBtn);
        btnPanel.add(copyYamlBtn);
        keyCard.add(btnPanel);

        add(keyCard, "growx, wrap");

        // ── 密钥状态卡片 ──
        JPanel statusCard = createCard("\u26bf  \u5bc6\u94a5\u72b6\u6001");
        statusCard.setLayout(new MigLayout("insets 16, gap 8", "[][grow]", "[][][]"));

        statusCard.add(createLabel("\u5bc6\u94a5\u72b6\u6001:"), "right");
        statusLabel.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 13));
        statusCard.add(statusLabel, "wrap");

        statusCard.add(createLabel("\u79c1\u94a5\u6587\u4ef6:"), "right");
        JLabel privateKeyPath = new JLabel(BmlLicenseKeygenApp.KEYS_DIR.resolve("bml-license-private.key")
                .toAbsolutePath().toString());
        privateKeyPath.setFont(new Font("Consolas", Font.PLAIN, 12));
        privateKeyPath.setForeground(new Color(100, 110, 130));
        statusCard.add(privateKeyPath, "wrap");

        statusCard.add(createLabel("\u516c\u94a5\u6587\u4ef6:"), "right");
        JLabel publicKeyPath = new JLabel(BmlLicenseKeygenApp.KEYS_DIR.resolve("bml-license-public.key")
                .toAbsolutePath().toString());
        publicKeyPath.setFont(new Font("Consolas", Font.PLAIN, 12));
        publicKeyPath.setForeground(new Color(100, 110, 130));
        statusCard.add(publicKeyPath, "wrap");

        add(statusCard, "growx, wrap");

        // ── 危险操作卡片 ──
        JPanel dangerCard = createCard("\u26a0  \u5371\u9669\u64cd\u4f5c");
        dangerCard.setLayout(new MigLayout("insets 16", "[grow][]", "[]"));
        dangerCard.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 200, 200), 1, true),
                "  \u26a0  \u5371\u9669\u64cd\u4f5c  "));
        ((javax.swing.border.TitledBorder) dangerCard.getBorder())
                .setTitleFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 13));
        ((javax.swing.border.TitledBorder) dangerCard.getBorder())
                .setTitleColor(BmlLicenseKeygenApp.COLOR_DANGER);

        JLabel warnLabel = new JLabel("<html>\u91cd\u65b0\u751f\u6210\u5bc6\u94a5\u5bf9\u5c06\u5bfc\u81f4<b>\u6240\u6709\u5df2\u7b7e\u53d1\u7684\u8bb8\u53ef\u8bc1\u5931\u6548</b>\uff0c\u540e\u7aef\u4e5f\u9700\u66f4\u65b0\u516c\u94a5\u914d\u7f6e\u3002</html>");
        warnLabel.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 13));
        warnLabel.setForeground(new Color(180, 60, 60));
        dangerCard.add(warnLabel, "growx");

        JButton regenBtn = createActionButton("\u91cd\u65b0\u751f\u6210\u5bc6\u94a5\u5bf9", BmlLicenseKeygenApp.COLOR_DANGER);
        regenBtn.addActionListener(e -> regenerateKeys());
        dangerCard.add(regenBtn);

        add(dangerCard, "growx");
    }

    /**
     * 刷新公钥显示。
     */
    private void refreshKeyDisplay() {
        if (keyManager.isReady()) {
            publicKeyArea.setText(keyManager.getPublicKeyBase64());
            statusLabel.setText("\u2705 \u5df2\u5c31\u7eea");
            statusLabel.setForeground(BmlLicenseKeygenApp.COLOR_SUCCESS);
        } else {
            publicKeyArea.setText("\u5bc6\u94a5\u5bf9\u672a\u521d\u59cb\u5316");
            statusLabel.setText("\u274c \u672a\u521d\u59cb\u5316");
            statusLabel.setForeground(BmlLicenseKeygenApp.COLOR_DANGER);
        }
    }

    /**
     * 复制公钥到剪贴板。
     */
    private void copyPublicKey() {
        if (!keyManager.isReady()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(keyManager.getPublicKeyBase64()), null);
        JOptionPane.showMessageDialog(this,
                "\u516c\u94a5\u5df2\u590d\u5236\u5230\u526a\u8d34\u677f\uff01",
                "\u590d\u5236\u6210\u529f", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 复制 YAML 配置片段到剪贴板。
     */
    private void copyYamlConfig() {
        if (!keyManager.isReady()) return;
        String yaml = "bml:\n  license:\n    public-key: " + keyManager.getPublicKeyBase64();
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(yaml), null);
        JOptionPane.showMessageDialog(this,
                "YAML \u914d\u7f6e\u5df2\u590d\u5236\u5230\u526a\u8d34\u677f\uff01\n\u7c98\u8d34\u5230 application.yml \u5373\u53ef\u3002",
                "\u590d\u5236\u6210\u529f", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 重新生成 RSA 密钥对。
     */
    private void regenerateKeys() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "\u786e\u5b9a\u8981\u91cd\u65b0\u751f\u6210\u5bc6\u94a5\u5bf9\u5417\uff1f\n\n\u6240\u6709\u5df2\u7b7e\u53d1\u7684\u8bb8\u53ef\u8bc1\u5c06\u5931\u6548\uff01\n\u8bf7\u8f93\u5165 YES \u786e\u8ba4\u3002",
                "\u5371\u9669\u64cd\u4f5c\u786e\u8ba4", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // 二次确认
        String input = JOptionPane.showInputDialog(this,
                "\u8bf7\u8f93\u5165 YES \u786e\u8ba4\u91cd\u65b0\u751f\u6210\uff1a",
                "\u4e8c\u6b21\u786e\u8ba4", JOptionPane.WARNING_MESSAGE);
        if (!"YES".equals(input)) {
            JOptionPane.showMessageDialog(this, "\u5df2\u53d6\u6d88\u64cd\u4f5c\u3002",
                    "\u53d6\u6d88", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            Path privateKeyPath = BmlLicenseKeygenApp.KEYS_DIR.resolve("bml-license-private.key");
            Path publicKeyPath = BmlLicenseKeygenApp.KEYS_DIR.resolve("bml-license-public.key");
            Files.deleteIfExists(privateKeyPath);
            Files.deleteIfExists(publicKeyPath);

            keyManager.init();
            refreshKeyDisplay();

            JOptionPane.showMessageDialog(this,
                    "\u2705 \u65b0\u7684 RSA-2048 \u5bc6\u94a5\u5bf9\u5df2\u751f\u6210\uff01\n\u8bf7\u590d\u5236\u65b0\u516c\u94a5\u914d\u7f6e\u5230\u540e\u7aef\u3002",
                    "\u751f\u6210\u6210\u529f", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "\u5bc6\u94a5\u5bf9\u751f\u6210\u5931\u8d25: " + ex.getMessage(),
                    "\u9519\u8bef", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── UI 工具方法 ──

    private static JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder(
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

    private static JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(6, 16, 6, 16));
        return btn;
    }
}

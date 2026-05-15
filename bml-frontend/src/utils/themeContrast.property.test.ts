/**
 * `PRESET_BEST` WCAG AA 对比度属性测试 - Property 10: 对比度（P_CONTRAST）。
 *
 * 验证目标（与 design.md / Correctness Properties / Property 10 对齐）：
 *   - `PRESET_BEST` 在 LIGHT / DARK × ADMIN / BUSINESS 共 4 个变体下，
 *     正文 `textColor` 与 `backgroundColor` 的对比度 ≥ 4.5:1（WCAG 2.1 AA 正文）；
 *   - 大号文本（≥ 18pt 或 ≥ 14pt 加粗）对比度 ≥ 3:1（WCAG 2.1 AA 大号文本）。
 *
 * 测试设计：
 *   1. PRESET_BEST 的 4 个变体直接来源于
 *      `bml-frontend/src/styles/tokens.preset-best.scss` 文件头注释中已离线核算
 *      的颜色值（同时与后端 Flyway 种子 `V2.16.0__init_theme.sql` 一致），
 *      在 TypeScript 端复刻为常量以便单元化对比度计算；
 *   2. 使用 fast-check 在「4 个变体 × 多个文本场景（正文 / 大号文本）」组合上
 *      属性化遍历，对每个组合调用 {@link contrastRatio} 并断言阈值；
 *   3. 同时对几个边界场景做 sanity check：纯黑 / 纯白对比度恰为 21:1，
 *     同色对比度恰为 1:1。
 *
 * WCAG 2.1 相对亮度计算公式（W3C / WCAG 2.1 §1.4.3 与 §1.4.6）：
 *   - 对每个 sRGB 分量 `c ∈ {R, G, B}`，先归一化到 `[0, 1]`：`c' = c / 255`；
 *   - 应用 sRGB 反伽马校正：
 *       cs = c' ≤ 0.03928 ? c' / 12.92 : ((c' + 0.055) / 1.055) ^ 2.4
 *   - 相对亮度：L = 0.2126·Rs + 0.7152·Gs + 0.0722·Bs；
 *   - 对比度：(L1 + 0.05) / (L2 + 0.05)，其中 L1 ≥ L2。
 *
 * 关联需求：Requirements 2.6, 11.3。
 *
 * **Validates: Requirements 2.6, 11.3**
 */

import { describe, expect, it } from 'vitest';
import fc from 'fast-check';

/* ──────────────────────────────────────────────────────
   一、PRESET_BEST 4 变体常量
   ──────────────────────────────────────────────────────
   严格对齐：
     - bml-frontend/src/styles/tokens.preset-best.scss 文件头部 WCAG 表格；
     - bml-app/src/main/resources/db/migration/V2.16.0__init_theme.sql 种子。
   暗色 textColor / backgroundColor 来自 tokens.preset-best.scss 中
   `$preset-best-dark-admin` / `$preset-best-dark-business` 的 text-1 / bg-1。
   ────────────────────────────────────────────────────── */

/** PRESET_BEST 单个变体的核心颜色（仅保留对比度相关字段）。 */
interface PresetBestVariant {
    /** 变体名称，便于在断言失败时输出具体变体。 */
    readonly name: string;
    /** 正文文字色（WCAG 前景色）。 */
    readonly textColor: string;
    /** 全局背景色（WCAG 背景色）。 */
    readonly backgroundColor: string;
}

/** PRESET_BEST 在 LIGHT / DARK × ADMIN / BUSINESS 下的 4 个变体。 */
const PRESET_BEST_VARIANTS: readonly PresetBestVariant[] = [
    { name: 'LIGHT/ADMIN', textColor: '#1D2129', backgroundColor: '#FFFFFF' },
    { name: 'LIGHT/BUSINESS', textColor: '#1D2129', backgroundColor: '#F7F8FA' },
    { name: 'DARK/ADMIN', textColor: '#F2F3F5', backgroundColor: '#17171A' },
    { name: 'DARK/BUSINESS', textColor: '#F2F3F5', backgroundColor: '#232324' },
] as const;

/* ──────────────────────────────────────────────────────
   二、WCAG 2.1 对比度计算工具
   ────────────────────────────────────────────────────── */

/**
 * 将 `#RRGGBB` 形式的 HEX 颜色解析为三个 0-255 的整数分量。
 *
 * @param hex 形如 `#1D2129` 的字符串，大小写不敏感。
 * @returns `[R, G, B]` 三元组。
 * @throws 当输入不是合法的 6 位 HEX 颜色字符串时抛出 {@link Error}。
 */
function parseHex(hex: string): [number, number, number] {
    const m = /^#([0-9a-fA-F]{6})$/.exec(hex.trim());
    if (!m) {
        throw new Error(`非法 HEX 颜色: ${hex}`);
    }
    const n = Number.parseInt(m[1], 16);
    return [(n >> 16) & 0xff, (n >> 8) & 0xff, n & 0xff];
}

/**
 * 计算单个 sRGB 分量经反伽马校正后的线性值。
 *
 * 实现严格遵循 WCAG 2.1 §1.4.3 / §1.4.6 中的「sRGB 反伽马」分段函数：
 *   - 归一化分量 c' ≤ 0.03928 时直接除以 12.92（线性段）；
 *   - 否则按 ((c' + 0.055) / 1.055) ^ 2.4 进行幂次校正。
 */
function srgbChannelToLinear(channel0to255: number): number {
    const c = channel0to255 / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
}

/**
 * 计算 sRGB 颜色的相对亮度 L（取值范围 0..1，0=纯黑 / 1=纯白）。
 *
 * 公式：`L = 0.2126·R + 0.7152·G + 0.0722·B`，其中 R/G/B 已经过反伽马校正。
 */
function relativeLuminance(hex: string): number {
    const [r, g, b] = parseHex(hex);
    const rs = srgbChannelToLinear(r);
    const gs = srgbChannelToLinear(g);
    const bs = srgbChannelToLinear(b);
    return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
}

/**
 * 计算两种颜色之间的 WCAG 对比度（CR）。
 *
 * 公式：`CR = (Lmax + 0.05) / (Lmin + 0.05)`，结果取值范围 1.0..21.0。
 * 颜色顺序无关，函数内部自动按亮度排序。
 */
function contrastRatio(foregroundHex: string, backgroundHex: string): number {
    const lf = relativeLuminance(foregroundHex);
    const lb = relativeLuminance(backgroundHex);
    const lmax = Math.max(lf, lb);
    const lmin = Math.min(lf, lb);
    return (lmax + 0.05) / (lmin + 0.05);
}

/* ──────────────────────────────────────────────────────
   三、辅助：sanity check（公式正确性自检）
   ──────────────────────────────────────────────────────
   在用 fast-check 对 PRESET_BEST 主张前，先用一组已知答案验证公式实现，
   防止公式错误时把所有变体都「错误地通过」造成假阳性。
   ────────────────────────────────────────────────────── */

describe('themeContrast WCAG 公式实现 - sanity check', () => {
    it('黑白对比度恰为 21:1（WCAG 理论最大值）', () => {
        // 计算误差容许 1e-9 量级即可；公式恒定，无需 fast-check
        const cr = contrastRatio('#000000', '#FFFFFF');
        expect(cr).toBeCloseTo(21, 6);
    });

    it('同色对比度恒为 1:1', () => {
        fc.assert(
            fc.property(
                fc.hexaString({ minLength: 6, maxLength: 6 }),
                (hexBody) => {
                    const cr = contrastRatio(`#${hexBody}`, `#${hexBody}`);
                    return Math.abs(cr - 1) < 1e-9;
                },
            ),
            { numRuns: 50 },
        );
    });

    it('对比度对前景 / 背景顺序对称', () => {
        fc.assert(
            fc.property(
                fc.hexaString({ minLength: 6, maxLength: 6 }),
                fc.hexaString({ minLength: 6, maxLength: 6 }),
                (a, b) => {
                    const cr1 = contrastRatio(`#${a}`, `#${b}`);
                    const cr2 = contrastRatio(`#${b}`, `#${a}`);
                    return Math.abs(cr1 - cr2) < 1e-9;
                },
            ),
            { numRuns: 50 },
        );
    });
});

/* ──────────────────────────────────────────────────────
   四、主属性 - Property 10: PRESET_BEST 满足 WCAG AA
   ────────────────────────────────────────────────────── */

describe('PRESET_BEST 属性测试 - Property 10: WCAG AA 对比度 (P_CONTRAST)', () => {
    /**
     * 主属性：PRESET_BEST 4 个变体下，正文对比度 ≥ 4.5:1，大号文本 ≥ 3:1。
     *
     * fast-check 在「变体 × 文本类型」笛卡尔积上属性化遍历；阈值与
     * WCAG 2.1 AA 严格对齐：
     *   - body  →  正文（普通文本，包含粗体）        阈值 4.5:1；
     *   - large →  大号文本（≥ 18pt 或 ≥ 14pt 粗体） 阈值 3.0:1。
     *
     * 注意：对比度 ≥ 4.5 隐含 ≥ 3.0，因此 LIGHT/ADMIN（理论 16.13:1）
     * 等高对比度变体在两个阈值上都会通过。但属性测试仍然分别校验两个阈值，
     * 以在 PRESET_BEST 未来调整为更接近边界的颜色时也能立即报警。
     */
    it('LIGHT / DARK × ADMIN / BUSINESS 四个变体下 textColor vs backgroundColor 满足 WCAG AA', () => {
        type TextSize = 'body' | 'large';
        const SIZE_THRESHOLD: Record<TextSize, number> = {
            body: 4.5,
            large: 3.0,
        };

        fc.assert(
            fc.property(
                fc.constantFrom(...PRESET_BEST_VARIANTS),
                fc.constantFrom<TextSize>('body', 'large'),
                (variant, size) => {
                    const cr = contrastRatio(variant.textColor, variant.backgroundColor);
                    const threshold = SIZE_THRESHOLD[size];

                    // 用断言而非 boolean 返回值：失败时 fast-check 会输出
                    // 具体的 variant.name 与 size，便于定位是哪个变体不合规。
                    if (cr < threshold) {
                        throw new Error(
                            `PRESET_BEST ${variant.name} 在 ${size} 文本下对比度 ${cr.toFixed(2)}:1 < ${threshold}:1 ` +
                            `（textColor=${variant.textColor}, backgroundColor=${variant.backgroundColor}）`,
                        );
                    }
                },
            ),
            // 4 变体 × 2 文本尺寸 = 8 组合，64 次足以覆盖全部组合且有重复抽样
            { numRuns: 64 },
        );
    });

});

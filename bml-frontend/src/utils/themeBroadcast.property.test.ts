/**
 * `themeBroadcast` 属性测试 - Property 7: 跨标签同步收敛（P_BROADCAST_CONVERGE）。
 *
 * 验证目标（与 design.md / Correctness Properties / Property 7 对齐）：
 *   - N (2..5) 个 {@link ThemeBroadcast} 实例订阅同一通道时，
 *     任一实例 `publish()` 一条消息后，1 秒以内其它所有实例的订阅回调都
 *     SHALL 收到与发起方一致的消息载荷（除 `senderId` 被发起方注入外，
 *     其余字段逐字段相等）；
 *   - 发起方自身订阅的回调 SHALL NOT 接收到该消息（`senderId` 过滤生效）；
 *   - 上述行为对 `profile-changed` / `preset-applied` / `restored` 三种
 *     {@link ThemeBroadcastMessage} 判别式联合分支均成立。
 *
 * 测试设计：
 *   1. 每次 fast-check 迭代使用独立的随机通道名，避免与应用级单例
 *      （默认通道名 `bml-theme-sync`）以及上一次迭代之间产生消息串台；
 *   2. N 个实例各自构造、各自订阅，发送方使用 `fc.nat({ max: N - 1 })`
 *      随机选取，确保任意位置的实例都能成为合法的发起方；
 *   3. 由于 vitest + jsdom 25 在 Node 18+ 环境下提供原生 `BroadcastChannel`
 *      实现（消息按事件循环异步派发），测试通过 1 秒预算内的轮询等待
 *      `非发送方收到 = N - 1` 完成，模拟 “1 秒内同步收敛” 的需求；
 *   4. teardown 阶段对所有实例调用 `dispose()`，避免遗留通道在后续测试
 *      用例中产生噪声。
 *
 * 关联需求：Requirements 6.2。
 *
 * **Validates: Requirements 6.2**
 *
 * @vitest-environment jsdom
 */

// @vitest-environment jsdom

import { describe, expect, it } from 'vitest';
import fc from 'fast-check';

import type {
    Density,
    FontScale,
    HeaderStyle,
    RadiusStyle,
    SidebarCollapsedStyle,
    SidebarStyle,
    ThemeBroadcastMessage,
    ThemeMode,
    ThemeProfile,
    ThemeScope,
} from '@/types/theme';
import { ThemeBroadcast } from './themeBroadcast';

/* ──────────────────────────────────────────────────────
   一、消息生成器（fast-check Arbitrary）
   ────────────────────────────────────────────────────── */

/** 生成一个 `#RRGGBB` 形式的合法十六进制颜色字符串。 */
const hexColorArb = fc
    .integer({ min: 0, max: 0xffffff })
    .map((n) => `#${n.toString(16).padStart(6, '0').toUpperCase()}`);

/** 生成一份完整且字段合法的 {@link ThemeProfile}，仅作为消息载荷使用。 */
const themeProfileArb: fc.Arbitrary<ThemeProfile> = fc.record({
    primaryColor: hexColorArb,
    secondaryColor: hexColorArb,
    accentColor: hexColorArb,
    successColor: hexColorArb,
    warningColor: hexColorArb,
    errorColor: hexColorArb,
    infoColor: hexColorArb,
    textColor: hexColorArb,
    backgroundColor: hexColorArb,
    borderColor: hexColorArb,
    mode: fc.constantFrom<ThemeMode>('LIGHT', 'DARK', 'AUTO'),
    radius: fc.constantFrom<RadiusStyle>('SHARP', 'SMALL', 'MEDIUM', 'LARGE'),
    density: fc.constantFrom<Density>('COMPACT', 'DEFAULT', 'LOOSE'),
    sidebarStyle: fc.constantFrom<SidebarStyle>('LIGHT', 'DARK', 'TRANSPARENT', 'PRIMARY'),
    sidebarCollapsedStyle: fc.constantFrom<SidebarCollapsedStyle>('LIGHT', 'DARK'),
    headerStyle: fc.constantFrom<HeaderStyle>('LIGHT', 'DARK', 'PRIMARY', 'TRANSPARENT'),
    fontScale: fc.constantFrom<FontScale>('SMALL', 'DEFAULT', 'LARGE', 'XLARGE'),
    presetRef: fc.option(fc.string({ minLength: 1, maxLength: 16 }), { nil: null }),
});

/** 主题作用域生成器。 */
const scopeArb = fc.constantFrom<ThemeScope>('ADMIN', 'BUSINESS');

/**
 * 生成不含 `senderId` 的 {@link ThemeBroadcastMessage}（由 `publish()` 注入），
 * 覆盖 `profile-changed` / `preset-applied` / `restored` 三种判别式分支。
 *
 * 这里返回的对象与 `ThemeBroadcastInput` 在结构上等价；测试中接收方实际
 * 收到的消息会额外包含 `senderId` 字段。
 */
type MessageWithoutSender = Omit<ThemeBroadcastMessage, 'senderId'>;

const messageArb: fc.Arbitrary<MessageWithoutSender> = fc.oneof(
    fc.record({
        kind: fc.constant<'profile-changed'>('profile-changed'),
        scope: scopeArb,
        profile: themeProfileArb,
    }),
    fc.record({
        kind: fc.constant<'preset-applied'>('preset-applied'),
        scope: scopeArb,
        presetId: fc.string({ minLength: 1, maxLength: 24 }),
    }),
    fc.record({
        kind: fc.constant<'restored'>('restored'),
        scope: scopeArb,
    }),
);

/* ──────────────────────────────────────────────────────
   二、辅助函数
   ────────────────────────────────────────────────────── */

/**
 * 异步轮询直到 `predicate` 为 `true` 或超过 `timeoutMs`。
 *
 * 用于在 1 秒预算内等待跨通道消息派发到达，避免对 `BroadcastChannel`
 * 内部调度细节做硬编码假设。
 *
 * @param predicate 判定条件，返回 `true` 即结束等待。
 * @param timeoutMs 超时时间（毫秒），默认 1000ms（与 R6.2 同步窗口对齐）。
 * @param intervalMs 轮询间隔（毫秒），默认 5ms。
 */
async function waitUntil(
    predicate: () => boolean,
    timeoutMs = 1000,
    intervalMs = 5,
): Promise<boolean> {
    const start = Date.now();
    while (!predicate()) {
        if (Date.now() - start >= timeoutMs) {
            return predicate();
        }
        await new Promise<void>((r) => setTimeout(r, intervalMs));
    }
    return true;
}

/** 生成一个独立通道名，避免不同迭代之间产生消息串台。 */
let channelCounter = 0;
function uniqueChannelName(): string {
    channelCounter += 1;
    return `bml-theme-sync-test-${Date.now().toString(36)}-${channelCounter}`;
}

/* ──────────────────────────────────────────────────────
   三、属性测试
   ────────────────────────────────────────────────────── */

describe('themeBroadcast 属性测试 - Property 7: 跨标签同步收敛 (P_BROADCAST_CONVERGE)', () => {
    /**
     * 主属性：N 个实例订阅同一通道，发起方 publish 后 1 秒内其它实例全部收敛。
     *
     * 不变量：
     *   1. 发送方自身的回调始终未被触发（`senderId` 过滤生效）；
     *   2. 所有非发送方在 1 秒内恰好收到一条消息；
     *   3. 收到的消息 `senderId` 等于发送方的 `senderId`，
     *      且其余字段与发送的载荷逐字段相等。
     */
    it('任一实例 publish 后 1 秒内非发送方收敛到同一消息，发送方自身不会收到', async () => {
        await fc.assert(
            fc.asyncProperty(
                fc.integer({ min: 2, max: 5 }),
                messageArb,
                // 在迭代外部决定发送者下标的“形状”：使用 0..N-1 的整数，
                // 在 predicate 内部对 N 取模以保证合法性。
                fc.nat({ max: 10 }),
                async (n, message, senderSeed) => {
                    const channelName = uniqueChannelName();
                    const instances: ThemeBroadcast[] = [];
                    /** 每个实例已收到的消息列表，下标与 instances 对齐。 */
                    const inboxes: ThemeBroadcastMessage[][] = [];

                    try {
                        for (let i = 0; i < n; i += 1) {
                            const bc = new ThemeBroadcast(channelName);
                            const inbox: ThemeBroadcastMessage[] = [];
                            bc.subscribe((m) => inbox.push(m));
                            instances.push(bc);
                            inboxes.push(inbox);
                        }

                        const senderIndex = senderSeed % n;
                        const sender = instances[senderIndex];
                        sender.publish(message);

                        // 等待 “非发送方全部至少收到 1 条消息” 或 1 秒预算耗尽。
                        const converged = await waitUntil(
                            () =>
                                inboxes.every(
                                    (inbox, idx) =>
                                        idx === senderIndex || inbox.length >= 1,
                                ),
                            1000,
                        );
                        expect(converged).toBe(true);

                        // 给可能尚未派发完的事件循环再一拍机会，
                        // 以便检测“发送方意外收到自身消息”这类假阳性。
                        await new Promise<void>((r) => setTimeout(r, 20));

                        // 不变量 1：发送方未触发自身回调。
                        expect(inboxes[senderIndex]).toEqual([]);

                        // 不变量 2 & 3：每个非发送方恰好收到一条消息，
                        // 且消息 = publish 载荷 + 发送方 senderId。
                        const expected: ThemeBroadcastMessage = {
                            ...(message as object),
                            senderId: sender.senderId,
                        } as ThemeBroadcastMessage;
                        for (let i = 0; i < n; i += 1) {
                            if (i === senderIndex) continue;
                            expect(inboxes[i]).toHaveLength(1);
                            expect(inboxes[i][0]).toEqual(expected);
                        }
                    } finally {
                        for (const bc of instances) {
                            bc.dispose();
                        }
                    }
                },
            ),
            { numRuns: 20 },
        );
    });

    /**
     * 辅助属性：连续多次发布时，所有非发送方按发送顺序累计收到全部消息。
     *
     * 该属性进一步保证 `BroadcastChannel` 包装层在 “消息洪水” 场景下也能
     * 维持 “收敛 + 不串台” 的语义（发送方仍然始终为 0 条）。
     */
    it('连续多次 publish 后非发送方累计收敛到全部消息序列', async () => {
        await fc.assert(
            fc.asyncProperty(
                fc.integer({ min: 2, max: 4 }),
                fc.array(messageArb, { minLength: 1, maxLength: 5 }),
                fc.nat({ max: 10 }),
                async (n, messages, senderSeed) => {
                    const channelName = uniqueChannelName();
                    const instances: ThemeBroadcast[] = [];
                    const inboxes: ThemeBroadcastMessage[][] = [];

                    try {
                        for (let i = 0; i < n; i += 1) {
                            const bc = new ThemeBroadcast(channelName);
                            const inbox: ThemeBroadcastMessage[] = [];
                            bc.subscribe((m) => inbox.push(m));
                            instances.push(bc);
                            inboxes.push(inbox);
                        }

                        const senderIndex = senderSeed % n;
                        const sender = instances[senderIndex];
                        for (const m of messages) {
                            sender.publish(m);
                        }

                        const converged = await waitUntil(
                            () =>
                                inboxes.every(
                                    (inbox, idx) =>
                                        idx === senderIndex ||
                                        inbox.length >= messages.length,
                                ),
                            1000,
                        );
                        expect(converged).toBe(true);

                        await new Promise<void>((r) => setTimeout(r, 20));

                        // 发送方仍然收不到自身消息。
                        expect(inboxes[senderIndex]).toEqual([]);

                        // 非发送方按发送顺序累计完整序列。
                        const expected: ThemeBroadcastMessage[] = messages.map(
                            (m) =>
                                ({
                                    ...(m as object),
                                    senderId: sender.senderId,
                                }) as ThemeBroadcastMessage,
                        );
                        for (let i = 0; i < n; i += 1) {
                            if (i === senderIndex) continue;
                            expect(inboxes[i]).toEqual(expected);
                        }
                    } finally {
                        for (const bc of instances) {
                            bc.dispose();
                        }
                    }
                },
            ),
            { numRuns: 10 },
        );
    });
});

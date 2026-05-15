/**
 * 主题跨标签广播工具（`themeBroadcast.ts`）
 * ──────────────────────────────────────────────────────
 * 基于浏览器原生 {@link BroadcastChannel} 包装的主题同步通道，
 * 是 `useTheme` Composable 与 `themeStore` 实现 “多标签 1 秒内同步” 能力的底层依赖。
 *
 * 工作机制：
 *   1. 构造时打开 `BroadcastChannel('bml-theme-sync')`，并为当前标签生成唯一
 *      `senderId`（优先使用 `crypto.randomUUID()`，回退到 `Math.random` 拼接）。
 *   2. {@link ThemeBroadcast.publish} 发送消息前自动注入本实例的 `senderId`，
 *      确保接收端可识别出自身发出的消息并忽略，避免回环触发。
 *   3. {@link ThemeBroadcast.subscribe} 注册的回调仅在收到的消息
 *      `senderId !== this.senderId` 时触发；返回值为反订阅函数。
 *   4. {@link ThemeBroadcast.dispose} 关闭通道、清空所有订阅者，幂等。
 *
 * 兼容性：
 *   - 在不支持 `BroadcastChannel` 的环境（如 SSR、老版本浏览器、部分测试运行时）
 *     下，本类降级为静默 No-op：`publish` 不抛错，`subscribe` 注册的回调
 *     不会被触发；开发模式（`import.meta.env.DEV`）下输出一次 `console.warn`
 *     提示当前环境不支持跨标签同步。
 *
 * 使用示例：
 * ```ts
 * import { themeBroadcast } from '@/utils/themeBroadcast';
 *
 * const off = themeBroadcast.subscribe((msg) => {
 *   if (msg.kind === 'profile-changed') {
 *     // 同步 Pinia store 与 DOM Token
 *   }
 * });
 *
 * themeBroadcast.publish({
 *   kind: 'preset-applied',
 *   scope: 'ADMIN',
 *   presetId: 'PRESET_BEST',
 * });
 *
 * // 组件卸载时取消订阅
 * off();
 * ```
 *
 * 关联需求：Requirements 6.2 / 10.2。
 */

import type { ThemeBroadcastMessage } from '@/types/theme';

/**
 * `BroadcastChannel` 名称常量。
 *
 * 与 `index.html` 同步引导脚本及后端文档保持一致，请勿随意修改；
 * 任何修改都必须同步更新 `design.md` 与 `themeBootstrap.ts`。
 */
export const THEME_BROADCAST_CHANNEL_NAME = 'bml-theme-sync';

/**
 * 主题广播消息处理回调签名。
 *
 * @param message 来自其它标签的 {@link ThemeBroadcastMessage} 消息（已过滤掉自身消息）。
 */
export type ThemeBroadcastHandler = (message: ThemeBroadcastMessage) => void;

/**
 * 发布消息时允许的入参类型：可省略 `senderId`，由 {@link ThemeBroadcast.publish} 自动注入。
 *
 * 业务调用方关心的是 `kind` / `scope` / 业务负载（如 `profile`、`presetId`），
 * 因此这里使用 TS 的分布式条件类型在保留判别式联合的前提下移除 `senderId`。
 */
export type ThemeBroadcastInput = ThemeBroadcastMessage extends infer M
    ? M extends { senderId: string }
    ? Omit<M, 'senderId'> & { senderId?: string }
    : never
    : never;

/**
 * 生成当前标签唯一的 `senderId`。
 *
 * 优先使用现代浏览器内置的 `crypto.randomUUID()`；当运行环境（旧浏览器 / Node 测试）
 * 不可用时回退到基于时间戳与 `Math.random` 的伪 UUID，保证类构造永不抛错。
 *
 * @returns 长度足以避免标签内冲突的 ID 字符串。
 */
function generateSenderId(): string {
    try {
        const c: Crypto | undefined =
            typeof globalThis !== 'undefined' ? (globalThis as { crypto?: Crypto }).crypto : undefined;
        if (c && typeof c.randomUUID === 'function') {
            return c.randomUUID();
        }
    } catch {
        /* 静默回退到下方 Math.random 实现 */
    }
    const rand = () => Math.random().toString(16).slice(2, 10);
    return `bml-${Date.now().toString(16)}-${rand()}-${rand()}`;
}

/**
 * 检测当前运行环境是否提供原生 `BroadcastChannel`。
 *
 * @returns `true` 表示可以正常构造通道；`false` 表示需要降级为 No-op。
 */
function isBroadcastChannelAvailable(): boolean {
    return typeof globalThis !== 'undefined' && typeof (globalThis as { BroadcastChannel?: unknown }).BroadcastChannel === 'function';
}

/**
 * 主题跨标签广播包装类。
 *
 * 一般业务代码无需直接构造，使用模块默认导出的 {@link themeBroadcast}
 * 单例即可；仅在编写单元/属性测试需要并发模拟多个标签时，
 * 才推荐显式 `new ThemeBroadcast()` 创建独立实例。
 */
export class ThemeBroadcast {
    /** 当前标签的发送者 ID，每个实例构造时随机生成且只读。 */
    public readonly senderId: string;

    /** 通道名称（通常为 {@link THEME_BROADCAST_CHANNEL_NAME}）。 */
    public readonly channelName: string;

    /** 实际持有的浏览器通道；不支持的环境为 `null`。 */
    private channel: BroadcastChannel | null = null;

    /** 已注册的处理回调集合。使用 `Set` 保证同一回调不会被重复触发。 */
    private readonly handlers: Set<ThemeBroadcastHandler> = new Set();

    /** 实际绑定到 `channel.onmessage` 的内部分发器，便于 `dispose` 时解绑。 */
    private readonly listener: (event: MessageEvent<ThemeBroadcastMessage>) => void;

    /** 是否已经调用过 {@link dispose}，调用后所有方法降级为 No-op。 */
    private disposed = false;

    /**
     * 构造一个主题广播实例。
     *
     * @param channelName 通道名称，默认 {@link THEME_BROADCAST_CHANNEL_NAME}。
     *                    单元测试中可传入独立名称以避免不同用例之间互相串台。
     */
    constructor(channelName: string = THEME_BROADCAST_CHANNEL_NAME) {
        this.channelName = channelName;
        this.senderId = generateSenderId();

        this.listener = (event: MessageEvent<ThemeBroadcastMessage>) => {
            const data = event?.data;
            if (!data || typeof data !== 'object') {
                return;
            }
            // 忽略自身消息以避免回环触发。
            if (data.senderId === this.senderId) {
                return;
            }
            // 保护性拷贝：迭代时若回调内调用 unsubscribe，不会影响当前一轮分发。
            const snapshot = Array.from(this.handlers);
            for (const handler of snapshot) {
                try {
                    handler(data);
                } catch (err) {
                    // 单个订阅者异常不应阻断其它订阅者，仅在控制台提示。
                    console.error('[BML Theme Broadcast] 订阅回调执行失败:', err);
                }
            }
        };

        if (isBroadcastChannelAvailable()) {
            try {
                this.channel = new BroadcastChannel(channelName);
                this.channel.onmessage = this.listener;
            } catch (err) {
                this.channel = null;
                if (import.meta.env?.DEV) {
                    console.warn('[BML Theme Broadcast] 创建 BroadcastChannel 失败，跨标签同步已禁用:', err);
                }
            }
        } else if (import.meta.env?.DEV) {
            console.warn(
                '[BML Theme Broadcast] 当前环境不支持 BroadcastChannel，跨标签同步已禁用（仅影响多标签场景）。',
            );
        }
    }

    /**
     * 向其它标签发布一条主题广播消息。
     *
     * 调用方可以省略 `senderId` 字段；如调用方显式传入了 `senderId`，
     * 仍会被本实例的 `senderId` 覆盖以保证一致性。
     *
     * @param message 不含 `senderId`（或显式忽略）的主题广播消息。
     */
    public publish(message: ThemeBroadcastInput): void {
        if (this.disposed || !this.channel) {
            return;
        }
        const payload = { ...(message as object), senderId: this.senderId } as ThemeBroadcastMessage;
        try {
            this.channel.postMessage(payload);
        } catch (err) {
            if (import.meta.env?.DEV) {
                console.warn('[BML Theme Broadcast] 发送主题广播消息失败:', err);
            }
        }
    }

    /**
     * 订阅来自其它标签的主题广播消息。
     *
     * 同一个 `handler` 引用重复订阅只会注册一次（`Set` 语义）。
     *
     * @param handler 收到外部消息时执行的回调；自身发出的消息不会触发此回调。
     * @returns 反订阅函数，幂等可多次调用。
     */
    public subscribe(handler: ThemeBroadcastHandler): () => void {
        if (typeof handler !== 'function') {
            throw new TypeError('[BML Theme Broadcast] subscribe 仅接受函数类型的回调。');
        }
        if (this.disposed) {
            return () => {
                /* 已销毁，直接 No-op 反订阅函数 */
            };
        }
        this.handlers.add(handler);
        return () => {
            this.handlers.delete(handler);
        };
    }

    /**
     * 关闭底层通道并清空所有订阅者。
     *
     * 调用后所有方法降级为 No-op，可幂等多次调用；通常只有应用关闭、
     * 单元测试 teardown 或显式重建实例时才需要执行。
     */
    public dispose(): void {
        if (this.disposed) {
            return;
        }
        this.disposed = true;
        this.handlers.clear();
        if (this.channel) {
            try {
                this.channel.onmessage = null;
                this.channel.close();
            } catch (err) {
                if (import.meta.env?.DEV) {
                    console.warn('[BML Theme Broadcast] 关闭 BroadcastChannel 时发生异常:', err);
                }
            } finally {
                this.channel = null;
            }
        }
    }
}

/**
 * 应用级共享的主题广播单例。
 *
 * Composables（如 `useTheme`）与 Pinia `themeStore` 应通过本单例完成
 * 跨标签同步；测试代码若需要并发模拟多个标签，请改用 `new ThemeBroadcast()`。
 */
export const themeBroadcast: ThemeBroadcast = new ThemeBroadcast();

export default themeBroadcast;

import { computed, reactive, ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue';
import type { Ref } from 'vue';
import { Message } from '@arco-design/web-vue';
import type { TableColumnData } from '@arco-design/web-vue';
import { usePermissionStore } from '../store/permission';
import {
    calcTableHeaderMinWidth,
    resolveTableColumnDefaultWidth,
    TABLE_COLUMN_WIDTH_SCHEME_VERSION,
} from '../utils/tableColumnWidth';

/**
 * 业务系统通用列配置项类型（与授权治理一致）。
 *
 * 设计说明：
 * - key:        列唯一标识（用于存储、拖拽、匹配）
 * - title:      列标题
 * - dataIndex:  数据字段名（可选，有自定义渲染时不需要）
 * - slotName:   自定义渲染插槽名（可选）
 * - width:      列宽（px）
 * - visible:    是否默认显示（决定"默认方案"中哪些列可见）
 * - fixed:      固定方向（'left' | 'right'），仅定义初始固定列位置
 * - locked:     是否锁定（锁定列始终可见、不可隐藏、不可排序，如操作列）
 * - ellipsis:   是否超出省略
 * - align:      对齐方式
 * - sortable:   是否可排序
 */
export interface BusinessTableColumn {
    key: string;
    title: string;
    dataIndex?: string;
    slotName?: string;
    width: number;
    visible: boolean;
    fixed?: 'left' | 'right';
    locked?: boolean;
    ellipsis?: boolean;
    align?: 'left' | 'center' | 'right';
    sortable?: boolean;
    /** 自定义表头插槽名（用于搜索图标等，Arco Table 规范） */
    titleSlotName?: string;
    /**
     * 字段权限标识（可选）。
     * 若配置了此字段，当前用户无对应 F 类型权限时，该列将自动隐藏且不出现在列设置面板中。
     * 例如：'system:dept:field:leader' — 控制“负责人”列的可见性。
     */
    permission?: string;
}

/**
 * 列设置面板中的展示项（含排序状态和固定状态）。
 * 与授权治理的 AccountTableColumnSettingItem 对应。
 */
export interface ColumnSettingItem {
    /** 列唯一标识 */
    key: string;
    /** 列标题 */
    title: string;
    /** 是否可见 */
    visible: boolean;
    /** 是否固定在左侧（用户可切换） */
    fixed?: 'left' | 'right';
    /** 是否锁定（锁定列不可隐藏、不可拖拽） */
    locked: boolean;
    /** 上移按钮是否禁用 */
    moveUpDisabled: boolean;
    /** 下移按钮是否禁用 */
    moveDownDisabled: boolean;
}

/**
 * 拖拽状态。
 */
interface DragState {
    draggingKey: string | null;
    overKey: string | null;
    dropPosition: 'before' | 'after' | null;
}

/**
 * 持久化存储的列布局信息（与授权治理一致）。
 * 每列存储：宽度、是否可见、排序权重、是否固定在左侧。
 */
interface ColumnLayout {
    width: number;
    visible: boolean;
    order: number;
    fixedFront: boolean;
}

/**
 * 业务系统通用表格列管理 Composable（与授权治理列管理完全一致）。
 *
 * 功能：
 * 1. 默认显示方案：核心字段默认可见，扩展字段默认隐藏
 * 2. 所有非锁定列可自由切换显示/隐藏
 * 3. 列"固定在左侧"开关（fixedFront），横向滚动时保持可见
 * 4. 列宽拖拽调整，最小 40px（与 Arco Table 一致）
 * 5. 列顺序拖拽调整（上下移动 + 拖拽排序）
 * 6. 持久化到 localStorage，刷新后保持用户配置
 * 7. "恢复默认"一键重置所有配置
 *
 * 使用方式：
 * ```ts
 * const { visibleColumns, columnSettingItems, handleColumnResize, ... } =
 *   useBusinessTableColumns('system-org', defaultColumns);
 * ```
 *
 * @param storageKey - localStorage 存储键名（建议使用页面名称，如 'system-org'）
 * @param defaultColumns - 默认列配置数组（定义所有列的初始状态）
 */
export function useBusinessTableColumns(
    storageKey: string,
    defaultColumns: BusinessTableColumn[]
) {
    // ── 常量 ──
    const STORAGE_KEY = `bml_table_columns_${storageKey}`;
    /** 列宽拖拽最小值，与 Arco Table 内置最小宽度对齐 */
    const COLUMN_RESIZE_MIN_WIDTH = 40;
    /** 计算当前列完整展示表头标题所需的最小宽度 */
    const getColumnHeaderMinWidth = (column: BusinessTableColumn): number => Math.max(
        COLUMN_RESIZE_MIN_WIDTH,
        calcTableHeaderMinWidth({
            title: column.title,
            hasTitleSlot: !!column.titleSlotName,
            sortable: !!column.sortable,
            resizable: true,
        })
    );
    /** 归一化列宽：默认宽度和用户拖拽宽度都不能小于表头完整展示宽度 */
    const resolveBusinessColumnWidth = (column: BusinessTableColumn, width = column.width): number => Math.max(
        resolveTableColumnDefaultWidth({
            title: column.title,
            hasTitleSlot: !!column.titleSlotName,
            sortable: !!column.sortable,
            resizable: true,
            width,
        }),
        getColumnHeaderMinWidth(column)
    );
    /**
     * 列布局版本指纹 —— 基于默认列宽的签名。
     * 当开发者在代码中调整了默认列宽，此指纹会变化，
     * 从而自动使 localStorage 中的旧缓存失效，新宽度立即生效。
     * 避免用户看到过窄的旧缓存宽度，无需手动重置列设置。
     */
    const LAYOUT_VERSION_KEY = `${STORAGE_KEY}_ver`;
    const layoutFingerprint = [
        `scheme:${TABLE_COLUMN_WIDTH_SCHEME_VERSION}`,
        ...defaultColumns.map(c => `${c.key}:${resolveBusinessColumnWidth(c)}:${c.title}:${c.titleSlotName ?? ''}:${c.sortable ? 1 : 0}`),
    ].join(',');

    const permissionStore = usePermissionStore();

    /**
     * 权限过滤后的列配置（排除用户无字段权限的列）。
     * - 权限尚未加载（buttonPermissionsLoaded === false）时返回全部列，避免首屏空表格闪烁。
     * - 权限已加载后，未配置 permission 的列始终保留；配置了 permission 但用户无此权限的列将被移除。
     */
    const permittedColumns = computed(() => {
        /* 权限尚未从后端加载 → 暂时放行所有列（路由守卫会在页面渲染前完成加载） */
        if (!permissionStore.buttonPermissionsLoaded) return defaultColumns;
        return defaultColumns.filter(col =>
            !col.permission || permissionStore.hasPermission(col.permission)
        );
    });

    /** 锁定列集合（始终可见、不可隐藏、不可排序的列 key） */
    const lockedKeys = new Set(
        defaultColumns.filter(c => c.locked).map(c => c.key)
    );

    /** 列基础信息映射（key → column），用于快速查找标题等信息 */
    const columnBaseMap = new Map(
        defaultColumns.map(c => [c.key, c])
    );

    // ── 创建默认布局 ──
    /**
     * 根据 defaultColumns 定义创建默认列布局。
     * 与授权治理的 createDefaultAccountTableColumnLayout 一致：
     * - visible：由 defaultColumns 的 visible 字段决定
     * - order：按 defaultColumns 数组顺序
     * - fixedFront：由 defaultColumns 的 fixed === 'left' 决定
     */
    function createDefaultLayout(): Record<string, ColumnLayout> {
        const layout: Record<string, ColumnLayout> = {};
        defaultColumns.forEach((col, index) => {
            layout[col.key] = {
                width: resolveBusinessColumnWidth(col),
                visible: col.locked ? true : col.visible,
                order: index,
                fixedFront: col.fixed === 'left',
            };
        });
        return layout;
    }

    // ── 响应式列布局 ──
    const columnLayout = reactive<Record<string, ColumnLayout>>(createDefaultLayout());

    // ── 持久化 ──
    function persistLayout() {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(columnLayout));
            localStorage.setItem(LAYOUT_VERSION_KEY, layoutFingerprint);
        } catch { /* 忽略存储失败 */ }
    }

    /** 从 localStorage 恢复列布局，与默认布局合并 */
    function restoreLayout() {
        try {
            /*
             * 版本指纹校验：如果开发者修改了默认列宽，
             * 则旧缓存自动失效，使用最新的默认宽度。
             */
            const storedVersion = localStorage.getItem(LAYOUT_VERSION_KEY);
            if (storedVersion !== layoutFingerprint) {
                localStorage.removeItem(STORAGE_KEY);
                localStorage.setItem(LAYOUT_VERSION_KEY, layoutFingerprint);
                return;
            }
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) return;
            const parsed = JSON.parse(raw) as Record<string, Partial<ColumnLayout>>;
            for (const key of Object.keys(columnLayout)) {
                const stored = parsed[key];
                if (!stored) continue;
                const base = columnBaseMap.get(key);
                if (!base) continue;
                /**
                 * 宽度恢复：锁定列（如操作列）始终使用代码中定义的默认宽度，
                 * 避免旧缓存覆盖开发者对锁定列宽度的调整。
                 * 非锁定列正常从缓存恢复用户自定义宽度。
                 */
                if (typeof stored.width === 'number' && Number.isFinite(stored.width) && !lockedKeys.has(key)) {
                    columnLayout[key].width = resolveBusinessColumnWidth(base, stored.width);
                }
                if (typeof stored.visible === 'boolean' && !lockedKeys.has(key)) {
                    columnLayout[key].visible = stored.visible;
                }
                if (typeof stored.order === 'number' && Number.isFinite(stored.order)) {
                    columnLayout[key].order = Math.round(stored.order);
                }
                if (typeof stored.fixedFront === 'boolean' && !lockedKeys.has(key)) {
                    columnLayout[key].fixedFront = stored.fixedFront;
                }
            }
            normalizeOrder();
        } catch {
            localStorage.removeItem(STORAGE_KEY);
        }
    }

    /** 规范化排序：非锁定列按 order 连续编号，锁定列排在末尾 */
    function normalizeOrder() {
        const unlocked = defaultColumns
            .map(c => c.key)
            .filter(key => !lockedKeys.has(key))
            .sort((a, b) => columnLayout[a].order - columnLayout[b].order);
        unlocked.forEach((key, index) => {
            columnLayout[key].order = index;
        });
        let cursor = unlocked.length;
        defaultColumns.forEach(col => {
            if (!lockedKeys.has(col.key)) return;
            columnLayout[col.key].order = cursor;
            columnLayout[col.key].visible = true;
            cursor += 1;
        });
    }

    // 初始化时恢复持久化配置
    restoreLayout();

    // ── 计算：实际渲染的列（仅显示可见列） ──
    const visibleColumns = computed((): TableColumnData[] => {
        /** 权限允许的列 key 集合（每次权限变化自动重算） */
        const permittedKeySet = new Set(permittedColumns.value.map(c => c.key));
        const allKeys = Object.keys(columnLayout)
            .sort((a, b) => {
                const la = columnLayout[a];
                const lb = columnLayout[b];
                const baseA = columnBaseMap.get(a);
                const baseB = columnBaseMap.get(b);
                // 固定左侧的列排在最前面
                const fixedLeftA = la.fixedFront || (baseA?.locked && baseA?.fixed === 'left') ? 1 : 0;
                const fixedLeftB = lb.fixedFront || (baseB?.locked && baseB?.fixed === 'left') ? 1 : 0;
                if (fixedLeftA !== fixedLeftB) return fixedLeftB - fixedLeftA;
                // 固定右侧的列排在最后面
                const fixedRightA = baseA?.locked && baseA?.fixed === 'right' ? 1 : 0;
                const fixedRightB = baseB?.locked && baseB?.fixed === 'right' ? 1 : 0;
                if (fixedRightA !== fixedRightB) return fixedRightA - fixedRightB;
                // 其余按 order 排序
                return la.order - lb.order;
            });

        return allKeys
            .filter(key => columnLayout[key].visible && permittedKeySet.has(key))
            .map(key => {
                const base = columnBaseMap.get(key)!;
                const layout = columnLayout[key];
                /*
                 * fixed 优先级：
                 * 1. 锁定列使用原始定义的 fixed（如操作列 fixed: 'right'）
                 * 2. 非锁定列通过 fixedFront 控制是否固定在左侧
                 */
                let fixed: 'left' | 'right' | undefined;
                if (base.locked && base.fixed) {
                    fixed = base.fixed;
                } else if (layout.fixedFront) {
                    fixed = 'left';
                }

                return {
                    key: base.key,
                    title: base.title,
                    dataIndex: base.dataIndex,
                    slotName: base.slotName,
                    width: resolveBusinessColumnWidth(base, layout.width),
                    fixed,
                    ellipsis: base.ellipsis,
                    align: base.align,
                    resizable: true,
                    /* 排序与自定义表头插槽（与授权治理一致） */
                    ...(base.sortable ? { sortable: { sortDirections: ['ascend', 'descend'] } } : {}),
                    ...(base.titleSlotName ? { titleSlotName: base.titleSlotName } : {}),
                };
            });
    });

    // ── 计算：列设置面板展示项 ──
    /**
     * 列设置面板排序逻辑（与授权治理一致）：
     * 1. 固定在左侧的锁定列 → 最顶部
     * 2. 可移动的普通列 → 按 order 排序显示在中间
     * 3. 固定在右侧的锁定列（如操作）→ 最底部
     */
    const columnSettingItems = computed((): ColumnSettingItem[] => {
        /** 权限允许的列 key 集合 */
        const permittedKeySet = new Set(permittedColumns.value.map(c => c.key));
        const allKeys = Object.keys(columnLayout)
            .filter(key => permittedKeySet.has(key))
            .sort((a, b) => columnLayout[a].order - columnLayout[b].order);

        const lockedLeftKeys = allKeys.filter(key =>
            lockedKeys.has(key) && columnBaseMap.get(key)?.fixed === 'left'
        );
        const lockedRightKeys = allKeys.filter(key =>
            lockedKeys.has(key) && columnBaseMap.get(key)?.fixed === 'right'
        );
        const movableKeys = allKeys.filter(key => !lockedKeys.has(key));
        const orderedKeys = [...lockedLeftKeys, ...movableKeys, ...lockedRightKeys];

        return orderedKeys.map(key => {
            const base = columnBaseMap.get(key)!;
            const layout = columnLayout[key];
            const locked = lockedKeys.has(key);
            const movableIndex = movableKeys.indexOf(key);
            return {
                key: base.key,
                title: base.title,
                visible: layout.visible,
                fixed: layout.fixedFront ? 'left' as const : base.locked ? base.fixed : undefined,
                locked,
                moveUpDisabled: locked || movableIndex <= 0,
                moveDownDisabled: locked || movableIndex < 0 || movableIndex >= movableKeys.length - 1,
            };
        });
    });

    // ── 拖拽状态 ──
    const dragState = reactive<DragState>({
        draggingKey: null,
        overKey: null,
        dropPosition: null,
    });

    // ── 列宽调整 ──
    function handleColumnResize(dataIndex: string, width: number) {
        /* 通过 key 或 dataIndex 找到列，记录用户拖拽的真实宽度 */
        const col = defaultColumns.find(c => c.key === dataIndex || c.dataIndex === dataIndex);
        if (col && columnLayout[col.key]) {
            columnLayout[col.key].width = resolveBusinessColumnWidth(col, width);
            persistLayout();
            /* 不调用 applyTableElementWidth，让 Arco 自行管理拖拽后的表格宽度 */
        }
    }

    // ── 列显示/隐藏 ──
    function toggleColumnVisible(key: string, visible: boolean) {
        if (lockedKeys.has(key)) return;
        if (!visible && columnLayout[key]?.visible) {
            const activeCount = Object.keys(columnLayout)
                .filter(k => !lockedKeys.has(k) && columnLayout[k].visible).length;
            if (activeCount <= 1) {
                Message.warning('至少保留一列业务字段');
                return;
            }
        }
        if (columnLayout[key]) {
            columnLayout[key].visible = visible;
            persistLayout();
            /* 列显示/隐藏变化时更新 Arco 的 scroll.x */
            nextTick(() => { updateScrollXForArco(); tableResetKey.value += 1; });
        }
    }

    // ── 列上移/下移 ──
    function moveColumn(key: string, direction: -1 | 1) {
        if (lockedKeys.has(key)) return;
        const movableKeys = columnSettingItems.value
            .filter(item => !item.locked)
            .map(item => item.key);
        const currentIndex = movableKeys.indexOf(key);
        const targetIndex = currentIndex + direction;
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= movableKeys.length) return;
        const targetKey = movableKeys[targetIndex];
        if (!targetKey) return;
        /* 交换两列的 order 值 */
        const currentOrder = columnLayout[key].order;
        columnLayout[key].order = columnLayout[targetKey].order;
        columnLayout[targetKey].order = currentOrder;
        normalizeOrder();
        persistLayout();
    }

    // ── 列固定切换（fixedFront） ──
    /**
     * 切换列"固定在左侧"状态（与授权治理一致）：
     * 开启后将该列固定在表格左侧，横向滚动时保持可见；
     * 关闭后恢复普通滚动列。配置持久化到本地，刷新后保持。
     */
    function toggleColumnFixed(key: string) {
        if (lockedKeys.has(key)) return;
        if (columnLayout[key]) {
            columnLayout[key].fixedFront = !columnLayout[key].fixedFront;
            persistLayout();
            nextTick(() => { updateScrollXForArco(); tableResetKey.value += 1; });
        }
    }

    // ── 拖拽排序 ──
    function handleDragStart(key: string, e: DragEvent) {
        if (lockedKeys.has(key)) return;
        dragState.draggingKey = key;
        if (e.dataTransfer) {
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', key);
        }
    }

    function handleDragOver(key: string, e: DragEvent) {
        if (lockedKeys.has(key)) return;
        if (!dragState.draggingKey || dragState.draggingKey === key) return;
        if (lockedKeys.has(dragState.draggingKey)) return;
        e.preventDefault();
        if (e.dataTransfer) {
            e.dataTransfer.dropEffect = 'move';
        }
        const container = e.currentTarget as HTMLElement | null;
        if (!container) return;
        const rect = container.getBoundingClientRect();
        const offsetY = e.clientY - rect.top;
        dragState.overKey = key;
        dragState.dropPosition = offsetY >= rect.height / 2 ? 'after' : 'before';
    }

    function handleDrop(key: string, e: DragEvent) {
        e.preventDefault();
        const draggingKey = dragState.draggingKey;
        const dropPosition = dragState.dropPosition;
        if (!draggingKey || !dropPosition || draggingKey === key) {
            handleDragEnd();
            return;
        }
        if (lockedKeys.has(draggingKey) || lockedKeys.has(key)) {
            handleDragEnd();
            return;
        }
        /* 获取可移动列的当前顺序 */
        const movableKeys = columnSettingItems.value
            .filter(item => !item.locked)
            .map(item => item.key);
        const dragIndex = movableKeys.indexOf(draggingKey);
        const targetIndex = movableKeys.indexOf(key);
        if (dragIndex < 0 || targetIndex < 0) {
            handleDragEnd();
            return;
        }
        /* 从原位置移除 */
        movableKeys.splice(dragIndex, 1);
        /* 计算插入位置 */
        let insertIndex: number;
        if (dragIndex < targetIndex) {
            insertIndex = dropPosition === 'after' ? targetIndex : targetIndex - 1;
        } else {
            insertIndex = dropPosition === 'after' ? targetIndex + 1 : targetIndex;
        }
        insertIndex = Math.max(0, Math.min(insertIndex, movableKeys.length));
        movableKeys.splice(insertIndex, 0, draggingKey);
        /* 更新 order */
        movableKeys.forEach((k, index) => {
            columnLayout[k].order = index;
        });
        normalizeOrder();
        persistLayout();
        handleDragEnd();
    }

    function handleDragEnd() {
        dragState.draggingKey = null;
        dragState.overKey = null;
        dragState.dropPosition = null;
    }

    // ── 表格重置键（用于强制重新挂载） ──
    /**
     * Arco Table 的 column-resizable 会在组件内部缓存用户拖拽过的列宽，
     * 即使 props.columns 中的 width 已经复位，表格仍会使用内部缓存值。
     * 解决方案：通过变更 tableResetKey（绑定到 <a-table :key>）强制 Vue 销毁并重建表格实例，
     * 从而彻底清除 Arco 内部的列宽缓存，确保列宽也被真正恢复。
     */
    const tableResetKey = ref(0);

    // ── 恢复默认 ──
    /** 恢复所有列到默认方案（宽度、可见性、顺序、固定状态） */
    function resetColumns() {
        const defaults = createDefaultLayout();
        for (const key of Object.keys(defaults)) {
            columnLayout[key] = defaults[key];
        }
        normalizeOrder();
        persistLayout();
        /* 自增 key 强制表格重新挂载，彻底清除 Arco 内部列宽缓存 */
        tableResetKey.value += 1;
        Message.success('列宽、顺序与显示列已恢复默认');
    }

    // ── 计算：表格横向滚动宽度 ──
    /**
     * 根据当前可见列宽度之和计算表格横向滚动区域总宽度。
     * <p>
     * <b>自适应填充策略：</b>
     * 使用 CSS {@code max()} 函数取"列总宽度"和"100%容器宽度"中的较大值。
     * - 当列较少时（如用户无字段权限），表格自动撑满容器，不留空白；
     * - 当列较多时（总宽度超过容器），启用横向滚动，保证列对齐。
     * </p>
     * <p>
     * <b>使用方式：</b>
     * <pre>{@code :scroll="{ x: scrollX, y: '100%' }"}</pre>
     * </p>
     */
    const scrollX = computed((): string => {
        const totalWidth = visibleColumns.value.reduce(
            (sum, col) => sum + ((col.width as number) || 100),
            0
        );
        return `${totalWidth}px`;
    });

    /**
     * 传给 Arco Table :scroll="{ x }" 的值。
     * 使用 ref 而非 computed，仅在列显示/隐藏变化时更新，
     * 拖拽列宽时不更新（避免 Arco 重新按比例分配列宽）。
     */
    const scrollXForArco = ref(0);
    const updateScrollXForArco = () => {
        const totalWidth = visibleColumns.value.reduce(
            (sum, col) => sum + ((col.width as number) || 100),
            0
        );
        scrollXForArco.value = totalWidth;
    };
    /* 初始化 */
    updateScrollXForArco();

    /* ──────────────────────────────────────────────────────────
     * 表格列宽 CSS 变量注入（主机制）
     * ──────────────────────────────────────────────────────────
     * 原理：
     *   通过 :style 将 --bml-table-scroll-width 设置在 <a-table> 的根 DOM 元素上。
     *   CSS 自定义属性自动向下继承至所有后代元素，包括 Arco 内部嵌套的
     *   <table class="arco-table-element">。
     *
     *   全局 CSS 规则使用此变量：
     *     .page-wrapper .arco-table-element {
     *       width: var(--bml-table-scroll-width) !important;
     *     }
     *
     *   CSS !important 优先级高于所有非 !important 规则（包括 Arco 的 inline style），
     *   因此可以 100% 可靠地覆盖 Arco 内部设置的 width: 100% 或 inline width。
     *
     * 绑定方式：
     *   <a-table :style="tableStyle" ...>
     * ────────────────────────────────────────────────────────── */
    const tableStyle = computed(() => ({
        '--bml-table-scroll-width': `${scrollXForArco.value}px`,
    }));

    /* ──────────────────────────────────────────────────────────
     * 表格列宽 DOM 强制校正（备用机制 — MutationObserver）
     * ──────────────────────────────────────────────────────────
     * 问题背景：
     *   Arco Table 在 scroll.x 有值时会对 <table class="arco-table-element">
     *   设置 inline style width。但 Vue 的响应式渲染（数据加载、列变化等）
     *   每次都会重新 patch VNode，导致通过 DOM API 设置的 style 被覆盖。
     *
     *   CSS 方案（!important 规则、CSS 变量、v-bind）由于 Arco 自身的
     *   width: 100% + min-width: 100% 规则以及 inline style 优先级，
     *   均无法在所有场景下可靠生效。
     *
     * 解决方案：
     *   使用 MutationObserver 监听 .arco-table-element 的 style 属性变化。
     *   每当 Vue / Arco 更新了 style，Observer 立即用 setProperty + !important
     *   将 width 校正为 scrollX。这样无论 Vue 何时重新渲染，宽度都能被修正。
     *
     * 生命周期：
     *   - onMounted → 首次查找并 observe 所有 .arco-table-element
     *   - watch(tableRef) → <a-table :key> 变化导致组件重建时重新 observe
     *   - watch(scrollX) → 列宽变化时重新应用宽度
     *   - onBeforeUnmount → 断开 observer，防止内存泄漏
     * ────────────────────────────────────────────────────────── */
    const tableRef: Ref<any> = ref(null);

    /** MutationObserver 实例，监听 .arco-table-element 的 style 变化 */
    let observer: MutationObserver | null = null;
    /** 当前被 observe 的 <table> 元素集合 */
    let observedTables: HTMLElement[] = [];
    /** 防止 Observer 回调中的 setProperty 再次触发自身（递归保护） */
    let isApplying = false;

    /**
     * 将 scrollX 应用到所有 .arco-table-element。
     * 仅在首次加载和列配置变化时应用，不在列宽拖拽时干预。
     * 使用 width 确保表格不会被容器压缩，但不阻止 Arco 的列宽拖拽行为。
     */
    const applyTableElementWidth = (): void => {
        const el = tableRef.value?.$el as HTMLElement | undefined;
        if (!el) return;
        const w = scrollX.value;
        isApplying = true;
        el.querySelectorAll<HTMLElement>('.arco-table-element').forEach((table) => {
            table.style.setProperty('width', w, 'important');
        });
        isApplying = false;
    };

    /**
     * 建立 MutationObserver 监听：
     *   找到当前 <a-table> 下所有 .arco-table-element，
     *   对每个元素 observe attributes（仅 style），
     *   当 style 被外部（Vue/Arco）修改时立即校正宽度。
     */
    const setupObserver = (): void => {
        /* 清理旧 observer */
        if (observer) {
            observer.disconnect();
            observer = null;
        }
        observedTables = [];

        const el = tableRef.value?.$el as HTMLElement | undefined;
        if (!el) return;

        /* 首次应用宽度 */
        applyTableElementWidth();

        /* 收集所有 <table> 元素 */
        const tables = el.querySelectorAll<HTMLElement>('.arco-table-element');
        if (tables.length === 0) return;

        /*
         * MutationObserver：当 Arco 内部修改表格 style 时，
         * 强制将 width 校正为我们计算的 scrollX，防止列宽被按比例重新分配。
         */
        observer = new MutationObserver(() => {
            if (isApplying) return;
            applyTableElementWidth();
        });

        tables.forEach((table) => {
            observedTables.push(table);
            observer!.observe(table, {
                attributes: true,
                attributeFilter: ['style'],
            });
        });
    };

    onMounted(() => {
        nextTick(setupObserver);
    });

    /*
     * 触发重新建立 Observer 的场景：
     *   - tableRef 变化 → <a-table :key> 导致组件重建，DOM 元素全部替换
     *   - tableResetKey 变化 → 同上
     *   - scrollX 变化 → 仅需重新应用宽度（不需要重建 observer）
     */
    watch(tableRef, () => nextTick(setupObserver), { flush: 'post' });
    watch(tableResetKey, () => nextTick(setupObserver), { flush: 'post' });
    /* scrollX 变化时不再自动应用（避免与 Arco 列宽拖拽冲突） */

    onBeforeUnmount(() => {
        if (observer) {
            observer.disconnect();
            observer = null;
        }
        observedTables = [];
    });

    return {
        columns: columnLayout,
        visibleColumns,
        columnSettingItems,
        dragState,
        /** 表格重置键：绑定到 <a-table :key> 强制重新挂载，确保列宽恢复 */
        tableResetKey,
        /** 表格横向滚动总宽度（传给 Arco :scroll="{ x: scrollX }"，仅在列增减时变化） */
        scrollX: scrollXForArco,
        /**
         * 表格内联样式 —— 绑定到 &lt;a-table :style="tableStyle"&gt;。
         * 设置 CSS 变量 --bml-table-scroll-width，通过全局 CSS !important 规则
         * 强制覆盖 .arco-table-element 的宽度，防止列被压缩。
         */
        tableStyle,
        /** 表格组件引用 —— 绑定到 &lt;a-table ref="tableRef"&gt;（MutationObserver 备用机制） */
        tableRef,
        handleColumnResize,
        toggleColumnVisible,
        moveColumn,
        toggleColumnFixed,
        handleDragStart,
        handleDragOver,
        handleDrop,
        handleDragEnd,
        resetColumns,
    };
}

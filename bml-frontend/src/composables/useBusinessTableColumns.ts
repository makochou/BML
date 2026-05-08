import { computed, reactive, ref } from 'vue';
import { Message } from '@arco-design/web-vue';
import type { TableColumnData } from '@arco-design/web-vue';
import { usePermissionStore } from '../store/permission';

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
                width: col.width,
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
        } catch { /* 忽略存储失败 */ }
    }

    /** 从 localStorage 恢复列布局，与默认布局合并 */
    function restoreLayout() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) return;
            const parsed = JSON.parse(raw) as Record<string, Partial<ColumnLayout>>;
            for (const key of Object.keys(columnLayout)) {
                const stored = parsed[key];
                if (!stored) continue;
                /**
                 * 宽度恢复：锁定列（如操作列）始终使用代码中定义的默认宽度，
                 * 避免旧缓存覆盖开发者对锁定列宽度的调整。
                 * 非锁定列正常从缓存恢复用户自定义宽度。
                 */
                if (typeof stored.width === 'number' && Number.isFinite(stored.width) && !lockedKeys.has(key)) {
                    columnLayout[key].width = Math.max(COLUMN_RESIZE_MIN_WIDTH, Math.round(stored.width));
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
            .sort((a, b) => columnLayout[a].order - columnLayout[b].order);

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
                    width: layout.width,
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
            columnLayout[col.key].width = Math.max(COLUMN_RESIZE_MIN_WIDTH, Math.round(width));
            persistLayout();
        }
    }

    // ── 列显示/隐藏 ──
    function toggleColumnVisible(key: string, visible: boolean) {
        if (lockedKeys.has(key)) return;
        /* 至少保留一列业务字段（与授权治理一致） */
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
     * <b>设计目的：</b>
     * Arco Design 树形表格使用 {@code scroll: { x: 'max-content' }} 时，
     * 树形展开缩进会导致 body 首列实际宽度大于 header 首列宽度，
     * 进而导致后续所有列的边框与表头不对齐。
     * 使用确定的数值宽度可以让 header 与 body 共用相同的 {@code <colgroup>}，
     * 从根本上解决列对齐问题。
     * </p>
     * <p>
     * <b>使用方式：</b>
     * <pre>{@code :scroll="{ x: scrollX, y: '100%' }"}</pre>
     * <b>注意：</b> scroll.x 必须精确等于 scrollX，不可添加任何额外缓冲。
     * 若 scroll.x ≠ 列宽之和，table-layout: fixed 会将差值分摊到各列，
     * 导致 Arco sticky 列偏移量与实际列位置不匹配，横向滚动时表头数据列错位。
     * </p>
     */
    const scrollX = computed((): number => {
        return visibleColumns.value.reduce(
            (sum, col) => sum + ((col.width as number) || 100),
            0
        );
    });

    return {
        columns: columnLayout,
        visibleColumns,
        columnSettingItems,
        dragState,
        /** 表格重置键：绑定到 <a-table :key> 强制重新挂载，确保列宽恢复 */
        tableResetKey,
        /** 表格横向滚动总宽度（所有可见列宽之和） */
        scrollX,
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

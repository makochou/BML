import { ref, computed, reactive } from 'vue';
import type { TableColumnData } from '@arco-design/web-vue';

/**
 * 业务系统通用列配置项类型。
 *
 * 每一列包含：
 * - key: 列唯一标识（用于存储和拖拽）
 * - title: 列标题
 * - dataIndex: 数据字段名（可选，有自定义渲染时不需要）
 * - slotName: 自定义渲染插槽名（可选）
 * - width: 列宽（px）
 * - visible: 是否显示
 * - fixed: 是否固定（'left' | 'right'）
 * - locked: 是否锁定（锁定列不可隐藏，如操作列）
 * - ellipsis: 是否超出省略
 * - align: 对齐方式
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
}

/**
 * 列设置面板中的展示项（含拖拽状态）。
 */
export interface ColumnSettingItem extends BusinessTableColumn {
    moveUpDisabled: boolean;
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
 * 业务系统通用表格列管理 Composable。
 *
 * 功能：
 * 1. 列显示/隐藏控制
 * 2. 列宽调整（拖拽列边缘）
 * 3. 列顺序调整（拖拽排序）
 * 4. 列固定（左侧固定）
 * 5. 持久化到 localStorage（按页面 key 区分）
 *
 * 使用方式：
 * ```ts
 * const { visibleColumns, columnSettingItems, handleColumnResize, ... } =
 *   useBusinessTableColumns('system-org', defaultColumns);
 * ```
 *
 * @param storageKey - localStorage 存储键名（建议使用页面名称，如 'system-org'）
 * @param defaultColumns - 默认列配置数组
 */
export function useBusinessTableColumns(
    storageKey: string,
    defaultColumns: BusinessTableColumn[]
) {
    // ── 从 localStorage 恢复列配置 ──
    const STORAGE_KEY = `bml_table_columns_${storageKey}`;

    function loadFromStorage(): BusinessTableColumn[] {
        try {
            const saved = localStorage.getItem(STORAGE_KEY);
            if (!saved) return defaultColumns.map(c => ({ ...c }));
            const parsed = JSON.parse(saved) as Partial<BusinessTableColumn>[];
            // 合并：以默认列为基准，用存储的配置覆盖 visible/width/order
            const savedMap = new Map(parsed.map((c, i) => [c.key, { ...c, _order: i }]));
            const merged = defaultColumns.map(col => {
                const saved = savedMap.get(col.key);
                if (!saved) return { ...col };
                return {
                    ...col,
                    visible: col.locked ? true : (saved.visible ?? col.visible),
                    width: saved.width ?? col.width,
                    fixed: saved.fixed ?? col.fixed,
                };
            });
            // 按存储顺序排序（锁定列保持原位）
            const lockedCols = merged.filter(c => c.locked);
            const nonLockedCols = merged.filter(c => !c.locked);
            nonLockedCols.sort((a, b) => {
                const ai = parsed.findIndex(p => p.key === a.key);
                const bi = parsed.findIndex(p => p.key === b.key);
                return (ai === -1 ? 999 : ai) - (bi === -1 ? 999 : bi);
            });
            // 操作列（fixed=right）始终放最后
            const actionCols = lockedCols.filter(c => c.fixed === 'right');
            const otherLockedCols = lockedCols.filter(c => c.fixed !== 'right');
            return [...otherLockedCols, ...nonLockedCols, ...actionCols];
        } catch {
            return defaultColumns.map(c => ({ ...c }));
        }
    }

    function saveToStorage(cols: BusinessTableColumn[]) {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(
                cols.map(c => ({ key: c.key, visible: c.visible, width: c.width, fixed: c.fixed }))
            ));
        } catch { /* 忽略存储失败 */ }
    }

    // ── 响应式列配置 ──
    const columns = ref<BusinessTableColumn[]>(loadFromStorage());

    // ── 计算：实际渲染的列（仅显示可见列） ──
    const visibleColumns = computed((): TableColumnData[] =>
        columns.value
            .filter(c => c.visible)
            .map(c => ({
                key: c.key,
                title: c.title,
                dataIndex: c.dataIndex,
                slotName: c.slotName,
                width: c.width,
                fixed: c.fixed,
                ellipsis: c.ellipsis,
                align: c.align,
                resizable: true,
            }))
    );

    // ── 计算：列设置面板展示项 ──
    const columnSettingItems = computed((): ColumnSettingItem[] => {
        const nonLocked = columns.value.filter(c => !c.locked);
        return columns.value.map((col, idx) => {
            const nonLockedIdx = nonLocked.findIndex(c => c.key === col.key);
            return {
                ...col,
                moveUpDisabled: col.locked || nonLockedIdx <= 0,
                moveDownDisabled: col.locked || nonLockedIdx >= nonLocked.length - 1,
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
        const col = columns.value.find(c => c.key === dataIndex || c.dataIndex === dataIndex);
        if (col) {
            col.width = Math.max(60, width);
            saveToStorage(columns.value);
        }
    }

    // ── 列显示/隐藏 ──
    function toggleColumnVisible(key: string, visible: boolean) {
        const col = columns.value.find(c => c.key === key);
        if (col && !col.locked) {
            col.visible = visible;
            saveToStorage(columns.value);
        }
    }

    // ── 列上移/下移 ──
    function moveColumn(key: string, direction: -1 | 1) {
        const nonLocked = columns.value.filter(c => !c.locked);
        const idx = nonLocked.findIndex(c => c.key === key);
        if (idx === -1) return;
        const targetIdx = idx + direction;
        if (targetIdx < 0 || targetIdx >= nonLocked.length) return;
        // 在原数组中找到对应位置并交换
        const allIdx = columns.value.findIndex(c => c.key === key);
        const allTargetIdx = columns.value.findIndex(c => c.key === nonLocked[targetIdx].key);
        const temp = columns.value[allIdx];
        columns.value[allIdx] = columns.value[allTargetIdx];
        columns.value[allTargetIdx] = temp;
        saveToStorage(columns.value);
    }

    // ── 列固定切换 ──
    function toggleColumnFixed(key: string) {
        const col = columns.value.find(c => c.key === key);
        if (col && !col.locked) {
            col.fixed = col.fixed === 'left' ? undefined : 'left';
            saveToStorage(columns.value);
        }
    }

    // ── 拖拽排序 ──
    function handleDragStart(key: string, e: DragEvent) {
        dragState.draggingKey = key;
        if (e.dataTransfer) {
            e.dataTransfer.effectAllowed = 'move';
        }
    }

    function handleDragOver(key: string, e: DragEvent) {
        e.preventDefault();
        if (!dragState.draggingKey || dragState.draggingKey === key) return;
        const target = e.currentTarget as HTMLElement;
        const rect = target.getBoundingClientRect();
        const midY = rect.top + rect.height / 2;
        dragState.overKey = key;
        dragState.dropPosition = e.clientY < midY ? 'before' : 'after';
    }

    function handleDrop(key: string, e: DragEvent) {
        e.preventDefault();
        if (!dragState.draggingKey || dragState.draggingKey === key) {
            handleDragEnd();
            return;
        }
        const fromKey = dragState.draggingKey;
        const toKey = key;
        const fromIdx = columns.value.findIndex(c => c.key === fromKey);
        const toIdx = columns.value.findIndex(c => c.key === toKey);
        if (fromIdx === -1 || toIdx === -1) { handleDragEnd(); return; }
        const fromCol = columns.value[fromIdx];
        const toCol = columns.value[toIdx];
        if (fromCol.locked || toCol.locked) { handleDragEnd(); return; }
        // 执行移动
        const newCols = [...columns.value];
        newCols.splice(fromIdx, 1);
        const newToIdx = newCols.findIndex(c => c.key === toKey);
        const insertIdx = dragState.dropPosition === 'before' ? newToIdx : newToIdx + 1;
        newCols.splice(insertIdx, 0, fromCol);
        columns.value = newCols;
        saveToStorage(columns.value);
        handleDragEnd();
    }

    function handleDragEnd() {
        dragState.draggingKey = null;
        dragState.overKey = null;
        dragState.dropPosition = null;
    }

    // ── 恢复默认 ──
    function resetColumns() {
        columns.value = defaultColumns.map(c => ({ ...c }));
        localStorage.removeItem(STORAGE_KEY);
    }

    return {
        columns,
        visibleColumns,
        columnSettingItems,
        dragState,
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

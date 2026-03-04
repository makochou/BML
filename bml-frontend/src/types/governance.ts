import type { Component } from 'vue';

export type WorkbenchStatTone = 'blue' | 'green' | 'teal' | 'gold' | 'violet';

/**
 * 工作台顶部与摘要区的指标卡模型。
 * 不同页面只需要替换数据源，即可复用统一的信息卡视觉规范。
 */
export type WorkbenchStatCard = {
  label: string;
  value: string | number;
  hint: string;
  tone: WorkbenchStatTone;
};

/**
 * 账号画像、事实信息卡模型。
 * 用于详情页、授权抽屉、回调日志等侧边信息区的统一渲染。
 */
export type FactCard = {
  label: string;
  value: string | number;
  hint: string;
  copyable?: boolean;
};

export type GovernanceWorkbenchTheme = 'emerald' | 'ocean' | 'teal' | 'violet';

/**
 * 页面顶部概览 Deck 的摘要卡模型。
 * 统一承载图标、主数值和说明文案，适用于首页、治理页或业务总览页的紧凑看板。
 */
export type GovernanceOverviewCard = {
  title: string;
  value: string | number;
  hint: string;
  accent: WorkbenchStatTone;
  icon: Component;
};

/**
 * 紧凑筛选面板头部的轻量元信息模型。
 * 用于在查询区以小徽标形式展示“当前结果 / 已启用筛选 / 当前模式”等摘要信息。
 */
export type GovernanceCompactMetaItem = {
  label: string;
  value: string | number;
  tone?: WorkbenchStatTone;
};

export type GovernanceFieldKind =
  | 'input'
  | 'select'
  | 'input-number'
  | 'date-picker'
  | 'textarea';

export type GovernanceFieldPriority = 'primary' | 'secondary';

/**
 * 通用治理表单字段模型。
 * 页面通过字段配置描述“字段是什么”和“应该如何渲染”，
 * 渲染器则统一负责表单项壳层、组件映射和交互绑定。
 */
export type GovernanceFormFieldSchema = {
  key: string;
  field: string;
  label: string;
  kind: GovernanceFieldKind;
  priority?: GovernanceFieldPriority;
  required?: boolean;
  helper?: string;
  /**
   * 字段横向跨列数，取值需与 section.columns 对齐。
   * 统一放开到 1~6，便于在高密度布局（如 4 列/6 列）下复用同一套 schema 类型。
   */
  colSpan?: 1 | 2 | 3 | 4 | 5 | 6;
  componentProps?: Record<string, unknown>;
};

/**
 * 通用治理表单分区模型。
 * 用于把复杂表单拆成多个配置化的业务分区，后续新增字段时优先修改 schema。
 */
export type GovernanceFormSectionSchema = {
  key: string;
  title: string;
  description: string;
  hideHeader?: boolean;
  layout?: 'grid' | 'single';
  columns?: 1 | 2 | 3 | 4 | 5 | 6;
  fields: GovernanceFormFieldSchema[];
};

import type { GovernanceFormSectionSchema } from '../types/governance';

type SelectOption = {
  label: string;
  value: string | number;
};

/**
 * API 账号回调日志筛选区 schema。
 * 统一维护回调日志筛选字段与组件参数，便于后续继续扩展业务类型、
 * 投递时间范围、响应码等筛选维度时优先改配置而不是回模板。
 */
export function useApiAccountCallbackLogFilterSchema(options: {
  callbackStatusOptions: SelectOption[];
}) {
  const sections: GovernanceFormSectionSchema[] = [
    {
      key: 'callback-query',
      title: '回调日志筛选',
      description: '按回调状态等条件定位失败、重试中或待处理的日志记录。',
      hideHeader: true,
      layout: 'single',
      fields: [
        {
          key: 'callbackStatus',
          field: 'callbackStatus',
          label: '回调状态',
          kind: 'select',
          componentProps: {
            allowClear: true,
            placeholder: '筛选回调状态',
            options: options.callbackStatusOptions
          }
        }
      ]
    }
  ];

  return { sections };
}

import type { GovernanceFormSectionSchema } from '../types/governance';

type SelectOption = {
  label: string;
  value: string | number;
};

/**
 * API 账号查询区字段 schema。
 * 统一维护筛选条件的字段结构与组件参数，后续扩展查询维度时优先修改此处。
 */
export function useApiAccountQuerySchema(options: {
  accountTypeOptions: SelectOption[];
  clientTypeOptions: SelectOption[];
  environmentOptions: SelectOption[];
  statusOptions: SelectOption[];
}) {
  const sections: GovernanceFormSectionSchema[] = [
    {
      key: 'query',
      title: '账号检索与治理筛选',
      description: '通过账号名称、业务系统、客户端、环境与状态组合筛选，快速定位目标账号。',
      hideHeader: true,
      layout: 'grid',
      columns: 6,
      fields: [
        {
          key: 'accountName',
          field: 'accountName',
          label: '账号名称',
          kind: 'input',
          priority: 'primary',
          componentProps: {
            allowClear: true,
            placeholder: '输入账号名称'
          }
        },
        {
          key: 'systemKeyword',
          field: 'systemKeyword',
          label: '业务系统',
          kind: 'input',
          priority: 'primary',
          componentProps: {
            allowClear: true,
            placeholder: '输入系统名称或编码'
          }
        },
        {
          key: 'status',
          field: 'status',
          label: '状态',
          kind: 'select',
          priority: 'primary',
          componentProps: {
            allowClear: true,
            placeholder: '全部状态',
            options: options.statusOptions
          }
        },
        {
          key: 'accountType',
          field: 'accountType',
          label: '账号类型',
          kind: 'select',
          priority: 'primary',
          componentProps: {
            allowClear: true,
            placeholder: '全部类型',
            options: options.accountTypeOptions
          }
        },
        {
          key: 'clientType',
          field: 'clientType',
          label: '调用客户端',
          kind: 'select',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '全部客户端',
            options: options.clientTypeOptions
          }
        },
        {
          key: 'accessEnvironment',
          field: 'accessEnvironment',
          label: '接入环境',
          kind: 'select',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '全部环境',
            options: options.environmentOptions
          }
        }
      ]
    }
  ];

  return { sections };
}

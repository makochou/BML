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
  signVersionOptions: SelectOption[];
}) {
  const dateTimePickerProps = {
    showTime: true,
    format: 'YYYY-MM-DD HH:mm:ss',
    valueFormat: 'YYYY-MM-DD HH:mm:ss',
    allowClear: true
  };

  const sections: GovernanceFormSectionSchema[] = [
    {
      key: 'query',
      title: '账号检索与治理筛选',
      description: '首屏保留 3 个高频条件，其余字段在“更多条件”中展开；字符匹配模式通过右下角按钮切换。',
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
        // 以下字段按“账号治理页实际查询频率”从高到低排序，便于展开后快速命中常用组合。
        // 第一层：运营筛选高频（类型 / 环境 / 客户端）
        {
          key: 'accountType',
          field: 'accountType',
          label: '账号类型',
          kind: 'select',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '全部类型',
            options: options.accountTypeOptions
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
        // 第二层：对象定位高频（ID / AccessKey / 系统编码）
        {
          key: 'accountId',
          field: 'accountId',
          label: '账号ID',
          kind: 'input-number',
          priority: 'secondary',
          componentProps: {
            min: 1,
            precision: 0,
            hideButton: true,
            placeholder: '输入账号ID'
          }
        },
        {
          key: 'accessKey',
          field: 'accessKey',
          label: 'AccessKey',
          kind: 'input',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '输入 AccessKey'
          }
        },
        {
          key: 'systemCode',
          field: 'systemCode',
          label: '系统编码',
          kind: 'input',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '输入系统编码'
          }
        },
        {
          key: 'systemName',
          field: 'systemName',
          label: '系统名称',
          kind: 'input',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '输入系统名称'
          }
        },
        // 第三层：治理责任与回调排障（负责人 / 回调 / 签名 / 白名单）
        {
          key: 'ownerName',
          field: 'ownerName',
          label: '负责人',
          kind: 'input',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '输入负责人姓名'
          }
        },
        {
          key: 'callbackUrl',
          field: 'callbackUrl',
          label: '回调地址',
          kind: 'input',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '输入回调地址'
          }
        },
        {
          key: 'signVersion',
          field: 'signVersion',
          label: '签名版本',
          kind: 'select',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '全部签名版本',
            options: options.signVersionOptions
          }
        },
        {
          key: 'ipKeyword',
          field: 'ipKeyword',
          label: '白名单IP',
          kind: 'input',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '输入 IP 或 CIDR'
          }
        },
        {
          key: 'ownerContact',
          field: 'ownerContact',
          label: '负责人联系方式',
          kind: 'input',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '输入手机号/邮箱/企业微信'
          }
        },
        // 第四层：时间与容量治理（更新时间 > 创建时间 > 到期时间 > 限流 > 备注）
        {
          key: 'updateTimeStart',
          field: 'updateTimeStart',
          label: '更新开始时间',
          kind: 'date-picker',
          priority: 'secondary',
          componentProps: {
            ...dateTimePickerProps,
            placeholder: '选择更新开始时间'
          }
        },
        {
          key: 'updateTimeEnd',
          field: 'updateTimeEnd',
          label: '更新结束时间',
          kind: 'date-picker',
          priority: 'secondary',
          componentProps: {
            ...dateTimePickerProps,
            placeholder: '选择更新结束时间'
          }
        },
        {
          key: 'createTimeStart',
          field: 'createTimeStart',
          label: '创建开始时间',
          kind: 'date-picker',
          priority: 'secondary',
          componentProps: {
            ...dateTimePickerProps,
            placeholder: '选择创建开始时间'
          }
        },
        {
          key: 'createTimeEnd',
          field: 'createTimeEnd',
          label: '创建结束时间',
          kind: 'date-picker',
          priority: 'secondary',
          componentProps: {
            ...dateTimePickerProps,
            placeholder: '选择创建结束时间'
          }
        },
        {
          key: 'rateLimitMin',
          field: 'rateLimitMin',
          label: '限流下限',
          kind: 'input-number',
          priority: 'secondary',
          componentProps: {
            min: 0,
            precision: 0,
            hideButton: true,
            placeholder: '输入最小限流值'
          }
        },
        {
          key: 'rateLimitMax',
          field: 'rateLimitMax',
          label: '限流上限',
          kind: 'input-number',
          priority: 'secondary',
          componentProps: {
            min: 0,
            precision: 0,
            hideButton: true,
            placeholder: '输入最大限流值'
          }
        },
        {
          key: 'expireTimeStart',
          field: 'expireTimeStart',
          label: '到期开始时间',
          kind: 'date-picker',
          priority: 'secondary',
          componentProps: {
            ...dateTimePickerProps,
            placeholder: '选择到期开始时间'
          }
        },
        {
          key: 'expireTimeEnd',
          field: 'expireTimeEnd',
          label: '到期结束时间',
          kind: 'date-picker',
          priority: 'secondary',
          componentProps: {
            ...dateTimePickerProps,
            placeholder: '选择到期结束时间'
          }
        },
        {
          key: 'remark',
          field: 'remark',
          label: '备注',
          kind: 'input',
          priority: 'secondary',
          componentProps: {
            allowClear: true,
            placeholder: '输入备注关键字'
          }
        }
      ]
    }
  ];

  return { sections };
}

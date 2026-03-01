import type { GovernanceFormSectionSchema } from '../types/governance';

type SelectOption = {
  label: string;
  value: string | number;
};

/**
 * API账号新建/编辑表单字段 schema。
 * 统一把字段结构、组件类型、占位文案和组件参数集中在配置中维护，
 * 后续新增治理字段时优先改 schema，而不是直接修改页面模板。
 */
export function useApiAccountFormSchema(options: {
  accountTypeOptions: SelectOption[];
  clientTypeOptions: SelectOption[];
  environmentOptions: SelectOption[];
  signVersionOptions: SelectOption[];
  statusOptions: SelectOption[];
}) {
  const sections: GovernanceFormSectionSchema[] = [
    {
      key: 'basic',
      title: '基础归属信息',
      description: '先完成账号主体、所属系统和联系人信息录入，便于后续授权排查和凭证联调。',
      layout: 'grid',
      fields: [
        {
          key: 'accountName',
          field: 'accountName',
          label: '账号名称',
          kind: 'input',
          required: true,
          componentProps: { maxlength: 100, placeholder: '例如：企业门户-华东区' }
        },
        {
          key: 'accountType',
          field: 'accountType',
          label: '账号类型',
          kind: 'select',
          required: true,
          componentProps: { options: options.accountTypeOptions }
        },
        {
          key: 'systemName',
          field: 'systemName',
          label: '业务系统名称',
          kind: 'input',
          required: true,
          componentProps: { maxlength: 100, placeholder: '例如：企业管理平台' }
        },
        {
          key: 'systemCode',
          field: 'systemCode',
          label: '业务系统编码',
          kind: 'input',
          required: true,
          componentProps: { maxlength: 64, placeholder: '例如：ENTERPRISE_PORTAL' }
        },
        {
          key: 'ownerName',
          field: 'ownerName',
          label: '接入方负责人',
          kind: 'input',
          required: true,
          componentProps: { maxlength: 50, placeholder: '例如：张三' }
        },
        {
          key: 'ownerContact',
          field: 'ownerContact',
          label: '联系方式',
          kind: 'input',
          required: true,
          componentProps: { maxlength: 100, placeholder: '填写手机号、邮箱或企业微信' }
        }
      ]
    },
    {
      key: 'security',
      title: '接入策略与安全配置',
      description: '统一维护客户端范围、接入环境、签名版本与状态策略，保证账号配置口径一致。',
      layout: 'grid',
      fields: [
        {
          key: 'clientTypes',
          field: 'clientTypes',
          label: '调用客户端',
          kind: 'select',
          required: true,
          componentProps: {
            options: options.clientTypeOptions,
            multiple: true,
            allowClear: true,
            maxTagCount: 'responsive',
            placeholder: '请选择客户端类型'
          }
        },
        {
          key: 'accessEnvironment',
          field: 'accessEnvironment',
          label: '接入环境',
          kind: 'select',
          required: true,
          componentProps: { options: options.environmentOptions }
        },
        {
          key: 'signVersion',
          field: 'signVersion',
          label: '签名算法版本',
          kind: 'select',
          required: true,
          componentProps: { options: options.signVersionOptions }
        },
        {
          key: 'rateLimit',
          field: 'rateLimit',
          label: '每分钟限流阈值',
          kind: 'input-number',
          required: true,
          componentProps: { min: 1, step: 100, mode: 'button' }
        },
        {
          key: 'status',
          field: 'status',
          label: '状态',
          kind: 'select',
          required: true,
          componentProps: { options: options.statusOptions }
        },
        {
          key: 'expireTime',
          field: 'expireTime',
          label: '过期时间',
          kind: 'date-picker',
          componentProps: {
            style: 'width: 100%;',
            showTime: true,
            valueFormat: 'YYYY-MM-DD HH:mm:ss',
            format: 'YYYY-MM-DD HH:mm:ss',
            placeholder: '为空表示不过期',
            allowClear: true
          }
        }
      ]
    },
    {
      key: 'callback',
      title: '回调与补充说明',
      description: '补充异步回调地址与运营备注，便于后续通知联调、问题追踪和风险记录。',
      layout: 'single',
      fields: [
        {
          key: 'callbackUrl',
          field: 'callbackUrl',
          label: '业务回调地址',
          kind: 'input',
          componentProps: {
            maxlength: 255,
            allowClear: true,
            placeholder: '例如：https://enterprise.example.com/open-api/callback'
          },
          helper: '用于异步通知和业务结果回推，仅支持 http / https，未配置可留空。'
        },
        {
          key: 'remark',
          field: 'remark',
          label: '备注',
          kind: 'textarea',
          componentProps: {
            maxlength: 500,
            autoSize: { minRows: 4, maxRows: 7 },
            placeholder: '填写客户来源、风险说明、联调备注等运营信息'
          }
        }
      ]
    }
  ];

  return { sections };
}

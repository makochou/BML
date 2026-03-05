import type {
  ApiAccountFormModel,
  EnvironmentIpWhitelist,
  EnvironmentIpWhitelistTextMap,
  SaveApiAccountPayload
} from '../types/apiAccount';

const SYSTEM_CODE_PATTERN = /^[A-Za-z0-9_-]{2,64}$/;

export type ApiAccountFormValidationResult =
  | {
    valid: true;
    payload: SaveApiAccountPayload;
    environmentIpWhitelist: EnvironmentIpWhitelist;
  }
  | {
    valid: false;
    message: string;
  };

/**
 * API 账号表单校验与提交负载构建。
 * 页面只负责展示和触发保存，字段必填、格式校验、标准化与白名单负载组装
 * 都统一收口到这里，便于多个页面共享同一套前端规则。
 */
export function useApiAccountFormValidation() {
  function parseIpWhitelistInput(value?: string | null) {
    return Array.from(
      new Set(
        (value || '')
          .split(/[\n,，;；]+/)
          .map(item => item.trim())
          .filter(Boolean)
      )
    );
  }

  function buildEnvironmentIpWhitelistPayload(
    textMap: EnvironmentIpWhitelistTextMap
  ): EnvironmentIpWhitelist {
    return {
      test: parseIpWhitelistInput(textMap.test),
      staging: parseIpWhitelistInput(textMap.staging),
      production: parseIpWhitelistInput(textMap.production)
    };
  }

  function isValidCallbackUrl(value: string) {
    try {
      const url = new URL(value);
      return url.protocol === 'http:' || url.protocol === 'https:';
    } catch {
      return false;
    }
  }

  function validateAndBuildPayload(
    form: ApiAccountFormModel
  ): ApiAccountFormValidationResult {
    const accountName = form.accountName.trim();
    if (!accountName) return { valid: false, message: '请输入账号名称' };

    const systemName = form.systemName.trim();
    if (!systemName) return { valid: false, message: '请输入业务系统名称' };

    const systemCode = form.systemCode.trim();
    if (!systemCode) return { valid: false, message: '请输入业务系统编码' };
    if (!SYSTEM_CODE_PATTERN.test(systemCode)) {
      return {
        valid: false,
        message: '业务系统编码仅支持 2-64 位字母、数字、下划线和中划线'
      };
    }

    const ownerName = form.ownerName.trim();
    if (!ownerName) return { valid: false, message: '请输入接入方负责人' };

    const ownerContact = form.ownerContact.trim();
    if (!ownerContact) return { valid: false, message: '请输入联系方式' };

    const clientTypes = Array.from(
      new Set((form.clientTypes || []).map(item => item.trim()).filter(Boolean))
    );
    if (!clientTypes.length) {
      return { valid: false, message: '请至少选择一个调用客户端' };
    }

    if (!form.accessEnvironment) {
      return { valid: false, message: '请选择接入环境' };
    }

    if (!form.signVersion) {
      return { valid: false, message: '请选择签名算法版本' };
    }

    const rateLimit = Number(form.rateLimit);
    if (!Number.isFinite(rateLimit) || rateLimit < 1) {
      return { valid: false, message: '请输入合法的每分钟限流阈值' };
    }

    const callbackUrl = form.callbackUrl.trim();
    if (callbackUrl && !isValidCallbackUrl(callbackUrl)) {
      return { valid: false, message: '请输入合法的 http / https 回调地址' };
    }

    const environmentIpWhitelist = buildEnvironmentIpWhitelistPayload(
      form.environmentIpWhitelistText
    );

    return {
      valid: true,
      environmentIpWhitelist,
      payload: {
        accountName,
        description: form.description.trim() || null,
        ownerName,
        ownerContact,
        systemName,
        systemCode: systemCode.toUpperCase(),
        accountType: form.accountType,
        clientTypes,
        accessEnvironment: form.accessEnvironment,
        ipWhitelist: environmentIpWhitelist[form.accessEnvironment],
        environmentIpWhitelist,
        signVersion: form.signVersion,
        allowedScopes: (form.allowedScopes || []).filter(Boolean),
        callbackUrl: callbackUrl || null,
        rateLimit,
        expireTime: form.expireTime || null,
        status: form.status,
        remark: form.remark.trim() || null
      }
    };
  }

  return {
    parseIpWhitelistInput,
    buildEnvironmentIpWhitelistPayload,
    isValidCallbackUrl,
    validateAndBuildPayload
  };
}

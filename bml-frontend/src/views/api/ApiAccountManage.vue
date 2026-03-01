<template>
  <div class="page">
    <GovernanceOverviewDeck
      eyebrow="Project API Authorization Center"
      badge="全项目接口授权工作台"
      title="API账号管理"
      description="统一维护 API 账号、接入治理字段、按环境白名单、接口授权和回调日志重试，让开放能力治理更集中、更清晰。"
      :tags="heroCapabilityTags"
      :hero-stats="overviewHeroStats"
      :summary-cards="overviewSummaryCards"
      theme="ocean"
      max-width="1260px"
      align="center"
      compact
    >
      <template #actions>
        <a-button type="primary" :loading="syncingRegistry" @click="handleSyncRegistry">
          <template #icon><icon-sync /></template>
          同步接口目录
        </a-button>
        <a-button @click="openCreateModal">
          <template #icon><icon-plus /></template>
          新建账号
        </a-button>
      </template>
    </GovernanceOverviewDeck>

    <GovernanceCompactQueryPanel
      class="query-panel"
      max-width="1260px"
    >
      <template v-if="querySecondarySections.length" #note>
        <a-button class="query-panel__toggle-btn" @click="toggleQueryAdvanced">
          <template #icon>
            <component :is="queryAdvancedExpanded ? IconUp : IconDown" />
          </template>
          {{ queryAdvancedToggleText }}
        </a-button>
      </template>

      <template #footerActions>
        <a-button type="primary" @click="handleSearch">查询账号</a-button>
        <a-button @click="handleResetSearch">重置条件</a-button>
      </template>

      <a-form :model="queryForm" layout="vertical" class="query-form">
        <GovernanceFormSections
          :model="queryFormAsRecord"
          :sections="queryPrimarySections"
          variant="embedded"
        />

        <transition name="query-advanced-fold">
          <div v-if="queryAdvancedExpanded && querySecondarySections.length" class="query-panel__advanced">
            <div class="query-panel__advanced-shell">
              <div class="query-panel__advanced-heading">
                <span>扩展条件</span>
                <small>按需展开的低频筛选项</small>
              </div>

              <GovernanceFormSections
                :model="queryFormAsRecord"
                :sections="querySecondarySections"
                variant="embedded"
              />
            </div>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage
      class="table-shell"
      max-width="1260px"
    >
      <a-table class="account-table" row-key="id" :data="accountList" :loading="tableLoading" :pagination="paginationConfig" :scroll="{ x: 1320 }" @row-dblclick="handleAccountRowDblClick" @page-change="handlePageChange" @page-size-change="handlePageSizeChange">
        <template #columns>
          <a-table-column v-for="column in accountTableColumns" :key="column.key" :title="column.title" :data-index="column.dataIndex" :width="column.width" :fixed="column.fixed" :ellipsis="column.ellipsis" :tooltip="column.tooltip">
            <template #cell="{ record }">
              <div v-if="column.kind === 'account'" class="table-account-main">
                <strong>{{ record.accountName }}</strong>
                <div class="table-account-main__meta">
                  <a-tag :color="getAccountTypeTagColor(record.accountType)">{{ getAccountTypeLabel(record.accountType) }}</a-tag>
                  <small>#{{ record.id }}</small>
                </div>
              </div>
              <div v-else-if="column.kind === 'system'" class="table-compact-cell">
                <strong>{{ record.systemName || '未维护' }}</strong>
                <small>{{ record.systemCode || '未维护系统编码' }}</small>
                <small>{{ record.ownerName || '未维护负责人' }}</small>
              </div>
              <div v-else-if="column.kind === 'access'" class="table-compact-cell table-compact-cell--inline">
                <div class="table-tag-row">
                  <a-tag :color="getEnvironmentTagColor(record.accessEnvironment)">{{ getAccessEnvironmentLabel(record.accessEnvironment) }}</a-tag>
                  <a-tag color="arcoblue">{{ getCompactClientCountLabel(record.clientTypes) }}</a-tag>
                </div>
                <small>{{ getCompactClientSummary(record.clientTypes) }}</small>
                <small>{{ getCallbackConfigLabel(record.callbackUrl) }} · {{ record.rateLimit || 0 }} / min</small>
              </div>
              <div v-else-if="column.kind === 'status'" class="table-status-cell">
                <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ getStatusLabel(record.status) }}</a-tag>
                <small>授权 {{ record.authorizedApiCount || 0 }} 项</small>
              </div>
              <div v-else-if="column.kind === 'updateTime'" class="table-compact-cell">
                <strong>{{ record.updateTime || '-' }}</strong>
                <small>{{ getExpireTimeLabel(record.expireTime) }}</small>
              </div>
              <div v-else-if="column.kind === 'actions'" class="table-row-actions" @click.stop @dblclick.stop>
                <a-button size="small" type="primary" class="table-action-button table-action-button--primary" @click="handleOpenAccountPreview(record)">详情</a-button>
                <a-button size="small" class="table-action-button" @click="handleEditAccount(record)">编辑</a-button>
                <a-button size="small" class="table-action-button" @click="handleAuthorizeAccount(record)">授权</a-button>
                <a-dropdown trigger="click" position="br">
                  <a-button size="small" class="table-action-button">更多</a-button>
                  <template #content>
                    <a-doption @click="handleCallbackLogAccount(record)">回调日志</a-doption>
                    <a-doption @click="confirmResetSecret(record)">重置密钥</a-doption>
                    <a-doption class="table-row-actions__danger" @click="confirmDeleteAccount(record)">删除账号</a-doption>
                  </template>
                </a-dropdown>
              </div>
              <span v-else>{{ getPlainText(record, column.dataIndex, '-') }}</span>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </GovernanceListStage>

    <ApiAccountPreviewModal
      :visible="accountPreview.visible"
      :loading="accountPreview.loading"
      :account="accountPreview.snapshot"
      :stats="accountPreviewStats"
      :basic-facts="accountPreviewBasicFacts"
      :access-facts="accountPreviewAccessFacts"
      :governance-facts="accountPreviewGovernanceFacts"
      @update:visible="handleAccountPreviewVisibleChange"
      @edit="handlePreviewEdit"
      @authorization="handlePreviewAuthorization"
      @callback="handlePreviewCallback"
      @reset-secret="handlePreviewResetSecret"
      @delete="handlePreviewDelete"
    />

    <a-modal v-model:visible="accountModal.visible" width="1240px" :footer="false" unmount-on-close>
      <template #title>
        <div class="account-modal-title">
          <div class="account-modal-title__main">
            <p>{{ accountModalTitleCaption }}</p>
            <strong>{{ accountModalTitle }}</strong>
          </div>
          <span class="account-modal-title__badge">{{ accountModalTitleBadge }}</span>
        </div>
      </template>

      <div class="account-modal-shell">
        <a-form :model="accountForm" layout="vertical">
          <GovernanceWorkbenchShell class="account-workbench" :eyebrow="accountModalTitleCaption" :title="accountWorkbenchTitle" :description="accountWorkbenchDescription" :tags="accountHeroTags" :stats="accountWorkbenchStats" theme="emerald">
            <template #titleBadge>{{ accountModalTitleBadge }}</template>

            <template #aside>
              <GovernancePanel class="account-panel" title="账号标识">
                <div class="account-panel__identity">
                  <span>系统账号ID</span>
                  <strong>{{ accountModal.mode === 'create' ? '保存后自动生成' : `#${accountModal.editingId}` }}</strong>
                  <small>用于工单、日志、授权排查和联调留档。</small>
                </div>
                <div class="account-pill-row">
                  <span class="account-pill">{{ accountModeLabel }}</span>
                  <span class="account-pill">{{ getAccountTypeLabel(accountForm.accountType) }}</span>
                  <span class="account-pill">{{ getAccessEnvironmentLabel(accountForm.accessEnvironment) }}</span>
                </div>
              </GovernancePanel>

              <GovernancePanel class="account-panel" title="当前概览">
                <GovernanceStatGrid class="account-overview-list" card-class="account-stat-card" :items="accountWorkbenchStats" />
              </GovernancePanel>

              <GovernancePanel class="account-panel account-panel--guide" title="填写建议">
                <ul class="account-guide-list">
                  <li v-for="item in accountGuideItems" :key="item">{{ item }}</li>
                </ul>
              </GovernancePanel>
            </template>

            <div class="account-main">
              <GovernanceFormSections :model="accountFormAsRecord" :sections="accountFormSections" />

              <ApiEnvironmentWhitelistEditor
                v-model="accountWhitelistModel"
                :access-environment="accountForm.accessEnvironment"
                :environment-options="environmentOptions"
              />

            </div>

            <template #footer>
              <div class="account-modal-footer">
                <div class="account-modal-footer__tip">
                  <strong>保存说明</strong>
                  <span>系统会统一校验业务系统编码、回调地址与环境白名单，并在保存时自动完成标准化、去重和当前生效清单回填。</span>
                </div>
                <div class="account-modal-footer__actions">
                  <a-button @click="accountModal.visible = false">取消</a-button>
                  <a-button type="primary" :loading="accountModal.submitting" @click="submitAccountForm">{{ accountModalSubmitText }}</a-button>
                </div>
              </div>
            </template>
          </GovernanceWorkbenchShell>
        </a-form>
      </div>
    </a-modal>
    <ApiCredentialDeliveryModal
      v-model:visible="credentialModal.visible"
      :payload="credentialModal.payload"
    />

    <ApiAuthorizationWorkbenchDrawer
      v-model:visible="authorizationDrawer.visible"
      :loading="authorizationDrawer.loading"
      :saving="authorizationDrawer.saving"
      :snapshot="authorizationDrawer.snapshot"
      :filters="authorizationFilters"
      :hero-tags="authorizationHeroTags"
      :hero-stats="authorizationHeroStats"
      :account-facts="authorizationAccountFacts"
      :guide-items="authorizationGuideItems"
      :module-cards="moduleCards"
      :summary-cards="authorizationSelectionCards"
      :visible-api-count="authorizationVisibleApiCount"
      :visible-controller-count="authorizationVisibleControllerCount"
      :module-options="moduleOptions"
      :tree-data="authorizationTreeData"
      :checked-keys="authorizationDrawer.checkedKeys"
      @update:checked-keys="value => authorizationDrawer.checkedKeys = value"
      @save="submitAuthorization"
      @select-visible="selectVisibleApis"
      @clear-visible="clearVisibleApis"
      @select-module="selectModuleApis"
      @clear-module="clearModuleApis"
    />

    <ApiCallbackLogWorkbenchDrawer
      v-model:visible="callbackLogDrawer.visible"
      :account="callbackLogDrawer.account"
      :loading="callbackLogDrawer.loading"
      :testing="callbackLogDrawer.testing"
      :retrying-id="callbackLogDrawer.retryingId"
      :logs="callbackLogDrawer.logs"
      :filters="callbackLogFilters"
      :pagination-config="callbackLogPaginationConfig"
      :hero-tags="callbackHeroTags"
      :hero-stats="callbackHeroStats"
      :account-facts="callbackAccountFacts"
      :guide-items="callbackGuideItems"
      :summary-cards="callbackTableStats"
      @search="handleCallbackLogSearch"
      @reset="handleCallbackLogReset"
      @page-change="handleCallbackLogPageChange"
      @page-size-change="handleCallbackLogPageSizeChange"
      @trigger-test="handleTriggerTestCallback()"
      @retry="handleRetryCallback"
    />
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Message, Modal } from '@arco-design/web-vue';
import { IconDown, IconLock, IconNotification, IconPlus, IconSync, IconUp, IconUserGroup } from '@arco-design/web-vue/es/icon';
import { createApiAccount, deleteApiAccount, fetchApiAccountDetail, fetchApiAccountList, fetchApiAccountPage, fetchApiCallbackLogs, fetchAuthorizationSnapshot, fetchOpenApiRegistryTree, resetApiAccountSecret, retryApiCallbackLog, saveAuthorization, syncOpenApiRegistry, triggerApiAccountTestCallback, updateApiAccount } from '../../api/apiAccount';
import ApiAuthorizationWorkbenchDrawer from '../../components/api-account/ApiAuthorizationWorkbenchDrawer.vue';
import ApiCallbackLogWorkbenchDrawer from '../../components/api-account/ApiCallbackLogWorkbenchDrawer.vue';
import ApiCredentialDeliveryModal from '../../components/api-account/ApiCredentialDeliveryModal.vue';
import ApiAccountPreviewModal from '../../components/api-account/ApiAccountPreviewModal.vue';
import ApiEnvironmentWhitelistEditor from '../../components/api-account/ApiEnvironmentWhitelistEditor.vue';
import GovernanceCompactQueryPanel from '../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceFormSections from '../../components/governance/GovernanceFormSections.vue';
import GovernanceListStage from '../../components/governance/GovernanceListStage.vue';
import GovernanceOverviewDeck from '../../components/governance/GovernanceOverviewDeck.vue';
import GovernancePanel from '../../components/governance/GovernancePanel.vue';
import GovernanceStatGrid from '../../components/governance/GovernanceStatGrid.vue';
import GovernanceWorkbenchShell from '../../components/governance/GovernanceWorkbenchShell.vue';
import { useApiAccountFormValidation } from '../../composables/useApiAccountFormValidation';
import { useApiAccountFormSchema } from '../../composables/useApiAccountFormSchema';
import { useApiAccountQuerySchema } from '../../composables/useApiAccountQuerySchema';
import { splitGovernanceSectionsByPriority } from '../../composables/useGovernanceSectionPriority';
import { defineTableColumns, useTableColumns } from '../../composables/useTableColumns';
import type {
  AccessEnvironment,
  ApiAccountDetail,
  ApiAccountFormModel,
  ApiAccountItem,
  ApiAuthorizationSnapshot,
  ApiCallbackLogFilterModel,
  ApiCallbackLogItem,
  ApiCredentialPayload,
  EnvironmentIpWhitelist,
  EnvironmentIpWhitelistTextMap,
  OpenApiGroupNode
} from '../../types/apiAccount';
import type { FactCard, GovernanceOverviewCard, WorkbenchStatCard } from '../../types/governance';

type AccountModalMode = 'create' | 'edit';
type ManageRouteAction = 'edit' | 'authorization' | 'callback' | 'reset-secret';
type AuthorizationTreeNode = { key: string; title: string; nodeType: 'module' | 'controller' | 'api'; description?: string; httpMethod?: string; apiUrl?: string; disableCheckbox?: boolean; children?: AuthorizationTreeNode[] };
type AuthorizationModuleCard = { moduleName: string; apiCount: number; controllerCount: number; selectedCount: number };
type AccountColumnKind = 'account' | 'system' | 'access' | 'status' | 'updateTime' | 'actions';

const accountTypeOptions = [{ label: '内部账号', value: 1 }, { label: '外部账号', value: 2 }];
const clientTypeOptions = [{ label: 'Web前端', value: 'web' }, { label: 'H5页面', value: 'h5' }, { label: 'APP', value: 'app' }, { label: '小程序', value: 'mini_program' }, { label: '服务端', value: 'server' }, { label: '第三方系统', value: 'third_party' }, { label: '其他客户端', value: 'other' }];
const environmentOptions: { label: string; value: AccessEnvironment }[] = [{ label: '测试环境', value: 'test' }, { label: '预发环境', value: 'staging' }, { label: '生产环境', value: 'production' }];
const signVersionOptions = [{ label: 'v1（当前正式版）', value: 'v1' }];
const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }];
const route = useRoute();
const router = useRouter();

const queryForm = reactive({ accountName: '', status: undefined as number | undefined, accountType: undefined as number | undefined, clientType: undefined as string | undefined, systemKeyword: '', accessEnvironment: undefined as AccessEnvironment | undefined });
const queryAdvancedExpanded = ref(false);
const accountList = ref<ApiAccountItem[]>([]);
const tableLoading = ref(false);
const syncingRegistry = ref(false);
const pagination = reactive({ current: 1, pageSize: 10, total: 0 });
const dashboardStats = reactive({
  totalAccounts: 0,
  enabledAccounts: 0,
  authorizedApiTotal: 0,
  latestRegistryCount: 0,
  productionAccounts: 0,
  callbackConfiguredAccounts: 0,
  externalAccounts: 0
});
const accountModal = reactive({ visible: false, mode: 'create' as AccountModalMode, editingId: 0, submitting: false });
// 列表只保留主信息，低频治理字段统一进入详情弹窗查看。
const accountPreview = reactive({ visible: false, loading: false, accountId: 0, snapshot: null as ApiAccountDetail | null });
const accountForm = reactive<ApiAccountFormModel>({ accountName: '', ownerName: '', ownerContact: '', systemName: '', systemCode: '', accountType: 2, clientTypes: [], accessEnvironment: 'production', signVersion: 'v1', environmentIpWhitelistText: createEmptyEnvironmentWhitelistText(), callbackUrl: '', rateLimit: 1000, expireTime: null, status: 1, remark: '' });
const credentialModal = reactive({ visible: false, payload: null as ApiCredentialPayload | null });
const authorizationDrawer = reactive({ visible: false, loading: false, saving: false, accountId: 0, snapshot: null as ApiAuthorizationSnapshot | null, checkedKeys: [] as string[] });
const authorizationFilters = reactive({ keyword: '', moduleName: '', method: '' });
const callbackLogDrawer = reactive({ visible: false, loading: false, testing: false, retryingId: null as number | null, account: null as ApiAccountItem | null, logs: [] as ApiCallbackLogItem[], summary: createSummary(), pagination: { current: 1, pageSize: 10, total: 0 } });
const callbackLogFilters = reactive<ApiCallbackLogFilterModel>({ callbackStatus: undefined });

const paginationConfig = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, showTotal: true, showPageSize: true, pageSizeOptions: [10, 20, 50, 100] }));
const callbackLogPaginationConfig = computed(() => ({ current: callbackLogDrawer.pagination.current, pageSize: callbackLogDrawer.pagination.pageSize, total: callbackLogDrawer.pagination.total, showTotal: true, showPageSize: true, pageSizeOptions: [10, 20, 50] }));
const queryFormAsRecord = queryForm as unknown as Record<string, unknown>;
const accountFormAsRecord = accountForm as unknown as Record<string, unknown>;
const accountWhitelistModel = computed<EnvironmentIpWhitelistTextMap>({
  get: () => accountForm.environmentIpWhitelistText,
  set: value => {
    accountForm.environmentIpWhitelistText = value;
  }
});
const { sections: queryFormSections } = useApiAccountQuerySchema({
  accountTypeOptions,
  clientTypeOptions,
  environmentOptions,
  statusOptions
});
// 查询条件按主次优先级拆分，首屏只展示高频项，低频项折叠后按需展开。
const {
  primarySections: queryPrimarySections,
  secondarySections: querySecondarySections,
} = splitGovernanceSectionsByPriority(queryFormSections);
const { sections: accountFormSections } = useApiAccountFormSchema({
  accountTypeOptions,
  clientTypeOptions,
  environmentOptions,
  signVersionOptions,
  statusOptions
});
const { parseIpWhitelistInput, validateAndBuildPayload } = useApiAccountFormValidation();
// 新建/编辑弹窗的概览与指引均使用配置化数据，便于后续治理页复用同一套信息架构。
const accountHeroTags = ['账号主体统一纳管', '环境白名单自动生效', '签名与限流统一治理', '保存后配置即时生效'];
const accountGuideItems = ['业务系统编码仅支持 2-64 位字母、数字、下划线和中划线，保存时会自动转为大写。', '建议在联系方式中填写手机号、邮箱或企业微信，方便授权工单与异常联络。', '环境白名单建议按测试、预发、生产分开维护，避免不同环境来源 IP 相互污染。', '创建成功或重置密钥后，SecretKey 只会展示一次，请在首次返回时立即保存。'];
const accountModalTitle = computed(() => accountModal.mode === 'create' ? '新建API账号' : '编辑API账号');
const accountModalTitleCaption = computed(() => accountModal.mode === 'create' ? 'Open API Account Provisioning Workspace' : 'Open API Account Governance Workspace');
const accountModalTitleBadge = computed(() => accountModal.mode === 'create' ? '创建后将返回首份凭证' : '保存后配置即时生效');
const accountWorkbenchTitle = computed(() => accountModal.mode === 'create' ? '账号开通治理台' : '账号编辑治理台');
const accountWorkbenchDescription = computed(() => accountModal.mode === 'create' ? '统一录入账号主体、客户端范围、环境策略与回调配置，创建完成后系统立即交付首份访问凭证。' : '统一调整账号归属、安全策略、环境白名单与回调配置，保存后当前账号的调用能力会即时更新。');
const accountModeLabel = computed(() => accountModal.mode === 'create' ? '创建模式' : '编辑模式');
const accountModalSubmitText = computed(() => accountModal.mode === 'create' ? '创建账号' : '保存变更');
const accountWorkbenchStats = computed<WorkbenchStatCard[]>(() => [{ label: '账号类型', value: getAccountTypeLabel(accountForm.accountType), hint: '区分内外部接入主体', tone: 'blue' }, { label: '客户端范围', value: accountForm.clientTypes.length ? `${accountForm.clientTypes.length} 类` : '未选择', hint: accountForm.clientTypes.length ? getClientTypeLabels(accountForm.clientTypes).join('、') : '至少选择一个客户端类型', tone: 'green' }, { label: '当前环境', value: getAccessEnvironmentLabel(accountForm.accessEnvironment), hint: `${getEnvironmentWhitelistCountLabel(accountForm.environmentIpWhitelistText[accountForm.accessEnvironment])} 生效条目`, tone: 'teal' }, { label: '回调配置', value: accountForm.callbackUrl.trim() ? '已配置' : '未配置', hint: accountForm.callbackUrl.trim() || '支持 http / https，可按需留空', tone: 'gold' }]);
// 顶部概览区统一改为紧凑式 Deck，指标与摘要卡分层展示，避免信息重复且方便后续复用到其他治理页面。
const heroCapabilityTags = ['统一纳管', '白名单隔离', '接口授权', '回调闭环'];
const overviewHeroStats = computed<WorkbenchStatCard[]>(() => [{ label: '纳管账号', value: dashboardStats.totalAccounts, hint: '当前已接入统一治理的 API 账号', tone: 'blue' }, { label: '累计授权', value: dashboardStats.authorizedApiTotal, hint: '所有账号当前已分配的接口授权条目', tone: 'gold' }, { label: '可授权接口', value: dashboardStats.latestRegistryCount, hint: '来自最新项目接口目录快照', tone: 'teal' }]);
const overviewSummaryCards = computed<GovernanceOverviewCard[]>(() => [{ title: '启用账号', value: dashboardStats.enabledAccounts, hint: '当前仍在提供调用能力的账号', accent: 'green', icon: IconLock }, { title: '生产接入', value: dashboardStats.productionAccounts, hint: '默认指向生产环境的账号主体', accent: 'blue', icon: IconSync }, { title: '已配回调', value: dashboardStats.callbackConfiguredAccounts, hint: '已维护业务回调地址的账号', accent: 'teal', icon: IconNotification }, { title: '外部账号', value: dashboardStats.externalAccounts, hint: '面向外部接入主体的账号数量', accent: 'violet', icon: IconUserGroup }]);
const queryAdvancedToggleText = computed(() => queryAdvancedExpanded.value ? '收起更多查询条件' : '展开更多查询条件');
// 详情弹窗中的内容仍然走配置化卡片模型，便于其他治理页复用相同的信息组织方式。
const accountPreviewStats = computed<WorkbenchStatCard[]>(() => {
  const account = accountPreview.snapshot;
  if (!account) return [];
  const whitelistCount = countEnvironmentWhitelistEntries(account.environmentIpWhitelist);
  return [
    { label: '累计授权', value: account.authorizedApiCount || 0, hint: '当前账号累计拥有的授权条目', tone: 'blue' },
    { label: '启用授权', value: account.enabledAuthorizedApiCount || 0, hint: '当前仍然生效的授权接口数', tone: 'green' },
    { label: '客户端范围', value: account.clientTypes?.length ? `${account.clientTypes.length} 类` : '未维护', hint: account.clientTypes?.length ? getClientTypeLabels(account.clientTypes).join('、') : '尚未声明客户端范围', tone: 'teal' },
    { label: '白名单条目', value: whitelistCount || '未限制', hint: `${getAccessEnvironmentLabel(account.accessEnvironment)}环境默认生效`, tone: 'gold' }
  ];
});
const accountPreviewBasicFacts = computed<FactCard[]>(() => {
  const account = accountPreview.snapshot;
  if (!account) return [];
  return [
    { label: '账号名称', value: account.accountName, hint: '列表主展示名称' },
    { label: '系统账号ID', value: `#${account.id}`, hint: '用于日志检索与工单定位', copyable: true },
    { label: 'AccessKey', value: account.accessKey, hint: '对接调用时使用的公开凭证标识', copyable: true },
    { label: '账号类型', value: getAccountTypeLabel(account.accountType), hint: '用于区分内部接入与外部接入主体' },
    { label: '业务系统', value: account.systemName || '未维护', hint: account.systemCode || '未维护系统编码' },
    { label: '负责人', value: account.ownerName || '未维护', hint: account.ownerContact || '未维护联系方式' }
  ];
});
const accountPreviewAccessFacts = computed<FactCard[]>(() => {
  const account = accountPreview.snapshot;
  if (!account) return [];
  return [
    { label: '接入环境', value: getAccessEnvironmentLabel(account.accessEnvironment), hint: '决定默认命中的环境白名单' },
    { label: '客户端范围', value: getClientTypeLabels(account.clientTypes).join('、') || '未维护', hint: '当前账号声明可调用的终端类型' },
    { label: '签名版本', value: account.signVersion || '未维护', hint: '请求验签遵循的版本号' },
    { label: '限流阈值', value: `${account.rateLimit || 0} / min`, hint: '单账号分钟级流量上限' },
    { label: '回调地址', value: account.callbackUrl || '未配置', hint: account.callbackUrl ? '业务方回调地址已配置' : '当前账号未配置回调地址', copyable: Boolean(account.callbackUrl) },
    { label: '到期时间', value: account.expireTime || '永久有效', hint: '为空表示不设置自动失效时间' }
  ];
});
const accountPreviewGovernanceFacts = computed<FactCard[]>(() => {
  const account = accountPreview.snapshot;
  if (!account) return [];
  return [
    { label: '测试白名单', value: getEnvironmentWhitelistArrayCountLabel(account.environmentIpWhitelist?.test), hint: '测试环境来源 IP 管理' },
    { label: '预发白名单', value: getEnvironmentWhitelistArrayCountLabel(account.environmentIpWhitelist?.staging), hint: '预发环境来源 IP 管理' },
    { label: '生产白名单', value: getEnvironmentWhitelistArrayCountLabel(account.environmentIpWhitelist?.production), hint: '生产环境来源 IP 管理' },
    { label: '账号状态', value: getStatusLabel(account.status), hint: `最近更新：${account.updateTime || '-'}` },
    { label: '治理备注', value: account.remark?.trim() || '未填写', hint: account.createTime ? `创建时间：${account.createTime}` : '暂无创建时间记录' }
  ];
});
// 列配置改为模型驱动，后续新增或调整列时优先修改此配置，而不是直接改模板结构。
// 列表回归“主信息单行 + 右侧操作”的工作台模式，其余低频字段通过双击弹窗查看。
const accountTableColumnModel = defineTableColumns<AccountColumnKind>([
  { key: 'account', title: '账号信息', kind: 'account', width: 260 },
  { key: 'system', title: '业务系统 / 负责人', kind: 'system', width: 230 },
  { key: 'access', title: '接入概览', kind: 'access', width: 230 },
  { key: 'status', title: '状态 / 授权', kind: 'status', width: 150 },
  { key: 'updateTime', title: '最近更新', kind: 'updateTime', width: 180 },
  { key: 'actions', title: '操作', kind: 'actions', width: 240, fixed: 'right' }
]);
const { columns: accountTableColumns, getPlainText } = useTableColumns(accountTableColumnModel);
const moduleOptions = computed(() => authorizationDrawer.snapshot?.groups.map(item => ({ label: item.moduleName, value: item.moduleName })) || []);
const filteredAuthorizationGroups = computed(() => { const groups = authorizationDrawer.snapshot?.groups || []; const keyword = authorizationFilters.keyword.trim().toLowerCase(); return groups.filter(group => !authorizationFilters.moduleName || group.moduleName === authorizationFilters.moduleName).map(group => ({ ...group, controllers: group.controllers.map(controller => ({ ...controller, apis: controller.apis.filter(api => { const keywordMatched = !keyword || api.apiName.toLowerCase().includes(keyword) || api.apiUrl.toLowerCase().includes(keyword) || (api.description || '').toLowerCase().includes(keyword) || controller.controllerName.toLowerCase().includes(keyword) || group.moduleName.toLowerCase().includes(keyword); const methodMatched = !authorizationFilters.method || api.httpMethod === authorizationFilters.method; return keywordMatched && methodMatched; }) })).filter(controller => controller.apis.length > 0) })).filter(group => group.controllers.length > 0); });
const authorizationTreeData = computed<AuthorizationTreeNode[]>(() => filteredAuthorizationGroups.value.map(group => ({ key: `module:${group.moduleName}`, title: group.moduleName, nodeType: 'module', description: `${countApisInGroup(group)} 个可授权接口`, disableCheckbox: true, children: group.controllers.map(controller => ({ key: `controller:${group.moduleName}:${controller.controllerName}`, title: controller.controllerName, nodeType: 'controller', description: `${controller.apis.length} 个接口`, disableCheckbox: true, children: controller.apis.map(api => ({ key: buildApiKey(api.id), title: api.apiName, nodeType: 'api', httpMethod: api.httpMethod, apiUrl: api.apiUrl })) })) })));
const selectedAuthorizationCount = computed(() => parseCheckedApiIds(authorizationDrawer.checkedKeys).length);
const moduleCards = computed<AuthorizationModuleCard[]>(() => { const selectedIds = new Set(parseCheckedApiIds(authorizationDrawer.checkedKeys)); return (authorizationDrawer.snapshot?.groups || []).map(group => { const apiIds = collectGroupApiIds(group); return { moduleName: group.moduleName, apiCount: apiIds.length, controllerCount: group.controllers.length, selectedCount: apiIds.filter(id => selectedIds.has(id)).length }; }); });
// 接口授权抽屉统一改成治理工作台布局，概览卡、账号画像和建议信息全部由此处集中维护。
const authorizationHeroTags = ['模块维度批量授权', '按方法与关键字筛选', '保存后即时生效', '全项目接口统一纳管'];
const authorizationVisibleApiCount = computed(() => collectApiIdsFromFilteredGroups(filteredAuthorizationGroups.value).length);
const authorizationVisibleControllerCount = computed(() => filteredAuthorizationGroups.value.reduce((sum, group) => sum + group.controllers.length, 0));
const authorizationHeroStats = computed<WorkbenchStatCard[]>(() => {
  const snapshot = authorizationDrawer.snapshot;
  if (!snapshot) return [];
  return [
    { label: '当前已选', value: selectedAuthorizationCount.value, hint: '本次将写入账号的接口授权数', tone: 'blue' },
    { label: '启用接口', value: snapshot.summary.enabledApiCount, hint: '当前目录中状态为启用的接口总数', tone: 'green' },
    { label: '当前视图', value: authorizationVisibleApiCount.value, hint: `${authorizationVisibleControllerCount.value} 个控制器命中当前筛选`, tone: 'teal' },
    { label: '模块数量', value: filteredAuthorizationGroups.value.length, hint: '当前筛选后仍可见的模块数', tone: 'gold' }
  ];
});
const authorizationAccountFacts = computed<FactCard[]>(() => {
  const account = authorizationDrawer.snapshot?.account;
  if (!account) return [];
  return [
    { label: '账号名称', value: account.accountName, hint: '当前执行授权配置的账号主体' },
    { label: '系统账号ID', value: `#${account.id}`, hint: '用于日志检索和授权追踪', copyable: true },
    { label: '业务系统', value: account.systemName || '未维护', hint: account.systemCode || '未维护系统编码' },
    { label: '接入环境', value: getAccessEnvironmentLabel(account.accessEnvironment), hint: '决定默认命中的环境白名单' },
    { label: '调用客户端', value: account.clientTypes?.length ? getClientTypeLabels(account.clientTypes).join('、') : '未维护', hint: '当前账号已声明的终端范围' },
    { label: '已授权启用接口', value: account.enabledAuthorizedApiCount || 0, hint: '当前账号已生效的授权接口数' }
  ];
});
const authorizationSelectionCards = computed<WorkbenchStatCard[]>(() => {
  const snapshot = authorizationDrawer.snapshot;
  if (!snapshot) return [];
  return [
    { label: '已选接口', value: selectedAuthorizationCount.value, hint: '当前勾选结果', tone: 'blue' },
    { label: '已选启用接口', value: parseCheckedApiIds(authorizationDrawer.checkedKeys).filter(id => isEnabledApiId(id, snapshot.groups)).length, hint: '勾选结果中处于启用状态的接口数', tone: 'green' },
    { label: '目录总接口', value: snapshot.summary.totalApiCount, hint: '该账号可参与授权配置的接口库存', tone: 'teal' },
    { label: '已保存授权', value: snapshot.summary.selectedApiCount, hint: '上一次保存后系统记录的授权接口数', tone: 'gold' }
  ];
});
const authorizationGuideItems = ['建议先按模块筛选，再用关键字缩小到具体控制器或接口，避免一次性勾选过多条目。', '保存授权后即刻生效，若账号正在联调，请先确认目标接口已启用且调用白名单已配置。', '模块卡支持整模块全选与清空，适合业务系统按域快速开通接口能力。'];
// 回调日志抽屉延续治理工作台设计语言，头部标签、指标、事实卡和表格列都集中配置，方便不同业务场景复用。
const callbackHeroTags = ['投递状态统一查看', '失败日志可即时重试', '回调测试与日志联动', '异常排查信息一页收口'];
const callbackHeroStats = computed<WorkbenchStatCard[]>(() => [{ label: '总记录数', value: callbackLogDrawer.summary.totalCount, hint: '当前账号累计回调投递记录', tone: 'blue' }, { label: '成功记录', value: callbackLogDrawer.summary.successCount, hint: '已成功完成业务回调的记录数', tone: 'green' }, { label: '重试中', value: callbackLogDrawer.summary.retryingCount, hint: '系统仍在自动重试中的记录数', tone: 'teal' }, { label: '失败记录', value: callbackLogDrawer.summary.failedCount, hint: '需要人工关注或再次投递的失败记录', tone: 'gold' }]);
const callbackGuideItems = ['联调时建议先发送一条测试回调，确认业务方地址可达、签名校验通过且返回 2xx。', '若日志持续失败，先检查回调地址、业务系统响应码、网络白名单与业务侧接收逻辑。', '“立即重试”仅对失败或重试中的记录开放，便于在修复后快速补投。'];
const callbackAccountFacts = computed<FactCard[]>(() => {
  const account = callbackLogDrawer.account;
  if (!account) return [];
  return [
    { label: '账号名称', value: account.accountName, hint: '当前查看回调日志的账号主体' },
    { label: '系统账号ID', value: `#${account.id}`, hint: '用于日志检索和工单排查', copyable: true },
    { label: '业务系统', value: account.systemName || '未维护', hint: account.systemCode || '未维护系统编码' },
    { label: '接入环境', value: getAccessEnvironmentLabel(account.accessEnvironment), hint: '用于识别当前联调或生产接入场景' },
    { label: '负责人', value: account.ownerName || '未维护', hint: account.ownerContact || '未维护联系方式' },
    { label: '回调地址', value: account.callbackUrl || '未配置', hint: '当前账号配置的业务回调地址', copyable: !!account.callbackUrl }
  ];
});
const callbackTableStats = computed<WorkbenchStatCard[]>(() => [{ label: '当前页日志', value: callbackLogDrawer.logs.length, hint: '当前分页已加载的日志条数', tone: 'blue' }, { label: '可立即重试', value: callbackLogDrawer.logs.filter(item => isCallbackRetryable(item.callbackStatus)).length, hint: '当前页中支持人工重试的日志条数', tone: 'green' }, { label: '已返回2xx', value: callbackLogDrawer.logs.filter(item => Number(item.responseStatusCode) >= 200 && Number(item.responseStatusCode) < 300).length, hint: '当前页收到成功响应的日志条数', tone: 'teal' }, { label: '最近失败', value: callbackLogDrawer.logs.filter(item => item.callbackStatus === 3).length, hint: '当前页状态为失败的日志条数', tone: 'gold' }]);
async function loadPageData() { tableLoading.value = true; try { const { data } = await fetchApiAccountPage({ pageNum: pagination.current, pageSize: pagination.pageSize, accountName: queryForm.accountName.trim() || undefined, status: queryForm.status, accountType: queryForm.accountType, clientType: queryForm.clientType, systemKeyword: queryForm.systemKeyword.trim() || undefined, accessEnvironment: queryForm.accessEnvironment }); accountList.value = data.records || []; pagination.total = data.total || 0; } finally { tableLoading.value = false; } }
async function loadDashboardStats() {
  const [{ data: accounts }, { data: groups }] = await Promise.all([fetchApiAccountList(), fetchOpenApiRegistryTree({ status: 1 })]);
  dashboardStats.totalAccounts = accounts.length;
  dashboardStats.enabledAccounts = accounts.filter(item => item.status === 1).length;
  dashboardStats.authorizedApiTotal = accounts.reduce((sum, item) => sum + (item.authorizedApiCount || 0), 0);
  dashboardStats.latestRegistryCount = countLeafApis(groups);
  dashboardStats.productionAccounts = accounts.filter(item => item.accessEnvironment === 'production').length;
  dashboardStats.callbackConfiguredAccounts = accounts.filter(item => !!item.callbackUrl).length;
  dashboardStats.externalAccounts = accounts.filter(item => item.accountType === 2).length;
}
async function refreshPage() { await Promise.all([loadPageData(), loadDashboardStats()]); }
function resetAccountForm() { Object.assign(accountForm, { accountName: '', ownerName: '', ownerContact: '', systemName: '', systemCode: '', accountType: 2, clientTypes: [], accessEnvironment: 'production', signVersion: 'v1', environmentIpWhitelistText: createEmptyEnvironmentWhitelistText(), callbackUrl: '', rateLimit: 1000, expireTime: null, status: 1, remark: '' }); }
function fillAccountForm(detail: ApiAccountDetail) { Object.assign(accountForm, { accountName: detail.accountName, ownerName: detail.ownerName, ownerContact: detail.ownerContact, systemName: detail.systemName, systemCode: detail.systemCode, accountType: detail.accountType, clientTypes: detail.clientTypes || [], accessEnvironment: (detail.accessEnvironment as AccessEnvironment) || 'production', signVersion: detail.signVersion || 'v1', environmentIpWhitelistText: buildEnvironmentWhitelistTextMap(detail.environmentIpWhitelist, detail.ipWhitelist, detail.accessEnvironment as AccessEnvironment | undefined), callbackUrl: detail.callbackUrl || '', rateLimit: detail.rateLimit || 1000, expireTime: detail.expireTime || null, status: detail.status, remark: detail.remark || '' }); }
function openCreateModal() { accountModal.mode = 'create'; accountModal.editingId = 0; resetAccountForm(); accountModal.visible = true; }
async function openEditModalById(accountId: number) { const { data } = await fetchApiAccountDetail(accountId); accountModal.mode = 'edit'; accountModal.editingId = accountId; fillAccountForm(data); accountModal.visible = true; }
async function openAuthorizationDrawerById(accountId: number) { authorizationDrawer.visible = true; Object.assign(authorizationFilters, { keyword: '', moduleName: '', method: '' }); await loadAuthorizationSnapshot(accountId); }
async function openCallbackLogDrawerById(accountId: number) { const { data } = await fetchApiAccountDetail(accountId); await openCallbackLogDrawer(data); }
async function openAccountPreviewModalById(accountId: number) {
  accountPreview.visible = true;
  accountPreview.loading = true;
  accountPreview.accountId = accountId;
  try {
    const { data } = await fetchApiAccountDetail(accountId);
    if (accountPreview.accountId === accountId) {
      accountPreview.snapshot = data;
    }
  } finally {
    if (accountPreview.accountId === accountId) {
      accountPreview.loading = false;
    }
  }
}
function handleAccountPreviewVisibleChange(visible: boolean) {
  accountPreview.visible = visible;
  if (!visible) {
    accountPreview.loading = false;
    accountPreview.accountId = 0;
    accountPreview.snapshot = null;
  }
}
function handleAccountRowDblClick(record: ApiAccountItem) { void openAccountPreviewModalById(record.id); }
function handleOpenAccountPreview(record: ApiAccountItem) { void openAccountPreviewModalById(record.id); }
async function handleEditAccount(record: ApiAccountItem) { await openEditModalById(record.id); }
async function handleAuthorizeAccount(record: ApiAccountItem) { await openAuthorizationDrawerById(record.id); }
async function handleCallbackLogAccount(record: ApiAccountItem) { await openCallbackLogDrawerById(record.id); }
async function handleResetSecretById(accountId: number) {
  const { data } = await resetApiAccountSecret(accountId);
  credentialModal.payload = data;
  credentialModal.visible = true;
  Message.success('密钥已重置');
  await refreshPage();
  if (accountPreview.visible && accountPreview.accountId === accountId) {
    await openAccountPreviewModalById(accountId);
  }
}
async function handleDeleteAccountById(accountId: number) {
  await deleteApiAccount(accountId);
  Message.success('账号已删除');
  if (accountPreview.accountId === accountId) handleAccountPreviewVisibleChange(false);
  if (pagination.current > 1 && accountList.value.length === 1) pagination.current -= 1;
  await refreshPage();
}
function confirmResetSecret(record: Pick<ApiAccountItem, 'id' | 'accountName'>) {
  Modal.confirm({
    title: '确认重置密钥',
    content: `重置后，“${record.accountName}” 的旧密钥会立即失效，并生成一份新的访问凭证。是否继续？`,
    onOk: () => handleResetSecretById(record.id)
  });
}
function confirmDeleteAccount(record: Pick<ApiAccountItem, 'id' | 'accountName'>) {
  Modal.confirm({
    title: '确认删除账号',
    content: `删除后无法恢复账号 “${record.accountName}” 的调用配置、授权记录和回调治理信息。是否继续？`,
    onOk: () => handleDeleteAccountById(record.id)
  });
}
async function handlePreviewEdit() {
  const accountId = accountPreview.accountId;
  if (!accountId) return;
  handleAccountPreviewVisibleChange(false);
  await openEditModalById(accountId);
}
async function handlePreviewAuthorization() {
  const accountId = accountPreview.accountId;
  if (!accountId) return;
  handleAccountPreviewVisibleChange(false);
  await openAuthorizationDrawerById(accountId);
}
async function handlePreviewCallback() {
  const accountId = accountPreview.accountId;
  if (!accountId) return;
  handleAccountPreviewVisibleChange(false);
  await openCallbackLogDrawerById(accountId);
}
async function handlePreviewResetSecret() {
  const account = accountPreview.snapshot;
  if (!account) return;
  confirmResetSecret(account);
}
async function handlePreviewDelete() {
  const account = accountPreview.snapshot;
  if (!account) return;
  confirmDeleteAccount(account);
}
async function submitAccountForm() {
  const validationResult = validateAndBuildPayload(accountForm);
  if (!validationResult.valid) {
    Message.warning(validationResult.message);
    return false;
  }

  accountModal.submitting = true;
  try {
    if (accountModal.mode === 'create') {
      const { data } = await createApiAccount(validationResult.payload);
      credentialModal.payload = data;
      credentialModal.visible = true;
      Message.success('API账号创建成功');
    } else {
      await updateApiAccount(accountModal.editingId, validationResult.payload);
      Message.success('API账号更新成功');
    }
    accountModal.visible = false;
    await refreshPage();
    return true;
  } finally {
    accountModal.submitting = false;
  }
}
async function handleSearch() { pagination.current = 1; await loadPageData(); }
function toggleQueryAdvanced() { queryAdvancedExpanded.value = !queryAdvancedExpanded.value; }
async function handleResetSearch() { Object.assign(queryForm, { accountName: '', status: undefined, accountType: undefined, clientType: undefined, systemKeyword: '', accessEnvironment: undefined }); queryAdvancedExpanded.value = false; pagination.current = 1; await loadPageData(); }
async function handlePageChange(page: number) { pagination.current = page; await loadPageData(); }
async function handlePageSizeChange(pageSize: number) { pagination.pageSize = pageSize; pagination.current = 1; await loadPageData(); }
async function handleSyncRegistry() { syncingRegistry.value = true; try { const { data } = await syncOpenApiRegistry(); dashboardStats.latestRegistryCount = data.totalDiscovered; Message.success(`项目接口目录同步完成，本次发现 ${data.totalDiscovered} 个可授权接口`); await refreshPage(); if (authorizationDrawer.visible && authorizationDrawer.accountId) await loadAuthorizationSnapshot(authorizationDrawer.accountId); } finally { syncingRegistry.value = false; } }
async function loadAuthorizationSnapshot(accountId: number) { authorizationDrawer.loading = true; try { const { data } = await fetchAuthorizationSnapshot(accountId); authorizationDrawer.accountId = accountId; authorizationDrawer.snapshot = data; authorizationDrawer.checkedKeys = (data.selectedApiIds || []).map(id => buildApiKey(id)); } finally { authorizationDrawer.loading = false; } }
async function submitAuthorization() { if (!authorizationDrawer.accountId) return; authorizationDrawer.saving = true; try { await saveAuthorization(authorizationDrawer.accountId, parseCheckedApiIds(authorizationDrawer.checkedKeys)); Message.success('接口授权已保存'); await Promise.all([loadAuthorizationSnapshot(authorizationDrawer.accountId), refreshPage()]); } finally { authorizationDrawer.saving = false; } }
function selectModuleApis(moduleName: string) { const group = (authorizationDrawer.snapshot?.groups || []).find(item => item.moduleName === moduleName); if (group) mergeCheckedKeys(collectGroupApiIds(group)); }
function clearModuleApis(moduleName: string) { const group = (authorizationDrawer.snapshot?.groups || []).find(item => item.moduleName === moduleName); if (group) removeCheckedKeys(collectGroupApiIds(group)); }
function selectVisibleApis() { mergeCheckedKeys(collectApiIdsFromFilteredGroups(filteredAuthorizationGroups.value)); }
function clearVisibleApis() { removeCheckedKeys(collectApiIdsFromFilteredGroups(filteredAuthorizationGroups.value)); }
function mergeCheckedKeys(apiIds: number[]) { const merged = new Set(authorizationDrawer.checkedKeys); apiIds.forEach(id => merged.add(buildApiKey(id))); authorizationDrawer.checkedKeys = Array.from(merged); }
function removeCheckedKeys(apiIds: number[]) { const removing = new Set(apiIds.map(id => buildApiKey(id))); authorizationDrawer.checkedKeys = authorizationDrawer.checkedKeys.filter(key => !removing.has(key)); }
async function openCallbackLogDrawer(record: ApiAccountItem) { callbackLogDrawer.visible = true; callbackLogDrawer.account = record; callbackLogDrawer.pagination.current = 1; callbackLogFilters.callbackStatus = undefined; await loadCallbackLogs(); }
async function loadCallbackLogs() { if (!callbackLogDrawer.account) return; callbackLogDrawer.loading = true; try { const { data } = await fetchApiCallbackLogs(callbackLogDrawer.account.id, { pageNum: callbackLogDrawer.pagination.current, pageSize: callbackLogDrawer.pagination.pageSize, callbackStatus: callbackLogFilters.callbackStatus }); callbackLogDrawer.logs = data.page.records || []; callbackLogDrawer.pagination.total = data.page.total || 0; callbackLogDrawer.summary = data.summary || createSummary(); } finally { callbackLogDrawer.loading = false; } }
async function handleCallbackLogSearch() { callbackLogDrawer.pagination.current = 1; await loadCallbackLogs(); }
async function handleCallbackLogReset() { callbackLogFilters.callbackStatus = undefined; callbackLogDrawer.pagination.current = 1; await loadCallbackLogs(); }
async function handleCallbackLogPageChange(page: number) { callbackLogDrawer.pagination.current = page; await loadCallbackLogs(); }
async function handleCallbackLogPageSizeChange(pageSize: number) { callbackLogDrawer.pagination.pageSize = pageSize; callbackLogDrawer.pagination.current = 1; await loadCallbackLogs(); }
async function handleTriggerTestCallback(record?: ApiAccountItem) { const target = record || callbackLogDrawer.account; if (!target) return; callbackLogDrawer.testing = true; try { await triggerApiAccountTestCallback(target.id); Message.success('测试回调已发起'); if (callbackLogDrawer.visible && callbackLogDrawer.account?.id === target.id) await loadCallbackLogs(); } finally { callbackLogDrawer.testing = false; } }
async function handleRetryCallback(record: ApiCallbackLogItem) { callbackLogDrawer.retryingId = record.id; try { await retryApiCallbackLog(record.id); Message.success('回调已重新投递'); await loadCallbackLogs(); } finally { callbackLogDrawer.retryingId = null; } }

function createSummary() { return { totalCount: 0, pendingCount: 0, retryingCount: 0, successCount: 0, failedCount: 0 }; }
function createEmptyEnvironmentWhitelistText(): EnvironmentIpWhitelistTextMap { return { test: '', staging: '', production: '' }; }
function buildApiKey(apiId: number) { return `api:${apiId}`; }
function parseCheckedApiIds(keys: string[]) { return keys.filter(key => key.startsWith('api:')).map(key => Number(key.slice(4))).filter(id => Number.isFinite(id)); }
function collectGroupApiIds(group: OpenApiGroupNode) { return group.controllers.flatMap(controller => controller.apis.map(api => api.id)); }
function collectApiIdsFromFilteredGroups(groups: OpenApiGroupNode[]) { return groups.flatMap(group => collectGroupApiIds(group)); }
function countLeafApis(groups: OpenApiGroupNode[]) { return collectApiIdsFromFilteredGroups(groups).length; }
function countApisInGroup(group: OpenApiGroupNode) { return collectGroupApiIds(group).length; }
function countEnvironmentWhitelistEntries(environmentIpWhitelist?: EnvironmentIpWhitelist) {
  return (environmentIpWhitelist?.test?.length || 0) + (environmentIpWhitelist?.staging?.length || 0) + (environmentIpWhitelist?.production?.length || 0);
}
function isEnabledApiId(apiId: number, groups: OpenApiGroupNode[]) { return groups.some(group => group.controllers.some(controller => controller.apis.some(api => api.id === apiId && api.status === 1))); }
function getAccountTypeLabel(value: number) { return accountTypeOptions.find(item => item.value === value)?.label || '未知类型'; }
function getAccountTypeTagColor(value: number) { return value === 1 ? 'arcoblue' : 'purple'; }
function getClientTypeLabels(values?: string[]) { return (values || []).map(value => clientTypeOptions.find(item => item.value === value)?.label || value); }
function getCompactClientCountLabel(values?: string[]) { return values?.length ? `${values.length}类客户端` : '未配客户端'; }
function getCompactClientSummary(values?: string[]) {
  const labels = getClientTypeLabels(values);
  if (!labels.length) return '当前账号未声明调用客户端';
  return labels.length <= 2 ? labels.join('、') : `${labels.slice(0, 2).join('、')} +${labels.length - 2}`;
}
function getAccessEnvironmentLabel(value?: string | null) { return environmentOptions.find(item => item.value === value)?.label || '未设置环境'; }
function getEnvironmentTagColor(value?: string | null) { return ({ test: 'arcoblue', staging: 'orange', production: 'green' } as Record<string, string>)[value || ''] || 'gray'; }
function getStatusLabel(value: number) { return value === 1 ? '启用' : '停用'; }
function getCallbackConfigLabel(value?: string | null) { return value?.trim() ? '已配置回调' : '未配置回调'; }
function getExpireTimeLabel(value?: string | null) { return value || '永久有效'; }
function getEnvironmentWhitelistArrayCountLabel(values?: string[]) { return values?.length ? `${values.length} 条来源` : '未限制'; }
function isCallbackRetryable(status: number) { return status === 1 || status === 3; }
function formatIpWhitelistInput(values?: string[]) { return (values || []).join('\n'); }
function buildEnvironmentWhitelistTextMap(environmentIpWhitelist?: EnvironmentIpWhitelist, fallbackIpWhitelist?: string[], accessEnvironment?: AccessEnvironment) { const result = createEmptyEnvironmentWhitelistText(); result.test = formatIpWhitelistInput(environmentIpWhitelist?.test); result.staging = formatIpWhitelistInput(environmentIpWhitelist?.staging); result.production = formatIpWhitelistInput(environmentIpWhitelist?.production); const current = accessEnvironment || 'production'; if (!result[current] && fallbackIpWhitelist?.length) result[current] = formatIpWhitelistInput(fallbackIpWhitelist); return result; }
function getEnvironmentWhitelistCountLabel(value: string) { const count = parseIpWhitelistInput(value).length; return count > 0 ? `${count} 条来源` : '未限制'; }
function parseManageRouteAction(): { action?: ManageRouteAction; accountId?: number } {
  const action = typeof route.query.action === 'string' ? route.query.action as ManageRouteAction : undefined;
  const accountId = Number(route.query.accountId);
  if (!action || !Number.isFinite(accountId) || accountId <= 0) return {};
  return { action, accountId };
}
async function clearManageRouteAction() {
  const query = { ...route.query };
  delete query.action;
  delete query.accountId;
  await router.replace({ path: route.path, query });
}
async function handleManageRouteAction() {
  const { action, accountId } = parseManageRouteAction();
  if (!action || !accountId) return;
  if (action === 'edit') {
    await openEditModalById(accountId);
  } else if (action === 'authorization') {
    await openAuthorizationDrawerById(accountId);
  } else if (action === 'callback') {
    await openCallbackLogDrawerById(accountId);
  } else if (action === 'reset-secret') {
    await handleResetSecretById(accountId);
  }
  await clearManageRouteAction();
}
async function initializePage() {
  await refreshPage();
  await handleManageRouteAction();
}
onMounted(initializePage);
watch(() => [route.query.action, route.query.accountId], async () => {
  if (route.path === '/admin/api/account') {
    await handleManageRouteAction();
  }
});
watch(() => accountPreview.visible, visible => {
  if (!visible) {
    accountPreview.loading = false;
    accountPreview.accountId = 0;
    accountPreview.snapshot = null;
  }
});
</script>

<style scoped>
.page {
  --page-bg-start: #f4f7fb;
  --page-bg-end: #f8fafc;
  --surface-color: #ffffff;
  --surface-border: #e5edf6;
  --text-primary: #0f172a;
  --text-secondary: #64748b;
  min-height: 100%;
  padding: 24px;
  background: linear-gradient(180deg, var(--page-bg-start), var(--page-bg-end));
  overflow: auto;
}

.card,
.banner,
.info,
.pane,
.summary,
.sum,
.white-card,
.meta-box {
  background: var(--surface-color);
  border: 1px solid var(--surface-border);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.05);
}

.drawer h2 {
  margin: 0;
  color: var(--text-primary);
}
.actions,
.tags,
.status,
.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.table-title small,
.stack small,
.helper,
.meta-box span,
.meta-box small,
.muted,
.banner span,
.banner small,
.guide p,
.white-card__head span {
  color: var(--text-secondary);
  font-size: 12px;
}

.card {
  margin-top: 20px;
  border-radius: 24px;
}

.query-panel,
.table-shell {
  overflow: hidden;
}

.query-panel {
  margin-top: 20px;
}

.query-panel__advanced {
  margin-top: 10px;
}

.query-panel__advanced-shell {
  padding: 12px 14px 2px;
  border-radius: 20px;
  border: 1px dashed rgba(186, 200, 218, 0.9);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(246, 250, 255, 0.94));
}

.query-panel__advanced-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.query-panel__advanced-heading span {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
}

.query-panel__advanced-heading small {
  color: var(--text-secondary);
  font-size: 12px;
}

.query-panel__toggle-btn {
  min-width: 152px;
  height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  border-color: rgba(211, 221, 234, 0.96);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(246, 250, 255, 0.96));
  color: #475569;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.94);
}

.query-advanced-fold-enter-active,
.query-advanced-fold-leave-active {
  transition: all 0.24s ease;
}

.query-advanced-fold-enter-from,
.query-advanced-fold-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.authorization-chip-row,
.authorization-tree-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.authorization-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.authorization-summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border-radius: 22px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.04);
}

.authorization-summary-card span,
.authorization-summary-card small {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.65;
}

.authorization-summary-card strong {
  font-size: 28px;
  line-height: 1.12;
  color: var(--text-primary);
}

.authorization-summary-card.tone-blue {
  border-color: rgba(59, 130, 246, 0.18);
}

.authorization-summary-card.tone-green {
  border-color: rgba(16, 185, 129, 0.18);
}

.authorization-summary-card.tone-teal {
  border-color: rgba(13, 148, 136, 0.18);
}

.authorization-summary-card.tone-gold {
  border-color: rgba(245, 158, 11, 0.2);
}

.authorization-hero__actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}

.table-shell {
  margin-top: 22px;
}

.table-account-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.table-account-main__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.table-compact-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.table-compact-cell--inline {
  gap: 6px;
}

.table-tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.table-status-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.table-account-main strong,
.table-compact-cell strong {
  color: var(--text-primary);
}

.table-account-main strong {
  font-size: 22px;
  line-height: 1.2;
}

.table-status-cell strong {
  color: var(--text-primary);
}

.table-account-main small,
.table-compact-cell small,
.table-status-cell small {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.65;
}

.table-row-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.table-action-button {
  min-width: 66px;
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  font-weight: 700;
}

.table-action-button--primary {
  border: 0;
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 12px 24px rgba(23, 105, 255, 0.14);
}

.table-row-actions__danger {
  color: #f53f3f;
}

.account-table :deep(.arco-table-container) {
  border-radius: 24px;
  border: 1px solid rgba(220, 229, 240, 0.94);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.96);
}

.account-table :deep(.arco-table) {
  background: transparent;
}

.account-table :deep(.arco-table-th) {
  padding-top: 16px;
  padding-bottom: 16px;
  background: linear-gradient(180deg, rgba(246, 250, 255, 0.98), rgba(239, 245, 251, 0.98));
  color: #334155;
  font-weight: 700;
  font-size: 14px;
}

.account-table :deep(.arco-table-tr) {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.account-table :deep(tbody .arco-table-tr) {
  cursor: pointer;
}

.account-table :deep(.arco-table-tr:hover td) {
  background: rgba(245, 250, 255, 0.96);
}

.account-table :deep(tbody .arco-table-tr:hover) {
  box-shadow: inset 0 0 0 1px rgba(23, 105, 255, 0.08);
}

.account-table :deep(.arco-table-td) {
  padding-top: 16px;
  padding-bottom: 16px;
  vertical-align: middle;
  background: rgba(255, 255, 255, 0.76);
}

.account-table :deep(.arco-table-fixed-right) {
  box-shadow: -10px 0 24px rgba(148, 163, 184, 0.08);
}

.account-table :deep(.arco-pagination-item),
.account-table :deep(.arco-pagination-jumper-input),
.account-table :deep(.arco-pagination-btn) {
  border-radius: 999px;
}

.account-table :deep(.arco-pagination) {
  padding-top: 18px;
}

.stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stack strong,
.module strong,
.node strong,
.meta-box strong {
  color: var(--text-primary);
}

.banner {
  display: grid;
  gap: 6px;
  margin-bottom: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.06), rgba(16, 185, 129, 0.06));
}

.banner strong {
  font-size: 20px;
  color: var(--text-primary);
}

.account-modal-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.account-modal-title__main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.account-modal-title__main p {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.account-modal-title__main strong {
  font-size: 26px;
  line-height: 1.2;
  color: var(--text-primary);
}

.account-modal-title__badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(23, 105, 255, 0.08), rgba(18, 184, 166, 0.12));
  color: #0f766e;
  font-size: 12px;
  font-weight: 600;
}

.account-modal-shell {
  min-height: 0;
}

/* 弹窗主体拆成概览侧栏与表单主区，兼顾信息密度和首次浏览体验。 */
.account-workbench {
  gap: 20px;
}

.account-aside,
.account-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.account-panel,
.form-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--surface-border);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
}

.account-panel {
  padding: 20px;
}

.account-panel--hero {
  color: #ffffff;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.22), transparent 34%),
    linear-gradient(160deg, #0f5de2, #1395cf 56%, #10b981);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 24px 56px rgba(15, 23, 42, 0.16);
}

.account-panel__eyebrow {
  margin: 0 0 18px;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.account-panel__identity {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.account-panel__identity span,
.account-panel__identity small {
  color: rgba(255, 255, 255, 0.74);
}

.account-panel__identity strong {
  font-size: 34px;
  line-height: 1.1;
  color: #ffffff;
}

.account-pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.account-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.16);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.92);
}

.section-mini-title {
  margin-bottom: 14px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.account-overview-list {
  display: grid;
  gap: 12px;
}

.account-stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(255, 255, 255, 0.98));
}

.account-stat-card span,
.account-stat-card small {
  color: var(--text-secondary);
  font-size: 12px;
}

.account-stat-card strong {
  font-size: 24px;
  line-height: 1.2;
  color: var(--text-primary);
  word-break: break-word;
}

.account-stat-card.tone-blue {
  border-color: rgba(37, 99, 235, 0.16);
}

.account-stat-card.tone-green {
  border-color: rgba(16, 185, 129, 0.18);
}

.account-stat-card.tone-teal {
  border-color: rgba(13, 148, 136, 0.18);
}

.account-stat-card.tone-gold {
  border-color: rgba(245, 158, 11, 0.18);
}

.account-panel--guide {
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
}

.account-guide-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

.form-panel {
  padding: 22px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.section-heading h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.section-heading p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.section-heading--row {
  align-items: center;
}

.account-modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: 100%;
}

.account-modal-footer__tip {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--text-secondary);
  text-align: left;
}

.account-modal-footer__tip strong {
  color: var(--text-primary);
}

.account-modal-footer__actions {
  display: flex;
  gap: 12px;
}

.account-modal-footer__actions :deep(.arco-btn) {
  min-width: 110px;
  border-radius: 999px;
}

.account-modal-footer__actions :deep(.arco-btn-primary) {
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 14px 28px rgba(23, 105, 255, 0.2);
}

.account-modal-shell :deep(.arco-form-item) {
  margin-bottom: 18px;
}

.account-modal-shell :deep(.arco-form-item-label-col > label) {
  color: var(--text-primary);
  font-weight: 600;
}

.account-modal-shell :deep(.arco-input-wrapper),
.account-modal-shell :deep(.arco-textarea-wrapper),
.account-modal-shell :deep(.arco-select-view),
.account-modal-shell :deep(.arco-picker),
.account-modal-shell :deep(.arco-input-number) {
  border-radius: 16px;
  border-color: #dde6f1;
  background: #f8fbff;
  transition: all 0.2s ease;
}

.account-modal-shell :deep(.arco-input-wrapper:hover),
.account-modal-shell :deep(.arco-textarea-wrapper:hover),
.account-modal-shell :deep(.arco-select-view:hover),
.account-modal-shell :deep(.arco-picker:hover),
.account-modal-shell :deep(.arco-input-number:hover) {
  border-color: rgba(23, 105, 255, 0.34);
  background: #ffffff;
}

.account-modal-shell :deep(.arco-input-wrapper.arco-input-focus),
.account-modal-shell :deep(.arco-textarea-focus),
.account-modal-shell :deep(.arco-select-view.arco-select-view-focus),
.account-modal-shell :deep(.arco-picker-focus),
.account-modal-shell :deep(.arco-input-number-focus) {
  border-color: rgba(23, 105, 255, 0.46);
  box-shadow: 0 0 0 4px rgba(23, 105, 255, 0.08);
  background: #ffffff;
}

.whitelist-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  padding: 16px 18px;
  border: 1px solid var(--surface-border);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.04), rgba(16, 185, 129, 0.05));
}

.whitelist-head p {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.white-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: 20px;
}

.white-card.active {
  border-color: rgba(37, 99, 235, 0.36);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.1);
}

.white-card__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.authorization-workbench {
  gap: 20px;
}

.authorization-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  gap: 20px;
  padding: 24px 26px;
  border-radius: 30px;
  overflow: hidden;
  color: #ffffff;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.18), transparent 34%),
    linear-gradient(135deg, #0f4fd6, #0f7edc 52%, #0ea5a3);
  box-shadow: 0 28px 68px rgba(15, 23, 42, 0.16);
}

.authorization-hero::before,
.authorization-hero::after {
  position: absolute;
  content: '';
  pointer-events: none;
}

.authorization-hero::before {
  inset: auto -70px -110px auto;
  width: 240px;
  height: 240px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  filter: blur(8px);
}

.authorization-hero::after {
  top: 20px;
  right: 24px;
  width: 170px;
  height: 170px;
  border-radius: 34px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  opacity: 0.4;
}

.authorization-hero__main,
.authorization-hero__side {
  position: relative;
  z-index: 1;
}

.authorization-hero__main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.authorization-hero__eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.76);
}

.authorization-hero h2 {
  margin: 0;
  font-size: 34px;
  line-height: 1.16;
  color: #ffffff;
}

.authorization-hero__desc {
  margin: 0;
  max-width: 720px;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.8;
}

.authorization-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(6, 23, 45, 0.16);
  backdrop-filter: blur(10px);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.92);
}

.authorization-hero__side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.authorization-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.authorization-kpi {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 126px;
  padding: 18px 16px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(10px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.authorization-kpi span,
.authorization-kpi small {
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  line-height: 1.65;
}

.authorization-kpi strong {
  font-size: 28px;
  line-height: 1.1;
  color: #ffffff;
  word-break: break-word;
}

.authorization-kpi.tone-blue {
  border-color: rgba(191, 219, 254, 0.3);
}

.authorization-kpi.tone-green {
  border-color: rgba(187, 247, 208, 0.3);
}

.authorization-kpi.tone-teal {
  border-color: rgba(153, 246, 228, 0.3);
}

.authorization-kpi.tone-gold {
  border-color: rgba(253, 230, 138, 0.3);
}

.authorization-hero__actions {
  justify-content: flex-end;
}

.authorization-hero__actions :deep(.arco-btn) {
  min-width: 120px;
  border-radius: 999px;
  border: 0;
}

.authorization-hero__actions :deep(.arco-btn-primary) {
  background: linear-gradient(135deg, #1fd4c4, #14b8a6);
  box-shadow: 0 14px 28px rgba(20, 184, 166, 0.24);
}

.authorization-hero__actions :deep(.arco-btn:not(.arco-btn-primary)) {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.authorization-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 20px;
  min-height: 0;
  flex: 1;
}

.authorization-side,
.authorization-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
  min-height: 0;
}

.authorization-panel {
  padding: 20px;
  border-radius: 26px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

.authorization-panel--identity {
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.07), transparent 36%),
    linear-gradient(180deg, #fbfdff, #ffffff);
}

.authorization-panel--guide {
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
}

.authorization-panel--modules {
  min-height: 0;
}

.authorization-panel--tree {
  display: flex;
  flex-direction: column;
}

.authorization-fact-grid {
  display: grid;
  gap: 12px;
}

.authorization-fact-card,
.authorization-module-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.authorization-fact-card span,
.authorization-fact-card small,
.authorization-module-card small {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.authorization-fact-card strong,
.authorization-module-card strong {
  color: var(--text-primary);
  font-size: 17px;
  line-height: 1.5;
  word-break: break-word;
}

.authorization-fact-card__copy {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.5;
  color: var(--text-primary);
  word-break: break-word;
}

.authorization-filter-form :deep(.arco-form-item) {
  margin-bottom: 18px;
}

.authorization-filter-form :deep(.arco-form-item:last-child) {
  margin-bottom: 0;
}

.authorization-filter-form :deep(.arco-form-item-label-col > label) {
  color: var(--text-primary);
  font-weight: 600;
}

.authorization-filter-form :deep(.arco-input-wrapper),
.authorization-filter-form :deep(.arco-select-view) {
  border-radius: 16px;
  border-color: #dde6f1;
  background: #f8fbff;
  transition: all 0.2s ease;
}

.authorization-filter-form :deep(.arco-input-wrapper:hover),
.authorization-filter-form :deep(.arco-select-view:hover) {
  border-color: rgba(23, 105, 255, 0.34);
  background: #ffffff;
}

.authorization-filter-form :deep(.arco-input-wrapper.arco-input-focus),
.authorization-filter-form :deep(.arco-select-view.arco-select-view-focus) {
  border-color: rgba(23, 105, 255, 0.46);
  box-shadow: 0 0 0 4px rgba(23, 105, 255, 0.08);
  background: #ffffff;
}

.authorization-guide-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

.authorization-module-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.authorization-module-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.authorization-module-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.authorization-module-card__actions :deep(.arco-btn) {
  border-radius: 999px;
}

.authorization-summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.authorization-tree-shell {
  min-height: 480px;
  margin-top: 16px;
  padding: 18px 20px;
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, #fbfdff, #f8fbff);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.authorization-tree-shell :deep(.arco-tree-node-title) {
  width: 100%;
  border-radius: 16px;
  transition: background 0.2s ease;
}

.authorization-tree-shell :deep(.arco-tree-node-title:hover) {
  background: rgba(37, 99, 235, 0.05);
}

.authorization-node {
  gap: 8px;
  padding: 8px 4px;
}

.authorization-tree-actions :deep(.arco-btn) {
  border-radius: 999px;
}

.authorization-tree-actions :deep(.arco-btn-primary) {
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 14px 28px rgba(23, 105, 255, 0.18);
}

.callback-workbench {
  gap: 20px;
}

.callback-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  gap: 20px;
  padding: 24px 26px;
  border-radius: 30px;
  overflow: hidden;
  color: #ffffff;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.18), transparent 34%),
    linear-gradient(135deg, #0f4fd6, #0b7ec3 54%, #0f9d8f);
  box-shadow: 0 28px 68px rgba(15, 23, 42, 0.16);
}

.callback-hero::before,
.callback-hero::after {
  position: absolute;
  content: '';
  pointer-events: none;
}

.callback-hero::before {
  inset: auto -70px -110px auto;
  width: 240px;
  height: 240px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  filter: blur(8px);
}

.callback-hero::after {
  top: 20px;
  right: 24px;
  width: 170px;
  height: 170px;
  border-radius: 34px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  opacity: 0.4;
}

.callback-hero__main,
.callback-hero__side {
  position: relative;
  z-index: 1;
}

.callback-hero__main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.callback-hero__eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.76);
}

.callback-hero h2 {
  margin: 0;
  font-size: 34px;
  line-height: 1.16;
  color: #ffffff;
}

.callback-hero__desc {
  margin: 0;
  max-width: 720px;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.8;
}

.callback-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.callback-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(6, 23, 45, 0.16);
  backdrop-filter: blur(10px);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.92);
}

.callback-hero__side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.callback-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.callback-kpi {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 126px;
  padding: 18px 16px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(10px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.callback-kpi span,
.callback-kpi small {
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  line-height: 1.65;
}

.callback-kpi strong {
  font-size: 28px;
  line-height: 1.1;
  color: #ffffff;
  word-break: break-word;
}

.callback-kpi.tone-blue {
  border-color: rgba(191, 219, 254, 0.3);
}

.callback-kpi.tone-green {
  border-color: rgba(187, 247, 208, 0.3);
}

.callback-kpi.tone-teal {
  border-color: rgba(153, 246, 228, 0.3);
}

.callback-kpi.tone-gold {
  border-color: rgba(253, 230, 138, 0.3);
}

.callback-hero__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.callback-hero__actions :deep(.arco-btn) {
  min-width: 120px;
  border-radius: 999px;
  border: 0;
}

.callback-hero__actions :deep(.arco-btn-primary) {
  background: linear-gradient(135deg, #1fd4c4, #14b8a6);
  box-shadow: 0 14px 28px rgba(20, 184, 166, 0.24);
}

.callback-hero__actions :deep(.arco-btn:not(.arco-btn-primary)) {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.callback-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 20px;
  min-height: 0;
  flex: 1;
}

.callback-side,
.callback-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
  min-height: 0;
}

.callback-panel {
  padding: 20px;
  border-radius: 26px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

.callback-panel--identity {
  background:
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.08), transparent 36%),
    linear-gradient(180deg, #fbfdff, #ffffff);
}

.callback-panel--guide {
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
}

.callback-panel--table {
  display: flex;
  flex-direction: column;
}

.callback-fact-grid {
  display: grid;
  gap: 12px;
}

.callback-fact-card,
.callback-summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.callback-fact-card span,
.callback-fact-card small,
.callback-summary-card span,
.callback-summary-card small {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.callback-fact-card strong,
.callback-summary-card strong {
  color: var(--text-primary);
  font-size: 17px;
  line-height: 1.5;
  word-break: break-word;
}

.callback-fact-card__copy {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.5;
  color: var(--text-primary);
  word-break: break-word;
}

.callback-filter-form :deep(.arco-form-item) {
  margin-bottom: 18px;
}

.callback-filter-form :deep(.arco-form-item:last-child) {
  margin-bottom: 0;
}

.callback-filter-form :deep(.arco-form-item-label-col > label) {
  color: var(--text-primary);
  font-weight: 600;
}

.callback-filter-form :deep(.arco-select-view) {
  border-radius: 16px;
  border-color: #dde6f1;
  background: #f8fbff;
  transition: all 0.2s ease;
}

.callback-filter-form :deep(.arco-select-view:hover) {
  border-color: rgba(23, 105, 255, 0.34);
  background: #ffffff;
}

.callback-filter-form :deep(.arco-select-view.arco-select-view-focus) {
  border-color: rgba(23, 105, 255, 0.46);
  box-shadow: 0 0 0 4px rgba(23, 105, 255, 0.08);
  background: #ffffff;
}

.callback-filter-actions,
.callback-table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.callback-filter-actions {
  margin-top: 4px;
}

.callback-filter-actions :deep(.arco-btn),
.callback-table-actions :deep(.arco-btn) {
  min-width: 110px;
  border-radius: 999px;
}

.callback-filter-actions :deep(.arco-btn-primary),
.callback-table-actions :deep(.arco-btn-primary) {
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 14px 28px rgba(23, 105, 255, 0.18);
}

.callback-guide-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

.callback-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.callback-summary-card.tone-blue {
  border-color: rgba(59, 130, 246, 0.18);
}

.callback-summary-card.tone-green {
  border-color: rgba(16, 185, 129, 0.18);
}

.callback-summary-card.tone-teal {
  border-color: rgba(13, 148, 136, 0.18);
}

.callback-summary-card.tone-gold {
  border-color: rgba(245, 158, 11, 0.2);
}

.callback-table-shell {
  min-height: 480px;
  margin-top: 16px;
}

.callback-table :deep(.arco-table-container) {
  border-radius: 22px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  overflow: hidden;
}

.callback-table :deep(.arco-table-th) {
  background: linear-gradient(180deg, #f8fbff, #f3f7fc);
  color: #334155;
  font-weight: 700;
}

.callback-table :deep(.arco-table-tr) {
  transition: background 0.2s ease;
}

.callback-table :deep(.arco-table-tr:hover td) {
  background: rgba(248, 250, 252, 0.9);
}

.callback-table :deep(.arco-table-td) {
  padding-top: 18px;
  padding-bottom: 18px;
  vertical-align: top;
}

.callback-table :deep(.arco-pagination-item),
.callback-table :deep(.arco-pagination-jumper-input),
.callback-table :deep(.arco-pagination-btn) {
  border-radius: 999px;
}

.drawer-body,
:deep(.arco-drawer-body) {
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.drawer {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: calc(100vh - 32px);
  padding: 20px 26px 26px;
  background: linear-gradient(180deg, #f8fbff, #f5f8fc);
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  padding: 8px 0 18px;
  overflow: visible;
}

.sticky {
  position: sticky;
  top: 0;
  z-index: 10;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.98), rgba(248, 251, 255, 0.94));
  backdrop-filter: blur(8px);
}

.drawer h2 {
  font-size: 34px;
  line-height: 1.2;
  padding-top: 4px;
}

.meta-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 18px;
}

.workspace {
  display: flex;
  gap: 20px;
  min-height: 0;
  flex: 1;
}

.side {
  display: flex;
  flex-direction: column;
  width: 340px;
  gap: 18px;
  min-height: 0;
}

.main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
  min-height: 0;
  flex: 1;
}

.pane {
  padding: 18px;
  border-radius: 22px;
}

.scroll {
  overflow: auto;
  min-height: 0;
}

.module {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--surface-border);
  border-radius: 18px;
  background: #fbfdff;
}

.module + .module {
  margin-top: 14px;
}

.module-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
  padding: 18px 20px;
}

.summary strong,
.sum strong {
  font-size: 30px;
  line-height: 1;
  color: var(--text-primary);
}

.tree {
  flex: 1;
  padding: 18px 22px;
}

.node {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 0;
}

.node-meta {
  display: flex;
  gap: 10px;
  color: var(--text-secondary);
  word-break: break-all;
}

.log-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.sum {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 20px;
}

.grow {
  flex: 1;
  min-height: 0;
}

.status {
  align-items: center;
}

.table-title span {
  font-weight: 700;
}

.actions :deep(.arco-btn) {
  border-radius: 999px;
}

:deep(.arco-card-body) {
  padding: 22px;
}

:deep(.arco-input-number) {
  width: 100%;
}

@media (max-width: 1280px) {
  .account-workbench,
  .authorization-hero,
  .authorization-layout,
  .authorization-summary-grid,
  .callback-hero,
  .callback-layout,
  .callback-summary-grid,
  .log-summary {
    grid-template-columns: 1fr;
  }

  .account-modal-footer,
  .toolbar,
  .whitelist-head,
  .drawer-header,
  .authorization-hero__actions,
  .callback-hero__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .authorization-hero__stats,
  .callback-hero__stats {
    grid-template-columns: 1fr;
  }

  .workspace {
    display: block;
  }

  .side {
    width: 100%;
    margin-bottom: 18px;
  }

  .authorization-side {
    order: 2;
  }

  .callback-side {
    order: 2;
  }
}

@media (max-width: 768px) {
  .page {
    padding: 16px;
  }

  .account-modal-title,
  .section-heading,
  .section-heading--row,
  .account-modal-footer,
  .callback-hero__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .authorization-hero h2,
  .callback-hero h2 {
    font-size: 28px;
  }

  .authorization-hero,
  .callback-hero {
    padding: 20px;
  }

  .query-panel__advanced-heading {
    flex-direction: column;
    align-items: flex-start;
  }

  .authorization-summary-grid,
  .authorization-hero__stats,
  .callback-summary-grid,
  .callback-hero__stats {
    grid-template-columns: 1fr;
  }

  .table-account-main {
    align-items: flex-start;
  }

  .actions,
  .meta-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

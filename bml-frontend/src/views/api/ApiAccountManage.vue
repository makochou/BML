<template>
  <div class="page">
    <GovernanceCompactQueryPanel
      class="query-panel"
      max-width="1260px"
      density="ultra"
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
        <div class="query-panel__mode-actions">
          <a-button
            type="primary"
            class="query-panel__mode-btn"
            :class="{ 'is-active': queryForm.textMatchMode === 'fuzzy', 'is-inactive': queryForm.textMatchMode !== 'fuzzy' }"
            @click="handleTextModeSearch('fuzzy')"
          >
            模糊查找
          </a-button>
          <a-button
            type="primary"
            class="query-panel__mode-btn"
            :class="{ 'is-active': queryForm.textMatchMode === 'exact', 'is-inactive': queryForm.textMatchMode !== 'exact' }"
            @click="handleTextModeSearch('exact')"
          >
            精确查找
          </a-button>
        </div>
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
      density="ultra"
    >
      <template #actions>
        <a-button :loading="syncingRegistry" @click="handleSyncRegistry">
          <template #icon><icon-sync /></template>
          同步接口目录
        </a-button>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><icon-plus /></template>
          新建账号
        </a-button>
      </template>
      <a-table class="account-table" size="small" row-key="id" :data="accountList" :loading="tableLoading" :pagination="paginationConfig" :scroll="{ x: 1320 }" @row-dblclick="handleAccountRowDblClick" @page-change="handlePageChange" @page-size-change="handlePageSizeChange">
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

    <a-modal
      v-model:visible="accountModal.visible"
      :fullscreen="accountModalViewport.fullscreen"
      :modal-style="accountModalStyle"
      :closable="false"
      :footer="false"
      align-center
      :modal-class="['account-modal', { 'account-modal--create': isCreateMode }]"
      :body-class="[
        'account-modal-body',
        { 'account-modal-body--create': isCreateMode, 'account-modal-body--fullscreen': accountModalViewport.fullscreen }
      ]"
      unmount-on-close
    >
      <template #title>
        <div class="account-modal-title">
          <div class="account-modal-title__main">
            <p>{{ accountModalTitleCaption }}</p>
            <strong>{{ accountModalTitle }}</strong>
          </div>
          <div class="account-modal-title__actions">
            <span class="account-modal-title__badge">{{ accountModalTitleBadge }}</span>
            <div class="account-modal-title__action-group">
              <a-tooltip :content="accountModalViewport.fullscreen ? '退出全屏' : '全屏展示'">
                <a-button
                  size="mini"
                  class="account-modal-title__action-btn"
                  @click.stop="toggleAccountModalFullscreen"
                >
                  <template #icon>
                    <component :is="accountModalViewport.fullscreen ? IconFullscreenExit : IconFullscreen" />
                  </template>
                </a-button>
              </a-tooltip>
              <a-tooltip content="关闭窗口">
                <a-button
                  size="mini"
                  class="account-modal-title__action-btn account-modal-title__action-btn--close"
                  @click.stop="closeAccountModal"
                >
                  <template #icon>
                    <icon-close />
                  </template>
                </a-button>
              </a-tooltip>
            </div>
          </div>
        </div>
      </template>

      <div
        class="account-modal-shell"
        :class="{ 'account-modal-shell--create': isCreateMode }"
      >
        <div v-if="isCreateMode" class="account-create-viewport">
          <div class="account-create-scroll-region">
            <a-form :model="accountForm" layout="vertical" class="account-create-form">
              <div class="account-create-stage">
                <div class="account-create-layout">
                  <div class="account-create-main">
                    <section class="account-create-form-shell">
                      <div class="account-create-form-shell__heading">
                        <div>
                          <p>Structured Fields</p>
                          <h4>账号开通表单</h4>
                          <span>字段仍然走统一 schema 驱动，方便后续扩展字段、校验规则与跨页面复用。</span>
                        </div>
                        <div class="account-create-form-shell__tags">
                          <span>统一校验</span>
                          <span>统一标准化</span>
                          <span>统一返回凭证</span>
                        </div>
                      </div>

                      <GovernanceFormSections :model="accountFormAsRecord" :sections="accountFormSections" />
                    </section>

                    <section class="account-create-form-shell account-create-form-shell--whitelist">
                      <div class="account-create-form-shell__heading">
                        <div>
                          <p>Environment Isolation</p>
                          <h4>环境白名单与调用边界</h4>
                          <span>测试、预发、生产分开治理，系统会根据当前接入环境自动命中对应白名单。</span>
                        </div>
                        <div class="account-create-form-shell__status">
                          <strong>{{ getAccessEnvironmentLabel(accountForm.accessEnvironment) }}</strong>
                          <small>当前默认生效环境</small>
                        </div>
                      </div>

                      <ApiEnvironmentWhitelistEditor
                        v-model="accountWhitelistModel"
                        :access-environment="accountForm.accessEnvironment"
                        :environment-options="environmentOptions"
                      />
                    </section>

                    <div class="account-create-footer">
                      <div class="account-create-footer__intro">
                        <strong>交付说明</strong>
                        <p>提交时会统一校验字段、标准化业务系统编码、去重白名单，并在创建成功后交付首份 AccessKey / SecretKey。</p>
                        <ul class="account-create-footer__list">
                          <li v-for="item in accountCreateFooterTips" :key="item">{{ item }}</li>
                        </ul>
                      </div>

                      <div class="account-create-footer__actions">
                        <a-button @click="resetAccountForm">重置表单</a-button>
                        <a-button @click="closeAccountModal">取消</a-button>
                        <a-button type="primary" :loading="accountModal.submitting" @click="submitAccountForm">
                          {{ accountModalSubmitText }}
                        </a-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </a-form>
          </div>
        </div>

        <a-form v-else :model="accountForm" layout="vertical">
          <GovernanceWorkbenchShell class="account-workbench" :eyebrow="accountModalTitleCaption" :title="accountWorkbenchTitle" :description="accountWorkbenchDescription" :tags="accountHeroTags" :stats="accountWorkbenchStats" theme="emerald" :hide-hero="isCreateMode">
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
                  <a-button @click="closeAccountModal">取消</a-button>
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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Message, Modal } from '@arco-design/web-vue';
import { IconClose, IconDown, IconFullscreen, IconFullscreenExit, IconPlus, IconSync, IconUp } from '@arco-design/web-vue/es/icon';
import { createApiAccount, deleteApiAccount, fetchApiAccountDetail, fetchApiAccountPage, fetchApiCallbackLogs, fetchAuthorizationSnapshot, resetApiAccountSecret, retryApiCallbackLog, saveAuthorization, syncOpenApiRegistry, triggerApiAccountTestCallback, updateApiAccount } from '../../api/apiAccount';
import ApiAuthorizationWorkbenchDrawer from '../../components/api-account/ApiAuthorizationWorkbenchDrawer.vue';
import ApiCallbackLogWorkbenchDrawer from '../../components/api-account/ApiCallbackLogWorkbenchDrawer.vue';
import ApiCredentialDeliveryModal from '../../components/api-account/ApiCredentialDeliveryModal.vue';
import ApiAccountPreviewModal from '../../components/api-account/ApiAccountPreviewModal.vue';
import ApiEnvironmentWhitelistEditor from '../../components/api-account/ApiEnvironmentWhitelistEditor.vue';
import GovernanceCompactQueryPanel from '../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceFormSections from '../../components/governance/GovernanceFormSections.vue';
import GovernanceListStage from '../../components/governance/GovernanceListStage.vue';
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
import type { FactCard, WorkbenchStatCard } from '../../types/governance';

type AccountModalMode = 'create' | 'edit';
type ManageRouteAction = 'edit' | 'authorization' | 'callback' | 'reset-secret';
type AuthorizationTreeNode = { key: string; title: string; nodeType: 'module' | 'controller' | 'api'; description?: string; httpMethod?: string; apiUrl?: string; disableCheckbox?: boolean; children?: AuthorizationTreeNode[] };
type AuthorizationModuleCard = { moduleName: string; apiCount: number; controllerCount: number; selectedCount: number };
type AccountColumnKind = 'account' | 'system' | 'access' | 'status' | 'updateTime' | 'actions';
type QueryTextMatchMode = 'fuzzy' | 'exact';
type AccountModalResizeDirection = 'n' | 's' | 'e' | 'w' | 'ne' | 'nw' | 'se' | 'sw' | '';
type AccountModalPointerAction = 'none' | 'drag' | 'resize';

const accountTypeOptions = [{ label: '内部账号', value: 1 }, { label: '外部账号', value: 2 }];
const clientTypeOptions = [{ label: 'Web前端', value: 'web' }, { label: 'H5页面', value: 'h5' }, { label: 'APP', value: 'app' }, { label: '小程序', value: 'mini_program' }, { label: '服务端', value: 'server' }, { label: '第三方系统', value: 'third_party' }, { label: '其他客户端', value: 'other' }];
const environmentOptions: { label: string; value: AccessEnvironment }[] = [{ label: '测试环境', value: 'test' }, { label: '预发环境', value: 'staging' }, { label: '生产环境', value: 'production' }];
const signVersionOptions = [{ label: 'v1（当前正式版）', value: 'v1' }];
const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }];
const route = useRoute();
const router = useRouter();

const queryForm = reactive({
  accountId: undefined as number | undefined,
  accountName: '',
  accessKey: '',
  ownerName: '',
  ownerContact: '',
  systemName: '',
  systemCode: '',
  systemKeyword: '',
  signVersion: undefined as string | undefined,
  callbackUrl: '',
  remark: '',
  ipKeyword: '',
  textMatchMode: 'fuzzy' as QueryTextMatchMode,
  status: undefined as number | undefined,
  accountType: undefined as number | undefined,
  clientType: undefined as string | undefined,
  accessEnvironment: undefined as AccessEnvironment | undefined,
  rateLimitMin: undefined as number | undefined,
  rateLimitMax: undefined as number | undefined,
  expireTimeStart: '',
  expireTimeEnd: '',
  createTimeStart: '',
  createTimeEnd: '',
  updateTimeStart: '',
  updateTimeEnd: ''
});
const queryAdvancedExpanded = ref(false);
const accountList = ref<ApiAccountItem[]>([]);
const tableLoading = ref(false);
const syncingRegistry = ref(false);
const pagination = reactive({ current: 1, pageSize: 10, total: 0 });
const accountModal = reactive({ visible: false, mode: 'create' as AccountModalMode, editingId: 0, submitting: false });
/**
 * 新建/编辑弹窗视口状态。
 * 统一管理宽高、位移和全屏状态，为后续其他治理弹窗复用“拖拽 + 缩放 + 全屏”能力提供基础。
 */
const accountModalViewport = reactive({
  width: 1160,
  height: 760,
  offsetX: 0,
  offsetY: 0,
  fullscreen: false
});
const accountModalPointerState = reactive({
  active: false,
  action: 'none' as AccountModalPointerAction,
  direction: '' as AccountModalResizeDirection,
  startClientX: 0,
  startClientY: 0,
  startWidth: 0,
  startHeight: 0,
  startOffsetX: 0,
  startOffsetY: 0
});
const accountModalElement = ref<HTMLElement | null>(null);
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
  statusOptions,
  signVersionOptions
});
// 查询条件按主次优先级拆分，首屏只展示高频项，低频项折叠后按需展开。
const {
  primarySections: queryPrimarySections,
  secondarySections: querySecondarySections,
} = splitGovernanceSectionsByPriority(queryFormSections);
const accountFormSections = computed(() => useApiAccountFormSchema({
  accountTypeOptions,
  clientTypeOptions,
  environmentOptions,
  signVersionOptions,
  statusOptions,
  // 新建弹窗采用紧凑多列布局，编辑弹窗保持可读性优先的标准布局。
  compactMode: accountModal.mode === 'create'
}).sections);
const { parseIpWhitelistInput, validateAndBuildPayload } = useApiAccountFormValidation();
// 新建/编辑弹窗的概览与指引均使用配置化数据，便于后续治理页复用同一套信息架构。
const accountHeroTags = ['账号主体统一纳管', '环境白名单自动生效', '签名与限流统一治理', '保存后配置即时生效'];
const accountGuideItems = ['业务系统编码仅支持 2-64 位字母、数字、下划线和中划线，保存时会自动转为大写。', '建议在联系方式中填写手机号、邮箱或企业微信，方便授权工单与异常联络。', '环境白名单建议按测试、预发、生产分开维护，避免不同环境来源 IP 相互污染。', '创建成功或重置密钥后，SecretKey 只会展示一次，请在首次返回时立即保存。'];
const accountCreateFooterTips = [
  '保存时自动完成业务系统编码标准化与白名单去重。',
  '当前接入环境会自动回填对应生效白名单条目。',
  '创建成功后请立即保存首份返回的 SecretKey，后续不会重复展示。'
] as const;
const accountModalTitle = computed(() => accountModal.mode === 'create' ? '新建API账号' : '编辑API账号');
const accountModalTitleCaption = computed(() => accountModal.mode === 'create' ? 'Open API Account Provisioning Workspace' : 'Open API Account Governance Workspace');
const accountModalTitleBadge = computed(() => accountModal.mode === 'create' ? '创建后将返回首份凭证' : '保存后配置即时生效');
// 新增模式用于控制弹窗的轻量布局（隐藏 Hero 与侧栏）。
const isCreateMode = computed(() => accountModal.mode === 'create');
const accountWorkbenchTitle = computed(() => accountModal.mode === 'create' ? '账号开通治理台' : '账号编辑治理台');
const accountWorkbenchDescription = computed(() => accountModal.mode === 'create' ? '统一录入账号主体、客户端范围、环境策略与回调配置，创建完成后系统立即交付首份访问凭证。' : '统一调整账号归属、安全策略、环境白名单与回调配置，保存后当前账号的调用能力会即时更新。');
const accountModeLabel = computed(() => accountModal.mode === 'create' ? '创建模式' : '编辑模式');
const accountModalSubmitText = computed(() => accountModal.mode === 'create' ? '创建账号' : '保存变更');
const accountModalStyle = computed<Record<string, string>>(() => {
  if (accountModalViewport.fullscreen) {
    return {} as Record<string, string>;
  }

  return {
    width: `${accountModalViewport.width}px`,
    height: `${accountModalViewport.height}px`,
    transform: `translate(${accountModalViewport.offsetX}px, ${accountModalViewport.offsetY}px)`
  };
});
const accountWorkbenchStats = computed<WorkbenchStatCard[]>(() => [{ label: '账号类型', value: getAccountTypeLabel(accountForm.accountType), hint: '区分内外部接入主体', tone: 'blue' }, { label: '客户端范围', value: accountForm.clientTypes.length ? `${accountForm.clientTypes.length} 类` : '未选择', hint: accountForm.clientTypes.length ? getClientTypeLabels(accountForm.clientTypes).join('、') : '至少选择一个客户端类型', tone: 'green' }, { label: '当前环境', value: getAccessEnvironmentLabel(accountForm.accessEnvironment), hint: `${getEnvironmentWhitelistCountLabel(accountForm.environmentIpWhitelistText[accountForm.accessEnvironment])} 生效条目`, tone: 'teal' }, { label: '回调配置', value: accountForm.callbackUrl.trim() ? '已配置' : '未配置', hint: accountForm.callbackUrl.trim() || '支持 http / https，可按需留空', tone: 'gold' }]);
// 在超紧凑模式下使用短文本，降低按钮占位，让底部操作区更聚焦。
const queryAdvancedToggleText = computed(() => queryAdvancedExpanded.value ? '收起条件' : '更多条件');
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
/**
 * 构建分页查询参数。
 * 统一处理空字符串归一化和默认值收口，避免请求层散落重复 trim 逻辑。
 */
function buildAccountPageQueryParams() {
  return {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    accountId: queryForm.accountId,
    accountName: normalizeQueryText(queryForm.accountName),
    accessKey: normalizeQueryText(queryForm.accessKey),
    ownerName: normalizeQueryText(queryForm.ownerName),
    ownerContact: normalizeQueryText(queryForm.ownerContact),
    systemName: normalizeQueryText(queryForm.systemName),
    systemCode: normalizeQueryText(queryForm.systemCode),
    systemKeyword: normalizeQueryText(queryForm.systemKeyword),
    signVersion: queryForm.signVersion,
    callbackUrl: normalizeQueryText(queryForm.callbackUrl),
    remark: normalizeQueryText(queryForm.remark),
    ipKeyword: normalizeQueryText(queryForm.ipKeyword),
    textMatchMode: queryForm.textMatchMode,
    status: queryForm.status,
    accountType: queryForm.accountType,
    clientType: queryForm.clientType,
    accessEnvironment: queryForm.accessEnvironment,
    rateLimitMin: queryForm.rateLimitMin,
    rateLimitMax: queryForm.rateLimitMax,
    expireTimeStart: normalizeQueryText(queryForm.expireTimeStart),
    expireTimeEnd: normalizeQueryText(queryForm.expireTimeEnd),
    createTimeStart: normalizeQueryText(queryForm.createTimeStart),
    createTimeEnd: normalizeQueryText(queryForm.createTimeEnd),
    updateTimeStart: normalizeQueryText(queryForm.updateTimeStart),
    updateTimeEnd: normalizeQueryText(queryForm.updateTimeEnd)
  };
}

async function loadPageData() {
  tableLoading.value = true;
  try {
    const { data } = await fetchApiAccountPage(buildAccountPageQueryParams());
    accountList.value = data.records || [];
    pagination.total = data.total || 0;
  } finally {
    tableLoading.value = false;
  }
}
async function refreshPage() { await loadPageData(); }
const ACCOUNT_MODAL_EDGE_HIT_SIZE = 18;
const ACCOUNT_MODAL_SAFE_MARGIN = 0;
const ACCOUNT_MODAL_MIN_WIDTH = 900;
const ACCOUNT_MODAL_MIN_HEIGHT = 520;
const ACCOUNT_MODAL_DEFAULT_HEIGHT = 700;
const ACCOUNT_MODAL_PAGE_LOCK_CLASS = 'account-modal-page-lock';
const ACCOUNT_MODAL_RESIZE_CURSOR_MAP: Record<AccountModalResizeDirection, string> = {
  n: 'ns-resize',
  s: 'ns-resize',
  e: 'ew-resize',
  w: 'ew-resize',
  ne: 'nesw-resize',
  nw: 'nwse-resize',
  se: 'nwse-resize',
  sw: 'nesw-resize',
  '': ''
};
function getAccountModalDefaultWidth(mode: AccountModalMode) {
  return mode === 'create' ? 1160 : 1240;
}
function getAccountModalWidthBounds() {
  const maxWidth = Math.max(420, window.innerWidth - ACCOUNT_MODAL_SAFE_MARGIN * 2);
  return {
    min: Math.min(ACCOUNT_MODAL_MIN_WIDTH, maxWidth),
    max: maxWidth
  };
}
function getAccountModalHeightBounds() {
  const maxHeight = Math.max(360, window.innerHeight - ACCOUNT_MODAL_SAFE_MARGIN * 2);
  return {
    min: Math.min(ACCOUNT_MODAL_MIN_HEIGHT, maxHeight),
    max: maxHeight
  };
}
function clampValue(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}
/**
 * 对弹窗宽高与位移进行统一约束。
 * 允许弹窗通过拖拽和缩放贴合浏览器四边，确保可直接拉满整个视口。
 */
function clampAccountModalViewport() {
  if (accountModalViewport.fullscreen) {
    return;
  }

  const widthBounds = getAccountModalWidthBounds();
  const heightBounds = getAccountModalHeightBounds();
  accountModalViewport.width = clampValue(accountModalViewport.width, widthBounds.min, widthBounds.max);
  accountModalViewport.height = clampValue(accountModalViewport.height, heightBounds.min, heightBounds.max);

  const maxOffsetX = (window.innerWidth - accountModalViewport.width) / 2 - ACCOUNT_MODAL_SAFE_MARGIN;
  const minOffsetX = -maxOffsetX;
  const maxOffsetY = (window.innerHeight - accountModalViewport.height) / 2 - ACCOUNT_MODAL_SAFE_MARGIN;
  const minOffsetY = -maxOffsetY;
  accountModalViewport.offsetX = clampValue(accountModalViewport.offsetX, minOffsetX, maxOffsetX);
  accountModalViewport.offsetY = clampValue(accountModalViewport.offsetY, minOffsetY, maxOffsetY);
}
/**
 * 新建/编辑打开前统一重置弹窗视口。
 * 以业务模式给出默认宽度，同时保留后续继续拖拽到全屏的空间。
 */
function resetAccountModalViewport(mode: AccountModalMode) {
  accountModalViewport.fullscreen = false;
  accountModalViewport.offsetX = 0;
  accountModalViewport.offsetY = 0;
  accountModalViewport.width = getAccountModalDefaultWidth(mode);
  accountModalViewport.height = ACCOUNT_MODAL_DEFAULT_HEIGHT;
  clampAccountModalViewport();
}
function resolveAccountModalResizeDirection(event: MouseEvent, rect: DOMRect): AccountModalResizeDirection {
  const nearLeft = event.clientX - rect.left <= ACCOUNT_MODAL_EDGE_HIT_SIZE;
  const nearRight = rect.right - event.clientX <= ACCOUNT_MODAL_EDGE_HIT_SIZE;
  const nearTop = event.clientY - rect.top <= ACCOUNT_MODAL_EDGE_HIT_SIZE;
  const nearBottom = rect.bottom - event.clientY <= ACCOUNT_MODAL_EDGE_HIT_SIZE;

  if (nearTop && nearLeft) return 'nw';
  if (nearTop && nearRight) return 'ne';
  if (nearBottom && nearLeft) return 'sw';
  if (nearBottom && nearRight) return 'se';
  if (nearTop) return 'n';
  if (nearBottom) return 's';
  if (nearLeft) return 'w';
  if (nearRight) return 'e';
  return '';
}
function updateAccountModalCursor(direction: AccountModalResizeDirection) {
  if (!accountModalElement.value || accountModalPointerState.active) {
    return;
  }
  accountModalElement.value.style.cursor = ACCOUNT_MODAL_RESIZE_CURSOR_MAP[direction] || '';
}
function resetAccountModalCursor() {
  if (!accountModalElement.value || accountModalPointerState.active) {
    return;
  }
  accountModalElement.value.style.cursor = '';
}
function isAccountModalHeaderDraggableTarget(target: EventTarget | null) {
  const element = target instanceof HTMLElement ? target : null;
  if (!element) return false;
  if (element.closest('.account-modal-title__action-btn')) return false;
  if (element.closest('.arco-modal-close-btn')) return false;
  return Boolean(element.closest('.arco-modal-header'));
}
function beginAccountModalPointerAction(event: MouseEvent, action: AccountModalPointerAction, direction: AccountModalResizeDirection = '') {
  if (event.button !== 0 || accountModalViewport.fullscreen) {
    return;
  }
  event.preventDefault();
  accountModalPointerState.active = true;
  accountModalPointerState.action = action;
  accountModalPointerState.direction = direction;
  accountModalPointerState.startClientX = event.clientX;
  accountModalPointerState.startClientY = event.clientY;
  accountModalPointerState.startWidth = accountModalViewport.width;
  accountModalPointerState.startHeight = accountModalViewport.height;
  accountModalPointerState.startOffsetX = accountModalViewport.offsetX;
  accountModalPointerState.startOffsetY = accountModalViewport.offsetY;
  document.body.style.userSelect = 'none';
  if (action === 'resize' && direction) {
    document.body.style.cursor = ACCOUNT_MODAL_RESIZE_CURSOR_MAP[direction] || 'default';
  } else if (action === 'drag') {
    document.body.style.cursor = 'move';
  }
  document.addEventListener('mousemove', handleAccountModalPointerMove);
  document.addEventListener('mouseup', handleAccountModalPointerUp);
}
function handleAccountModalMouseDown(event: MouseEvent) {
  if (!accountModalElement.value || accountModalViewport.fullscreen) {
    return;
  }

  const rect = accountModalElement.value.getBoundingClientRect();
  const direction = resolveAccountModalResizeDirection(event, rect);
  if (direction) {
    beginAccountModalPointerAction(event, 'resize', direction);
    return;
  }

  if (isAccountModalHeaderDraggableTarget(event.target)) {
    beginAccountModalPointerAction(event, 'drag');
  }
}
function handleAccountModalMouseMove(event: MouseEvent) {
  if (!accountModalElement.value || accountModalPointerState.active || accountModalViewport.fullscreen) {
    return;
  }

  const rect = accountModalElement.value.getBoundingClientRect();
  const direction = resolveAccountModalResizeDirection(event, rect);
  updateAccountModalCursor(direction);
}
function handleAccountModalPointerMove(event: MouseEvent) {
  if (!accountModalPointerState.active || accountModalViewport.fullscreen) {
    return;
  }

  const deltaX = event.clientX - accountModalPointerState.startClientX;
  const deltaY = event.clientY - accountModalPointerState.startClientY;
  const widthBounds = getAccountModalWidthBounds();
  const heightBounds = getAccountModalHeightBounds();

  if (accountModalPointerState.action === 'drag') {
    accountModalViewport.offsetX = accountModalPointerState.startOffsetX + deltaX;
    accountModalViewport.offsetY = accountModalPointerState.startOffsetY + deltaY;
    clampAccountModalViewport();
    return;
  }

  let nextWidth = accountModalPointerState.startWidth;
  let nextHeight = accountModalPointerState.startHeight;
  let nextOffsetX = accountModalPointerState.startOffsetX;
  let nextOffsetY = accountModalPointerState.startOffsetY;
  const direction = accountModalPointerState.direction;

  if (direction.includes('e')) {
    nextWidth = clampValue(accountModalPointerState.startWidth + deltaX, widthBounds.min, widthBounds.max);
  }
  if (direction.includes('s')) {
    nextHeight = clampValue(accountModalPointerState.startHeight + deltaY, heightBounds.min, heightBounds.max);
  }
  if (direction.includes('w')) {
    nextWidth = clampValue(accountModalPointerState.startWidth - deltaX, widthBounds.min, widthBounds.max);
    const widthDelta = accountModalPointerState.startWidth - nextWidth;
    nextOffsetX = accountModalPointerState.startOffsetX + widthDelta / 2;
  }
  if (direction.includes('n')) {
    nextHeight = clampValue(accountModalPointerState.startHeight - deltaY, heightBounds.min, heightBounds.max);
    const heightDelta = accountModalPointerState.startHeight - nextHeight;
    nextOffsetY = accountModalPointerState.startOffsetY + heightDelta / 2;
  }

  accountModalViewport.width = nextWidth;
  accountModalViewport.height = nextHeight;
  accountModalViewport.offsetX = nextOffsetX;
  accountModalViewport.offsetY = nextOffsetY;
  clampAccountModalViewport();
}
function stopAccountModalPointerAction() {
  if (!accountModalPointerState.active) {
    return;
  }
  accountModalPointerState.active = false;
  accountModalPointerState.action = 'none';
  accountModalPointerState.direction = '';
  document.body.style.userSelect = '';
  document.body.style.cursor = '';
  document.removeEventListener('mousemove', handleAccountModalPointerMove);
  document.removeEventListener('mouseup', handleAccountModalPointerUp);
  resetAccountModalCursor();
}
function handleAccountModalPointerUp() {
  stopAccountModalPointerAction();
}
function bindAccountModalInteractiveListeners() {
  const modal = document.querySelector('.account-modal') as HTMLElement | null;
  if (!modal) {
    return;
  }
  accountModalElement.value = modal;
  modal.addEventListener('mousedown', handleAccountModalMouseDown);
  modal.addEventListener('mousemove', handleAccountModalMouseMove);
  modal.addEventListener('mouseleave', resetAccountModalCursor);
}
function unbindAccountModalInteractiveListeners() {
  if (!accountModalElement.value) {
    return;
  }
  accountModalElement.value.removeEventListener('mousedown', handleAccountModalMouseDown);
  accountModalElement.value.removeEventListener('mousemove', handleAccountModalMouseMove);
  accountModalElement.value.removeEventListener('mouseleave', resetAccountModalCursor);
  accountModalElement.value = null;
}
function handleAccountModalWindowResize() {
  clampAccountModalViewport();
}
/**
 * 锁定页面滚动，只保留弹窗白卡自身滚动。
 * 这样用户滚动鼠标时，交互焦点始终停留在新增账号弹窗内，不会把背景页面一起带动。
 */
function syncAccountModalPageScrollLock(locked: boolean) {
  document.documentElement.classList.toggle(ACCOUNT_MODAL_PAGE_LOCK_CLASS, locked);
  document.body.classList.toggle(ACCOUNT_MODAL_PAGE_LOCK_CLASS, locked);
}
/**
 * 全屏模式切换。
 * 使用组件原生 fullscreen 能力，并保留“退出全屏后回到上一次尺寸/位置”的连续交互体验。
 */
function toggleAccountModalFullscreen() {
  stopAccountModalPointerAction();
  accountModalViewport.fullscreen = !accountModalViewport.fullscreen;
  if (!accountModalViewport.fullscreen) {
    clampAccountModalViewport();
  }
}
/**
 * 统一关闭账号治理弹窗。
 * 将关闭入口收口到同一方法，避免标题栏、底部按钮等多个入口后续出现行为分叉。
 */
function closeAccountModal() {
  stopAccountModalPointerAction();
  accountModal.visible = false;
}
function resetAccountForm() { Object.assign(accountForm, { accountName: '', ownerName: '', ownerContact: '', systemName: '', systemCode: '', accountType: 2, clientTypes: [], accessEnvironment: 'production', signVersion: 'v1', environmentIpWhitelistText: createEmptyEnvironmentWhitelistText(), callbackUrl: '', rateLimit: 1000, expireTime: null, status: 1, remark: '' }); }
function fillAccountForm(detail: ApiAccountDetail) { Object.assign(accountForm, { accountName: detail.accountName, ownerName: detail.ownerName, ownerContact: detail.ownerContact, systemName: detail.systemName, systemCode: detail.systemCode, accountType: detail.accountType, clientTypes: detail.clientTypes || [], accessEnvironment: (detail.accessEnvironment as AccessEnvironment) || 'production', signVersion: detail.signVersion || 'v1', environmentIpWhitelistText: buildEnvironmentWhitelistTextMap(detail.environmentIpWhitelist, detail.ipWhitelist, detail.accessEnvironment as AccessEnvironment | undefined), callbackUrl: detail.callbackUrl || '', rateLimit: detail.rateLimit || 1000, expireTime: detail.expireTime || null, status: detail.status, remark: detail.remark || '' }); }
function openCreateModal() {
  accountModal.mode = 'create';
  accountModal.editingId = 0;
  resetAccountForm();
  resetAccountModalViewport('create');
  accountModal.visible = true;
}
async function openEditModalById(accountId: number) {
  const { data } = await fetchApiAccountDetail(accountId);
  accountModal.mode = 'edit';
  accountModal.editingId = accountId;
  fillAccountForm(data);
  resetAccountModalViewport('edit');
  accountModal.visible = true;
}
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
/**
 * 设置字符匹配模式。
 * 统一使用按钮切换模糊/精准模式，保证所有输入型查询字段匹配行为一致。
 */
function setQueryTextMatchMode(mode: QueryTextMatchMode) {
  queryForm.textMatchMode = mode;
}
/**
 * 字符匹配查询动作。
 * 点击“模糊查找 / 精确查找”按钮时，同时切换匹配模式并立即执行查询。
 */
async function handleTextModeSearch(mode: QueryTextMatchMode) {
  setQueryTextMatchMode(mode);
  await handleSearch();
}
async function handleResetSearch() {
  Object.assign(queryForm, {
    accountId: undefined,
    accountName: '',
    accessKey: '',
    ownerName: '',
    ownerContact: '',
    systemName: '',
    systemCode: '',
    systemKeyword: '',
    signVersion: undefined,
    callbackUrl: '',
    remark: '',
    ipKeyword: '',
    textMatchMode: 'fuzzy',
    status: undefined,
    accountType: undefined,
    clientType: undefined,
    accessEnvironment: undefined,
    rateLimitMin: undefined,
    rateLimitMax: undefined,
    expireTimeStart: '',
    expireTimeEnd: '',
    createTimeStart: '',
    createTimeEnd: '',
    updateTimeStart: '',
    updateTimeEnd: ''
  });
  queryAdvancedExpanded.value = false;
  pagination.current = 1;
  await loadPageData();
}
async function handlePageChange(page: number) { pagination.current = page; await loadPageData(); }
async function handlePageSizeChange(pageSize: number) { pagination.pageSize = pageSize; pagination.current = 1; await loadPageData(); }
async function handleSyncRegistry() { syncingRegistry.value = true; try { const { data } = await syncOpenApiRegistry(); Message.success(`项目接口目录同步完成，本次发现 ${data.totalDiscovered} 个可授权接口`); await refreshPage(); if (authorizationDrawer.visible && authorizationDrawer.accountId) await loadAuthorizationSnapshot(authorizationDrawer.accountId); } finally { syncingRegistry.value = false; } }
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
function normalizeQueryText(value: string) {
  const normalized = value.trim();
  return normalized ? normalized : undefined;
}
function buildApiKey(apiId: number) { return `api:${apiId}`; }
function parseCheckedApiIds(keys: string[]) { return keys.filter(key => key.startsWith('api:')).map(key => Number(key.slice(4))).filter(id => Number.isFinite(id)); }
function collectGroupApiIds(group: OpenApiGroupNode) { return group.controllers.flatMap(controller => controller.apis.map(api => api.id)); }
function collectApiIdsFromFilteredGroups(groups: OpenApiGroupNode[]) { return groups.flatMap(group => collectGroupApiIds(group)); }
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
watch(() => accountModal.visible, async visible => {
  if (visible) {
    syncAccountModalPageScrollLock(true);
    clampAccountModalViewport();
    await nextTick();
    bindAccountModalInteractiveListeners();
    window.addEventListener('resize', handleAccountModalWindowResize);
    return;
  }

  syncAccountModalPageScrollLock(false);
  stopAccountModalPointerAction();
  unbindAccountModalInteractiveListeners();
  window.removeEventListener('resize', handleAccountModalWindowResize);
});
onBeforeUnmount(() => {
  syncAccountModalPageScrollLock(false);
  stopAccountModalPointerAction();
  unbindAccountModalInteractiveListeners();
  window.removeEventListener('resize', handleAccountModalWindowResize);
});
</script>

<style scoped>
:global(html.account-modal-page-lock),
:global(body.account-modal-page-lock) {
  overflow: hidden;
}

.page {
  --page-bg-start: #f4f7fb;
  --page-bg-end: #f8fafc;
  --surface-color: #ffffff;
  --surface-border: #e5edf6;
  --text-primary: #0f172a;
  --text-secondary: #64748b;
  /* 查询卡片与列表卡片间距变量：统一收口为可配置项，便于后续页面直接复用同一套垂直节奏。 */
  --api-account-query-offset-y: -8px;
  --api-account-stage-gap-y: 8px;
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
  /* 查询区超紧凑变量覆盖：基于通用组件变量二次收口，避免散落硬编码。 */
  --query-panel-padding: 6px;
  --query-panel-radius: 14px;
  --query-panel-body-margin-top: 4px;
  --query-panel-body-padding: 5px 6px 0;
  --query-panel-body-radius: 10px;
  --query-panel-footer-margin-top: 3px;
  --query-panel-footer-gap: 6px;
  --query-panel-field-gap-x: 5px;
  --query-panel-form-item-gap: 1px;
  --query-panel-label-size: 11px;
  --query-panel-input-min-height: 28px;
  --query-panel-button-min-width: 78px;
  --query-panel-button-height: 28px;
  --query-panel-button-padding-inline: 8px;
  margin-top: var(--api-account-query-offset-y);
}

.query-panel__advanced {
  margin-top: 4px;
}

.query-panel__advanced-shell {
  padding: 6px 8px 0;
  border-radius: 10px;
  border: 1px dashed rgba(186, 200, 218, 0.9);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(246, 250, 255, 0.94));
}

.query-panel__advanced-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.query-panel__advanced-heading span {
  color: var(--text-primary);
  font-size: 11px;
  font-weight: 700;
}

.query-panel__toggle-btn {
  min-width: 96px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  border-color: rgba(211, 221, 234, 0.96);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(246, 250, 255, 0.96));
  color: #475569;
  font-weight: 700;
  font-size: 11px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.94);
}

.query-panel__mode-actions {
  display: inline-flex;
  gap: 6px;
  margin-right: 2px;
}

.query-panel__mode-btn {
  min-width: 84px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.query-panel__mode-btn.is-active {
  opacity: 1;
}

.query-panel__mode-btn.is-inactive {
  opacity: 0.72;
  box-shadow: none;
}

.query-advanced-fold-enter-active,
.query-advanced-fold-leave-active {
  transition: all 0.2s ease;
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
  margin-top: var(--api-account-stage-gap-y);
}

.table-account-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-width: 0;
}

.table-account-main__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.table-compact-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.table-compact-cell--inline {
  gap: 4px;
}

.table-tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.table-status-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.table-account-main strong,
.table-compact-cell strong {
  color: var(--text-primary);
}

.table-account-main strong {
  font-size: 16px;
  line-height: 1.2;
}

.table-status-cell strong {
  color: var(--text-primary);
}

.table-account-main small,
.table-compact-cell small,
.table-status-cell small {
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.5;
}

.table-row-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.table-action-button {
  min-width: 56px;
  height: 26px;
  padding: 0 9px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 12px;
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
  border-radius: 12px;
  border: 1px solid rgba(220, 229, 240, 0.94);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.96);
}

.account-table :deep(.arco-table) {
  background: transparent;
}

.account-table :deep(.arco-table-th) {
  padding-top: 8px;
  padding-bottom: 8px;
  background: linear-gradient(180deg, rgba(246, 250, 255, 0.98), rgba(239, 245, 251, 0.98));
  color: #334155;
  font-weight: 700;
  font-size: 12px;
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
  padding-top: 8px;
  padding-bottom: 8px;
  vertical-align: middle;
  background: rgba(255, 255, 255, 0.76);
}

.account-table :deep(.arco-table-fixed-right) {
  box-shadow: -8px 0 18px rgba(148, 163, 184, 0.08);
}

.account-table :deep(.arco-tag) {
  height: 20px;
  line-height: 20px;
  padding: 0 7px;
  font-size: 11px;
}

.account-table :deep(.arco-pagination-item),
.account-table :deep(.arco-pagination-jumper-input),
.account-table :deep(.arco-pagination-btn) {
  border-radius: 999px;
}

.account-table :deep(.arco-pagination) {
  padding-top: 8px;
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
  justify-content: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(23, 105, 255, 0.08), rgba(18, 184, 166, 0.12));
  color: #0f766e;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  text-align: center;
}

.account-modal-title__actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  max-width: min(100%, 420px);
}

.account-modal-title__action-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.94);
}

.account-modal-title__action-btn {
  width: 32px;
  min-width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 999px;
  border-color: rgba(203, 213, 225, 0.9);
  color: #334155;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
}

.account-modal-title__action-btn--close {
  color: #b42318;
  border-color: rgba(254, 205, 211, 0.96);
  background: linear-gradient(180deg, #fff5f5, #fff1f2);
}

.account-modal-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  height: 100%;
  min-height: 0;
}

.account-modal-shell--create {
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.account-create-viewport {
  display: flex;
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

/**
 * 新建态固定弹窗背景布，只允许内部滚动承载层上下滚动。
 * 后续如果继续追加字段或卡片，优先放入该滚动承载层，不要再直接把内容挂到弹窗 body 上。
 */
.account-create-scroll-region {
  flex: 1;
  height: 100%;
  min-height: 0;
  padding: 8px 0 24px;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.78) transparent;
}

.account-create-scroll-region::-webkit-scrollbar {
  width: 10px;
}

.account-create-scroll-region::-webkit-scrollbar-track {
  background: transparent;
}

.account-create-scroll-region::-webkit-scrollbar-thumb {
  border: 2px solid transparent;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.92), rgba(100, 116, 139, 0.92));
  background-clip: padding-box;
}

.account-create-scroll-region::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(100, 116, 139, 0.96), rgba(71, 85, 105, 0.96));
  background-clip: padding-box;
}

.account-create-form {
  display: block;
  width: 100%;
  min-height: max-content;
}

.account-create-stage {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;
  min-height: max-content;
  padding-bottom: 8px;
}

.account-create-layout {
  display: flex;
  flex-direction: column;
  flex: 0 0 auto;
  gap: 18px;
  min-width: 0;
  min-height: max-content;
}

.account-create-form-shell {
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.84);
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(23, 105, 255, 0.05), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.08);
}

.account-create-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
  min-height: 0;
}

.account-create-footer__list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.account-create-footer__list li {
  position: relative;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

.account-create-footer__list li::before {
  position: absolute;
  left: 0;
  top: 10px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  content: '';
}

.account-create-form-shell {
  padding: 22px;
}

.account-create-form-shell__heading {
  margin-bottom: 18px;
}

.account-create-form-shell__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.account-create-form-shell__tags span {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(23, 105, 255, 0.08), rgba(18, 184, 166, 0.08));
  color: #0f5de2;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.account-create-form-shell__status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 20px;
  background: rgba(15, 118, 110, 0.06);
}

.account-create-form-shell__status strong {
  color: #0f766e;
  font-size: 16px;
}

.account-create-form-shell__status small {
  color: #64748b;
}

.account-create-main :deep(.governance-form-sections) {
  gap: 16px;
}

.account-create-main :deep(.governance-form-panel) {
  border-radius: 24px;
  border-color: rgba(226, 232, 240, 0.88);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
}

.account-create-main :deep(.governance-form-panel__fields.layout-grid) {
  gap: 0 14px;
}

.account-create-main :deep(.governance-form-panel__heading h3) {
  font-size: 18px;
}

.account-create-main :deep(.governance-form-panel__heading p) {
  font-size: 13px;
}

.account-create-main :deep(.governance-form-panel__helper) {
  font-size: 12px;
}

.account-create-main :deep(.api-environment-whitelist-editor) {
  padding: 0;
  border: 0;
  box-shadow: none;
  background: transparent;
  backdrop-filter: none;
}

.account-create-footer {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(14px);
}

.account-create-footer__intro {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.account-create-footer__intro strong {
  color: #0f172a;
  font-size: 16px;
}

.account-create-footer__intro p {
  margin: 0;
  color: #64748b;
  line-height: 1.8;
}

.account-create-footer__list {
  display: grid;
  gap: 8px;
}

.account-create-footer__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.account-create-footer__actions :deep(.arco-btn) {
  min-width: 112px;
  border-radius: 999px;
}

.account-create-footer__actions :deep(.arco-btn-primary) {
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 14px 28px rgba(23, 105, 255, 0.18);
}

.account-modal-shell :deep(.arco-form) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

/* 统一控制弹窗可视高度与滚动行为，保证窗口上下保留安全留白，不贴浏览器边缘。 */
:deep(.account-modal) {
  display: inline-grid;
  grid-template-rows: auto minmax(0, 1fr);
  box-sizing: border-box;
  width: min(1240px, calc(100vw - 144px));
  height: min(700px, calc(100vh - 144px));
  max-width: calc(100vw - 144px);
  max-height: calc(100vh - 144px);
  min-height: 520px;
  border-radius: 22px;
  overflow: hidden;
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.2);
}

:deep(.account-modal.account-modal--create) {
  border: 1px solid rgba(59, 130, 246, 0.32);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.56),
    0 28px 72px rgba(15, 23, 42, 0.22);
}

:deep(.account-modal.account-modal--create)::before {
  position: absolute;
  inset: 12px;
  content: '';
  border: 1px solid rgba(45, 212, 191, 0.2);
  border-radius: 18px;
  pointer-events: none;
}

:deep(.account-modal.account-modal--create)::after {
  position: absolute;
  inset: 12px;
  content: '';
  background:
    linear-gradient(rgba(59, 130, 246, 0.92), rgba(59, 130, 246, 0.92)) left top / 16px 2px no-repeat,
    linear-gradient(rgba(59, 130, 246, 0.92), rgba(59, 130, 246, 0.92)) left top / 2px 16px no-repeat,
    linear-gradient(rgba(45, 212, 191, 0.92), rgba(45, 212, 191, 0.92)) right top / 16px 2px no-repeat,
    linear-gradient(rgba(45, 212, 191, 0.92), rgba(45, 212, 191, 0.92)) right top / 2px 16px no-repeat,
    linear-gradient(rgba(59, 130, 246, 0.92), rgba(59, 130, 246, 0.92)) left bottom / 16px 2px no-repeat,
    linear-gradient(rgba(59, 130, 246, 0.92), rgba(59, 130, 246, 0.92)) left bottom / 2px 16px no-repeat,
    linear-gradient(rgba(45, 212, 191, 0.92), rgba(45, 212, 191, 0.92)) right bottom / 16px 2px no-repeat,
    linear-gradient(rgba(45, 212, 191, 0.92), rgba(45, 212, 191, 0.92)) right bottom / 2px 16px no-repeat;
  pointer-events: none;
}

:deep(.account-modal.account-modal--create:hover) {
  border-color: rgba(59, 130, 246, 0.52);
}

:deep(.account-modal.arco-modal-fullscreen) {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  border-radius: 0;
}

:deep(.account-modal .arco-modal-header) {
  min-height: 72px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.82);
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(248, 250, 252, 0.92)),
    radial-gradient(circle at top right, rgba(18, 184, 166, 0.08), transparent 38%);
  cursor: move;
}

:deep(.account-modal.arco-modal-fullscreen .arco-modal-header) {
  cursor: default;
}

:deep(.account-modal .arco-modal-close-btn),
:deep(.account-modal .account-modal-title__action-btn) {
  cursor: pointer;
}

:deep(.account-modal-body) {
  display: flex;
  flex: 1;
  box-sizing: border-box;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  padding: 10px 20px 20px;
  overflow-y: scroll;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.78) transparent;
}

/**
 * 统一使用弹窗白卡内容区承载纵向滚动。
 * 这样用户只要在卡片上滚动鼠标，就能继续查看所有后续字段与分区内容。
 */
:deep(.account-modal-body::-webkit-scrollbar) {
  width: 10px;
}

:deep(.account-modal-body::-webkit-scrollbar-track) {
  background: transparent;
}

:deep(.account-modal-body::-webkit-scrollbar-thumb) {
  border: 2px solid transparent;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.92), rgba(100, 116, 139, 0.92));
  background-clip: padding-box;
}

:deep(.account-modal-body::-webkit-scrollbar-thumb:hover) {
  background: linear-gradient(180deg, rgba(100, 116, 139, 0.96), rgba(71, 85, 105, 0.96));
  background-clip: padding-box;
}

:deep(.account-modal-body--create) {
  /* 新建态背景布保持固定，内部内容滚动交给 account-create-scroll-region 统一承载。 */
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  min-height: 0;
  padding: 0 20px 0;
  overflow: hidden;
}

:deep(.account-modal-body--fullscreen) {
  padding: 10px 18px 16px;
}

/* 弹窗主体拆成概览侧栏与表单主区，兼顾信息密度和首次浏览体验。 */
.account-workbench {
  min-height: max-content;
  gap: 20px;
}

.account-aside,
.account-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 新建模式使用紧凑排版：缩小区块留白、减少字段间距，并保留响应式降级。 */
.account-main--create {
  gap: 12px;
}

.account-main--create :deep(.governance-form-sections) {
  gap: 12px;
}

.account-main--create :deep(.governance-form-panel) {
  padding: 16px 18px;
  border-radius: 20px;
}

.account-main--create :deep(.governance-form-panel__heading) {
  margin-bottom: 10px;
}

.account-main--create :deep(.governance-form-panel__heading h3) {
  font-size: 16px;
}

.account-main--create :deep(.governance-form-panel__heading p) {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
}

.account-main--create :deep(.governance-form-panel__fields.layout-grid) {
  gap: 0 12px;
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
  padding: 16px 18px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12px);
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

.account-modal-shell :deep(.governance-workbench__footer) {
  position: sticky;
  bottom: 0;
  z-index: 3;
  margin-top: 4px;
  padding-top: 12px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0), rgba(248, 250, 252, 0.9) 24%, rgba(248, 250, 252, 0.98));
}

.account-modal-shell--create :deep(.governance-workbench) {
  gap: 12px;
}

.account-modal-shell--create :deep(.governance-workbench__footer) {
  margin-top: 2px;
}

.account-modal-shell--create :deep(.arco-form-item) {
  margin-bottom: 10px;
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
  .account-create-footer,
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
    --api-account-query-offset-y: 0;
    --api-account-stage-gap-y: 8px;
    padding: 16px;
  }

  :deep(.account-modal) {
    width: calc(100vw - 24px) !important;
    max-width: calc(100vw - 24px);
    min-height: 0;
    border-radius: 20px;
  }

  :deep(.account-modal-body) {
    padding: 8px 14px 14px;
    max-height: calc(100vh - 120px);
  }

  :deep(.account-modal-body--create) {
    padding: 0 14px 0;
    max-height: calc(100vh - 120px);
  }

  .account-create-scroll-region {
    padding: 8px 0 14px;
  }

  .account-modal-title,
  .section-heading,
  .section-heading--row,
  .account-create-form-shell__heading,
  .account-modal-footer,
  .account-create-footer,
  .callback-hero__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .account-modal-title__actions {
    width: 100%;
    max-width: 100%;
    justify-content: space-between;
  }

  .account-modal-title__badge {
    flex: 1 1 100%;
  }

  .account-modal-title__action-group {
    margin-left: auto;
  }

  .account-create-footer__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .account-create-form-shell__tags {
    justify-content: flex-start;
  }

  .account-create-form-shell__status {
    align-items: flex-start;
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

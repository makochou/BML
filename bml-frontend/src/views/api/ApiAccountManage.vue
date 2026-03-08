<template>
  <div ref="apiAccountPageRef" class="page api-account-page">
    <GovernanceCompactQueryPanel class="query-panel" :max-width="accountWorkspaceMaxWidth" density="ultra"
      theme="aurora">
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
          <a-button type="primary" class="query-panel__mode-btn"
            :class="{ 'is-active': queryForm.textMatchMode === 'fuzzy', 'is-inactive': queryForm.textMatchMode !== 'fuzzy' }"
            @click="handleTextModeSearch('fuzzy')">
            模糊查找
          </a-button>
          <a-button type="primary" class="query-panel__mode-btn"
            :class="{ 'is-active': queryForm.textMatchMode === 'exact', 'is-inactive': queryForm.textMatchMode !== 'exact' }"
            @click="handleTextModeSearch('exact')">
            精确查找
          </a-button>
        </div>
        <a-button @click="handleResetSearch">重置条件</a-button>
      </template>

      <a-form :model="queryForm" layout="vertical" class="query-form">
        <GovernanceFormSections :model="queryFormAsRecord" :sections="queryPrimarySections" variant="embedded"
          label-layout="inline" />

        <transition name="query-advanced-fold">
          <div v-if="queryAdvancedExpanded && querySecondarySections.length" class="query-panel__advanced">
            <GovernanceFormSections :model="queryFormAsRecord" :sections="querySecondarySections" variant="embedded"
              label-layout="inline" />
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage class="table-shell" :max-width="accountWorkspaceMaxWidth" density="ultra" body-fill>
      <template #actions>
        <a-button :loading="syncingRegistry" @click="handleSyncRegistry">
          <template #icon><icon-sync /></template>
          同步接口目录
        </a-button>
        <a-popover trigger="click" position="bl" :popup-style="{ padding: '0' }">
          <a-button class="table-column-setting-btn">
            <template #icon><icon-settings /></template>
            列设置
          </a-button>
          <template #content>
            <div class="table-column-setting-panel">
              <div class="table-column-setting-panel__head">
                <strong>字段显示与顺序</strong>
                <a-link @click="resetAccountTableColumnLayout">恢复默认</a-link>
              </div>
              <div class="table-column-setting-panel__list">
                <div v-for="item in accountTableColumnSettingItems" :key="item.kind"
                  class="table-column-setting-panel__item" :class="{
                    'is-draggable': !item.locked,
                    'is-drag-source': columnSettingDragState.draggingKind === item.kind,
                    'is-drag-over-before': columnSettingDragState.overKind === item.kind && columnSettingDragState.dropPosition === 'before',
                    'is-drag-over-after': columnSettingDragState.overKind === item.kind && columnSettingDragState.dropPosition === 'after'
                  }" @dragover="handleColumnSettingDragOver(item.kind, $event)"
                  @drop="handleColumnSettingDrop(item.kind, $event)">
                  <div class="table-column-setting-panel__label">
                    <span v-if="!item.locked" class="table-column-setting-panel__drag-handle" draggable="true"
                      title="拖动调整字段顺序" @dragstart="handleColumnSettingDragStart(item.kind, $event)"
                      @dragend="handleColumnSettingDragEnd">
                      <icon-drag-arrow />
                    </span>
                    <span>{{ item.title }}</span>
                    <small v-if="item.locked">固定</small>
                  </div>
                  <div class="table-column-setting-panel__actions">
                    <a-button size="mini" class="table-column-setting-panel__order-btn" :disabled="item.moveUpDisabled"
                      @click="moveAccountTableColumn(item.kind, -1)">
                      <template #icon><icon-up /></template>
                    </a-button>
                    <a-button size="mini" class="table-column-setting-panel__order-btn"
                      :disabled="item.moveDownDisabled" @click="moveAccountTableColumn(item.kind, 1)">
                      <template #icon><icon-down /></template>
                    </a-button>
                    <a-tooltip :content="item.fixedFront ? '取消前置固定' : '固定在前列（左侧）'">
                      <a-button size="mini" class="table-column-setting-panel__fixed-btn"
                        :class="{ 'is-active': item.fixedFront }" :disabled="item.fixedFrontDisabled"
                        @click="toggleAccountColumnFixedFront(item.kind)">
                        <template #icon><icon-pushpin /></template>
                      </a-button>
                    </a-tooltip>
                    <a-switch size="small" :model-value="item.visible" :disabled="item.locked"
                      @change="handleAccountColumnVisibilityChange(item.kind, Boolean($event))" />
                  </div>
                </div>
              </div>
            </div>
          </template>
        </a-popover>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><icon-plus /></template>
          新建账号
        </a-button>
      </template>
      <div class="table-shell__split">
        <div ref="accountTableListRegionRef" class="table-shell__list-region">
          <a-table :key="`account-table-${accountTableRenderVersion}`" class="account-table" size="small" row-key="id"
            :data="accountList" :loading="tableLoading" :pagination="false"
            v-model:selected-keys="selectedKeys" :row-selection="rowSelection"
            :scroll="{ x: accountTableScrollX, y: '100%' }" :scrollbar="true" sticky-header column-resizable
            :columns="accountTableColumns"
            :row-class="(record: ApiAccountItem) => record.id === activeRowId ? 'is-row-active' : ''"
            @row-click="(record: ApiAccountItem) => activeRowId = record.id"
            @column-resize="handleAccountColumnResize" @row-dblclick="handleAccountRowDblClick">
            <!-- 序号列 -->
            <template #index="{ rowIndex }">
              <div class="table-field-cell table-field-cell--index">
                {{ (accountTablePagination.current - 1) * accountTablePagination.pageSize + rowIndex + 1 }}
              </div>
            </template>

            <!-- 账号名称 -->
            <template #accountName="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText :text="getPlainText(record, 'accountName', '-')" variant="primary" />
              </div>
            </template>

            <!-- 状态 -->
            <template #status="{ record }">
              <div class="table-field-cell">
                <a-tag :color="record.status === 1 ? 'green' : 'red'">
                  {{ getStatusLabel(record.status) }}
                </a-tag>
              </div>
            </template>

            <!-- 账号类型 -->
            <template #accountType="{ record }">
              <div class="table-field-cell">
                <a-tag :color="getAccountTypeTagColor(record.accountType)">
                  {{ getAccountTypeLabel(record.accountType) }}
                </a-tag>
              </div>
            </template>

            <!-- 业务系统 -->
            <template #systemName="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText :text="record.systemName || '-'" />
              </div>
            </template>

            <!-- 负责人 -->
            <template #ownerName="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText :text="record.ownerName || '-'" />
              </div>
            </template>

            <!-- 接入环境 -->
            <template #accessEnvironment="{ record }">
              <div class="table-field-cell">
                <a-tag :color="getEnvironmentTagColor(record.accessEnvironment)">
                  {{ getAccessEnvironmentLabel(record.accessEnvironment) }}
                </a-tag>
              </div>
            </template>

            <!-- 授权数 -->
            <template #authorizedCount="{ record }">
              <div class="table-field-cell">
                <span class="table-field-cell__text">{{ record.authorizedApiCount || 0 }}</span>
              </div>
            </template>

            <!-- 客户端类型 -->
            <template #clientTypes="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText :text="getCompactClientSummary(record.clientTypes)"
                  :tooltip-text="getFullClientTypeSummary(record.clientTypes)" />
              </div>
            </template>

            <!-- 权限范围 -->
            <template #allowedScopes="{ record }">
              <div class="table-field-cell">
                <template v-if="record.allowedScopes && record.allowedScopes.length">
                  <a-tag v-for="scope in record.allowedScopes.slice(0, 3)" :key="scope" size="small" color="arcoblue"
                    style="margin: 1px 2px;">{{ scope }}</a-tag>
                  <a-tag v-if="record.allowedScopes.length > 3" size="small" color="gray" style="margin: 1px 2px;">+{{
                    record.allowedScopes.length - 3 }}</a-tag>
                </template>
                <span v-else class="table-field-cell__text">-</span>
              </div>
            </template>

            <!-- 访问密钥 -->
            <template #accessKey="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText
                  :text="record.accessKey ? `${record.accessKey.substring(0, 8)}****${record.accessKey.substring(record.accessKey.length - 4)}` : '-'"
                  variant="mono" />
              </div>
            </template>

            <!-- 最近更新 / 创建时间 / 过期时间 -->
            <template #updateTime="{ record }">
              <div class="table-field-cell table-field-cell--time">
                <span class="table-field-cell__mono">{{ record.updateTime || '-' }}</span>
              </div>
            </template>
            <template #createTime="{ record }">
              <div class="table-field-cell table-field-cell--time">
                <span class="table-field-cell__mono">{{ record.createTime || '-' }}</span>
              </div>
            </template>
            <template #expireTime="{ record }">
              <div class="table-field-cell table-field-cell--time">
                <span class="table-field-cell__mono">{{ record.expireTime || '永久有效' }}</span>
              </div>
            </template>

            <!-- 操作列 -->
            <template #actions="{ record }">
              <div class="table-row-actions" @click.stop @dblclick.stop>
                <a-button type="primary" size="mini" class="table-action-btn table-action-btn--primary"
                  @click="handleEditAccount(record)">
                  <template #icon><icon-edit /></template>
                  编辑
                </a-button>
                <a-button type="primary" size="mini" class="table-action-btn table-action-btn--primary"
                  @click="handleAuthorizeAccount(record)">
                  <template #icon><icon-safe /></template>
                  授权
                </a-button>
                <a-dropdown trigger="click" position="br">
                  <a-button size="mini" class="table-action-btn table-action-btn--more">
                    <template #icon><icon-more /></template>
                  </a-button>
                  <template #content>
                    <a-doption @click="handleCopyAccount(record)">复制</a-doption>
                    <a-doption @click="handleCallbackLogAccount(record)">回调日志</a-doption>
                    <a-doption @click="confirmResetSecret(record)">重置密钥</a-doption>
                    <a-doption class="table-row-actions__danger" @click="confirmDeleteAccount(record)">删除账号</a-doption>
                  </template>
                </a-dropdown>
              </div>
            </template>

            <!-- 账号ID -->
            <template #accountId="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText :text="record.id ? `#${record.id}` : '-'" variant="mono" />
              </div>
            </template>

            <!-- 系统编码 -->
            <template #systemCode="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText :text="record.systemCode || '-'" variant="mono" />
              </div>
            </template>

            <!-- 负责人联系方式 -->
            <template #ownerContact="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText :text="record.ownerContact || '-'" />
              </div>
            </template>

            <!-- 签名版本 -->
            <template #signVersion="{ record }">
              <div class="table-field-cell">
                <a-tag size="small">{{ record.signVersion || '-' }}</a-tag>
              </div>
            </template>

            <!-- 限流配置 -->
            <template #rateLimit="{ record }">
              <div class="table-field-cell">
                <span class="table-field-cell__text">{{ record.rateLimit }} QPS</span>
              </div>
            </template>

            <!-- 回调地址 -->
            <template #callbackUrl="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText :text="record.callbackUrl || '-'" />
              </div>
            </template>

            <!-- 治理备注 -->
            <template #remark="{ record }">
              <div class="table-field-cell">
                <EllipsisTooltipText :text="record.remark || '-'" />
              </div>
            </template>
          </a-table>
          <div v-show="showAccountBottomScrollbar" ref="accountTableBottomScrollbarRef" class="table-shell__x-scrollbar"
            @scroll="handleAccountBottomScrollbarScroll">
            <div class="table-shell__x-scrollbar-track" :style="{ width: `${accountTableContentWidth}px` }">
            </div>
          </div>
        </div>

        <div class="table-shell__footer">
          <a-pagination v-bind="tablePaginationConfig" size="small" @change="handlePageChange"
            @page-size-change="handlePageSizeChange" />
        </div>
      </div>
    </GovernanceListStage>

    <!-- 详情预览面板已合并至编辑弹窗 -->

    <teleport to="body">
      <div v-if="accountModal.visible" class="account-modal-overlay">
        <div class="account-modal-overlay__mask"></div>
        <div class="account-modal-overlay__viewport"
          :class="{ 'is-fullscreen': accountModalController.fullscreen.value }">
          <div class="account-modal account-modal--create" :class="{
            'arco-modal-fullscreen': accountModalController.fullscreen.value,
            'is-dragging': accountModalController.isDragging.value,
            'is-resizing': accountModalController.isResizing.value
          }" :style="accountCreateModalStyle">
            <!-- 缩放热区 (Resize Handles) -->
            <div class="account-modal-resize-handle account-modal-resize-handle--n"
              @mousedown.stop.prevent="(e) => accountModalController.startResize('n', e)"></div>
            <div class="account-modal-resize-handle account-modal-resize-handle--s"
              @mousedown.stop.prevent="(e) => accountModalController.startResize('s', e)"></div>
            <div class="account-modal-resize-handle account-modal-resize-handle--e"
              @mousedown.stop.prevent="(e) => accountModalController.startResize('e', e)"></div>
            <div class="account-modal-resize-handle account-modal-resize-handle--w"
              @mousedown.stop.prevent="(e) => accountModalController.startResize('w', e)"></div>
            <div class="account-modal-resize-handle account-modal-resize-handle--ne"
              @mousedown.stop.prevent="(e) => accountModalController.startResize('ne', e)"></div>
            <div class="account-modal-resize-handle account-modal-resize-handle--nw"
              @mousedown.stop.prevent="(e) => accountModalController.startResize('nw', e)"></div>
            <div class="account-modal-resize-handle account-modal-resize-handle--se"
              @mousedown.stop.prevent="(e) => accountModalController.startResize('se', e)"></div>
            <div class="account-modal-resize-handle account-modal-resize-handle--sw"
              @mousedown.stop.prevent="(e) => accountModalController.startResize('sw', e)"></div>

            <div class="arco-modal-header" @mousedown="accountModalController.startDrag">
              <div class="account-modal-title">
                <div class="account-modal-title__main">
                  <strong>{{ accountModalTitle }}</strong>
                </div>
                <div class="account-modal-title__actions">
                  <span class="account-modal-title__badge">{{ accountModalTitleBadge }}</span>
                  <div class="account-modal-title__action-group">
                    <a-tooltip :content="accountModalController.fullscreen.value ? '退出全屏' : '全屏展示'">
                      <a-button size="mini" class="account-modal-title__action-btn"
                        @click.stop="toggleAccountModalFullscreen">
                        <template #icon>
                          <component
                            :is="accountModalController.fullscreen.value ? IconFullscreenExit : IconFullscreen" />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip v-if="accountModal.mode === 'edit'" content="以此为模板复制并新建">
                      <a-button size="mini" class="account-modal-title__action-btn" @click.stop="handleCopyInsideModal">
                        <template #icon>
                          <icon-copy />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip content="关闭窗口">
                      <a-button size="mini"
                        class="account-modal-title__action-btn account-modal-title__action-btn--close"
                        @click.stop="closeAccountModal">
                        <template #icon>
                          <icon-close />
                        </template>
                      </a-button>
                    </a-tooltip>
                  </div>
                </div>
              </div>
            </div>

            <div class="account-modal-body account-modal-body--create" @wheel.stop>
              <div class="account-modal-shell account-modal-shell--create">
                <div class="account-create-viewport" @wheel.stop>
                  <a-form :model="accountForm" layout="vertical" class="account-create-form">
                    <div class="account-create-stage">
                      <div class="account-create-layout account-create-layout--single">
                        <div class="account-create-main">
                          <section class="account-create-form-shell">
                            <!-- 按产品要求移除首块装饰标题层，字段区直接展示，减少视觉噪音。 -->
                            <GovernanceFormSections :model="accountFormAsRecord" :sections="accountFormSections"
                              variant="embedded" />
                          </section>

                          <section class="account-create-form-shell">
                            <div class="account-create-form-shell__heading">
                              <div class="account-create-form-shell__heading-main">
                                <h3>三环境白名单独立维护</h3>
                                <p>
                                  测试、预发、生产来源 IP 分离管理，系统会按当前接入环境自动命中生效清单，
                                  同时在保存时完成标准化、去重与当前环境白名单回填。
                                </p>
                              </div>
                              <div class="account-create-form-shell__summary">
                                <div class="account-create-form-shell__status">
                                  <strong>{{ accountCreateWhitelistSummaryTitle }}</strong>
                                  <small>{{ accountCreateWhitelistSummaryHint }}</small>
                                </div>
                              </div>
                            </div>

                            <ApiEnvironmentWhitelistEditor v-model="accountWhitelistModel"
                              :access-environment="accountForm.accessEnvironment"
                              :environment-options="environmentOptions" :show-heading="false" :show-callout="false"
                              surface-mode="flat" :readonly="isDetailMode" />
                          </section>

                          <div class="account-create-footer account-create-footer--actions-only">
                            <div class="account-create-footer__actions">
                              <a-button @click="closeAccountModal">取消</a-button>
                              <a-button @click="resetAccountForm">重置表单</a-button>
                              <a-button type="primary" html-type="submit" :loading="accountModal.submitting"
                                @click.prevent="submitAccountForm">{{ accountModalSubmitText }}</a-button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </a-form>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </teleport>

    <ApiCredentialDeliveryModal v-model:visible="credentialModal.visible" :payload="credentialModal.payload" />

    <ApiAuthorizationWorkbenchDrawer v-model:visible="authorizationDrawer.visible"
      :loading="authorizationDrawer.loading" :saving="authorizationDrawer.saving"
      :snapshot="authorizationDrawer.snapshot" :filters="authorizationFilters" :hero-tags="authorizationHeroTags"
      :hero-stats="authorizationHeroStats" :account-facts="authorizationAccountFacts"
      :guide-items="authorizationGuideItems" :module-cards="moduleCards" :summary-cards="authorizationSelectionCards"
      :visible-api-count="authorizationVisibleApiCount" :visible-controller-count="authorizationVisibleControllerCount"
      :module-options="moduleOptions" :tree-data="authorizationTreeData" :checked-keys="authorizationDrawer.checkedKeys"
      @update:checked-keys="value => authorizationDrawer.checkedKeys = value" @save="submitAuthorization"
      @select-visible="selectVisibleApis" @clear-visible="clearVisibleApis" @select-module="selectModuleApis"
      @clear-module="clearModuleApis" />

    <ApiCallbackLogWorkbenchDrawer v-model:visible="callbackLogDrawer.visible" :account="callbackLogDrawer.account"
      :loading="callbackLogDrawer.loading" :testing="callbackLogDrawer.testing"
      :retrying-id="callbackLogDrawer.retryingId" :logs="callbackLogDrawer.logs" :filters="callbackLogFilters"
      :pagination-config="callbackLogPaginationConfig" :hero-tags="callbackHeroTags" :hero-stats="callbackHeroStats"
      :account-facts="callbackAccountFacts" :guide-items="callbackGuideItems" :summary-cards="callbackTableStats"
      @search="handleCallbackLogSearch" @reset="handleCallbackLogReset" @page-change="handleCallbackLogPageChange"
      @page-size-change="handleCallbackLogPageSizeChange" @trigger-test="handleTriggerTestCallback()"
      @retry="handleRetryCallback" />
  </div>
</template>

<script lang="ts" setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch, h } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Message, Modal } from '@arco-design/web-vue';
import { IconClose, IconDown, IconDragArrow, IconFullscreen, IconFullscreenExit, IconPlus, IconPushpin, IconSettings, IconSync, IconUp, IconMore, IconEdit, IconSafe, IconCopy } from '@arco-design/web-vue/es/icon';
import { createApiAccount, deleteApiAccount, fetchApiAccountCopy, fetchApiAccountDetail, fetchApiAccountPage, fetchApiCallbackLogs, fetchAuthorizationSnapshot, resetApiAccountSecret, retryApiCallbackLog, saveAuthorization, syncOpenApiRegistry, triggerApiAccountTestCallback, updateApiAccount } from '../../api/apiAccount';
import ApiAuthorizationWorkbenchDrawer from '../../components/api-account/ApiAuthorizationWorkbenchDrawer.vue';
import ApiCallbackLogWorkbenchDrawer from '../../components/api-account/ApiCallbackLogWorkbenchDrawer.vue';
import ApiCredentialDeliveryModal from '../../components/api-account/ApiCredentialDeliveryModal.vue';
import ApiEnvironmentWhitelistEditor from '../../components/api-account/ApiEnvironmentWhitelistEditor.vue';
import GovernanceCompactQueryPanel from '../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceFormSections from '../../components/governance/GovernanceFormSections.vue';
import GovernanceListStage from '../../components/governance/GovernanceListStage.vue';
import EllipsisTooltipText from '../../components/common/EllipsisTooltipText.vue';
import { useApiAccountFormValidation } from '../../composables/useApiAccountFormValidation';
import { useApiAccountFormSchema } from '../../composables/useApiAccountFormSchema';
import { useApiAccountQuerySchema } from '../../composables/useApiAccountQuerySchema';
import { splitGovernanceSectionsByPriority } from '../../composables/useGovernanceSectionPriority';
import { defineTableColumns, useTableColumns } from '../../composables/useTableColumns';
import { useResizableModal } from '../../composables/useResizableModal';
import { buildUserScopedStorageKey } from '../../utils/userScopedStorage';
import { getModuleDisplayName, getControllerDisplayName } from '../../utils/api-account-governance';
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
import type { ApiCatalogTreeNode } from '../../types/apiList';
import type { FactCard, WorkbenchStatCard } from '../../types/governance';

type AccountModalMode = 'create' | 'edit';
type ManageRouteAction = 'edit' | 'copy' | 'authorization' | 'callback' | 'reset-secret';

type AuthorizationModuleCard = { moduleName: string; apiCount: number; controllerCount: number; selectedCount: number };
type AccountColumnKind =
  | 'index'
  | 'accountName'
  | 'status'
  | 'accountType'
  | 'systemName'
  | 'ownerName'
  | 'accessEnvironment'
  | 'authorizedCount'
  | 'updateTime'
  | 'actions'
  | 'accountId'
  | 'accessKey'
  | 'systemCode'
  | 'ownerContact'
  | 'signVersion'
  | 'rateLimit'
  | 'expireTime'
  | 'callbackUrl'
  | 'clientTypes'
  | 'allowedScopes'
  | 'remark'
  | 'createTime';
type QueryTextMatchMode = 'fuzzy' | 'exact';
type AccountTableColumnLayout = {
  width: number;
  visible: boolean;
  order: number;
  fixedFront: boolean;
};
type AccountModalScrollLockTargetState = {
  element: HTMLElement;
  scrollTop: number;
  overflow: string;
  overscrollBehavior: string;
  scrollbarGutter: string;
};
type AccountModalWrapperLockState = {
  element: HTMLElement;
  scrollTop: number;
  overflow: string;
  overscrollBehavior: string;
};
type AccountTableColumnSettingItem = {
  kind: AccountColumnKind;
  title: string;
  visible: boolean;
  fixedFront: boolean;
  fixedFrontDisabled: boolean;
  locked: boolean;
  moveUpDisabled: boolean;
  moveDownDisabled: boolean;
};

const accountTypeOptions = [{ label: '内部账号', value: 1 }, { label: '外部账号', value: 2 }];
const clientTypeOptions = [{ label: 'Web前端', value: 'web' }, { label: 'H5页面', value: 'h5' }, { label: 'APP', value: 'app' }, { label: '小程序', value: 'mini_program' }, { label: '服务端', value: 'server' }, { label: '第三方系统', value: 'third_party' }, { label: '其他客户端', value: 'other' }];
const environmentOptions: { label: string; value: AccessEnvironment }[] = [{ label: '测试环境', value: 'test' }, { label: '预发环境', value: 'staging' }, { label: '生产环境', value: 'production' }];
const signVersionOptions = [{ label: 'v1（当前正式版）', value: 'v1' }];
const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }];
const scopeOptions = [{ label: '只读访问 (read)', value: 'read' }, { label: '读写访问 (write)', value: 'write' }, { label: '管理访问 (admin)', value: 'admin' }, { label: '导出访问 (export)', value: 'export' }, { label: '回调访问 (callback)', value: 'callback' }];
/**
 * 页面主工作区最大宽度。
 * 统一设置为 100%，保证左侧菜单收起/展开时右侧查询卡、列表卡都能跟随可用视口铺满。
 * 如后续某些场景需要“限宽阅读模式”，仅需在此处切换为固定值或响应式表达式。
 */
const accountWorkspaceMaxWidth = '100%';
const route = useRoute();
const router = useRouter();
const apiAccountPageRef = ref<HTMLElement | null>(null);

const queryForm = reactive({
  accountId: undefined as number | undefined,
  accountName: '',
  description: '',
  accessKey: '',
  ownerName: '',
  ownerContact: '',
  systemName: '',
  systemCode: '',
  systemKeyword: '',
  signVersion: undefined as string | undefined,
  allowedScope: undefined as string | undefined,
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
const accountTablePagination = reactive({ current: 1, pageSize: 10, total: 0 });
const accountModal = reactive({ visible: false, mode: 'create' as AccountModalMode, editingId: 0, submitting: false });
/**
 * 新建/编辑弹窗视口状态。
 * 统一管理宽高、位移和全屏状态，为后续其他治理弹窗复用“拖拽 + 缩放 + 全屏”能力提供基础。
 * - left / top：创建态自定义弹层的绝对定位坐标，保证拖拽时可精准贴边。
 * - offsetX / offsetY：编辑态 Arco 弹窗沿用中心点偏移模型，避免影响既有实现。
 */
const accountModalViewport = reactive({
  width: 1160,
  height: 760,
  left: 0,
  top: 0,
  offsetX: 0,
  offsetY: 0,
  fullscreen: false
});

const accountModalController = useResizableModal({
  initialWidth: 1024,
  initialHeight: 700,
  minWidth: 860,
  minHeight: 520,
  gap: 32,
  /**
   * 允许新建账号弹窗通过边框缩放铺满到浏览器边缘（左/右/下均可到 0）。
   * 与居中留白解耦：打开时仍保持视觉留白，用户手动缩放时可拉满整个视口。
   */
  resizeViewportInset: 0
});


/**
 * 底部横向滚动条同步状态：
 * - list-region: 当前列表区域容器。
 * - bodyScrollable: Arco 表格真实横向滚动容器（.arco-table-body）。
 * - bottomScrollbar: 页面底部独立滚动条容器。
 */
const accountTableListRegionRef = ref<HTMLElement | null>(null);
const accountTableBottomScrollbarRef = ref<HTMLElement | null>(null);
const accountTableBodyScrollableRef = ref<HTMLElement | null>(null);
const showAccountBottomScrollbar = ref(false);
/**
 * 表格内容真实宽度：
 * 通过 ResizeObserver 动态监听表格 Body 的 scrollWidth，
 * 确保底部自定义滚动条的滑块范围与表格内容完全对齐，解决手动拖拽列宽后滚动到底部仍有遮挡的问题。
 */
const accountTableContentWidth = ref(0);
let accountTableResizeObserver: ResizeObserver | null = null;
/**
 * 表格渲染版本号：
 * Arco 的列拖拽改宽会保留内部宽度状态，恢复默认时需要强制重建实例，
 * 才能确保默认列宽立即回显到页面。
 */
const accountTableRenderVersion = ref(0);
const activeRowId = ref<number | null>(null);
const selectedKeys = ref<(string | number)[]>([]);

/**
 * 表格行选择配置：
 * 显式声明 checkbox 模式并启用全选，确保 Arco Table 正确渲染选择列。
 */
const rowSelection = reactive({
  type: 'checkbox' as const,
  showCheckedAll: true,
  onlyCurrent: false,
});

/**
 * 表格行选择配置：
 * 显式声明 checkbox 模式并启用全选，确保 Arco Table 正确渲染选择列。
 */


let accountTableBodyScrollHandler: ((event: Event) => void) | null = null;
let accountBottomScrollbarSyncing = false;
let accountBottomScrollbarRafId: number | null = null;
const accountForm = reactive<ApiAccountFormModel>({ accountName: '', description: '', ownerName: '', ownerContact: '', systemName: '', systemCode: '', accountType: 2, clientTypes: [], accessEnvironment: 'production', signVersion: 'v1', allowedScopes: [], environmentIpWhitelistText: createEmptyEnvironmentWhitelistText(), callbackUrl: '', rateLimit: 1000, expireTime: null, status: 1, remark: '' });
const credentialModal = reactive({ visible: false, payload: null as ApiCredentialPayload | null });
const authorizationDrawer = reactive({ visible: false, loading: false, saving: false, accountId: 0, snapshot: null as ApiAuthorizationSnapshot | null, checkedKeys: [] as string[] });
const authorizationFilters = reactive({ keyword: '', moduleName: '', method: '' });
const callbackLogDrawer = reactive({ visible: false, loading: false, testing: false, retryingId: null as number | null, account: null as ApiAccountItem | null, logs: [] as ApiCallbackLogItem[], summary: createSummary(), pagination: { current: 1, pageSize: 10, total: 0 } });
const callbackLogFilters = reactive<ApiCallbackLogFilterModel>({ callbackStatus: undefined });

const tablePaginationConfig = computed(() => ({
  current: accountTablePagination.current,
  pageSize: accountTablePagination.pageSize,
  total: accountTablePagination.total,
  showTotal: true,
  showPageSize: true,
  pageSizeOptions: [10, 20, 50, 100]
}));
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
  signVersionOptions,
  scopeOptions
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
  scopeOptions,
  statusOptions,
  // 新建与编辑弹窗均采用紧凑多列布局。
  compactMode: true
}).sections);
const { parseIpWhitelistInput, validateAndBuildPayload } = useApiAccountFormValidation();
// 新建/编辑弹窗的概览与指引均使用配置化数据，便于后续治理页复用同一套信息架构。
const accountModalTitle = computed(() => accountModal.mode === 'create' ? '新建API账号' : '编辑API账号');
const accountModalTitleBadge = computed(() => accountModal.mode === 'create' ? '创建后将返回首份凭证' : '保存后配置即时生效');
// 新增模式用于控制弹窗的轻量布局（隐藏 Hero 与侧栏）。
const isCreateMode = computed(() => accountModal.mode === 'create');
// 详情功能已下线，统一转为编辑模式。
const isDetailMode = computed(() => false);

const accountModalSubmitText = computed(() => accountModal.mode === 'create' ? '创建账号' : '保存变更');
/**
 * 创建态自定义弹层样式。
 * 直接复用 useResizableModal 提供的样式计算。
 */
const accountCreateModalStyle = computed(() => accountModalController.modalStyle.value);
const accountCreateWhitelistSummaryTitle = computed(() => getEnvironmentWhitelistCountLabel(accountForm.environmentIpWhitelistText[accountForm.accessEnvironment]));
const accountCreateWhitelistSummaryHint = computed(() => `${getAccessEnvironmentLabel(accountForm.accessEnvironment)} 当前默认命中`);
// 在超紧凑模式下使用短文本，降低按钮占位，让底部操作区更聚焦。
const queryAdvancedToggleText = computed(() => queryAdvancedExpanded.value ? '收起条件' : '更多条件');
// 详情相关的所有 Fact 统计已移除，逻辑合并至编辑表单。
/**
 * 列布局本地缓存版本号。
 * 本次将“复合列”重构为“单字段列”，并改为按登录用户隔离，避免不同账号互相覆盖布局配置。
 */
/**
 * 列布局本地缓存版本号。
 * v9：全面开放 21 个字段供用户选择，并根据行业标准预设了默认显示的 7 个核心字段及先后顺序。
 */
const ACCOUNT_TABLE_LAYOUT_STORAGE_KEY_PREFIX = 'bml.api-account.manage.table-layout.v9';
function getAccountTableLayoutStorageKey() {
  return buildUserScopedStorageKey(ACCOUNT_TABLE_LAYOUT_STORAGE_KEY_PREFIX);
}
const ACCOUNT_TABLE_LOCKED_COLUMN_KINDS = new Set<AccountColumnKind>(['actions']);
/**
 * 列宽拖拽最小值统一与 Arco Table 内置最小宽度对齐（40px）。
 * 之前按业务可读性设置了较大的列最小宽度，导致用户拖窄后刷新会被“回弹”到较大值，
 * 看起来像“没有记住用户配置”。这里改为记录用户真实拖拽结果，只在恢复默认时回到设计宽度。
 */
const ACCOUNT_TABLE_COLUMN_RESIZE_MIN_WIDTH = 40;
const ACCOUNT_TABLE_COLUMN_MAX_WIDTH: Record<AccountColumnKind, number> = {
  index: 120,
  accountName: 1200,
  status: 300,
  accountType: 300,
  systemName: 800,
  ownerName: 400,
  accessEnvironment: 400,
  authorizedCount: 300,
  updateTime: 600,
  actions: 600,
  accountId: 400,
  accessKey: 800,
  systemCode: 600,
  ownerContact: 600,
  signVersion: 400,
  rateLimit: 400,
  expireTime: 600,
  callbackUrl: 1000,
  clientTypes: 600,
  allowedScopes: 800,
  remark: 1000,
  createTime: 600
};
// 列基础模型（默认顺序 + 默认宽度 + 固定信息）集中收口。
// 按日常查看频率排序：核心标识 → 运营状态 → 归属信息 → 策略范围 → 低频字段 → 操作列。
const accountTableColumnBaseModel = defineTableColumns<AccountColumnKind>([
  { key: 'index', dataIndex: 'index', title: '序号', kind: 'index', width: 70, fixed: 'left', slotName: 'index' },
  { key: 'accountName', dataIndex: 'accountName', title: '账号名称', kind: 'accountName', width: 220, fixed: 'left', slotName: 'accountName' },
  { key: 'status', dataIndex: 'status', title: '状态', kind: 'status', width: 100, slotName: 'status' },
  { key: 'accountType', dataIndex: 'accountType', title: '账号类型', kind: 'accountType', width: 110, slotName: 'accountType' },
  { key: 'systemName', dataIndex: 'systemName', title: '业务系统', kind: 'systemName', width: 180, slotName: 'systemName' },
  { key: 'ownerName', dataIndex: 'ownerName', title: '负责人', kind: 'ownerName', width: 120, slotName: 'ownerName' },
  { key: 'accessEnvironment', dataIndex: 'accessEnvironment', title: '接入环境', kind: 'accessEnvironment', width: 110, slotName: 'accessEnvironment' },
  { key: 'authorizedCount', dataIndex: 'authorizedApiCount', title: '授权数', kind: 'authorizedCount', width: 100, slotName: 'authorizedCount' },
  { key: 'updateTime', dataIndex: 'updateTime', title: '最近更新', kind: 'updateTime', width: 180, slotName: 'updateTime' },
  { key: 'actions', dataIndex: 'actions', title: '操作', kind: 'actions', width: 170, fixed: 'right', slotName: 'actions' },
  // 扩展字段，默认隐藏
  { key: 'accountId', dataIndex: 'id', title: '账号ID', kind: 'accountId', width: 100, slotName: 'accountId' },
  { key: 'accessKey', dataIndex: 'accessKey', title: '访问密钥', kind: 'accessKey', width: 220, slotName: 'accessKey' },
  { key: 'systemCode', dataIndex: 'systemCode', title: '系统编码', kind: 'systemCode', width: 160, slotName: 'systemCode' },
  { key: 'ownerContact', dataIndex: 'ownerContact', title: '负责人联系方式', kind: 'ownerContact', width: 180, slotName: 'ownerContact' },
  { key: 'signVersion', dataIndex: 'signVersion', title: '签名版本', kind: 'signVersion', width: 110, slotName: 'signVersion' },
  { key: 'rateLimit', dataIndex: 'rateLimit', title: '限流配置', kind: 'rateLimit', width: 120, slotName: 'rateLimit' },
  { key: 'expireTime', dataIndex: 'expireTime', title: '过期时间', kind: 'expireTime', width: 180, slotName: 'expireTime' },
  { key: 'callbackUrl', dataIndex: 'callbackUrl', title: '回调地址', kind: 'callbackUrl', width: 260, slotName: 'callbackUrl' },
  { key: 'clientTypes', dataIndex: 'clientTypes', title: '客户端类型', kind: 'clientTypes', width: 180, slotName: 'clientTypes' },
  { key: 'allowedScopes', dataIndex: 'allowedScopes', title: '权限范围', kind: 'allowedScopes', width: 200, slotName: 'allowedScopes' },
  { key: 'remark', dataIndex: 'remark', title: '治理备注', kind: 'remark', width: 220, slotName: 'remark' },
  { key: 'createTime', dataIndex: 'createTime', title: '创建时间', kind: 'createTime', width: 180, slotName: 'createTime' }
]);
const accountTableColumnBaseMap = (accountTableColumnBaseModel as any[]).reduce((accumulator, column) => {
  accumulator[column.kind] = column;
  return accumulator;
}, {} as Record<string, any>);
function createDefaultAccountTableColumnLayout(): Record<AccountColumnKind, AccountTableColumnLayout> {
  const layout = {} as any;

  /**
   * 默认显示方案与行业标准排序：
   * 1. 账号名称 (Fixed Left)
   * 2. 状态
   * 3. 账号类型
   * 4. 业务系统
   * 5. 负责人
   * 6. 接入环境
   * 7. 授权数
   * 8. 最近更新
   * 9. 操作 (Fixed Right)
   */
  const defaultVisibleOrder = [
    'index',
    'accountName',
    'status',
    'accountType',
    'systemName',
    'ownerName',
    'accessEnvironment',
    'authorizedCount',
    'updateTime',
    'actions'
  ];

  accountTableColumnBaseModel.forEach((col: any, index: number) => {
    const visibleIndex = defaultVisibleOrder.indexOf(col.kind);
    layout[col.kind as AccountColumnKind] = {
      width: col.width,
      visible: visibleIndex !== -1,
      order: visibleIndex !== -1 ? visibleIndex : 100 + index, // 非默认显示的字段排在后面
      fixedFront: col.fixed === 'left'
    };
  });
  return layout;
}
const accountTableColumnLayout = reactive<Record<AccountColumnKind, AccountTableColumnLayout>>(createDefaultAccountTableColumnLayout());
/**
 * 列设置拖拽状态：
 * 统一维护拖拽源、悬停目标与投放位置，避免在模板中散落临时变量。
 */
const columnSettingDragState = reactive({
  draggingKind: '' as AccountColumnKind | '',
  overKind: '' as AccountColumnKind | '',
  dropPosition: '' as 'before' | 'after' | ''
});
function clampAccountColumnWidth(kind: AccountColumnKind, width: number) {
  const max = ACCOUNT_TABLE_COLUMN_MAX_WIDTH[kind];
  const min = kind === 'index' ? 60 : ACCOUNT_TABLE_COLUMN_RESIZE_MIN_WIDTH;
  return Math.min(max, Math.max(min, Math.round(width)));
}
function normalizeAccountTableColumnOrder() {
  const unlockedKinds = accountTableColumnBaseModel
    .map(item => item.kind)
    .filter(kind => !ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind))
    .sort((left, right) => accountTableColumnLayout[left].order - accountTableColumnLayout[right].order);
  unlockedKinds.forEach((kind, index) => {
    (accountTableColumnLayout as any)[kind].order = index;
  });

  let lockedOrderCursor = unlockedKinds.length;
  accountTableColumnBaseModel.forEach(column => {
    if (!ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(column.kind)) return;
    (accountTableColumnLayout as any)[column.kind].order = lockedOrderCursor;
    (accountTableColumnLayout as any)[column.kind].visible = true;
    (accountTableColumnLayout as any)[column.kind].fixedFront = false;
    lockedOrderCursor += 1;
  });
}
function persistAccountTableColumnLayout() {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(getAccountTableLayoutStorageKey(), JSON.stringify(accountTableColumnLayout));
}
function restoreAccountTableColumnLayout() {
  if (typeof window === 'undefined') return;
  const storageKey = getAccountTableLayoutStorageKey();
  try {
    const raw = window.localStorage.getItem(storageKey);
    if (!raw) return;
    const parsed = JSON.parse(raw) as Partial<Record<AccountColumnKind, Partial<AccountTableColumnLayout>>>;
    for (const kind of Object.keys(accountTableColumnLayout) as AccountColumnKind[]) {
      const storedItem = parsed[kind];
      if (!storedItem) continue;
      if (typeof storedItem.width === 'number' && Number.isFinite(storedItem.width)) {
        accountTableColumnLayout[kind].width = clampAccountColumnWidth(kind, storedItem.width);
      }
      if (typeof storedItem.visible === 'boolean' && !ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind)) {
        accountTableColumnLayout[kind].visible = storedItem.visible;
      }
      if (typeof storedItem.order === 'number' && Number.isFinite(storedItem.order)) {
        accountTableColumnLayout[kind].order = Math.round(storedItem.order);
      }
      if (typeof storedItem.fixedFront === 'boolean' && !ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind)) {
        accountTableColumnLayout[kind].fixedFront = storedItem.fixedFront;
      }
    }
    normalizeAccountTableColumnOrder();
    accountTableRenderVersion.value += 1;
  } catch {
    window.localStorage.removeItem(storageKey);
  }
}
restoreAccountTableColumnLayout();
const accountTableColumnSettingItems = computed<AccountTableColumnSettingItem[]>(() => {
  const allKinds = Object.keys(accountTableColumnLayout)
    .map(kind => kind as AccountColumnKind)
    .sort((left, right) => accountTableColumnLayout[left].order - accountTableColumnLayout[right].order);

  /**
   * 列设置面板排序逻辑：
   * 1. 固定在左侧的锁定列（如账号名称）→ 最顶部
   * 2. 可移动的普通列 → 按 order 排序显示在中间
   * 3. 固定在右侧的锁定列（如操作）→ 最底部
   */
  const lockedLeftKinds = allKinds.filter(kind => ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind) && accountTableColumnBaseMap[kind].fixed === 'left');
  const lockedRightKinds = allKinds.filter(kind => ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind) && accountTableColumnBaseMap[kind].fixed === 'right');
  const movableKinds = allKinds.filter(kind => !ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind));
  const orderedKinds = [...lockedLeftKinds, ...movableKinds, ...lockedRightKinds];

  return orderedKinds.map(kind => {
    const movableIndex = movableKinds.indexOf(kind);
    const locked = ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind);
    return {
      kind,
      title: accountTableColumnBaseMap[kind].title,
      visible: accountTableColumnLayout[kind].visible,
      fixedFront: accountTableColumnLayout[kind].fixedFront,
      fixedFrontDisabled: locked,
      locked,
      moveUpDisabled: locked || movableIndex <= 0,
      moveDownDisabled: locked || movableIndex < 0 || movableIndex >= movableKinds.length - 1
    };
  });
});
function moveAccountTableColumn(kind: AccountColumnKind, direction: -1 | 1) {
  if (ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind)) return;
  const movableKinds = accountTableColumnSettingItems.value
    .filter(item => !item.locked)
    .map(item => item.kind);
  const currentIndex = movableKinds.indexOf(kind);
  const targetIndex = currentIndex + direction;
  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= movableKinds.length) return;
  const targetKind = movableKinds[targetIndex];
  if (!targetKind) return;
  const currentOrder = accountTableColumnLayout[kind].order;
  accountTableColumnLayout[kind].order = accountTableColumnLayout[targetKind].order;
  accountTableColumnLayout[targetKind].order = currentOrder;
  normalizeAccountTableColumnOrder();
  persistAccountTableColumnLayout();
}
function resetColumnSettingDragState() {
  columnSettingDragState.draggingKind = '';
  columnSettingDragState.overKind = '';
  columnSettingDragState.dropPosition = '';
}
function handleColumnSettingDragStart(kind: AccountColumnKind, event: DragEvent) {
  if (ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind)) return;
  if (!event.dataTransfer) return;
  event.dataTransfer.effectAllowed = 'move';
  event.dataTransfer.setData('text/plain', kind);
  columnSettingDragState.draggingKind = kind;
  columnSettingDragState.overKind = '';
  columnSettingDragState.dropPosition = '';
}
function handleColumnSettingDragOver(targetKind: AccountColumnKind, event: DragEvent) {
  if (ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(targetKind)) return;
  const draggingKind = columnSettingDragState.draggingKind;
  if (!draggingKind || draggingKind === targetKind) return;
  if (ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(draggingKind)) return;

  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move';
  }

  const container = event.currentTarget as HTMLElement | null;
  if (!container) return;
  const rect = container.getBoundingClientRect();
  const offsetY = event.clientY - rect.top;
  columnSettingDragState.overKind = targetKind;
  columnSettingDragState.dropPosition = offsetY >= rect.height / 2 ? 'after' : 'before';
}
function handleColumnSettingDrop(targetKind: AccountColumnKind, event: DragEvent) {
  event.preventDefault();
  const draggingKind = columnSettingDragState.draggingKind;
  const dropPosition = columnSettingDragState.dropPosition;
  if (!draggingKind || !dropPosition || draggingKind === targetKind) {
    resetColumnSettingDragState();
    return;
  }
  if (ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(draggingKind) || ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(targetKind)) {
    resetColumnSettingDragState();
    return;
  }

  const movableKinds = accountTableColumnSettingItems.value
    .filter(item => !item.locked)
    .map(item => item.kind);
  const dragIndex = movableKinds.indexOf(draggingKind);
  const targetIndex = movableKinds.indexOf(targetKind);
  if (dragIndex < 0 || targetIndex < 0) {
    resetColumnSettingDragState();
    return;
  }

  const [draggingItem] = movableKinds.splice(dragIndex, 1);
  if (!draggingItem) {
    resetColumnSettingDragState();
    return;
  }

  let insertIndex = targetIndex;
  if (dragIndex < targetIndex) {
    insertIndex = dropPosition === 'after' ? targetIndex : targetIndex - 1;
  } else {
    insertIndex = dropPosition === 'after' ? targetIndex + 1 : targetIndex;
  }
  insertIndex = Math.max(0, Math.min(insertIndex, movableKinds.length));
  movableKinds.splice(insertIndex, 0, draggingItem);

  movableKinds.forEach((kind, index) => {
    accountTableColumnLayout[kind].order = index;
  });
  normalizeAccountTableColumnOrder();
  persistAccountTableColumnLayout();
  resetColumnSettingDragState();
}
function handleColumnSettingDragEnd() {
  resetColumnSettingDragState();
}
function handleAccountColumnVisibilityChange(kind: AccountColumnKind, visible: boolean) {
  if (ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind)) return;
  const activeCount = accountTableColumnSettingItems.value.filter(item => !item.locked && item.visible).length;
  if (!visible && accountTableColumnLayout[kind].visible && activeCount <= 1) {
    Message.warning('至少保留一列业务字段');
    return;
  }
  accountTableColumnLayout[kind].visible = visible;
  persistAccountTableColumnLayout();
}
/**
 * 列“固定在前”开关：
 * 开启后将该列固定在表格左侧，横向滚动时保持可见；
 * 关闭后恢复普通滚动列。配置持久化到本地，刷新后保持。
 */
function handleAccountColumnFixedFrontChange(kind: AccountColumnKind, fixedFront: boolean) {
  if (ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind)) return;
  accountTableColumnLayout[kind].fixedFront = fixedFront;
  persistAccountTableColumnLayout();
  /**
   * Arco Table 对 fixed 列在部分场景下会缓存列偏移，
   * 切换固定状态后主动重建一次实例，确保“固定在左侧”立即生效且横向滚动不跟随。
   */
  accountTableRenderVersion.value += 1;
  nextTick(() => {
    scheduleAccountBottomScrollbarSync();
  });
}
function toggleAccountColumnFixedFront(kind: AccountColumnKind) {
  if (ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind)) return;
  handleAccountColumnFixedFrontChange(kind, !accountTableColumnLayout[kind].fixedFront);
}
function resetAccountTableColumnLayout() {
  const defaults = createDefaultAccountTableColumnLayout();
  for (const kind of Object.keys(defaults) as AccountColumnKind[]) {
    accountTableColumnLayout[kind] = defaults[kind];
  }
  normalizeAccountTableColumnOrder();
  persistAccountTableColumnLayout();
  accountTableRenderVersion.value += 1;
  Message.success('列宽、顺序与显示列已恢复默认');
}
function handleAccountColumnResize(dataIndex: string, width: number) {
  /**
   * 兼容不同表格事件实现：
   * - Arco 标准为 dataIndex；
   * - 部分场景可能回传 key / kind。
   * 统一做多键命中，避免列宽拖拽后未落盘导致刷新丢失。
   */
  const hitColumn = accountTableColumnBaseModel.find(column => (
    column.dataIndex === dataIndex
    || column.key === dataIndex
    || column.kind === dataIndex
  ));
  if (!hitColumn || !Number.isFinite(width)) return;
  const kind = hitColumn.kind;
  accountTableColumnLayout[kind].width = clampAccountColumnWidth(kind, width);
  persistAccountTableColumnLayout();
}
const accountTableColumnModel = computed(() => {
  const visibleItems = accountTableColumnSettingItems.value.filter(item => item.visible || item.locked);
  const lockedLeftItems = visibleItems.filter(item => item.locked && accountTableColumnBaseMap[item.kind].fixed === 'left');
  const lockedRightItems = visibleItems.filter(item => item.locked && accountTableColumnBaseMap[item.kind].fixed === 'right');
  const frontFixedItems = visibleItems.filter(item => item.fixedFront && !item.locked);
  const normalItems = visibleItems.filter(item => !item.fixedFront && !item.locked);
  const orderedItems = [...lockedLeftItems, ...frontFixedItems, ...normalItems, ...lockedRightItems];

  const columns: any[] = [];
  
  orderedItems.forEach(item => {
    const base = accountTableColumnBaseMap[item.kind];
    const column = {
      ...base,
      width: accountTableColumnLayout[item.kind]!.width,
      fixed: item.kind === 'actions'
        ? 'right'
        : (accountTableColumnLayout[item.kind]!.fixedFront ? 'left' : (item.locked ? base.fixed : undefined))
    };
    columns.push(column);

  });

  return columns;
});
/**
 * 表格横向强制宽度：
 * Arco Table 需要一个预设的 x 滚动值来启用横向滚动模式。
 * 这里计算所有可见列的宽度之和，并额外增加 60px 的安全缓冲（包含操作列偏移与容器边距），
 * 确保在列宽拖大后，表格容器有足够的弹性空间显示出最右侧的操作按钮。
 */
const accountTableScrollX = computed(() => {
  const baseWidth = accountTableColumnModel.value.reduce((sum, item) => sum + (item.width || item.minWidth || 0), 0);
  return baseWidth + 100;
});
const { columns: accountTableColumns, getPlainText } = useTableColumns(() => accountTableColumnModel.value);
function clearAccountBottomScrollbarRaf() {
  if (accountBottomScrollbarRafId == null) return;
  window.cancelAnimationFrame(accountBottomScrollbarRafId);
  accountBottomScrollbarRafId = null;
}
function syncAccountBottomScrollbarState() {
  const body = accountTableBodyScrollableRef.value;
  const bottom = accountTableBottomScrollbarRef.value;
  if (!body || !bottom) {
    showAccountBottomScrollbar.value = false;
    return;
  }

  const hasOverflow = body.scrollWidth - body.clientWidth > 1;
  showAccountBottomScrollbar.value = hasOverflow;
  if (!hasOverflow) {
    bottom.scrollLeft = 0;
    return;
  }

  if (Math.abs(bottom.scrollLeft - body.scrollLeft) > 1) {
    bottom.scrollLeft = body.scrollLeft;
  }
}
function scheduleAccountBottomScrollbarSync() {
  clearAccountBottomScrollbarRaf();
  accountBottomScrollbarRafId = window.requestAnimationFrame(() => {
    accountBottomScrollbarRafId = null;
    syncAccountBottomScrollbarState();
  });
}
function updateAccountTableContentWidth() {
  const body = accountTableBodyScrollableRef.value;
  if (body) {
    // 优先读取 DOM 的 scrollWidth（含所有列宽），保证滚动条范围 100% 准确。
    accountTableContentWidth.value = body.scrollWidth;
  } else {
    // 回退到 Reactivity 计算值。
    accountTableContentWidth.value = accountTableScrollX.value;
  }
}
function unbindAccountTableResizeObserver() {
  if (accountTableResizeObserver) {
    accountTableResizeObserver.disconnect();
    accountTableResizeObserver = null;
  }
}
function bindAccountTableResizeObserver() {
  unbindAccountTableResizeObserver();
  const body = accountTableBodyScrollableRef.value;
  if (!body) return;

  accountTableResizeObserver = new ResizeObserver(() => {
    updateAccountTableContentWidth();
    scheduleAccountBottomScrollbarSync();
  });
  
  // 监听表格 body 容器的尺寸变化。
  accountTableResizeObserver.observe(body);
  
  // 针对内部 table 元素也要监听，因为列宽调整可能不改变容器 clientWidth 但改变 scrollWidth。
  const tableEl = body.querySelector('table');
  if (tableEl) {
    accountTableResizeObserver.observe(tableEl);
  }
  
  updateAccountTableContentWidth();
}
function unbindAccountTableBodyScrollSync() {
  if (accountTableBodyScrollableRef.value && accountTableBodyScrollHandler) {
    accountTableBodyScrollableRef.value.removeEventListener('scroll', accountTableBodyScrollHandler);
  }
  accountTableBodyScrollHandler = null;
  accountTableBodyScrollableRef.value = null;
}
function bindAccountTableBodyScrollSync() {
  const listRegion = accountTableListRegionRef.value;
  const hitBody = listRegion?.querySelector('.arco-table-body') as HTMLElement | null;
  if (!hitBody) {
    unbindAccountTableBodyScrollSync();
    showAccountBottomScrollbar.value = false;
    return;
  }
  /**
   * Arco 在 scrollbar 模式下，真实滚动节点是 `.arco-scrollbar-container`。
   * 这里优先命中该节点；若未启用则回退到 `.arco-table-body`。
   */
  const scrollHost = (hitBody.querySelector('.arco-scrollbar-container') as HTMLElement | null) || hitBody;
  if (accountTableBodyScrollableRef.value === scrollHost && accountTableBodyScrollHandler) {
    scheduleAccountBottomScrollbarSync();
    return;
  }

  unbindAccountTableBodyScrollSync();
  accountTableBodyScrollableRef.value = scrollHost;
  accountTableBodyScrollHandler = () => {
    const bottom = accountTableBottomScrollbarRef.value;
    const body = accountTableBodyScrollableRef.value;
    if (!bottom || !body) return;
    if (accountBottomScrollbarSyncing) return;
    accountBottomScrollbarSyncing = true;
    bottom.scrollLeft = body.scrollLeft;
    window.requestAnimationFrame(() => {
      accountBottomScrollbarSyncing = false;
      syncAccountBottomScrollbarState();
    });
  };
  scrollHost.addEventListener('scroll', accountTableBodyScrollHandler, { passive: true });
  bindAccountTableResizeObserver();
  scheduleAccountBottomScrollbarSync();
}
function handleAccountBottomScrollbarScroll(event: Event) {
  const body = accountTableBodyScrollableRef.value;
  if (!body) return;
  if (accountBottomScrollbarSyncing) return;
  const target = event.target as HTMLElement;
  accountBottomScrollbarSyncing = true;
  body.scrollLeft = target.scrollLeft;
  window.requestAnimationFrame(() => {
    accountBottomScrollbarSyncing = false;
  });
}
const moduleOptions = computed(() => authorizationDrawer.snapshot?.groups.map(item => ({ 
  label: getModuleDisplayName(item.moduleName), 
  value: item.moduleName 
})) || []);
const filteredAuthorizationGroups = computed(() => { const groups = authorizationDrawer.snapshot?.groups || []; const keyword = authorizationFilters.keyword.trim().toLowerCase(); return groups.filter(group => !authorizationFilters.moduleName || group.moduleName === authorizationFilters.moduleName).map(group => ({ ...group, controllers: group.controllers.map(controller => ({ ...controller, apis: controller.apis.filter(api => { const keywordMatched = !keyword || api.apiName.toLowerCase().includes(keyword) || api.apiUrl.toLowerCase().includes(keyword) || (api.description || '').toLowerCase().includes(keyword) || controller.controllerName.toLowerCase().includes(keyword) || group.moduleName.toLowerCase().includes(keyword); const methodMatched = !authorizationFilters.method || api.httpMethod === authorizationFilters.method; return keywordMatched && methodMatched; }) })).filter(controller => controller.apis.length > 0) })).filter(group => group.controllers.length > 0); });
const authorizationTreeData = computed<ApiCatalogTreeNode[]>(() => filteredAuthorizationGroups.value.map(group => ({
  id: `module:${group.moduleName}`,
  label: getModuleDisplayName(group.moduleName),
  type: 'MODULE',
  description: `${countApisInGroup(group)} 个可授权接口`,
  children: group.controllers.map(controller => ({
    id: `controller:${group.moduleName}:${controller.controllerName}`,
    label: getControllerDisplayName(controller.controllerName),
    type: 'RESOURCE',
    description: `${controller.apis.length} 个接口`,
    children: controller.apis.map(api => ({
      id: buildApiKey(api.id),
      label: api.apiName,
      type: 'API',
      httpMethod: api.httpMethod,
      apiUrl: api.apiUrl
    }))
  }))
})));
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
    pageNum: accountTablePagination.current,
    pageSize: accountTablePagination.pageSize,
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
    accountTablePagination.total = data.total || 0;
  } finally {
    tableLoading.value = false;
  }
}
async function refreshPage() { await loadPageData(); }

const ACCOUNT_MODAL_SAFE_MARGIN = 0;
const ACCOUNT_MODAL_MIN_WIDTH = 900;
const ACCOUNT_MODAL_MIN_HEIGHT = 520;
const ACCOUNT_MODAL_DEFAULT_HEIGHT = 700;
const ACCOUNT_MODAL_PAGE_LOCK_CLASS = 'account-modal-page-lock';
/**
 * 弹窗打开期间的页面滚动锁状态。
 * - scrollTop：记录弹窗打开前页面滚动位置，关闭后用于精准恢复。
 * - locked：避免重复加锁/解锁时反复覆盖 body 定位样式。
 *
 * 这样处理后，背景页会被真正固定在原位置，第①层标题栏也不会因为页面滚动而发生位移。
 */
const accountModalPageLockState = reactive({
  scrollTop: 0,
  locked: false
});
/**
 * 当前页面级滚动容器锁定快照。
 * 除了浏览器窗口本身，布局内容区与业务页根容器也可能承担滚动；
 * 因此需要统一记录并在弹窗关闭后恢复，避免“标题层跟着页面容器一起跑”的问题再次出现。
 */
const accountModalScrollLockTargets = ref<AccountModalScrollLockTargetState[]>([]);
/**
 * Arco Modal 外层 wrapper 锁定快照。
 * 真正显示在最右侧的滚动条来自 `.arco-modal-wrapper`；
 * 如果不把这一层关掉，鼠标滚轮会推动整个弹窗壳体一起上移，导致第①层标题栏看起来“仍在动”。
 */
const accountModalWrapperLockState = ref<AccountModalWrapperLockState | null>(null);

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
function pushUniqueScrollLockElement(elements: HTMLElement[], element: HTMLElement | null | undefined) {
  if (!element) return;
  if (elements.includes(element)) return;
  elements.push(element);
}
/**
 * 解析当前页实际参与滚动的容器链。
 * 之所以不只锁 body，是因为当前系统布局采用“内容区内部滚动”模式，
 * 真正可滚动的节点可能是业务页根容器、`page-container` 或 Arco Layout 内容区。
 */
function resolveAccountModalScrollLockElements() {
  const elements: HTMLElement[] = [];
  const pageElement = apiAccountPageRef.value;
  pushUniqueScrollLockElement(elements, pageElement);
  pushUniqueScrollLockElement(elements, pageElement?.closest('.page-container') as HTMLElement | null);
  pushUniqueScrollLockElement(elements, pageElement?.closest('.arco-layout-content') as HTMLElement | null);
  return elements;
}
/**
 * 锁定当前页所有可能参与滚动的容器。
 * 使用内联样式直接覆盖，避免仅依赖 scoped CSS 时因为层级或容器变化导致锁定失效。
 */
function syncAccountModalContainerScrollLock(locked: boolean) {
  if (locked) {
    if (accountModalScrollLockTargets.value.length) return;
    accountModalScrollLockTargets.value = resolveAccountModalScrollLockElements().map(element => ({
      element,
      scrollTop: element.scrollTop,
      overflow: element.style.overflow,
      overscrollBehavior: element.style.overscrollBehavior,
      scrollbarGutter: element.style.scrollbarGutter
    }));
    accountModalScrollLockTargets.value.forEach(target => {
      target.element.style.overflow = 'hidden';
      target.element.style.overscrollBehavior = 'none';
      target.element.style.scrollbarGutter = 'stable';
    });
    return;
  }

  accountModalScrollLockTargets.value.forEach(target => {
    target.element.style.overflow = target.overflow;
    target.element.style.overscrollBehavior = target.overscrollBehavior;
    target.element.style.scrollbarGutter = target.scrollbarGutter;
    target.element.scrollTop = target.scrollTop;
  });
  accountModalScrollLockTargets.value = [];
}
/**
 * 锁定 Arco Modal 外层滚动壳。
 * 该层默认 `overflow: auto`，需要显式关闭，才能保证滚轮只作用于弹窗内部内容区。
 */
function syncAccountModalWrapperScrollLock(locked: boolean) {
  const modalElement = document.querySelector('.account-modal') as HTMLElement | null;
  const wrapperElement = modalElement?.closest('.arco-modal-wrapper') as HTMLElement | null;
  if (locked) {
    if (!wrapperElement || accountModalWrapperLockState.value) return;
    accountModalWrapperLockState.value = {
      element: wrapperElement,
      scrollTop: wrapperElement.scrollTop,
      overflow: wrapperElement.style.overflow,
      overscrollBehavior: wrapperElement.style.overscrollBehavior
    };
    wrapperElement.style.overflow = 'hidden';
    wrapperElement.style.overscrollBehavior = 'none';
    wrapperElement.scrollTop = 0;
    return;
  }

  const state = accountModalWrapperLockState.value;
  if (!state) return;
  state.element.style.overflow = state.overflow;
  state.element.style.overscrollBehavior = state.overscrollBehavior;
  state.element.scrollTop = state.scrollTop;
  accountModalWrapperLockState.value = null;
}
/**
 * 对弹窗宽高与位移进行统一约束。
 * 允许弹窗通过拖拽和缩放贴合浏览器四边，确保可直接拉满整个视口。
 */
function clampAccountModalViewport() {
  if (accountModalController.fullscreen.value) {
    return;
  }

  const widthBounds = getAccountModalWidthBounds();
  const heightBounds = getAccountModalHeightBounds();
  accountModalViewport.width = clampValue(accountModalViewport.width, widthBounds.min, widthBounds.max);
  accountModalViewport.height = clampValue(accountModalViewport.height, heightBounds.min, heightBounds.max);

  if (accountModal.mode === 'create') {
    /**
     * 创建态采用绝对定位坐标：
     * left / top 直接表示弹层左上角位置，因此边界约束只需要保证窗口整体不越出视口。
     */
    const maxLeft = Math.max(0, window.innerWidth - accountModalViewport.width - ACCOUNT_MODAL_SAFE_MARGIN);
    const maxTop = Math.max(0, window.innerHeight - accountModalViewport.height - ACCOUNT_MODAL_SAFE_MARGIN);
    accountModalViewport.left = clampValue(accountModalViewport.left, ACCOUNT_MODAL_SAFE_MARGIN, maxLeft);
    accountModalViewport.top = clampValue(accountModalViewport.top, ACCOUNT_MODAL_SAFE_MARGIN, maxTop);
    return;
  }

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
  accountModalViewport.width = getAccountModalDefaultWidth(mode);
  accountModalViewport.height = ACCOUNT_MODAL_DEFAULT_HEIGHT;
  /**
   * 新建/编辑账号弹窗每次打开都回到“浏览器视口中心”。
   * 这里统一调用可复用的 recenter 能力，避免页面层自行拼装 left/top 计算。
   */
  accountModalController.recenter({
    width: getAccountModalDefaultWidth(mode),
    height: ACCOUNT_MODAL_DEFAULT_HEIGHT,
    force: true
  });
}


function handleAccountModalWindowResize() {
  /**
   * 新建账号弹窗采用自定义可拖拽层。
   * 视口尺寸变化时，统一回到中心点，避免出现“窗口偏离可视区中心”的体验问题。
   */
  if (accountModal.visible && !accountModalController.fullscreen.value) {
    accountModalController.centerInViewport();
    return;
  }
  clampAccountModalViewport();
}
/**
 * 锁定页面滚动，只保留弹窗白卡自身滚动。
 * 这样用户滚动鼠标时，交互焦点始终停留在新增账号弹窗内，不会把背景页面一起带动。
 */
function syncAccountModalPageScrollLock(locked: boolean) {
  if (typeof window === 'undefined') return;

  const html = document.documentElement;
  const body = document.body;
  if (locked) {
    if (accountModalPageLockState.locked) return;
    const scrollTop = window.scrollY || html.scrollTop || body.scrollTop || 0;
    accountModalPageLockState.scrollTop = scrollTop;
    accountModalPageLockState.locked = true;
    html.classList.add(ACCOUNT_MODAL_PAGE_LOCK_CLASS);
    body.classList.add(ACCOUNT_MODAL_PAGE_LOCK_CLASS);
    body.style.setProperty('--account-modal-page-lock-top', `-${scrollTop}px`);
    html.style.overflow = 'hidden';
    body.style.overflow = 'hidden';
    html.style.overscrollBehavior = 'none';
    body.style.overscrollBehavior = 'none';
    syncAccountModalContainerScrollLock(true);
    return;
  }

  if (!accountModalPageLockState.locked) return;
  const restoreScrollTop = accountModalPageLockState.scrollTop;
  accountModalPageLockState.locked = false;
  html.classList.remove(ACCOUNT_MODAL_PAGE_LOCK_CLASS);
  body.classList.remove(ACCOUNT_MODAL_PAGE_LOCK_CLASS);
  body.style.removeProperty('--account-modal-page-lock-top');
  html.style.overflow = '';
  body.style.overflow = '';
  html.style.overscrollBehavior = '';
  body.style.overscrollBehavior = '';
  syncAccountModalContainerScrollLock(false);
  window.scrollTo({ top: restoreScrollTop, left: 0, behavior: 'auto' });
}

/**
 * 新建账号弹窗触发器浮层层级同步：
 * 自定义 Teleport 弹层 z-index 较高，若不提升 Arco 触发器浮层，
 * Select / DatePicker 的下拉面板会被遮罩压住，出现“无法选择”的现象。
 */
function syncAccountCreatePopupLayerLock(locked: boolean) {
  if (typeof document === 'undefined') return;
  document.body.classList.toggle('account-create-popup-open', locked);
}
function toggleAccountModalFullscreen() {
  accountModalController.toggleFullscreen();
}
/**
 * 统一关闭账号治理弹窗。
 * 将关闭入口收口到同一方法，避免标题栏、底部按钮等多个入口后续出现行为分叉。
 */
/**
 * 在弹窗内部执行“复制到新建”：
 * 将当前表单数据保留，但将模式切换为 'create'，并重置编辑 ID。
 * 标题和提交按钮文本会自动更新。
 */
/**
 * 在弹窗内部执行“以此为模板复制并新建”：
 * 采用后端驱动的克隆模式，确保审计字段、主键 ID 与 AK 的清理逻辑在服务端闭环，
 * 前端只负责接管新数据并触发“模式切换”视觉动效。
 */
async function handleCopyInsideModal() {
  if (!accountModal.editingId) return;
  
  accountModal.submitting = true;
  try {
    const { data } = await fetchApiAccountCopy(accountModal.editingId);
    
    // 切换至新建模式
    accountModal.mode = 'create';
    accountModal.editingId = 0;
    
    // 使用后端生成的克隆数据回填表单（已包含 _Copy 后缀）
    fillAccountForm(data);
    
    // 触发一个极简的视觉反馈，让用户感知“保存”变为了“创建”
    Message.info({
      content: '已切换至全量克隆模式，修改后即可保存为新账号',
      icon: () => h(IconCopy, { style: { color: '#165dff' } })
    });
  } finally {
    accountModal.submitting = false;
  }
}
function closeAccountModal() {
  accountModal.visible = false;
}
function resetAccountForm() { Object.assign(accountForm, { accountName: '', description: '', ownerName: '', ownerContact: '', systemName: '', systemCode: '', accountType: 2, clientTypes: [], accessEnvironment: 'production', signVersion: 'v1', allowedScopes: [], environmentIpWhitelistText: createEmptyEnvironmentWhitelistText(), callbackUrl: '', rateLimit: 1000, expireTime: null, status: 1, remark: '' }); }
function fillAccountForm(detail: ApiAccountDetail) { Object.assign(accountForm, { accountName: detail.accountName, description: detail.description || '', ownerName: detail.ownerName, ownerContact: detail.ownerContact, systemName: detail.systemName, systemCode: detail.systemCode, accountType: detail.accountType, clientTypes: detail.clientTypes || [], accessEnvironment: (detail.accessEnvironment as AccessEnvironment) || 'production', signVersion: detail.signVersion || 'v1', allowedScopes: detail.allowedScopes || [], environmentIpWhitelistText: buildEnvironmentWhitelistTextMap(detail.environmentIpWhitelist, detail.ipWhitelist, detail.accessEnvironment as AccessEnvironment | undefined), callbackUrl: detail.callbackUrl || '', rateLimit: detail.rateLimit || 1000, expireTime: detail.expireTime || null, status: detail.status, remark: detail.remark || '' }); }
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
function handleAccountRowDblClick(record: ApiAccountItem) { void handleEditAccount(record); }

async function handleEditAccount(record: ApiAccountItem) { await openEditModalById(record.id); }
async function handleAuthorizeAccount(record: ApiAccountItem) { await openAuthorizationDrawerById(record.id); }
async function handleCallbackLogAccount(record: ApiAccountItem) { await openCallbackLogDrawerById(record.id); }

/**
 * 列表项复制：获取行数据详情并进入新建模式。
 * 逻辑与弹窗内复制一致，但针对列表项触发。
 */
/**
 * 列表项复制：采用标准化 API 驱动 cloner 模式。
 * 相比于前端直接浅拷贝 JS 对象，调用 /copy 接口能确保后端拦截器、审计字段清理
 * 以及业务规则（如名称后缀、权限剥离）在多端统一。
 */
async function handleCopyAccount(record: ApiAccountItem) {
  tableLoading.value = true;
  try {
    const { data } = await fetchApiAccountCopy(record.id);
    
    accountModal.mode = 'create';
    accountModal.editingId = 0;
    
    // 数据回填
    fillAccountForm(data);
    
    // 定制化初始化视口位姿
    resetAccountModalViewport('create');
    accountModal.visible = true;
    
    Message.info('已复制当前账号信息，可在此基础上创建新账号');
  } finally {
    tableLoading.value = false;
  }
}
async function handleResetSecretById(accountId: number) {
  const { data } = await resetApiAccountSecret(accountId);
  const account = accountList.value.find(a => a.id === accountId);
  credentialModal.payload = {
    ...data,
    rateLimit: account?.rateLimit,
    authorizedApiCount: account?.authorizedApiCount
  };
  credentialModal.visible = true;
  Message.success('密钥已重置');
  await refreshPage();
}
async function handleDeleteAccountById(accountId: number) {
  await deleteApiAccount(accountId);
  Message.success('账号已删除');
  if (accountTablePagination.current > 1 && accountList.value.length === 1) accountTablePagination.current -= 1;
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
// 详情预览弹窗已移除。
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
      credentialModal.payload = {
        ...data,
        rateLimit: accountForm.rateLimit,
        authorizedApiCount: 0
      };
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
async function handleSearch() { accountTablePagination.current = 1; await loadPageData(); }
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
  accountTablePagination.current = 1;
  await loadPageData();
}
async function handlePageChange(page: number) { accountTablePagination.current = page; await loadPageData(); }
async function handlePageSizeChange(pageSize: number) { accountTablePagination.pageSize = pageSize; accountTablePagination.current = 1; await loadPageData(); }
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

function isEnabledApiId(apiId: number, groups: OpenApiGroupNode[]) { return groups.some(group => group.controllers.some(controller => controller.apis.some(api => api.id === apiId && api.status === 1))); }
function getAccountTypeLabel(value: number) { return accountTypeOptions.find(item => item.value === value)?.label || '未知类型'; }
function getAccountTypeTagColor(value: number) { return value === 1 ? 'arcoblue' : 'purple'; }
function getClientTypeLabels(values?: string[]) { return (values || []).map(value => clientTypeOptions.find(item => item.value === value)?.label || value); }
function getCompactClientSummary(values?: string[]) {
  const labels = getClientTypeLabels(values);
  if (!labels.length) return '未配置';
  return labels.length <= 2 ? labels.join('、') : `${labels.slice(0, 2).join('、')} +${labels.length - 2}`;
}
/**
 * 客户端完整文本：
 * 列内仍保持紧凑摘要，悬浮时显示完整客户端集合，兼顾可读性与信息完整度。
 */
function getFullClientTypeSummary(values?: string[]) {
  const labels = getClientTypeLabels(values);
  if (!labels.length) return '未配置';
  return labels.join('、');
}
function getAccessEnvironmentLabel(value?: string | null) { return environmentOptions.find(item => item.value === value)?.label || '未设置环境'; }
function getEnvironmentTagColor(value?: string | null) { return ({ test: 'arcoblue', staging: 'orange', production: 'green' } as Record<string, string>)[value || ''] || 'gray'; }
function getStatusLabel(value: number) { return value === 1 ? '启用' : '停用'; }

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
  } else if (action === 'copy') {
    await handleCopyAccount({ id: accountId } as ApiAccountItem);
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
onMounted(async () => {
  await initializePage();
  await nextTick();
  bindAccountTableBodyScrollSync();
  scheduleAccountBottomScrollbarSync();
  window.addEventListener('resize', scheduleAccountBottomScrollbarSync);
});
watch(
  () => ({
    loading: tableLoading.value,
    size: accountList.value.length,
    columnSignature: accountTableColumns.value.map(column => `${column.key}:${column.width}`).join('|')
  }),
  () => {
    void nextTick(() => {
      bindAccountTableBodyScrollSync();
      scheduleAccountBottomScrollbarSync();
    });
  },
  { flush: 'post' }
);
watch(() => [route.query.action, route.query.accountId], async () => {
  if (route.path === '/admin/api/account') {
    await handleManageRouteAction();
  }
});
// 详情相关 Watch 已移除。
watch(() => accountModal.visible, async visible => {
  syncAccountCreatePopupLayerLock(visible);

  if (visible) {
    syncAccountModalPageScrollLock(true);
    clampAccountModalViewport();
    await nextTick();
    window.addEventListener('resize', handleAccountModalWindowResize);
    return;
  }

  syncAccountModalPageScrollLock(false);
  syncAccountCreatePopupLayerLock(false);
  window.removeEventListener('resize', handleAccountModalWindowResize);
});
onBeforeUnmount(() => {
  clearAccountBottomScrollbarRaf();
  unbindAccountTableBodyScrollSync();
  unbindAccountTableResizeObserver();
  window.removeEventListener('resize', scheduleAccountBottomScrollbarSync);
  if (!isCreateMode.value) {
    syncAccountModalWrapperScrollLock(false);
  }
  syncAccountModalPageScrollLock(false);
  syncAccountCreatePopupLayerLock(false);
  window.removeEventListener('resize', handleAccountModalWindowResize);
});
</script>

<style scoped>
:global(html.account-modal-page-lock),
:global(body.account-modal-page-lock) {
  overflow: hidden;
  overscroll-behavior: none;
}

:global(body.account-modal-page-lock) {
  position: fixed;
  top: var(--account-modal-page-lock-top, 0px);
  left: 0;
  right: 0;
  width: 100%;
}

/**
 * 新建弹窗打开时统一抬高 Arco 触发器浮层：
 * 覆盖 Select / DatePicker / Dropdown 的默认层级，确保可见可点选。
 */
:global(body.account-create-popup-open .arco-trigger-popup),
:global(body.account-create-popup-open .arco-select-dropdown),
:global(body.account-create-popup-open .arco-picker-dropdown),
:global(body.account-create-popup-open .arco-picker-container),
:global(body.account-create-popup-open .arco-time-picker-popup),
:global(body.account-create-popup-open .arco-dropdown) {
  z-index: 3205 !important;
}

:global(body.account-modal-page-lock) .api-account-page {
  /**
   * 弹窗打开时，仅锁定当前页面容器滚动，防止右侧全局滚动条继续出现。
   * 滚动统一由弹窗内部承载区处理，确保第①层保持固定不动。
   */
  overflow: hidden !important;
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
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 24px;
  background: linear-gradient(180deg, var(--page-bg-start), var(--page-bg-end));
  overflow: hidden;
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
  /**
   * API 账号页查询区主题变量。
   * 采用“冰川玻璃”视觉：弱化炫光、强化层次，保证长时间查看仍然耐看。
   */
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
  --query-panel-shell-border-color: rgba(150, 182, 214, 0.74);
  --query-panel-shell-background:
    radial-gradient(circle at 3% -10%, rgba(88, 152, 228, 0.2), transparent 40%),
    radial-gradient(circle at 100% 110%, rgba(32, 177, 162, 0.16), transparent 42%),
    linear-gradient(148deg, rgba(253, 255, 255, 0.95), rgba(242, 249, 255, 0.9));
  --query-panel-shell-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 16px 36px rgba(19, 58, 108, 0.13),
    0 1px 0 rgba(255, 255, 255, 0.9);
  --query-panel-accent-bar-background: linear-gradient(90deg, #2b7de6 0%, #3f9be7 46%, #22b5a5 100%);
  --query-panel-accent-bar-opacity: 0.84;
  --query-panel-orb-background:
    radial-gradient(circle at 34% 30%, rgba(140, 205, 255, 0.3), rgba(140, 205, 255, 0.04) 58%, transparent 82%);
  --query-panel-orb-top: -70px;
  --query-panel-orb-right: -62px;
  --query-panel-orb-size: 166px;
  --query-panel-orb-opacity: 0.55;
  --query-panel-orb-filter: blur(2px);
  --query-panel-body-border-color: rgba(158, 190, 224, 0.56);
  --query-panel-body-background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.84), rgba(241, 248, 255, 0.8)),
    linear-gradient(0deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0.34));
  --query-panel-body-overlay:
    linear-gradient(126deg, rgba(255, 255, 255, 0.44), rgba(255, 255, 255, 0.03) 42%),
    repeating-linear-gradient(135deg, rgba(132, 166, 201, 0.14) 0 1px, transparent 1px 9px);
  --query-panel-body-overlay-opacity: 0.24;
  --query-panel-body-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 10px 20px rgba(23, 67, 124, 0.1);
  --query-panel-input-border-color: rgba(160, 186, 216, 0.76);
  --query-panel-input-background: rgba(255, 255, 255, 0.82);
  --query-panel-input-hover-border-color: rgba(71, 136, 219, 0.6);
  --query-panel-input-hover-background: rgba(255, 255, 255, 0.9);
  --query-panel-input-focus-border-color: rgba(56, 124, 214, 0.82);
  --query-panel-input-focus-shadow: 0 0 0 4px rgba(72, 139, 225, 0.12);
  --query-panel-muted-button-border: rgba(155, 185, 216, 0.72);
  --query-panel-muted-button-background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(242, 248, 255, 0.9));
  --query-panel-muted-button-color: #3b5a7b;
  --query-panel-muted-button-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.94),
    0 6px 14px rgba(36, 102, 169, 0.08);
  margin-top: var(--api-account-query-offset-y);
  flex-shrink: 0;
}

.query-panel__advanced {
  /**
   * 查询区改为两层结构：
   * 扩展条件并入主字段层（第②层），仅在展开时追加渲染，
   * 不再使用第③层独立边框盒，避免视觉割裂。
   */
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed rgba(159, 187, 217, 0.56);
}

.query-panel__toggle-btn {
  min-width: 96px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  border-color: rgba(153, 186, 219, 0.72);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(242, 248, 255, 0.9));
  color: #39597b;
  font-weight: 700;
  font-size: 11px;
  transition: all 0.2s ease;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.94),
    0 6px 12px rgba(35, 100, 167, 0.08);
}

.query-panel__toggle-btn:hover {
  border-color: rgba(96, 146, 205, 0.76);
  color: #2d4d71;
  transform: translateY(-1px);
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
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease, filter 0.2s ease;
  box-shadow: 0 7px 14px rgba(22, 97, 186, 0.16);
}

.query-panel__mode-btn:hover {
  transform: translateY(-1px);
}

.query-panel__mode-btn.is-active {
  opacity: 1;
  filter: saturate(1.03) contrast(1.02);
}

.query-panel__mode-btn.is-inactive {
  opacity: 0.72;
  box-shadow: 0 4px 8px rgba(56, 116, 181, 0.08);
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
  /**
   * 列表舞台高度策略：
   * 在首屏下给列表区一个稳定的最小高度，配合 body-fill 与表格 flex 伸展，
   * 让卡片始终铺满列表区域，避免底部出现大块空白。
   * 为实现吸底效果，移除 clamp 限制，改用 flex: 1。
   */
  margin-top: var(--api-account-stage-gap-y);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.table-shell :deep(.governance-list-stage__body) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.table-shell__split {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.table-shell__list-region {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

/**
 * 明确约束列表组件高度继承：
 * list-region 作为“可伸缩主区域”时，account-table 默认撑满 100%，
 * 让上方列表始终占满“除分页外”的全部可用高度。
 */
.table-shell__list-region>.account-table {
  height: 100%;
}

/**
 * 滚动条美化 (Premium Scrollbar Styling):
 * 为中间列表区域增加符合冰川蓝主题的垂直滚动条，
 * 采用半透明背景与渐变滑块，提升交互反馈感。
 */
.account-table :deep(.arco-scrollbar-track-direction-vertical) {
  width: 8px;
  background-color: rgba(226, 232, 240, 0.45);
  border-radius: 999px;
  margin-right: 2px;
}

.account-table :deep(.arco-scrollbar-thumb-direction-vertical) {
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.76), rgba(100, 116, 139, 0.68));
  border: 1.5px solid rgba(255, 255, 255, 0.4);
  border-radius: 999px;
  transition: background 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.account-table :deep(.arco-scrollbar-thumb-direction-vertical:hover) {
  background: linear-gradient(180deg, rgba(100, 116, 139, 0.88), rgba(71, 85, 105, 0.82));
}

.table-field-cell {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-field-cell--index {
  justify-content: center;
  font-weight: 500;
  color: var(--text-secondary);
  width: 100%;
}

/**
 * 隐藏 Arco 内置的横向滚动条 (Hide Built-in Horizontal Scrollbar):
 * 由于下方已经存在自定义同步的横向滚动条，
 * 这里通过 CSS 隐藏 Arco 表格内部产生的横向滚动条，避免视觉重合。
 */
.account-table :deep(.arco-scrollbar-track-direction-horizontal),
.account-table :deep(.arco-scrollbar-thumb-direction-horizontal) {
  display: none !important;
}

/**
 * 自定义横向滚动条样式优化
 * 使用现代化设计，与授权抽屉的滚动条风格保持一致
 */
.table-shell__x-scrollbar {
  flex-shrink: 0;
  height: 14px; /* 从 12px 增加到 14px，提供更好的可点击区域 */
  margin-top: 10px; /* 从 8px 增加到 10px，与表格保持更好的间距 */
  overflow-x: auto;
  overflow-y: hidden;
  border-radius: 10px; /* 从 999px 改为 10px，更现代的圆角 */
  background: rgba(241, 245, 249, 0.6); /* 添加背景色，让滚动条轨道更明显 */
  padding: 2px; /* 添加内边距，让滚动条滑块与轨道有间隙 */
}

.table-shell__x-scrollbar-track {
  height: 1px;
}

/**
 * Webkit 浏览器（Chrome, Safari, Edge）滚动条样式
 */
.table-shell__x-scrollbar::-webkit-scrollbar {
  height: 10px; /* 保持 10px 高度 */
}

.table-shell__x-scrollbar::-webkit-scrollbar-track {
  background: rgba(241, 245, 249, 0.6); /* 使用更柔和的轨道颜色 */
  border-radius: 10px;
  margin: 0 4px; /* 添加左右边距，让滚动条不贴边 */
}

.table-shell__x-scrollbar::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #cbd5e1 0%, #94a3b8 100%); /* 使用渐变色，更有质感 */
  border-radius: 10px;
  transition: background 0.3s ease; /* 添加过渡动画 */
}

.table-shell__x-scrollbar::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #94a3b8 0%, #64748b 100%); /* 悬停时颜色加深 */
}

.table-shell__x-scrollbar::-webkit-scrollbar-thumb:active {
  background: linear-gradient(180deg, #64748b 0%, #475569 100%); /* 点击时颜色更深 */
}

/**
 * Firefox 浏览器滚动条样式
 */
.table-shell__x-scrollbar {
  scrollbar-color: #cbd5e1 rgba(241, 245, 249, 0.6); /* 滑块颜色 轨道颜色 */
  scrollbar-width: thin; /* 使用细滚动条 */
}

.table-shell__footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-shrink: 0;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid rgba(214, 225, 239, 0.82);
}

.table-shell__footer :deep(.arco-pagination) {
  margin-left: auto;
}

.table-shell__footer :deep(.arco-pagination-item),
.table-shell__footer :deep(.arco-pagination-jumper-input),
.table-shell__footer :deep(.arco-pagination-btn) {
  border-radius: 999px;
}

.table-column-setting-btn {
  min-width: 88px;
}

/**
 * 列设置面板：
 * 在列表第①层右上角提供字段显隐、排序和恢复默认入口，
 * 支持业务用户按场景自定义列表信息密度。
 * 
 * 设计优化（V6 - 根据最新红线标注）：
 * 1. 弹窗宽度调整到红线位置（约280px）
 * 2. 弹窗高度保持合适尺寸
 * 3. 字体大小清晰易读
 * 4. 间距紧凑但不拥挤
 */
.table-column-setting-panel {
  width: 280px; /* 调整为 280px，对齐到红线位置 */
  padding: 10px 12px; /* 调整内边距以适应更窄的宽度 */
  border-radius: 12px;
  border: 1px solid rgba(209, 220, 235, 0.9);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 250, 255, 0.96));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.96),
    0 12px 24px rgba(16, 43, 82, 0.12);
}

.table-column-setting-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.table-column-setting-panel__head strong {
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
}

/**
 * 列设置面板列表容器样式优化
 * 
 * V5 优化（根据实际红线标注）：
 * 1. 最大高度调整为 320px，精确对齐到表格底部滚动条上方
 * 2. 避免弹窗过高导致超出表格范围
 * 3. 保持美观的滚动条样式
 */
.table-column-setting-panel__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px; /* 调整为 320px，精确对齐到表格底部滚动条上方 */
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px;
  /* 自定义滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 rgba(241, 245, 249, 0.6);
}

/**
 * Webkit 浏览器滚动条样式
 */
.table-column-setting-panel__list::-webkit-scrollbar {
  width: 6px;
}

.table-column-setting-panel__list::-webkit-scrollbar-track {
  background: rgba(241, 245, 249, 0.6);
  border-radius: 10px;
  margin: 4px 0;
}

.table-column-setting-panel__list::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #cbd5e1 0%, #94a3b8 100%);
  border-radius: 10px;
  transition: background 0.3s ease;
}

.table-column-setting-panel__list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #94a3b8 0%, #64748b 100%);
}

.table-column-setting-panel__list::-webkit-scrollbar-thumb:active {
  background: linear-gradient(180deg, #64748b 0%, #475569 100%);
}

/**
 * 列设置面板单项容器样式优化
 * 
 * V6 优化（适配更窄的宽度）：
 * 1. 调整间距以适应 280px 的宽度
 * 2. 保持舒适的点击区域
 */
.table-column-setting-panel__item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px; /* 减少到 4px，适应更窄的宽度 */
  padding: 6px 8px; /* 调整为 6px 8px，适应更窄的宽度 */
  min-height: 36px;
  border-radius: 6px;
  border: 1px solid rgba(220, 230, 243, 0.92);
  background: rgba(255, 255, 255, 0.92);
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.table-column-setting-panel__item.is-draggable {
  cursor: default;
}

.table-column-setting-panel__item.is-drag-source {
  opacity: 0.65;
  border-color: rgba(96, 146, 205, 0.5);
}

.table-column-setting-panel__item.is-drag-over-before::before,
.table-column-setting-panel__item.is-drag-over-after::after {
  content: '';
  position: absolute;
  left: 10px;
  right: 10px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, #2f80ed, #22b5a5);
}

.table-column-setting-panel__item.is-drag-over-before::before {
  top: -1px;
}

.table-column-setting-panel__item.is-drag-over-after::after {
  bottom: -1px;
}

/**
 * 列设置面板标签区域样式
 * 
 * V6 优化（适配更窄的宽度）：
 * 1. 减少间距到 4px，适应更窄的宽度
 */
.table-column-setting-panel__label {
  display: flex;
  align-items: center;
  gap: 4px; /* 减少到 4px，适应更窄的宽度 */
  min-width: 0;
  flex: 1;
}

/**
 * 列设置面板拖拽手柄样式
 * 
 * V6 优化（适配更窄的宽度）：
 * 1. 调整手柄尺寸以适应更窄的宽度
 */
.table-column-setting-panel__drag-handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px; /* 调整为 16px，适应更窄的宽度 */
  height: 16px;
  border-radius: 4px;
  color: #94a3b8;
  cursor: grab;
  user-select: none;
  flex: 0 0 auto;
  transition: color 0.2s ease, background-color 0.2s ease;
  font-size: 11px;
}

.table-column-setting-panel__drag-handle:hover {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.12);
}

.table-column-setting-panel__drag-handle:active {
  cursor: grabbing;
}

.table-column-setting-panel__label span {
  color: #334155;
  font-size: 12px; /* 调整为 12px，适应更窄的宽度 */
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-column-setting-panel__label small {
  padding: 1px 4px; /* 调整为 1px 4px，适应更窄的宽度 */
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.9);
  color: #64748b;
  font-size: 9px; /* 调整为 9px，适应更窄的宽度 */
  font-weight: 700;
  flex-shrink: 0;
}

/**
 * 列设置面板操作按钮区域样式优化
 * 
 * V6 优化（适配更窄的宽度）：
 * 1. 减少按钮间距到 3px，适应更窄的宽度
 */
.table-column-setting-panel__actions {
  display: inline-flex;
  align-items: center;
  gap: 3px; /* 减少到 3px，适应更窄的宽度 */
  flex-shrink: 0;
}

/**
 * 列设置面板开关样式优化
 * 确保开关按钮使用主题色，清晰可见且美观
 */
.table-column-setting-panel__actions :deep(.arco-switch) {
  flex-shrink: 0; /* 防止开关被压缩 */
}

.table-column-setting-panel__actions :deep(.arco-switch-checked) {
  background-color: var(--color-primary, #165dff) !important;
}

.table-column-setting-panel__actions :deep(.arco-switch-checked:hover) {
  background-color: var(--color-primary-light-4, #4080ff) !important;
}

.table-column-setting-panel__actions :deep(.arco-switch:not(.arco-switch-checked)) {
  background-color: #e5e6eb !important;
}

.table-column-setting-panel__actions :deep(.arco-switch:not(.arco-switch-checked):hover) {
  background-color: #c9cdd4 !important;
}

/**
 * 列设置面板按钮尺寸优化
 * 
 * V6 优化（适配更窄的宽度）：
 * 1. 调整按钮尺寸以适应更窄的宽度
 */
.table-column-setting-panel__fixed-btn,
.table-column-setting-panel__order-btn {
  width: 20px !important; /* 调整为 20px，适应更窄的宽度 */
  min-width: 20px !important;
  height: 20px !important;
  padding: 0 !important;
  border-radius: 4px !important;
  font-size: 11px !important;
}

/**
 * 列设置面板固定按钮样式
 * 
 * V6 优化（适配更窄的宽度）：
 * 1. 调整按钮尺寸以适应更窄的宽度
 */
.table-column-setting-panel__fixed-btn {
  width: 20px;
  min-width: 20px;
  height: 20px;
  padding: 0;
  border-radius: 4px;
  color: #64748b;
  border-color: rgba(203, 213, 225, 0.88);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.96));
  font-size: 11px;
}

.table-column-setting-panel__fixed-btn:hover {
  color: #1d4ed8;
  border-color: rgba(59, 130, 246, 0.52);
}

.table-column-setting-panel__fixed-btn.is-active {
  color: #1d4ed8;
  border-color: rgba(59, 130, 246, 0.62);
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.95), rgba(224, 242, 254, 0.95));
  box-shadow: inset 0 0 0 1px rgba(147, 197, 253, 0.48);
}

.table-column-setting-panel__fixed-btn:disabled,
.table-column-setting-panel__fixed-btn.is-active:disabled {
  color: #94a3b8;
  border-color: rgba(226, 232, 240, 0.9);
  background: rgba(248, 250, 252, 0.96);
  box-shadow: none;
}

/**
 * 列设置面板排序按钮样式
 * 
 * V6 优化（适配更窄的宽度）：
 * 1. 调整按钮尺寸以适应更窄的宽度
 */
.table-column-setting-panel__order-btn {
  width: 20px;
  min-width: 20px;
  height: 20px;
  padding: 0;
  border-radius: 4px;
  font-size: 11px;
}

.account-table {
  display: flex;
  flex-direction: column;
  /**
   * 列表内容保持顶部展示，同时让底部分页区稳定贴底。
   * 通过 flex:1 占满可用高度，横向滚动条与分页自然沉到底部。
   */
  flex: 1;
  width: 100%;
  min-height: 0;
}

/**
 * 列表字段单元格样式：
 * 每个单元格仅承载一个字段值，遵循“可扫读、可对比、可拖拽调宽”的数据表规范。
 */
.table-field-cell {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
}

.table-field-cell__primary,
.table-field-cell__text {
  min-width: 0;
  color: #1e293b;
  font-size: 14px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-field-cell__primary {
  font-weight: 700;
}

.table-field-cell__text {
  font-weight: 500;
}

.table-field-cell__mono {
  min-width: 0;
  color: #334155;
  font-family: 'SFMono-Regular', 'Consolas', 'Menlo', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
}

/**
 * 最近更新时间按要求保持完整展示：
 * 单行展示且不省略，列宽不足时由整体横向滚动承接。
 */
.table-field-cell--time {
  overflow: visible;
}

.table-field-cell--time .table-field-cell__mono {
  overflow: visible;
  text-overflow: clip;
}

/**
 * 操作列改为单行紧凑布局，避免多行堆叠导致单条记录高度被拉高。
 */
/**
 * 操作列按钮容器：
 * flex 紧凑排列，居中对齐，保证在固定宽度列内不溢出。
 */
.table-row-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 6px;
  width: 100%;
}

/**
 * 操作列通用按钮尺寸微调：
 * 不覆盖 Arco 原生颜色 / 圆角 / 字重，仅保证"不换行 + 不缩小"。
 * 详情、授权使用 type="primary" → 与"新建账号"完全一致的蓝色填充外观。
 * 更多使用默认 type → 与"列设置"完全一致的灰色描边外观。
 */
.table-action-btn {
  flex-shrink: 0;
  white-space: nowrap;
}

/**
 * 主要操作按钮样式强化
 * 确保按钮在任何主题下都清晰可见
 */
.table-action-btn--primary {
  background: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
  color: #fff !important;
}

.table-action-btn--primary:hover {
  background: var(--color-primary-light-4, #4080ff) !important;
  border-color: var(--color-primary-light-4, #4080ff) !important;
  color: #fff !important;
}

.table-action-btn--primary:active {
  background: var(--color-primary-dark-1, #0e42d2) !important;
  border-color: var(--color-primary-dark-1, #0e42d2) !important;
  color: #fff !important;
}

/**
 * 更多按钮样式
 * 使用默认灰色边框样式
 */
.table-action-btn--more {
  background: #fff !important;
  border-color: #e5e6eb !important;
  color: #4e5969 !important;
}

.table-action-btn--more:hover {
  background: #f7f8fa !important;
  border-color: #c9cdd4 !important;
  color: #1d2129 !important;
}

.table-action-btn--more:active {
  background: #f2f3f5 !important;
  border-color: #a9aeb8 !important;
  color: #1d2129 !important;
}

/**
 * 按钮图标颜色确保可见
 */
.table-action-btn--primary .arco-icon {
  color: #fff !important;
}

.table-action-btn--more .arco-icon {
  color: #4e5969 !important;
}

.table-action-btn--more:hover .arco-icon {
  color: #1d2129 !important;
}

/** 删除选项标红警示 */
.table-row-actions__danger {
  color: #f53f3f;
}

.account-table :deep(.arco-table-container) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
  border-radius: 12px;
  border: 1px solid rgba(220, 229, 240, 0.94);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.96);
}

.account-table :deep(.arco-table) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  background: transparent;
}

/**
 * 列表区使用“上表格 + 下分页”两段式结构：
 * 表格本体只负责吃满可用高度（滚动条随内容区沉底），
 * 分页独立放在外层 footer 中固定贴底并右对齐。
 */
.account-table :deep(.arco-spin),
.account-table :deep(.arco-spin-children) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.account-table :deep(.arco-spin-children > .arco-table-container) {
  flex: 1;
  min-height: 0;
}

.account-table :deep(.arco-table-content) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
}

.account-table :deep(.arco-table-body) {
  /**
   * 表体保持垂直滚动，横向滚动由底部独立滚动条代理并同步。
   * 注意：绝不能使用 overflow-x: hidden，否则 CSS position: sticky 会完全失效，
   * 导致 Arco 的 fixed 固定列（如操作列）无法固定在滚动视口边缘。
   * 正确做法：保留 overflow-x: auto 让浏览器知道这是一个可横向滚动的容器，
   * 然后通过 scrollbar-width: none 与 ::-webkit-scrollbar 仅在视觉上隐藏原生滚动条。
   */
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: auto;
  overflow-y: auto;
  /* Firefox：隐藏原生滚动条但保留滚动能力 */
  scrollbar-width: none;
}

/* Chrome / Safari / Edge：隐藏原生滚动条但保留滚动能力 */
.account-table :deep(.arco-table-body::-webkit-scrollbar:horizontal) {
  display: none;
  height: 0;
}

/**
 * 只保留底部独立横向滚动条：
 * 隐藏 Arco 内建的 scrollbar 组件轨道与滑块，避免出现"两条横向滚动条"。
 */
.account-table :deep(.arco-table-body .arco-scrollbar-track-direction-horizontal),
.account-table :deep(.arco-table-body .arco-scrollbar-thumb-direction-horizontal) {
  display: none !important;
}

.account-table :deep(.arco-table-th) {
  padding-top: 8px;
  padding-bottom: 8px;
  background: linear-gradient(180deg, #f6faff, #eff5fb);
  color: #334155;
  font-weight: 700;
  font-size: 12px;
  /* 表头标题始终单行显示，防止窄列时文字换行导致高度不一致 */
  white-space: nowrap;
}

/**
 * 为非固定列设定 position: relative，用于列分割线 ::after 伪元素的定位基准。
 * 注意：绝不能对 .arco-table-col-fixed-left / right 设定 position: relative，
 * 因为 Arco 的固定列依赖 position: sticky 来实现滚动时"钉住"效果，
 * 而 CSS 只允许一个 position 值，relative 会直接覆盖 sticky 导致固定列失效。
 */
.account-table :deep(.arco-table-th:not(.arco-table-col-fixed-left):not(.arco-table-col-fixed-right)),
.account-table :deep(.arco-table-td:not(.arco-table-col-fixed-left):not(.arco-table-col-fixed-right)) {
  position: relative;
}

/**
 * 列分割线与拖拽手柄视觉增强：
 * 让“表头拖拽调间距（列宽）”的交互意图更明显，贴近电子表格操作体验。
 */
.account-table :deep(.arco-table-th:not(:last-child)::after) {
  content: '';
  position: absolute;
  top: 12%;
  bottom: 12%;
  right: 0;
  width: 1px;
  background: linear-gradient(180deg, rgba(186, 201, 220, 0.24), rgba(186, 201, 220, 0.66), rgba(186, 201, 220, 0.24));
  pointer-events: none;
}

.account-table :deep(.arco-table-td:not(:last-child)::after) {
  content: '';
  position: absolute;
  top: 12%;
  bottom: 12%;
  right: 0;
  width: 1px;
  background: linear-gradient(180deg, rgba(186, 201, 220, 0.12), rgba(186, 201, 220, 0.33), rgba(186, 201, 220, 0.12));
  pointer-events: none;
}

/* 隐藏空位列的边框，原生Arco会生成 filler 但是不需要边框 */
.account-table :deep(.arco-table-td.arco-table-filler::after),
.account-table :deep(.arco-table-th.arco-table-filler::after) {
  display: none !important;
}

.account-table :deep(.arco-table-column-handle) {
  position: absolute;
  top: 0;
  right: -4px;
  width: 9px;
  height: 100%;
  cursor: col-resize;
  z-index: 6;
}

.account-table :deep(.arco-table-column-handle::before) {
  content: '';
  position: absolute;
  top: 16%;
  bottom: 16%;
  left: 50%;
  width: 2px;
  border-radius: 2px;
  transform: translateX(-50%);
  background: rgba(122, 147, 178, 0.32);
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.account-table :deep(.arco-table-th:hover .arco-table-column-handle::before),
.account-table :deep(.arco-table-th.arco-table-th-resizing .arco-table-column-handle::before) {
  background: linear-gradient(180deg, #2f80ed, #2ac8bf);
  box-shadow: 0 0 0 1px rgba(47, 128, 237, 0.16);
}

.account-table :deep(.arco-table-tr) {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.account-table :deep(tbody .arco-table-tr) {
  cursor: pointer;
}

.account-table :deep(.arco-table-td) {
  padding-top: 8px;
  padding-bottom: 8px;
  vertical-align: middle;
  background: #ffffff;
}

/* 恢复默认的高亮 hover 设定，使用不透明色以避免与悬浮列表格穿透层叠问题 */
.account-table :deep(.arco-table-tr:hover td),
.account-table :deep(.arco-table-tr:hover td.arco-table-col-fixed-right),
.account-table :deep(.arco-table-tr:hover td.arco-table-col-fixed-left) {
  background: #f5faff !important;
}

.account-table :deep(tbody .arco-table-tr:hover) {
  box-shadow: inset 0 0 0 1px rgba(23, 105, 255, 0.08);
}

/**
 * 表格横向节奏统一：
 * 普通列使用较舒展的左右内边距，操作列单独收窄，保证信息列可读性的同时压缩操作占位。
 */
.account-table :deep(.arco-table-th),
.account-table :deep(.arco-table-td) {
  padding-left: 14px;
  padding-right: 14px;
}

.account-table :deep(.arco-table-th:last-child),
.account-table :deep(.arco-table-td:last-child) {
  padding-left: 8px;
  padding-right: 8px;
}

.account-table :deep(.arco-tag) {
  height: 20px;
  line-height: 20px;
  padding: 0 7px;
  font-size: 11px;
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
  gap: 6px;
  min-width: 0;
}

.account-modal-title__markers {
  display: inline-flex;
  flex-direction: column;
  gap: 6px;
  width: fit-content;
  /* 极简模式：去除了陈旧的层级数字标记 */
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

.account-modal-title__window-grip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  min-width: 36px;
  height: 36px;
  border: 1px dashed rgba(148, 163, 184, 0.58);
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.92);
  color: #0f5de2;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.88);
  cursor: move;
}

.account-modal-title__window-grip :deep(.arco-icon) {
  font-size: 16px;
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

:deep(.account-modal.account-modal--create .account-modal-title) {
  align-items: flex-start;
  gap: 20px;
}

:deep(.account-modal.account-modal--create .account-modal-title__markers) {
  flex-direction: row;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.account-modal.account-modal--create .account-modal-title__main p) {
  color: #7c8ba1;
}

:deep(.account-modal.account-modal--create .account-modal-title__main strong) {
  font-size: 24px;
  color: #0f172a;
}

:deep(.account-modal.account-modal--create .account-modal-title__badge) {
  min-height: 40px;
  padding: 0 16px;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.08), rgba(18, 184, 166, 0.14));
  color: #0f766e;
  font-weight: 700;
}

:deep(.account-modal.account-modal--create .account-modal-title__actions) {
  flex: 0 0 auto;
  margin-left: auto;
}

.account-modal-resize-handle {
  position: absolute;
  z-index: 14;
  background: transparent;
}

.account-modal-resize-handle--n,
.account-modal-resize-handle--s {
  left: 18px;
  right: 18px;
  height: 16px;
}

.account-modal-resize-handle--n {
  top: 0;
  cursor: ns-resize;
}

.account-modal-resize-handle--s {
  bottom: 0;
  cursor: ns-resize;
}

.account-modal-resize-handle--e,
.account-modal-resize-handle--w {
  top: 18px;
  bottom: 18px;
  width: 16px;
}

.account-modal-resize-handle--e {
  right: 0;
  cursor: ew-resize;
}

.account-modal-resize-handle--w {
  left: 0;
  cursor: ew-resize;
}

.account-modal-resize-handle--ne,
.account-modal-resize-handle--nw,
.account-modal-resize-handle--se,
.account-modal-resize-handle--sw {
  width: 22px;
  height: 22px;
}

.account-modal-resize-handle--ne {
  top: 2px;
  right: 2px;
  cursor: nesw-resize;
}

.account-modal-resize-handle--nw {
  top: 2px;
  left: 2px;
  cursor: nwse-resize;
}

.account-modal-resize-handle--se {
  right: 2px;
  bottom: 2px;
  cursor: nwse-resize;
}

.account-modal-resize-handle--sw {
  bottom: 2px;
  left: 2px;
  cursor: nesw-resize;
}



:deep(.account-modal.account-modal--create.is-dragging) {
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.58),
    0 36px 96px rgba(15, 23, 42, 0.28);
}

:deep(.account-modal.account-modal--create.is-resizing) {
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.62),
    0 0 0 1px rgba(59, 130, 246, 0.26),
    0 32px 92px rgba(15, 23, 42, 0.26);
}

:deep(.account-modal.account-modal--create.arco-modal-fullscreen) .account-modal-resize-handle {
  display: none;
}

.account-modal-shell {
  position: relative;
  display: block;
  flex: none;
  height: auto;
  min-height: max-content;
}

.account-modal-shell--create {
  display: block;
  min-height: max-content;
  padding-bottom: 20px;
}

.account-create-viewport {
  position: relative;
  display: block;
  min-height: max-content;
}

.account-create-form {
  position: relative;
  display: block;
  width: 100%;
  min-height: max-content;
}

.account-create-stage {
  /**
   * 第②层与第③层内容全部编排在第①层内部内容壳中。
   * 第①层标题固定，第①层内容壳独占纵向滚动，避免外层 wrapper 或 body 再次抢占滚动焦点。
   */
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  min-height: max-content;
  padding: 6px 0 8px;
}

.account-create-layout {
  display: grid;
  grid-template-columns: minmax(0, 10fr) minmax(0, 14fr);
  align-items: start;
  gap: 18px;
  min-width: 0;
  min-height: max-content;
}

/**
 * 新建账号精简版布局（移除 Hero 与左侧摘要区）：
 * 主表单区改为单列铺满，避免保留双列网格导致右侧内容无法占满可用宽度。
 */
.account-create-layout--single {
  grid-template-columns: minmax(0, 1fr);
}

.account-create-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.85fr);
  gap: 24px;
  padding: 32px 36px;
  border-radius: 32px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  overflow: hidden;
  color: #ffffff;
  /* 现代科技感：深空灰混紫罗兰/深海蓝渐变，而不是普通蓝绿色 */
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.1), transparent 40%),
    linear-gradient(135deg, #0f172a, #1e1b4b 40%, #1e1b4b 60%, #172554);
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.25);
}

.account-create-hero::before {
  position: absolute;
  inset: auto auto -90px -40px;
  width: 240px;
  height: 240px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.26), transparent 72%);
  content: '';
  pointer-events: none;
}

.account-create-hero::after {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 26px 26px;
  opacity: 0.18;
  content: '';
  pointer-events: none;
}

.account-create-hero__main,
.account-create-hero__summary {
  position: relative;
  z-index: 1;
}

.account-create-hero__main {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  min-width: 0;
}

.account-create-hero__eyebrow {
  margin: 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.account-create-hero__main h2 {
  margin: 0;
  font-family: 'Segoe UI Semibold', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: clamp(30px, 3vw, 42px);
  line-height: 1.1;
  color: #ffffff;
}

.account-create-hero__description {
  max-width: 720px;
  margin: 0;
  color: rgba(239, 246, 255, 0.9);
  font-size: 14px;
  line-height: 1.9;
}

.account-create-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.account-create-hero__tags span {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: #f8fafc;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  backdrop-filter: blur(12px);
}

.account-create-hero__summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.08));
  backdrop-filter: blur(16px);
}

.account-create-hero__summary-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.account-create-hero__summary-head strong {
  color: #ffffff;
  font-size: 18px;
}

.account-create-hero__summary-head p {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.82);
  line-height: 1.7;
}

.account-create-hero__summary-badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #eff6ff;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.account-create-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.account-create-hero__stat-card {
  padding: 16px 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  transition: transform 0.2s ease, background 0.2s ease;
}

.account-create-hero__stat-card:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.account-create-hero__stat-card span,
.account-create-hero__stat-card small {
  color: rgba(226, 232, 240, 0.88);
}

.account-create-hero__stat-card strong {
  color: #ffffff;
  font-size: 24px;
  line-height: 1.15;
  word-break: break-all;
  overflow-wrap: anywhere;
}

.account-create-hero__stat-card :deep(.arco-typography) {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.account-create-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.account-create-side-card {
  position: relative;
  overflow: hidden;
  padding: 20px;
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.92));
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(14px);
}

.account-create-side-card--identity {
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.12), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(241, 245, 249, 0.94));
}

.account-create-side-card__heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.account-create-side-card__heading p {
  margin: 0 0 8px;
  color: #0f5de2;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.account-create-side-card__heading h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.account-create-side-card__badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(23, 105, 255, 0.08), rgba(18, 184, 166, 0.12));
  color: #0f5de2;
  font-size: 12px;
  font-weight: 700;
}

.account-create-fact-grid {
  display: grid;
  gap: 12px;
}

.account-create-fact-card {
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.account-create-fact-card span,
.account-create-fact-card small {
  color: #64748b;
}

.account-create-fact-card strong,
.account-create-fact-card :deep(.arco-typography) {
  color: #0f172a;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.45;
}

.account-create-flow-list,
.account-create-environment-list {
  display: grid;
  gap: 12px;
}

.account-create-flow-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  padding: 14px 16px;
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
}

.account-create-flow-item__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 14px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.account-create-flow-item__content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.account-create-flow-item__content strong {
  color: #0f172a;
  font-size: 15px;
}

.account-create-flow-item__content p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.account-create-flow-item.tone-blue .account-create-flow-item__index {
  background: linear-gradient(135deg, #2563eb, #38bdf8);
}

.account-create-flow-item.tone-teal .account-create-flow-item__index {
  background: linear-gradient(135deg, #0f766e, #14b8a6);
}

.account-create-flow-item.tone-gold .account-create-flow-item__index {
  background: linear-gradient(135deg, #c2410c, #f59e0b);
}

.account-create-environment-card {
  padding: 16px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.8);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.account-create-environment-card.is-active {
  transform: translateY(-2px);
  border-color: rgba(23, 105, 255, 0.24);
  box-shadow: 0 16px 28px rgba(37, 99, 235, 0.12);
}

.account-create-environment-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.account-create-environment-card__head strong {
  color: #0f172a;
  font-size: 15px;
}

.account-create-environment-card__head span {
  color: #0f5de2;
  font-size: 12px;
  font-weight: 700;
}

.account-create-environment-card__value {
  display: block;
  margin-top: 12px;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-field-cell--index {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.account-create-environment-card p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.account-create-form-shell {
  box-sizing: border-box;
  position: relative;
  /**
   * 去除“外层玻璃底板”：
   * 新建页当前只保留内层业务卡片（governance-form-panel / 白名单编辑器），
   * 外层壳层设为透明无装饰，避免出现用户反馈的“双底层”视觉叠加。
   */
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
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
  padding: 0;
}

.account-create-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
  min-height: 0;
}

.account-create-form-shell__heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.account-create-form-shell__heading-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.account-create-form-shell__heading-main h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.account-create-form-shell__heading-main p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.account-create-form-shell__eyebrow {
  margin: 0;
  color: #0f5de2;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.account-create-form-shell__summary {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.account-create-form-shell__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  max-width: 360px;
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

/**
 * 新建页底部精简模式：
 * 去掉左侧说明后，仅保留右侧操作按钮，并保持视觉重心落在提交动作。
 */
.account-create-footer--actions-only {
  justify-content: flex-end;
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

.account-modal-shell--create :deep(.arco-form) {
  display: block;
  flex: none;
  min-height: auto;
}

.account-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 3100;
  --surface-color: #ffffff;
  --surface-border: #e5edf6;
  --text-primary: #0f172a;
  --text-secondary: #64748b;
  color: #0f172a;
}

.account-modal-overlay__mask {
  position: absolute;
  inset: 0;
  /**
   * 创建弹层后方只保留纯色遮罩，不再使用毛玻璃虚化。
   * 这样可以避免背景页面内容发糊，同时仍然维持弹层聚焦效果。
   */
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: none;
}

.account-modal-overlay__viewport {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 0;
  overflow: hidden;
  perspective: 1200px;
  /* Optional enhancement for 3D interactions */
}

.account-modal-overlay__viewport.is-fullscreen {
  padding: 0;
}

.account-modal-overlay__viewport :deep(.account-modal) {
  pointer-events: auto;
}

/* 缩放手柄样式 */
:deep(.account-modal-resize-handle) {
  position: absolute;
  z-index: 100;
  user-select: none;
}

:deep(.account-modal-resize-handle--n) {
  top: -6px;
  left: 10px;
  right: 10px;
  height: 12px;
  cursor: ns-resize;
}

:deep(.account-modal-resize-handle--s) {
  bottom: -6px;
  left: 10px;
  right: 10px;
  height: 12px;
  cursor: ns-resize;
}

:deep(.account-modal-resize-handle--e) {
  top: 10px;
  bottom: 10px;
  right: -6px;
  width: 12px;
  cursor: ew-resize;
}

:deep(.account-modal-resize-handle--w) {
  top: 10px;
  bottom: 10px;
  left: -6px;
  width: 12px;
  cursor: ew-resize;
}

:deep(.account-modal-resize-handle--ne) {
  top: -6px;
  right: -6px;
  width: 16px;
  height: 16px;
  cursor: nesw-resize;
}

:deep(.account-modal-resize-handle--nw) {
  top: -6px;
  left: -6px;
  width: 16px;
  height: 16px;
  cursor: nwse-resize;
}

:deep(.account-modal-resize-handle--se) {
  bottom: -6px;
  right: -6px;
  width: 16px;
  height: 16px;
  cursor: nwse-resize;
}

:deep(.account-modal-resize-handle--sw) {
  bottom: -6px;
  left: -6px;
  width: 16px;
  height: 16px;
  cursor: nesw-resize;
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
  position: absolute;
  margin: 0;
  max-width: none !important;
  max-height: none !important;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.45);
  overflow: hidden;
  will-change: left, top, width, height, transform;
  transition: box-shadow 0.25s cubic-bezier(0.16, 1, 0.3, 1), border-color 0.25s ease;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    0 12px 32px rgba(15, 23, 42, 0.12),
    0 4px 12px rgba(15, 23, 42, 0.04);
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
  position: sticky;
  top: 0;
  z-index: 8;
  min-height: 72px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.82);
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(248, 250, 252, 0.92)),
    radial-gradient(circle at top right, rgba(18, 184, 166, 0.08), transparent 38%);
  backdrop-filter: blur(14px);
  cursor: move;
}

:deep(.account-modal.account-modal--create .arco-modal-header) {
  position: relative;
  top: auto;
  z-index: 12;
  min-height: auto;
  padding: 14px 20px 12px;
  background: transparent;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.2);
  cursor: move;
}

:deep(.account-modal.account-modal--create .arco-modal-header)::after {
  position: absolute;
  right: 20px;
  bottom: 0;
  left: 20px;
  height: 1px;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.22), rgba(148, 163, 184, 0.32), rgba(45, 212, 191, 0.22));
  content: '';
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
  display: block;
  flex: 1 1 auto;
  height: 0;
  min-height: 0;
  padding: 12px 20px 20px;
  overflow-y: auto;
  overflow-x: hidden;
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
  min-width: 0;
}

.account-create-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
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

.account-modal-shell :deep(.arco-form) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.account-modal-shell--create :deep(.arco-form) {
  display: block;
  flex: none;
  min-height: auto;
}

.account-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 3100;
  --surface-color: #ffffff;
  --surface-border: #e5edf6;
  --text-primary: #0f172a;
  --text-secondary: #64748b;
  color: #0f172a;
}

.account-modal-overlay__mask {
  position: absolute;
  inset: 0;
  /**
   * 创建弹层后方只保留纯色遮罩，不再使用毛玻璃虚化。
   * 这样可以避免背景页面内容发糊，同时仍然维持弹层聚焦效果。
   */
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: none;
}

.account-modal-overlay__viewport {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 0;
  overflow: hidden;
  perspective: 1200px;
  /* Optional enhancement for 3D interactions */
}

.account-modal-overlay__viewport.is-fullscreen {
  padding: 0;
}

.account-modal-overlay__viewport :deep(.account-modal) {
  pointer-events: auto;
}

/* 缩放手柄样式 */
:deep(.account-modal-resize-handle) {
  position: absolute;
  z-index: 100;
  user-select: none;
}

:deep(.account-modal-resize-handle--n) {
  top: -6px;
  left: 10px;
  right: 10px;
  height: 12px;
  cursor: ns-resize;
}

:deep(.account-modal-resize-handle--s) {
  bottom: -6px;
  left: 10px;
  right: 10px;
  height: 12px;
  cursor: ns-resize;
}

:deep(.account-modal-resize-handle--e) {
  top: 10px;
  bottom: 10px;
  right: -6px;
  width: 12px;
  cursor: ew-resize;
}

:deep(.account-modal-resize-handle--w) {
  top: 10px;
  bottom: 10px;
  left: -6px;
  width: 12px;
  cursor: ew-resize;
}

:deep(.account-modal-resize-handle--ne) {
  top: -6px;
  right: -6px;
  width: 16px;
  height: 16px;
  cursor: nesw-resize;
}

:deep(.account-modal-resize-handle--nw) {
  top: -6px;
  left: -6px;
  width: 16px;
  height: 16px;
  cursor: nwse-resize;
}

:deep(.account-modal-resize-handle--se) {
  bottom: -6px;
  right: -6px;
  width: 16px;
  height: 16px;
  cursor: nwse-resize;
}

:deep(.account-modal-resize-handle--sw) {
  bottom: -6px;
  left: -6px;
  width: 16px;
  height: 16px;
  cursor: nesw-resize;
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
  position: absolute;
  margin: 0;
  max-width: none !important;
  max-height: none !important;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.45);
  overflow: hidden;
  will-change: left, top, width, height, transform;
  transition: box-shadow 0.25s cubic-bezier(0.16, 1, 0.3, 1), border-color 0.25s ease;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    0 12px 32px rgba(15, 23, 42, 0.12),
    0 4px 12px rgba(15, 23, 42, 0.04);
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
  position: sticky;
  top: 0;
  z-index: 8;
  min-height: 72px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.82);
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(248, 250, 252, 0.92)),
    radial-gradient(circle at top right, rgba(18, 184, 166, 0.08), transparent 38%);
  backdrop-filter: blur(14px);
  cursor: move;
}

:deep(.account-modal.account-modal--create .arco-modal-header) {
  position: relative;
  flex: 0 0 auto;
  top: auto;
  z-index: 12;
  min-height: auto;
  padding: 14px 20px 12px;
  background: transparent;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.2);
  cursor: move;
}

:deep(.account-modal.account-modal--create .arco-modal-header)::after {
  position: absolute;
  right: 20px;
  bottom: 0;
  left: 20px;
  height: 1px;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.22), rgba(148, 163, 184, 0.32), rgba(45, 212, 191, 0.22));
  content: '';
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
  min-width: 0;
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

.account-modal-shell :deep(.arco-form) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  min-width: 0;
}

.account-modal-shell--create :deep(.arco-form) {
  display: block;
  flex: none;
  min-height: auto;
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

.module+.module {
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

/**
 * 新建账号弹窗的字段编排在中等宽度下存在最小可读宽度；
 * 当前按产品要求改为“自适应收缩优先”，禁用横向滚动，
 * 由字段网格与输入控件在可用宽度内自动缩放，确保无需左右拖动滚动条。
 */
:deep(.account-modal-body--create) {
  /**
   * 统一收紧左右内边距，释放可用宽度给字段列，避免右侧字段被裁切。
   */
  display: block;
  padding: 10px 12px 16px;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-gutter: auto;
}

/**
 * 新建弹窗“铺满链路”统一规则：
 * body 在后续重复样式中可能被回退为 flex 横向排版，进而导致内容壳只按内容宽度渲染，
 * 在全屏/自由拉伸状态下右侧出现空白。这里显式约束核心容器全部 100% 宽度并取消 max-width。
 */
.account-modal-shell--create,
.account-create-viewport,
.account-create-form,
.account-create-stage,
.account-create-layout,
.account-create-layout--single,
.account-create-main,
.account-create-form-shell,
.account-create-footer {
  box-sizing: border-box;
  width: 100%;
  max-width: none;
  min-width: 0;
}

.account-modal-shell--create {
  flex: 1 1 auto;
}

.account-create-main :deep(.governance-form-sections),
.account-create-main :deep(.governance-form-panel.variant-embedded),
.account-create-main :deep(.governance-form-panel__fields.layout-grid) {
  width: 100%;
  max-width: none;
  min-width: 0;
}

:deep(.account-modal-body--create::-webkit-scrollbar) {
  width: 10px;
}

:deep(.account-modal-body--create::-webkit-scrollbar-track) {
  background: transparent;
}

:deep(.account-modal-body--create::-webkit-scrollbar-thumb) {
  border: 2px solid transparent;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.92), rgba(100, 116, 139, 0.92));
  background-clip: padding-box;
}

:deep(.account-modal-body--create::-webkit-scrollbar-thumb:hover) {
  background: linear-gradient(180deg, rgba(100, 116, 139, 0.96), rgba(71, 85, 105, 0.96));
  background-clip: padding-box;
}

.account-create-layout {
  /**
   * 去掉固定最小宽度约束：
   * 允许主布局始终以当前弹窗可用宽度为准进行流式收缩，
   * 防止内容区被强制撑宽后触发横向滚动条。
   */
  min-width: 100%;
}

/**
 * 新建弹窗字段区无横向滚动策略：
 * 1. 新建态主字段网格提升为 4 列，满足一行可录入 4 个字段；
 * 2. 同步压缩控件尺寸与间距，提高同屏信息密度；
 * 3. 在较小宽度下自动降级为 2 列/1 列，确保不出现横向滚动。
 */
.account-create-main :deep(.governance-form-panel__fields.layout-grid.columns-4) {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0 10px;
}

.account-create-main :deep(.governance-form-panel.variant-embedded) {
  /**
   * 新建弹窗采用“内容直出”模式：
   * 彻底移除治理分区卡片壳（边框、圆角、阴影、背景），
   * 避免出现用户反馈的多层卡片视觉。
   */
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}

.account-create-main :deep(.governance-form-panel.variant-embedded + .governance-form-panel.variant-embedded) {
  margin-top: 8px;
  padding-top: 18px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
}

.account-create-main :deep(.api-environment-whitelist-editor__grid) {
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.account-create-main :deep(.governance-form-panel__item) {
  margin-bottom: 12px;
}

.account-create-main :deep(.governance-form-panel .arco-form-item-label-col > label) {
  font-size: 13px;
  font-weight: 600;
}

.account-create-main :deep(.governance-form-panel__item),
.account-create-main :deep(.governance-form-panel .arco-form-item-wrapper-col),
.account-create-main :deep(.governance-form-panel .arco-input-wrapper),
.account-create-main :deep(.governance-form-panel .arco-select-view),
.account-create-main :deep(.governance-form-panel .arco-picker),
.account-create-main :deep(.governance-form-panel .arco-input-number),
.account-create-main :deep(.governance-form-panel .arco-textarea-wrapper) {
  min-width: 0;
}

.account-create-main :deep(.governance-form-panel .arco-input-wrapper),
.account-create-main :deep(.governance-form-panel .arco-select-view),
.account-create-main :deep(.governance-form-panel .arco-picker),
.account-create-main :deep(.governance-form-panel .arco-input-number) {
  min-height: 34px;
  border-radius: 12px;
}

/**
 * 新建账号弹窗视觉主题升级（无业务逻辑改动）：
 * - 统一底层背景、弹窗壳层、标题栏、输入控件和操作按钮风格；
 * - 保持“内容直出 + 四列录入”的结构不变，仅优化观感与交互质感；
 * - 通过局部样式覆盖实现，可复用、可回退、低风险。
 */
.account-modal-overlay__mask {
  background:
    radial-gradient(circle at 14% -22%, rgba(56, 189, 248, 0.2), transparent 36%),
    radial-gradient(circle at 88% -16%, rgba(45, 212, 191, 0.18), transparent 34%),
    rgba(15, 23, 42, 0.42);
}

:deep(.account-modal.account-modal--create) {
  --create-accent-primary: #2f6df6;
  --create-accent-secondary: #15b8a7;
  --create-border: rgba(148, 163, 184, 0.42);
  --create-surface-0: rgba(255, 255, 255, 0.98);
  --create-surface-1: rgba(244, 250, 255, 0.95);
  background:
    radial-gradient(circle at 12% -18%, rgba(56, 189, 248, 0.16), transparent 44%),
    radial-gradient(circle at 100% -14%, rgba(45, 212, 191, 0.15), transparent 42%),
    linear-gradient(180deg, var(--create-surface-0), var(--create-surface-1));
  border: 1px solid var(--create-border);
  box-shadow:
    0 28px 72px rgba(15, 23, 42, 0.24),
    0 10px 28px rgba(47, 109, 246, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

:deep(.account-modal.account-modal--create:hover) {
  border-color: rgba(47, 109, 246, 0.54);
}

:deep(.account-modal.account-modal--create .arco-modal-header) {
  padding: 12px 18px 10px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.95)),
    radial-gradient(circle at 92% 8%, rgba(59, 130, 246, 0.08), transparent 32%);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.05);
}

:deep(.account-modal.account-modal--create .arco-modal-header)::before {
  position: absolute;
  left: 18px;
  right: 18px;
  top: 0;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #2f6df6, #1fa2ff 46%, #15b8a7);
  content: '';
  opacity: 0.78;
}

:deep(.account-modal.account-modal--create .account-modal-title__main strong) {
  font-size: 22px;
  letter-spacing: 0.02em;
  color: #0f172a;
}

:deep(.account-modal.account-modal--create .account-modal-title__badge) {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: linear-gradient(135deg, rgba(236, 250, 255, 0.9), rgba(223, 248, 244, 0.9));
  color: #0f766e;
  font-weight: 700;
}

:deep(.account-modal.account-modal--create .account-modal-title__action-group) {
  border-color: rgba(203, 213, 225, 0.88);
  background: linear-gradient(180deg, #ffffff, #f7faff);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.94),
    0 6px 14px rgba(15, 23, 42, 0.06);
}

:deep(.account-modal.account-modal--create .account-modal-title__action-btn) {
  border-color: rgba(191, 206, 228, 0.9);
  background: linear-gradient(180deg, #ffffff, #f3f8ff);
  color: #334155;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

:deep(.account-modal.account-modal--create .account-modal-title__action-btn:hover) {
  transform: translateY(-1px);
  border-color: rgba(47, 109, 246, 0.42);
  box-shadow: 0 8px 16px rgba(47, 109, 246, 0.16);
}

:deep(.account-modal.account-modal--create .account-modal-title__action-btn--close) {
  border-color: rgba(254, 205, 211, 0.95);
  background: linear-gradient(180deg, #fff5f5, #fff1f2);
  color: #b42318;
}

:deep(.account-modal.account-modal--create .account-modal-title__action-btn--close:hover) {
  border-color: rgba(252, 165, 165, 0.96);
  box-shadow: 0 8px 16px rgba(244, 63, 94, 0.18);
}

:deep(.account-modal-body--create) {
  background:
    linear-gradient(180deg, rgba(251, 254, 255, 0.78), rgba(244, 249, 255, 0.74)),
    radial-gradient(circle at 95% 6%, rgba(59, 130, 246, 0.08), transparent 30%);
}

.account-create-main :deep(.governance-form-panel.variant-embedded + .governance-form-panel.variant-embedded) {
  padding-top: 20px;
  border-top: 1px solid rgba(203, 213, 225, 0.76);
}

.account-create-main :deep(.governance-form-panel .arco-form-item-label-col > label) {
  color: #0f172a;
}

.account-create-main :deep(.governance-form-panel .arco-input-wrapper),
.account-create-main :deep(.governance-form-panel .arco-select-view),
.account-create-main :deep(.governance-form-panel .arco-picker),
.account-create-main :deep(.governance-form-panel .arco-input-number),
.account-create-main :deep(.governance-form-panel .arco-textarea-wrapper) {
  border-color: rgba(191, 206, 228, 0.88);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.account-create-main :deep(.governance-form-panel .arco-input-wrapper:hover),
.account-create-main :deep(.governance-form-panel .arco-select-view:hover),
.account-create-main :deep(.governance-form-panel .arco-picker:hover),
.account-create-main :deep(.governance-form-panel .arco-input-number:hover),
.account-create-main :deep(.governance-form-panel .arco-textarea-wrapper:hover) {
  border-color: rgba(47, 109, 246, 0.46);
  background: #ffffff;
}

.account-create-main :deep(.governance-form-panel .arco-input-wrapper.arco-input-focus),
.account-create-main :deep(.governance-form-panel .arco-select-view.arco-select-view-focus),
.account-create-main :deep(.governance-form-panel .arco-picker-focus),
.account-create-main :deep(.governance-form-panel .arco-input-number-focus),
.account-create-main :deep(.governance-form-panel .arco-textarea-focus) {
  border-color: rgba(47, 109, 246, 0.62);
  box-shadow: 0 0 0 3px rgba(47, 109, 246, 0.14);
}

.account-create-footer {
  position: sticky;
  bottom: 0;
  z-index: 4;
  padding: 12px 14px;
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 255, 0.96));
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(10px);
}

.account-create-footer::before {
  position: absolute;
  left: 14px;
  right: 14px;
  top: 0;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(47, 109, 246, 0.7), rgba(21, 184, 167, 0.72));
  content: '';
}

.account-create-footer__actions {
  gap: 10px;
}

.account-create-footer__actions :deep(.arco-btn) {
  min-width: 118px;
  height: 38px;
  padding: 0 18px;
  border-radius: 12px;
  font-weight: 700;
}

.account-create-footer__actions :deep(.arco-btn:not(.arco-btn-primary)) {
  border-color: rgba(191, 206, 228, 0.9);
  background: linear-gradient(180deg, #ffffff, #f5f8ff);
  color: #334155;
}

.account-create-footer__actions :deep(.arco-btn:not(.arco-btn-primary):hover) {
  border-color: rgba(47, 109, 246, 0.38);
  color: #1d4ed8;
}

.account-create-footer__actions :deep(.arco-btn-primary) {
  border: 0;
  background: linear-gradient(135deg, #2f6df6 0%, #1f8bff 46%, #12b8a6 100%);
  box-shadow: 0 14px 26px rgba(47, 109, 246, 0.28);
}

.account-create-footer__actions :deep(.arco-btn-primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 16px 30px rgba(47, 109, 246, 0.32);
}

@media (max-width: 1360px) {
  .account-create-main :deep(.governance-form-panel__fields.layout-grid.columns-4) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1180px) {

  .account-create-main :deep(.governance-form-panel__fields.layout-grid.columns-2),
  .account-create-main :deep(.governance-form-panel__fields.layout-grid.columns-3),
  .account-create-main :deep(.governance-form-panel__fields.layout-grid.columns-4) {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 1280px) {

  .account-create-hero,
  .account-create-layout,
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
  .account-create-form-shell__heading,
  .toolbar,
  .whitelist-head,
  .drawer-header,
  .authorization-hero__actions,
  .callback-hero__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .authorization-hero__stats,
  .account-create-hero__stats,
  .callback-hero__stats {
    grid-template-columns: 1fr;
  }

  .account-create-form-shell__summary {
    align-items: flex-start;
  }

  .account-create-form-shell__tags {
    justify-content: flex-start;
    max-width: none;
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

  .account-create-layout {
    min-width: 100%;
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
    padding: 0 14px 14px;
    max-height: none;
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

  .account-create-hero {
    padding: 22px 18px;
    border-radius: 26px;
  }

  .account-create-side-card,
  .account-create-form-shell,
  .account-create-footer {
    border-radius: 24px;
  }

  .account-create-hero__summary-head {
    flex-direction: column;
    align-items: stretch;
  }

  .account-create-hero__summary-badge {
    width: fit-content;
  }

  .account-create-hero__stats {
    grid-template-columns: 1fr;
  }

  .account-create-form-shell__tags {
    justify-content: flex-start;
  }

  .account-create-form-shell__status {
    align-items: flex-start;
  }

  .account-create-flow-item {
    grid-template-columns: 1fr;
  }

  .account-create-environment-card__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .account-layer-marker {
    width: 100%;
    justify-content: flex-start;
  }

  .account-modal-title__markers {
    width: 100%;
  }

  .account-layer-marker__text {
    white-space: normal;
  }

  .authorization-hero h2,
  .callback-hero h2 {
    font-size: 28px;
  }

  .authorization-hero,
  .callback-hero {
    padding: 20px;
  }

  .authorization-summary-grid,
  .authorization-hero__stats,
  .callback-summary-grid,
  .callback-hero__stats {
    grid-template-columns: 1fr;
  }

  .actions,
  .meta-row {
    flex-direction: column;
    align-items: stretch;
  }
}

/**
 * 账号列表行选中高亮样式 (光标显示)
 * 采用下沉到 td 的渲染方案，确保 tr 在 relative 定位下不破坏表格布局。
 */
:deep(.account-table .arco-table-tr.is-row-active .arco-table-td) {
  background-color: var(--color-primary-light-1) !important;
  transition: background-color 0.2s ease;
}

/* 即使在有斑马纹或鼠标悬停时，也强制保持选中色 */
:deep(.account-table .arco-table-tr.is-row-active:hover .arco-table-td) {
  background-color: var(--color-primary-light-1) !important;
}

/* 在第一列单元格使用内阴影实现侧边指示条，这种方式不会改变单元格尺寸或导致布局崩坏 */
:deep(.account-table .arco-table-tr.is-row-active .arco-table-td:first-child) {
  box-shadow: inset 3px 0 0 var(--color-primary-light-4);
}
</style>

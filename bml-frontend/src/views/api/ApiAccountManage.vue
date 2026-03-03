<template>
  <div ref="apiAccountPageRef" class="page api-account-page">
    <GovernanceCompactQueryPanel
      class="query-panel"
      max-width="1260px"
      density="ultra"
      theme="aurora"
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
          label-layout="inline"
        />

        <transition name="query-advanced-fold">
          <div v-if="queryAdvancedExpanded && querySecondarySections.length" class="query-panel__advanced">
            <GovernanceFormSections
              :model="queryFormAsRecord"
              :sections="querySecondarySections"
              variant="embedded"
              label-layout="inline"
            />
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage
      class="table-shell"
      max-width="1260px"
      density="ultra"
      body-fill
    >
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
                <div
                  v-for="item in accountTableColumnSettingItems"
                  :key="item.kind"
                  class="table-column-setting-panel__item"
                  :class="{
                    'is-draggable': !item.locked,
                    'is-drag-source': columnSettingDragState.draggingKind === item.kind,
                    'is-drag-over-before': columnSettingDragState.overKind === item.kind && columnSettingDragState.dropPosition === 'before',
                    'is-drag-over-after': columnSettingDragState.overKind === item.kind && columnSettingDragState.dropPosition === 'after'
                  }"
                  @dragover="handleColumnSettingDragOver(item.kind, $event)"
                  @drop="handleColumnSettingDrop(item.kind, $event)"
                >
                  <div class="table-column-setting-panel__label">
                    <span
                      v-if="!item.locked"
                      class="table-column-setting-panel__drag-handle"
                      draggable="true"
                      title="拖动调整字段顺序"
                      @dragstart="handleColumnSettingDragStart(item.kind, $event)"
                      @dragend="handleColumnSettingDragEnd"
                    >
                      <icon-drag-arrow />
                    </span>
                    <span>{{ item.title }}</span>
                    <small v-if="item.locked">固定</small>
                  </div>
                  <div class="table-column-setting-panel__actions">
                    <a-switch
                      size="small"
                      :model-value="item.visible"
                      :disabled="item.locked"
                      @change="handleAccountColumnVisibilityChange(item.kind, Boolean($event))"
                    />
                    <a-button size="mini" class="table-column-setting-panel__order-btn" :disabled="item.moveUpDisabled" @click="moveAccountTableColumn(item.kind, -1)">
                      <template #icon><icon-up /></template>
                    </a-button>
                    <a-button size="mini" class="table-column-setting-panel__order-btn" :disabled="item.moveDownDisabled" @click="moveAccountTableColumn(item.kind, 1)">
                      <template #icon><icon-down /></template>
                    </a-button>
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
          <a-table
            :key="`account-table-${accountTableRenderVersion}`"
            class="account-table"
            size="small"
            row-key="id"
            :data="accountList"
            :loading="tableLoading"
            :pagination="false"
            :scroll="{ x: accountTableScrollX, y: '100%' }"
            :scrollbar="false"
            sticky-header
            column-resizable
            @column-resize="handleAccountColumnResize"
            @row-dblclick="handleAccountRowDblClick"
          >
            <template #columns>
              <a-table-column v-for="column in accountTableColumns" :key="column.key" :title="column.title" :data-index="column.dataIndex" :width="column.width" :fixed="column.kind === 'actions' ? 'right' : column.fixed" :ellipsis="column.ellipsis" :tooltip="column.tooltip">
                <template #cell="{ record }">
                  <div v-if="column.kind === 'accountName'" class="table-field-cell">
                    <EllipsisTooltipText
                      :text="getPlainText(record, 'accountName', '-')"
                      variant="primary"
                    />
                  </div>
                  <div v-else-if="column.kind === 'accountType'" class="table-field-cell">
                    <a-tag :color="getAccountTypeTagColor(record.accountType)">{{ getAccountTypeLabel(record.accountType) }}</a-tag>
                  </div>
                  <div v-else-if="column.kind === 'accountId'" class="table-field-cell">
                    <EllipsisTooltipText
                      :text="record.id ? `#${record.id}` : '-'"
                      variant="mono"
                    />
                  </div>
                  <div v-else-if="column.kind === 'systemName'" class="table-field-cell">
                    <EllipsisTooltipText
                      :text="record.systemName || '-'"
                    />
                  </div>
                  <div v-else-if="column.kind === 'accessEnvironment'" class="table-field-cell">
                    <a-tag :color="getEnvironmentTagColor(record.accessEnvironment)">{{ getAccessEnvironmentLabel(record.accessEnvironment) }}</a-tag>
                  </div>
                  <div v-else-if="column.kind === 'clientTypes'" class="table-field-cell">
                    <EllipsisTooltipText
                      :text="getCompactClientSummary(record.clientTypes)"
                      :tooltip-text="getFullClientTypeSummary(record.clientTypes)"
                    />
                  </div>
                  <div v-else-if="column.kind === 'status'" class="table-field-cell">
                    <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ getStatusLabel(record.status) }}</a-tag>
                  </div>
                  <div v-else-if="column.kind === 'authorizedCount'" class="table-field-cell">
                    <span class="table-field-cell__text">{{ record.authorizedApiCount || 0 }}</span>
                  </div>
                  <div v-else-if="column.kind === 'updateTime'" class="table-field-cell table-field-cell--time">
                    <span class="table-field-cell__mono">{{ record.updateTime || '-' }}</span>
                  </div>
                  <div v-else-if="column.kind === 'actions'" class="table-row-actions" @click.stop @dblclick.stop>
                    <a-button size="small" type="primary" class="table-action-button table-action-button--primary" @click="handleOpenAccountPreview(record)">详情</a-button>
                    <a-button size="small" class="table-action-button" @click="handleAuthorizeAccount(record)">授权</a-button>
                    <div class="table-action-more-wrapper">
                      <a-dropdown trigger="click" position="br">
                        <a-button size="small" class="table-action-button">更多</a-button>
                        <template #content>
                          <a-doption @click="handleEditAccount(record)">编辑</a-doption>
                          <a-doption @click="handleCallbackLogAccount(record)">回调日志</a-doption>
                          <a-doption @click="confirmResetSecret(record)">重置密钥</a-doption>
                          <a-doption class="table-row-actions__danger" @click="confirmDeleteAccount(record)">删除账号</a-doption>
                        </template>
                      </a-dropdown>
                    </div>
                  </div>
                  <span v-else>{{ getPlainText(record, column.dataIndex, '-') }}</span>
                </template>
              </a-table-column>
            </template>
          </a-table>
          <div
            v-show="showAccountBottomScrollbar"
            ref="accountTableBottomScrollbarRef"
            class="table-shell__x-scrollbar"
            @scroll="handleAccountBottomScrollbarScroll"
          >
            <div class="table-shell__x-scrollbar-track" :style="{ width: `${accountTableScrollX}px` }"></div>
          </div>
        </div>

        <div class="table-shell__footer">
          <a-pagination
            v-bind="tablePaginationConfig"
            size="small"
            @change="handlePageChange"
            @page-size-change="handlePageSizeChange"
          />
        </div>
      </div>
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

    <teleport to="body">
      <div v-if="accountModal.visible && isCreateMode" class="account-modal-overlay">
        <div class="account-modal-overlay__mask"></div>
        <div class="account-modal-overlay__viewport" :class="{ 'is-fullscreen': accountModalViewport.fullscreen }">
          <div
            ref="accountModalCreateElementRef"
            class="account-modal account-modal--create"
            :class="{
              'arco-modal-fullscreen': accountModalViewport.fullscreen,
              'is-dragging': accountModalPointerState.action === 'drag' && accountModalPointerState.active,
              'is-resizing': accountModalPointerState.action === 'resize' && accountModalPointerState.active
            }"
            :style="accountCreateModalStyle"
            @mousemove="handleAccountModalMouseMove"
            @mouseleave="resetAccountModalCursor"
          >
            <div class="arco-modal-header" @mousedown="handleAccountCreateHeaderMouseDown">
              <div class="account-modal-title">
                <div class="account-modal-title__main">
                  <div class="account-modal-title__markers">
                    <div class="account-layer-marker account-layer-marker--lv3">
                      <span class="account-layer-marker__index">③</span>
                      <span class="account-layer-marker__text">三级：业务字段分组</span>
                    </div>
                    <div class="account-layer-marker account-layer-marker--lv1">
                      <span class="account-layer-marker__index">①</span>
                      <span class="account-layer-marker__text">一级：弹窗标题层</span>
                    </div>
                  </div>
                  <p>{{ accountModalTitleCaption }}</p>
                  <strong>{{ accountModalTitle }}</strong>
                </div>
                <div class="account-modal-title__actions">
                  <span class="account-modal-title__badge">{{ accountModalTitleBadge }}</span>
                  <a-tooltip content="拖动标题栏可移动窗口，拖动边框可调整大小">
                    <span class="account-modal-title__window-grip" aria-hidden="true">
                      <icon-drag-arrow />
                    </span>
                  </a-tooltip>
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
            </div>

            <div class="account-modal-body account-modal-body--create" @wheel.stop>
              <div class="account-modal-shell account-modal-shell--create">
                <div class="account-create-viewport" @wheel.stop>
                  <EmptyWorkbenchCanvas class="account-create-blank-page" pure />
                  <div class="account-create-content-shell" @wheel.stop>
                    <a-form :model="accountForm" layout="vertical" class="account-create-form">
                      <div class="account-create-stage">
                        <section class="account-create-hero">
                          <div class="account-create-hero__main">
                            <p class="account-create-hero__eyebrow">Open API Delivery Studio</p>
                            <h2>创建一套可立即交付的 API 账号凭证</h2>
                            <p class="account-create-hero__description">
                              统一录入账号主体、安全策略、环境白名单与回调配置，创建完成后系统立即交付首份 AccessKey / SecretKey，
                              同时将当前配置纳入标准治理闭环。
                            </p>
                            <div class="account-create-hero__tags">
                              <span v-for="tag in accountCreateHeroTags" :key="tag">{{ tag }}</span>
                            </div>
                          </div>

                          <div class="account-create-hero__summary">
                            <div class="account-create-hero__summary-head">
                              <div>
                                <strong>开通摘要</strong>
                                <p>当前配置将直接决定首份凭证的交付形态与生效策略。</p>
                              </div>
                              <span class="account-create-hero__summary-badge">{{ accountModeLabel }}</span>
                            </div>
                            <GovernanceStatGrid
                              class="account-create-hero__stats"
                              card-class="account-create-hero__stat-card"
                              :items="accountCreateOverviewStats"
                            />
                          </div>
                        </section>

                        <div class="account-create-layout">
                          <aside class="account-create-side">
                            <section class="account-create-side-card account-create-side-card--identity">
                              <div class="account-create-side-card__heading">
                                <div>
                                  <p>身份与交付</p>
                                  <h3>账号交付画像</h3>
                                </div>
                                <span class="account-create-side-card__badge">{{ accountModalTitleBadge }}</span>
                              </div>
                              <GovernanceFactGrid
                                class="account-create-fact-grid"
                                card-class="account-create-fact-card"
                                :items="accountCreateIdentityFacts"
                              />
                            </section>

                            <section class="account-create-side-card">
                              <div class="account-create-side-card__heading">
                                <div>
                                  <p>开通节奏</p>
                                  <h3>三步完成交付</h3>
                                </div>
                              </div>
                              <div class="account-create-flow-list">
                                <article
                                  v-for="(step, index) in accountCreateFlowSteps"
                                  :key="step.key"
                                  class="account-create-flow-item"
                                  :class="`tone-${step.tone}`"
                                >
                                  <span class="account-create-flow-item__index">0{{ index + 1 }}</span>
                                  <div class="account-create-flow-item__content">
                                    <strong>{{ step.title }}</strong>
                                    <p>{{ step.description }}</p>
                                  </div>
                                </article>
                              </div>
                            </section>

                            <section class="account-create-side-card">
                              <div class="account-create-side-card__heading">
                                <div>
                                  <p>环境治理</p>
                                  <h3>三环境白名单概览</h3>
                                </div>
                              </div>
                              <div class="account-create-environment-list">
                                <article
                                  v-for="item in accountCreateEnvironmentCards"
                                  :key="item.key"
                                  class="account-create-environment-card"
                                  :class="{ 'is-active': item.active }"
                                >
                                  <div class="account-create-environment-card__head">
                                    <strong>{{ item.label }}</strong>
                                    <span>{{ item.state }}</span>
                                  </div>
                                  <strong class="account-create-environment-card__value">{{ item.countLabel }}</strong>
                                  <p>{{ item.hint }}</p>
                                </article>
                              </div>
                            </section>
                          </aside>

                          <div class="account-create-main">
                            <section class="account-create-form-shell">
                              <div class="account-create-form-shell__heading">
                                <div class="account-create-form-shell__heading-main">
                                  <p class="account-create-form-shell__eyebrow">③ 业务字段分组</p>
                                  <h3>接入资料与安全策略</h3>
                                  <p>
                                    该区域继续复用统一 Schema 渲染字段、统一校验规则与统一提交模型；
                                    后续新增普通字段时优先修改配置，不直接侵入页面模板。
                                  </p>
                                </div>
                                <div class="account-create-form-shell__summary">
                                  <div class="account-create-form-shell__tags">
                                    <span v-for="tag in accountCreateFormTags" :key="tag">{{ tag }}</span>
                                  </div>
                                  <div class="account-create-form-shell__status">
                                    <strong>{{ accountCreateFormStatusTitle }}</strong>
                                    <small>{{ accountCreateFormStatusHint }}</small>
                                  </div>
                                </div>
                              </div>

                              <GovernanceFormSections
                                :model="accountFormAsRecord"
                                :sections="accountFormSections"
                              />
                            </section>

                            <section class="account-create-form-shell">
                              <div class="account-create-form-shell__heading">
                                <div class="account-create-form-shell__heading-main">
                                  <p class="account-create-form-shell__eyebrow">环境来源治理</p>
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

                              <ApiEnvironmentWhitelistEditor
                                v-model="accountWhitelistModel"
                                :access-environment="accountForm.accessEnvironment"
                                :environment-options="environmentOptions"
                              />
                            </section>

                            <div class="account-create-footer">
                              <div class="account-create-footer__intro">
                                <strong>保存后自动进入凭证交付</strong>
                                <p>
                                  创建动作继续复用统一校验与统一提交逻辑，不额外分叉接口契约；
                                  校验通过后立即返回首份凭证，便于接入方直接保存与联调。
                                </p>
                                <ul class="account-create-footer__list">
                                  <li v-for="item in accountCreateFooterTips" :key="item">{{ item }}</li>
                                </ul>
                              </div>
                              <div class="account-create-footer__actions">
                                <a-button @click="closeAccountModal">取消</a-button>
                                <a-button @click="resetAccountForm">重置表单</a-button>
                                <a-button
                                  type="primary"
                                  :loading="accountModal.submitting"
                                  @click="submitAccountForm"
                                >
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
              </div>
            </div>

            <div
              v-for="handle in accountModalResizeHandles"
              :key="handle.key"
              class="account-modal-resize-handle"
              :class="`account-modal-resize-handle--${handle.direction}`"
              :data-direction="handle.direction"
              @mousedown.stop.prevent="handleAccountCreateResizeHandleMouseDown(handle.direction, $event)"
              aria-hidden="true"
            ></div>
          </div>
        </div>
      </div>
    </teleport>

    <a-modal
      v-if="!isCreateMode"
      v-model:visible="accountModal.visible"
      :fullscreen="accountModalViewport.fullscreen"
      :render-to-body="true"
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
            <div class="account-modal-title__markers">
              <div class="account-layer-marker account-layer-marker--lv3">
                <span class="account-layer-marker__index">③</span>
                <span class="account-layer-marker__text">三级：业务字段分组</span>
              </div>
              <div class="account-layer-marker account-layer-marker--lv1">
                <span class="account-layer-marker__index">①</span>
                <span class="account-layer-marker__text">一级：弹窗标题层</span>
              </div>
            </div>
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

      <div class="account-modal-shell">
        <a-form :model="accountForm" layout="vertical">
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
import { IconClose, IconDown, IconDragArrow, IconFullscreen, IconFullscreenExit, IconPlus, IconSettings, IconSync, IconUp } from '@arco-design/web-vue/es/icon';
import { createApiAccount, deleteApiAccount, fetchApiAccountDetail, fetchApiAccountPage, fetchApiCallbackLogs, fetchAuthorizationSnapshot, resetApiAccountSecret, retryApiCallbackLog, saveAuthorization, syncOpenApiRegistry, triggerApiAccountTestCallback, updateApiAccount } from '../../api/apiAccount';
import ApiAuthorizationWorkbenchDrawer from '../../components/api-account/ApiAuthorizationWorkbenchDrawer.vue';
import ApiCallbackLogWorkbenchDrawer from '../../components/api-account/ApiCallbackLogWorkbenchDrawer.vue';
import ApiCredentialDeliveryModal from '../../components/api-account/ApiCredentialDeliveryModal.vue';
import ApiAccountPreviewModal from '../../components/api-account/ApiAccountPreviewModal.vue';
import ApiEnvironmentWhitelistEditor from '../../components/api-account/ApiEnvironmentWhitelistEditor.vue';
import GovernanceCompactQueryPanel from '../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceFactGrid from '../../components/governance/GovernanceFactGrid.vue';
import GovernanceFormSections from '../../components/governance/GovernanceFormSections.vue';
import GovernanceListStage from '../../components/governance/GovernanceListStage.vue';
import GovernancePanel from '../../components/governance/GovernancePanel.vue';
import GovernanceStatGrid from '../../components/governance/GovernanceStatGrid.vue';
import GovernanceWorkbenchShell from '../../components/governance/GovernanceWorkbenchShell.vue';
import EllipsisTooltipText from '../../components/common/EllipsisTooltipText.vue';
import EmptyWorkbenchCanvas from '../../components/common/EmptyWorkbenchCanvas.vue';
import { useApiAccountFormValidation } from '../../composables/useApiAccountFormValidation';
import { useApiAccountFormSchema } from '../../composables/useApiAccountFormSchema';
import { useApiAccountQuerySchema } from '../../composables/useApiAccountQuerySchema';
import { splitGovernanceSectionsByPriority } from '../../composables/useGovernanceSectionPriority';
import { defineTableColumns, useTableColumns } from '../../composables/useTableColumns';
import { buildUserScopedStorageKey } from '../../utils/userScopedStorage';
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
type AccountColumnKind =
  | 'accountName'
  | 'accountType'
  | 'accountId'
  | 'systemName'
  | 'accessEnvironment'
  | 'clientTypes'
  | 'status'
  | 'authorizedCount'
  | 'updateTime'
  | 'actions';
type QueryTextMatchMode = 'fuzzy' | 'exact';
type AccountModalResizeDirection = 'n' | 's' | 'e' | 'w' | 'ne' | 'nw' | 'se' | 'sw' | '';
type AccountModalPointerAction = 'none' | 'drag' | 'resize';
type AccountTableColumnLayout = {
  width: number;
  visible: boolean;
  order: number;
};
type AccountCreateFlowStep = {
  key: string;
  title: string;
  description: string;
  tone: WorkbenchStatCard['tone'];
};
type AccountCreateEnvironmentCard = {
  key: AccessEnvironment;
  label: string;
  state: string;
  countLabel: string;
  hint: string;
  active: boolean;
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
type AccountModalResizeHandleMeta = {
  key: string;
  direction: Exclude<AccountModalResizeDirection, ''>;
};
type AccountTableColumnSettingItem = {
  kind: AccountColumnKind;
  title: string;
  visible: boolean;
  locked: boolean;
  moveUpDisabled: boolean;
  moveDownDisabled: boolean;
};

const accountTypeOptions = [{ label: '内部账号', value: 1 }, { label: '外部账号', value: 2 }];
const clientTypeOptions = [{ label: 'Web前端', value: 'web' }, { label: 'H5页面', value: 'h5' }, { label: 'APP', value: 'app' }, { label: '小程序', value: 'mini_program' }, { label: '服务端', value: 'server' }, { label: '第三方系统', value: 'third_party' }, { label: '其他客户端', value: 'other' }];
const environmentOptions: { label: string; value: AccessEnvironment }[] = [{ label: '测试环境', value: 'test' }, { label: '预发环境', value: 'staging' }, { label: '生产环境', value: 'production' }];
const signVersionOptions = [{ label: 'v1（当前正式版）', value: 'v1' }];
const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }];
const route = useRoute();
const router = useRouter();
const apiAccountPageRef = ref<HTMLElement | null>(null);

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
const accountModalPointerState = reactive({
  active: false,
  action: 'none' as AccountModalPointerAction,
  direction: '' as AccountModalResizeDirection,
  startClientX: 0,
  startClientY: 0,
  startWidth: 0,
  startHeight: 0,
  startLeft: 0,
  startTop: 0,
  startOffsetX: 0,
  startOffsetY: 0
});
const accountModalElement = ref<HTMLElement | null>(null);
const accountModalCreateElementRef = ref<HTMLElement | null>(null);
/**
 * 创建态可视化缩放手柄配置。
 * 统一由配置驱动渲染，便于后续在其他自定义治理弹层中直接复用同一套拖拽缩放骨架。
 */
const accountModalResizeHandles: AccountModalResizeHandleMeta[] = [
  { key: 'north', direction: 'n' },
  { key: 'south', direction: 's' },
  { key: 'east', direction: 'e' },
  { key: 'west', direction: 'w' },
  { key: 'north-east', direction: 'ne' },
  { key: 'north-west', direction: 'nw' },
  { key: 'south-east', direction: 'se' },
  { key: 'south-west', direction: 'sw' }
];
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
 * 表格渲染版本号：
 * Arco 的列拖拽改宽会保留内部宽度状态，恢复默认时需要强制重建实例，
 * 才能确保默认列宽立即回显到页面。
 */
const accountTableRenderVersion = ref(0);
let accountTableBodyScrollHandler: ((event: Event) => void) | null = null;
let accountBottomScrollbarSyncing = false;
let accountBottomScrollbarRafId: number | null = null;
// 列表只保留主信息，低频治理字段统一进入详情弹窗查看。
const accountPreview = reactive({ visible: false, loading: false, accountId: 0, snapshot: null as ApiAccountDetail | null });
const accountForm = reactive<ApiAccountFormModel>({ accountName: '', ownerName: '', ownerContact: '', systemName: '', systemCode: '', accountType: 2, clientTypes: [], accessEnvironment: 'production', signVersion: 'v1', environmentIpWhitelistText: createEmptyEnvironmentWhitelistText(), callbackUrl: '', rateLimit: 1000, expireTime: null, status: 1, remark: '' });
const credentialModal = reactive({ visible: false, payload: null as ApiCredentialPayload | null });
const authorizationDrawer = reactive({ visible: false, loading: false, saving: false, accountId: 0, snapshot: null as ApiAuthorizationSnapshot | null, checkedKeys: [] as string[] });
const authorizationFilters = reactive({ keyword: '', moduleName: '', method: '' });
const callbackLogDrawer = reactive({ visible: false, loading: false, testing: false, retryingId: null as number | null, account: null as ApiAccountItem | null, logs: [] as ApiCallbackLogItem[], summary: createSummary(), pagination: { current: 1, pageSize: 10, total: 0 } });
const callbackLogFilters = reactive<ApiCallbackLogFilterModel>({ callbackStatus: undefined });

const tablePaginationConfig = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
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
/**
 * 新建工作台顶部标签。
 * 该组标签只表达创建场景最核心的交付卖点，后续如需替换视觉主题，优先调整此处配置。
 */
const accountCreateHeroTags = ['首份凭证即时交付', '环境白名单独立治理', '签名与限流一次定标', '创建后立即纳入统一治理'];
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
/**
 * 创建态自定义弹层样式。
 * 不再依赖“居中布局 + transform 偏移”去计算视觉位置，而是直接输出 left / top / width / height，
 * 这样拖动过程会更顺畅，窗口也能精准贴合浏览器四边。
 */
const accountCreateModalStyle = computed<Record<string, string>>(() => {
  if (accountModalViewport.fullscreen) {
    return {
      left: '0px',
      top: '0px',
      width: '100%',
      height: '100%'
    };
  }

  return {
    left: `${accountModalViewport.left}px`,
    top: `${accountModalViewport.top}px`,
    width: `${accountModalViewport.width}px`,
    height: `${accountModalViewport.height}px`
  };
});
const accountWorkbenchStats = computed<WorkbenchStatCard[]>(() => [{ label: '账号类型', value: getAccountTypeLabel(accountForm.accountType), hint: '区分内外部接入主体', tone: 'blue' }, { label: '客户端范围', value: accountForm.clientTypes.length ? `${accountForm.clientTypes.length} 类` : '未选择', hint: accountForm.clientTypes.length ? getClientTypeLabels(accountForm.clientTypes).join('、') : '至少选择一个客户端类型', tone: 'green' }, { label: '当前环境', value: getAccessEnvironmentLabel(accountForm.accessEnvironment), hint: `${getEnvironmentWhitelistCountLabel(accountForm.environmentIpWhitelistText[accountForm.accessEnvironment])} 生效条目`, tone: 'teal' }, { label: '回调配置', value: accountForm.callbackUrl.trim() ? '已配置' : '未配置', hint: accountForm.callbackUrl.trim() || '支持 http / https，可按需留空', tone: 'gold' }]);
/**
 * 新建工作台顶部摘要卡。
 * 与编辑态共享相同数据口径，但文案更聚焦“开通交付”，避免在创建场景展示过多存量治理术语。
 */
const accountCreateOverviewStats = computed<WorkbenchStatCard[]>(() => [
  {
    label: '接入环境',
    value: getAccessEnvironmentLabel(accountForm.accessEnvironment),
    hint: `${getEnvironmentWhitelistCountLabel(accountForm.environmentIpWhitelistText[accountForm.accessEnvironment])} 当前生效`,
    tone: 'blue'
  },
  {
    label: '客户端范围',
    value: accountForm.clientTypes.length ? `${accountForm.clientTypes.length} 类` : '待选择',
    hint: accountForm.clientTypes.length ? getClientTypeLabels(accountForm.clientTypes).join('、') : '至少选择一个调用终端',
    tone: 'teal'
  },
  {
    label: '限流阈值',
    value: `${accountForm.rateLimit || 0}/min`,
    hint: '统一分钟级流量治理',
    tone: 'gold'
  },
  {
    label: '回调交付',
    value: accountForm.callbackUrl.trim() ? '已配置' : '按需配置',
    hint: accountForm.callbackUrl.trim() || '支持 http / https，可留空',
    tone: 'green'
  }
]);
/**
 * 新建左侧账号画像卡。
 * 通过事实卡统一承载“交付后会得到什么、当前配置处于什么状态”这类高频信息。
 */
const accountCreateIdentityFacts = computed<FactCard[]>(() => [
  { label: '系统账号ID', value: '保存后自动生成', hint: '创建成功后进入凭证交付弹窗' },
  { label: '业务系统编码', value: accountForm.systemCode.trim() ? accountForm.systemCode.trim().toUpperCase() : '待填写', hint: '保存时统一转大写并校验格式' },
  { label: '账号状态', value: getStatusLabel(accountForm.status), hint: '默认启用，可按需在创建阶段直接停用' },
  { label: '签名版本', value: accountForm.signVersion || '待选择', hint: '首份凭证将遵循当前验签版本' }
]);
/**
 * 新建工作台左侧流程卡。
 * 使用配置化步骤而非写死模板文本，方便未来复用到其他“接入账号开通”类页面。
 */
const accountCreateFlowSteps = computed<AccountCreateFlowStep[]>(() => [
  {
    key: 'identity',
    title: '账号身份建档',
    description: accountForm.accountName.trim() ? `已录入账号名称“${accountForm.accountName.trim()}”` : '先定义账号名称、类型、业务系统与负责人信息',
    tone: 'blue'
  },
  {
    key: 'strategy',
    title: '接入策略定标',
    description: `${getAccessEnvironmentLabel(accountForm.accessEnvironment)} · ${accountForm.rateLimit || 0}/min · ${accountForm.clientTypes.length ? `${accountForm.clientTypes.length} 类客户端` : '待选客户端'}`,
    tone: 'teal'
  },
  {
    key: 'delivery',
    title: '凭证与联调交付',
    description: accountForm.callbackUrl.trim() ? '已维护业务回调地址，创建后可直接进入联调' : '创建后返回首份凭证，回调配置可按需维护',
    tone: 'gold'
  }
]);
/**
 * 新建左侧环境概览卡。
 * 统一从环境选项与白名单文本模型派生，避免环境卡与编辑器状态出现双维护偏差。
 */
const accountCreateEnvironmentCards = computed<AccountCreateEnvironmentCard[]>(() => environmentOptions.map(item => ({
  key: item.value,
  label: item.label,
  state: accountForm.accessEnvironment === item.value ? '当前生效环境' : '待命环境',
  countLabel: getEnvironmentWhitelistCountLabel(accountForm.environmentIpWhitelistText[item.value]),
  hint: accountForm.accessEnvironment === item.value ? '保存时会同步回填为当前命中的生效清单' : '独立维护，避免不同环境来源相互污染',
  active: accountForm.accessEnvironment === item.value
})));
const accountCreateFormTags = computed(() => [
  getAccountTypeLabel(accountForm.accountType),
  getAccessEnvironmentLabel(accountForm.accessEnvironment),
  accountForm.clientTypes.length ? `${accountForm.clientTypes.length} 类客户端` : '待选客户端',
  accountForm.callbackUrl.trim() ? '已配置回调' : '未配置回调'
]);
const accountCreateFormStatusTitle = computed(() => `${getAccessEnvironmentLabel(accountForm.accessEnvironment)} · ${getEnvironmentWhitelistCountLabel(accountForm.environmentIpWhitelistText[accountForm.accessEnvironment])}`);
const accountCreateFormStatusHint = computed(() => accountForm.callbackUrl.trim() ? '回调地址已配置，创建后可直接进入联调交付。' : '当前未配置回调地址，可先完成账号开通，后续再补充联调地址。');
const accountCreateWhitelistSummaryTitle = computed(() => getEnvironmentWhitelistCountLabel(accountForm.environmentIpWhitelistText[accountForm.accessEnvironment]));
const accountCreateWhitelistSummaryHint = computed(() => `${getAccessEnvironmentLabel(accountForm.accessEnvironment)} 当前默认命中`);
const accountCreateFooterTips = ['保存前系统会统一校验系统编码、回调地址与白名单格式。', 'SecretKey 只在首次创建成功后展示一次，请在返回时立即妥善保存。', '环境白名单支持换行、英文逗号、中文逗号和分号混输，提交时会自动标准化并去重。'];
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
/**
 * 列布局本地缓存版本号。
 * 本次将“复合列”重构为“单字段列”，并改为按登录用户隔离，避免不同账号互相覆盖布局配置。
 */
const ACCOUNT_TABLE_LAYOUT_STORAGE_KEY_PREFIX = 'bml.api-account.manage.table-layout.v4';
function getAccountTableLayoutStorageKey() {
  return buildUserScopedStorageKey(ACCOUNT_TABLE_LAYOUT_STORAGE_KEY_PREFIX);
}
const ACCOUNT_TABLE_LOCKED_COLUMN_KINDS = new Set<AccountColumnKind>(['actions']);
const ACCOUNT_TABLE_COLUMN_MIN_WIDTH: Record<AccountColumnKind, number> = {
  accountName: 160,
  accountType: 110,
  accountId: 170,
  systemName: 180,
  accessEnvironment: 120,
  clientTypes: 150,
  status: 100,
  authorizedCount: 100,
  updateTime: 190,
  actions: 186
};
const ACCOUNT_TABLE_COLUMN_MAX_WIDTH: Record<AccountColumnKind, number> = {
  accountName: 360,
  accountType: 180,
  accountId: 320,
  systemName: 340,
  accessEnvironment: 220,
  clientTypes: 320,
  status: 180,
  authorizedCount: 220,
  updateTime: 340,
  actions: 320
};
// 列基础模型（默认顺序 + 默认宽度 + 固定信息）集中收口。
// 按“一个单元格一个字段”组织高频字段，低频字段统一走详情弹窗查看，保证列表可扫读性。
const accountTableColumnBaseModel = defineTableColumns<AccountColumnKind>([
  { key: 'accountName', dataIndex: 'account_name', title: '账号名称', kind: 'accountName', width: 180 },
  { key: 'accountType', dataIndex: 'account_type', title: '账号类型', kind: 'accountType', width: 112 },
  { key: 'accountId', dataIndex: 'account_id', title: '账号ID', kind: 'accountId', width: 186 },
  { key: 'systemName', dataIndex: 'system_name', title: '业务系统', kind: 'systemName', width: 190 },
  { key: 'accessEnvironment', dataIndex: 'access_environment', title: '接入环境', kind: 'accessEnvironment', width: 120 },
  { key: 'clientTypes', dataIndex: 'client_types', title: '客户端', kind: 'clientTypes', width: 170 },
  { key: 'status', dataIndex: 'status', title: '状态', kind: 'status', width: 104 },
  { key: 'authorizedCount', dataIndex: 'authorized_count', title: '授权数', kind: 'authorizedCount', width: 106 },
  { key: 'updateTime', dataIndex: 'update_time', title: '最近更新', kind: 'updateTime', width: 190 },
  { key: 'actions', dataIndex: 'actions', title: '操作', kind: 'actions', width: 192, fixed: 'right' }
]);
const accountTableColumnBaseMap = accountTableColumnBaseModel.reduce((accumulator, column) => {
  accumulator[column.kind] = column;
  return accumulator;
}, {} as Record<AccountColumnKind, (typeof accountTableColumnBaseModel)[number]>);
function createDefaultAccountTableColumnLayout(): Record<AccountColumnKind, AccountTableColumnLayout> {
  return accountTableColumnBaseModel.reduce((accumulator, column, index) => {
    accumulator[column.kind] = {
      width: column.width,
      visible: true,
      order: index
    };
    return accumulator;
  }, {} as Record<AccountColumnKind, AccountTableColumnLayout>);
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
  const min = ACCOUNT_TABLE_COLUMN_MIN_WIDTH[kind];
  const max = ACCOUNT_TABLE_COLUMN_MAX_WIDTH[kind];
  return Math.min(max, Math.max(min, Math.round(width)));
}
function normalizeAccountTableColumnOrder() {
  const unlockedKinds = accountTableColumnBaseModel
    .map(item => item.kind)
    .filter(kind => !ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind))
    .sort((left, right) => accountTableColumnLayout[left].order - accountTableColumnLayout[right].order);
  unlockedKinds.forEach((kind, index) => {
    accountTableColumnLayout[kind].order = index;
  });

  let lockedOrderCursor = unlockedKinds.length;
  accountTableColumnBaseModel.forEach(column => {
    if (!ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(column.kind)) return;
    accountTableColumnLayout[column.kind].order = lockedOrderCursor;
    accountTableColumnLayout[column.kind].visible = true;
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
    }
    normalizeAccountTableColumnOrder();
    accountTableRenderVersion.value += 1;
  } catch {
    window.localStorage.removeItem(storageKey);
  }
}
restoreAccountTableColumnLayout();
const accountTableColumnSettingItems = computed<AccountTableColumnSettingItem[]>(() => {
  const orderedKinds = Object.keys(accountTableColumnLayout)
    .map(kind => kind as AccountColumnKind)
    .sort((left, right) => accountTableColumnLayout[left].order - accountTableColumnLayout[right].order);
  const movableKinds = orderedKinds.filter(kind => !ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind));

  return orderedKinds.map(kind => {
    const movableIndex = movableKinds.indexOf(kind);
    const locked = ACCOUNT_TABLE_LOCKED_COLUMN_KINDS.has(kind);
    return {
      kind,
      title: accountTableColumnBaseMap[kind].title,
      visible: accountTableColumnLayout[kind].visible,
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
  const hitColumn = accountTableColumnBaseModel.find(column => column.dataIndex === dataIndex);
  if (!hitColumn || !Number.isFinite(width)) return;
  const kind = hitColumn.kind;
  accountTableColumnLayout[kind].width = clampAccountColumnWidth(kind, width);
  persistAccountTableColumnLayout();
}
const accountTableColumnModel = computed(() => accountTableColumnSettingItems.value
  .filter(item => item.visible || item.locked)
  .map(item => ({
    ...accountTableColumnBaseMap[item.kind],
    width: accountTableColumnLayout[item.kind].width,
    fixed: item.kind === 'actions' ? 'right' : accountTableColumnBaseMap[item.kind].fixed
  })));
const accountTableScrollX = computed(() => accountTableColumnModel.value.reduce((sum, item) => sum + item.width, 0) + 28);
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
  const modalElement = accountModalElement.value;
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
  if (accountModalViewport.fullscreen) {
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
  accountModalViewport.fullscreen = false;
  accountModalViewport.width = getAccountModalDefaultWidth(mode);
  accountModalViewport.height = ACCOUNT_MODAL_DEFAULT_HEIGHT;
  if (mode === 'create') {
    accountModalViewport.left = (window.innerWidth - accountModalViewport.width) / 2;
    accountModalViewport.top = (window.innerHeight - accountModalViewport.height) / 2;
    accountModalViewport.offsetX = 0;
    accountModalViewport.offsetY = 0;
  } else {
    accountModalViewport.left = 0;
    accountModalViewport.top = 0;
    accountModalViewport.offsetX = 0;
    accountModalViewport.offsetY = 0;
  }
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
  if (element.closest('.account-modal-resize-handle')) return false;
  if (element.closest('.account-modal-title__action-btn')) return false;
  if (element.closest('.arco-modal-close-btn')) return false;
  return Boolean(element.closest('.arco-modal-header'));
}
/**
 * 解析显式缩放手柄命中的方向。
 * 创建态新增了八个可视化拖拽手柄，优先使用手柄方向，可避免仅靠边缘命中时的可发现性不足问题。
 */
function resolveAccountModalResizeHandleDirection(target: EventTarget | null): AccountModalResizeDirection {
  const element = target instanceof HTMLElement ? target.closest('.account-modal-resize-handle') as HTMLElement | null : null;
  const direction = element?.dataset.direction as AccountModalResizeDirection | undefined;
  return direction || '';
}
/**
 * 创建态标题栏拖动入口。
 * 新建弹层改为模板内显式绑定，避免再依赖运行时查询 DOM 后绑定，降低事件丢失与命中不稳定问题。
 */
function handleAccountCreateHeaderMouseDown(event: MouseEvent) {
  if (!isAccountModalHeaderDraggableTarget(event.target)) return;
  beginAccountModalPointerAction(event, 'drag');
}
/**
 * 创建态缩放手柄入口。
 * 由模板直接传入方向，保证上 / 下 / 左 / 右 / 四角缩放都能稳定命中。
 */
function handleAccountCreateResizeHandleMouseDown(direction: Exclude<AccountModalResizeDirection, ''>, event: MouseEvent) {
  beginAccountModalPointerAction(event, 'resize', direction);
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
  accountModalPointerState.startLeft = accountModalViewport.left;
  accountModalPointerState.startTop = accountModalViewport.top;
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

  const handleDirection = resolveAccountModalResizeHandleDirection(event.target);
  if (handleDirection) {
    beginAccountModalPointerAction(event, 'resize', handleDirection);
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

  if (accountModal.mode === 'create') {
    if (accountModalPointerState.action === 'drag') {
      accountModalViewport.left = accountModalPointerState.startLeft + deltaX;
      accountModalViewport.top = accountModalPointerState.startTop + deltaY;
      clampAccountModalViewport();
      return;
    }

    let nextWidth = accountModalPointerState.startWidth;
    let nextHeight = accountModalPointerState.startHeight;
    let nextLeft = accountModalPointerState.startLeft;
    let nextTop = accountModalPointerState.startTop;
    const direction = accountModalPointerState.direction;

    if (direction.includes('e')) {
      nextWidth = clampValue(accountModalPointerState.startWidth + deltaX, widthBounds.min, widthBounds.max);
    }
    if (direction.includes('s')) {
      nextHeight = clampValue(accountModalPointerState.startHeight + deltaY, heightBounds.min, heightBounds.max);
    }
    if (direction.includes('w')) {
      nextWidth = clampValue(accountModalPointerState.startWidth - deltaX, widthBounds.min, widthBounds.max);
      nextLeft = accountModalPointerState.startLeft + (accountModalPointerState.startWidth - nextWidth);
    }
    if (direction.includes('n')) {
      nextHeight = clampValue(accountModalPointerState.startHeight - deltaY, heightBounds.min, heightBounds.max);
      nextTop = accountModalPointerState.startTop + (accountModalPointerState.startHeight - nextHeight);
    }

    accountModalViewport.width = nextWidth;
    accountModalViewport.height = nextHeight;
    accountModalViewport.left = nextLeft;
    accountModalViewport.top = nextTop;
    clampAccountModalViewport();
    return;
  }

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
  const modal = isCreateMode.value
    ? accountModalCreateElementRef.value
    : document.querySelector('.account-modal') as HTMLElement | null;
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
    if (isCreateMode.value) {
      accountModalElement.value = accountModalCreateElementRef.value;
    } else {
      bindAccountModalInteractiveListeners();
      syncAccountModalWrapperScrollLock(true);
    }
    window.addEventListener('resize', handleAccountModalWindowResize);
    return;
  }

  if (!isCreateMode.value) {
    syncAccountModalWrapperScrollLock(false);
  }
  syncAccountModalPageScrollLock(false);
  stopAccountModalPointerAction();
  unbindAccountModalInteractiveListeners();
  window.removeEventListener('resize', handleAccountModalWindowResize);
});
onBeforeUnmount(() => {
  clearAccountBottomScrollbarRaf();
  unbindAccountTableBodyScrollSync();
  window.removeEventListener('resize', scheduleAccountBottomScrollbarSync);
  if (!isCreateMode.value) {
    syncAccountModalWrapperScrollLock(false);
  }
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
  overscroll-behavior: none;
}

:global(body.account-modal-page-lock) {
  position: fixed;
  top: var(--account-modal-page-lock-top, 0px);
  left: 0;
  right: 0;
  width: 100%;
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
   */
  margin-top: var(--api-account-stage-gap-y);
  min-height: clamp(420px, calc(100vh - 250px), 860px);
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
.table-shell__list-region > .account-table {
  height: 100%;
}

.table-shell__x-scrollbar {
  flex-shrink: 0;
  height: 12px;
  margin-top: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  border-radius: 999px;
}

.table-shell__x-scrollbar-track {
  height: 1px;
}

.table-shell__x-scrollbar::-webkit-scrollbar {
  height: 10px;
}

.table-shell__x-scrollbar::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.84);
  border-radius: 999px;
}

.table-shell__x-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.92);
  border-radius: 999px;
}

.table-shell__x-scrollbar {
  scrollbar-color: rgba(148, 163, 184, 0.92) rgba(226, 232, 240, 0.84);
  scrollbar-width: thin;
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
 */
.table-column-setting-panel {
  width: 312px;
  padding: 10px 12px;
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

.table-column-setting-panel__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 280px;
  overflow: auto;
}

.table-column-setting-panel__item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
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

.table-column-setting-panel__label {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.table-column-setting-panel__drag-handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  color: #94a3b8;
  cursor: grab;
  user-select: none;
  flex: 0 0 auto;
  transition: color 0.2s ease, background-color 0.2s ease;
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
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.table-column-setting-panel__label small {
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.9);
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
}

.table-column-setting-panel__actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.table-column-setting-panel__order-btn {
  width: 24px;
  min-width: 24px;
  height: 24px;
  padding: 0;
  border-radius: 8px;
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
.table-row-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 4px;
  width: 100%;
  min-width: 0;
  margin-left: 0;
  padding-right: 2px;
  box-sizing: border-box;
  overflow: hidden;
}

.table-action-button {
  flex: 0 0 auto;
  min-width: 40px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 11px;
  line-height: 1;
  letter-spacing: 0.015em;
}

.table-action-button--primary {
  border: 0;
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 6px 12px rgba(23, 105, 255, 0.14);
}

.table-action-more-wrapper {
  flex: 0 0 auto;
}

.table-action-more-wrapper :deep(.arco-btn) {
  min-width: 40px;
}

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
   * 这样可以稳定把“左右滚动条”固定在分页上方底部区域。
   */
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden !important;
  overflow-y: auto;
}

/**
 * 只保留底部独立横向滚动条：
 * 隐藏表格内置的水平滚动轨道与滑块，避免出现“两条横向滚动条”。
 */
.account-table :deep(.arco-table-body .arco-scrollbar-track-direction-horizontal),
.account-table :deep(.arco-table-body .arco-scrollbar-thumb-direction-horizontal) {
  display: none !important;
}

.account-table :deep(.arco-table-body::-webkit-scrollbar:horizontal) {
  display: none;
  height: 0;
}

.account-table :deep(.arco-table-th) {
  padding-top: 8px;
  padding-bottom: 8px;
  background: linear-gradient(180deg, rgba(246, 250, 255, 0.98), rgba(239, 245, 251, 0.98));
  color: #334155;
  font-weight: 700;
  font-size: 12px;
}

.account-table :deep(.arco-table-th),
.account-table :deep(.arco-table-td) {
  position: relative;
}

/**
 * 列分割线与拖拽手柄视觉增强：
 * 让“表头拖拽调间距（列宽）”的交互意图更明显，贴近电子表格操作体验。
 */
.account-table :deep(.arco-table-th:not(:last-child)::after),
.account-table :deep(.arco-table-td:not(:last-child)::after) {
  content: '';
  position: absolute;
  top: 12%;
  bottom: 12%;
  right: 0;
  width: 1px;
  background: linear-gradient(180deg, rgba(186, 201, 220, 0.24), rgba(186, 201, 220, 0.66), rgba(186, 201, 220, 0.24));
  pointer-events: none;
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

.account-table :deep(.arco-table-fixed-right) {
  box-shadow: -8px 0 18px rgba(148, 163, 184, 0.08);
}

/**
 * 操作列固定在最右侧：
 * 提升固定列层级并补齐背景，避免横向滚动时出现穿透或遮挡。
 */
.account-table :deep(.arco-table-th.arco-table-col-fixed-right),
.account-table :deep(.arco-table-td.arco-table-col-fixed-right) {
  position: sticky;
  right: 0 !important;
  z-index: 13;
  text-align: right;
  background: #f8fcff;
}

.account-table :deep(.arco-table-th.arco-table-col-fixed-right-first::after),
.account-table :deep(.arco-table-td.arco-table-col-fixed-right-first::after) {
  box-shadow: inset -8px 0 12px -8px rgba(71, 85, 105, 0.28);
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
}

.account-layer-marker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  max-width: 100%;
  padding: 3px 10px 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.account-layer-marker__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #ffffff;
  font-size: 12px;
  font-weight: 800;
}

.account-layer-marker__text {
  white-space: nowrap;
}

.account-layer-marker--lv1 {
  background: linear-gradient(135deg, rgba(23, 105, 255, 0.14), rgba(18, 184, 166, 0.16));
  color: #0f5de2;
}

.account-layer-marker--lv1 .account-layer-marker__index {
  color: #0f5de2;
}

.account-layer-marker--lv3 {
  background: linear-gradient(135deg, rgba(217, 119, 6, 0.14), rgba(249, 115, 22, 0.16));
  color: #b45309;
}

.account-layer-marker--lv3 .account-layer-marker__index {
  color: #b45309;
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

.account-modal-resize-handle--ne::before,
.account-modal-resize-handle--nw::before,
.account-modal-resize-handle--se::before,
.account-modal-resize-handle--sw::before {
  content: none;
}

.account-modal-resize-handle--ne::after,
.account-modal-resize-handle--nw::after,
.account-modal-resize-handle--se::after,
.account-modal-resize-handle--sw::after {
  position: absolute;
  inset: 2px;
  border-radius: 8px;
  background:
    linear-gradient(135deg, transparent 52%, rgba(15, 94, 226, 0.78) 52%, rgba(15, 94, 226, 0.78) 60%, transparent 60%),
    linear-gradient(135deg, transparent 68%, rgba(18, 184, 166, 0.72) 68%, rgba(18, 184, 166, 0.72) 76%, transparent 76%);
  content: '';
  opacity: 0;
  transition: opacity 0.18s ease, transform 0.18s ease;
}

:deep(.account-modal.account-modal--create:hover) .account-modal-resize-handle--ne::after,
:deep(.account-modal.account-modal--create:hover) .account-modal-resize-handle--nw::after,
:deep(.account-modal.account-modal--create:hover) .account-modal-resize-handle--se::after,
:deep(.account-modal.account-modal--create:hover) .account-modal-resize-handle--sw::after {
  opacity: 0.52;
}

:deep(.account-modal.account-modal--create.is-resizing) .account-modal-resize-handle--ne::after,
:deep(.account-modal.account-modal--create.is-resizing) .account-modal-resize-handle--nw::after,
:deep(.account-modal.account-modal--create.is-resizing) .account-modal-resize-handle--se::after,
:deep(.account-modal.account-modal--create.is-resizing) .account-modal-resize-handle--sw::after {
  opacity: 0.9;
  transform: scale(1.04);
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
  display: flex;
  flex-direction: column;
  flex: 1;
  height: 100%;
  min-height: 0;
}

.account-modal-shell--create {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.account-create-viewport {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  isolation: isolate;
}

.account-create-blank-page {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  opacity: 0.96;
  pointer-events: none;
}

.account-create-content-shell {
  position: relative;
  z-index: 1;
  flex: 1 1 auto;
  width: 100%;
  max-height: 100%;
  min-width: 0;
  min-height: 0;
  height: 100%;
  padding: 2px 4px 0 0;
  overflow-y: scroll !important;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.92) rgba(226, 232, 240, 0.4);
}

.account-create-content-shell::-webkit-scrollbar {
  width: 14px;
  height: 12px;
}

.account-create-content-shell::-webkit-scrollbar-track {
  border-radius: 999px;
  border: 1px solid rgba(203, 213, 225, 0.66);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.46);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(226, 232, 240, 0.72), rgba(226, 232, 240, 0.38));
}

.account-create-content-shell::-webkit-scrollbar-thumb {
  border: 2px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(100, 116, 139, 0.96), rgba(71, 85, 105, 0.96));
  background-clip: padding-box;
}

.account-create-content-shell::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(71, 85, 105, 0.98), rgba(51, 65, 85, 0.98));
  background-clip: padding-box;
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
  grid-template-columns: minmax(280px, 332px) minmax(0, 1fr);
  align-items: start;
  gap: 18px;
  min-width: 0;
  min-height: max-content;
}

.account-create-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(340px, 0.92fr);
  gap: 20px;
  padding: 28px;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 32px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.2), transparent 32%),
    radial-gradient(circle at left bottom, rgba(16, 185, 129, 0.22), transparent 28%),
    linear-gradient(135deg, rgba(7, 52, 135, 0.96), rgba(17, 103, 196, 0.92) 48%, rgba(13, 148, 136, 0.9) 100%);
  box-shadow:
    0 30px 80px rgba(15, 23, 42, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
  overflow: hidden !important;
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
  padding: 16px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.08));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.account-create-hero__stat-card span,
.account-create-hero__stat-card small {
  color: rgba(226, 232, 240, 0.88);
}

.account-create-hero__stat-card strong {
  color: #ffffff;
  font-size: 24px;
  line-height: 1.15;
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
}

.account-create-environment-card p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.account-create-form-shell__heading-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.account-create-form-shell__heading-main h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.account-create-form-shell__heading-main > p:last-child {
  margin: 0;
  color: #64748b;
  line-height: 1.85;
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
}

.account-modal-overlay__viewport.is-fullscreen {
  padding: 0;
}

.account-modal-overlay__viewport :deep(.account-modal) {
  pointer-events: auto;
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
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: linear-gradient(180deg, #f8fbff, #eef5fb 46%, #f7fbff 100%);
  border: 1px solid rgba(59, 130, 246, 0.32);
  overflow: hidden;
  will-change: left, top, width, height;
  transition: box-shadow 0.18s ease, border-color 0.18s ease;
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
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 250, 252, 0.985)),
    radial-gradient(circle at top right, rgba(18, 184, 166, 0.08), transparent 38%);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
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
  /**
   * 第①层内部只保留“固定标题 + 固定内容视口”两段结构。
   * 第②层 / 第③层全部塞进 `account-create-content-shell` 中，由该内容壳独占右侧纵向滚动条。
   * 这样可以同时满足：
   * 1. 标题层固定不动；
   * 2. 内容全部在第①层内部显示；
   * 3. 右侧滚动条明确出现在第①层内部内容区。
   */
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  padding: 12px 20px 20px;
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
</style>

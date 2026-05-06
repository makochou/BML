<template>
  <div class="page-wrapper">
    <!-- ════════════════════════════════════════════════
         查询面板
         ════════════════════════════════════════════════ -->
    <GovernanceCompactQueryPanel density="ultra" theme="aurora">
      <template #note>
        <a-button class="query-panel-toggle-btn" @click="queryExpanded = !queryExpanded">
          <template #icon>
            <component :is="queryExpanded ? IconUp : IconDown" />
          </template>
          {{ queryExpanded ? '收起条件' : '更多条件' }}
        </a-button>
      </template>
      <template #footerActions>
        <div class="query-panel-mode-actions">
          <a-button type="primary" class="query-panel-mode-btn"
            :class="{ 'is-active': textMatchMode === 'fuzzy', 'is-inactive': textMatchMode !== 'fuzzy' }"
            @click="textMatchMode = 'fuzzy'; handleSearch()">模糊查找</a-button>
          <a-button type="primary" class="query-panel-mode-btn"
            :class="{ 'is-active': textMatchMode === 'exact', 'is-inactive': textMatchMode !== 'exact' }"
            @click="textMatchMode = 'exact'; handleSearch()">精确查找</a-button>
        </div>
        <a-button @click="handleReset">重置条件</a-button>
      </template>
      <a-form :model="queryParams" layout="inline" class="biz-query-form">
        <!-- 主要字段（默认显示 3 列） -->
        <div class="biz-query-form-primary">
          <a-form-item field="orgName" label="机构名称">
            <a-input v-model="queryParams.orgName" placeholder="请输入机构名称" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="orgCode" label="机构编码">
            <a-input v-model="queryParams.orgCode" placeholder="请输入机构编码" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="status" label="状态">
            <a-select v-model="queryParams.status" placeholder="全部" allow-clear @change="handleSearch">
              <a-option :value="1">正常</a-option>
              <a-option :value="0">停用</a-option>
            </a-select>
          </a-form-item>
        </div>

        <!-- 次要字段（展开时显示，4 列网格） -->
        <transition name="query-expand">
          <div v-if="queryExpanded" class="biz-query-form-extra">
            <a-form-item field="orgType" label="机构类型">
              <a-select v-model="queryParams.orgType" placeholder="全部" allow-clear @change="handleSearch">
                <a-option v-for="opt in ORG_TYPE_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</a-option>
              </a-select>
            </a-form-item>
            <a-form-item field="dataIsolation" label="数据隔离">
              <a-select v-model="queryParams.dataIsolation" placeholder="全部" allow-clear @change="handleSearch">
                <a-option v-for="iso in ISOLATION_OPTIONS" :key="iso.value" :value="iso.value">{{ iso.label }}</a-option>
              </a-select>
            </a-form-item>
            <a-form-item field="leader" label="负责人">
              <a-input v-model="queryParams.leader" placeholder="请输入负责人" allow-clear @press-enter="handleSearch" />
            </a-form-item>
            <a-form-item field="phone" label="联系电话">
              <a-input v-model="queryParams.phone" placeholder="请输入联系电话" allow-clear @press-enter="handleSearch" />
            </a-form-item>
            <a-form-item field="creditCode" label="信用代码">
              <a-input v-model="queryParams.creditCode" placeholder="统一社会信用代码" allow-clear @press-enter="handleSearch" />
            </a-form-item>
            <a-form-item field="legalPerson" label="法人代表">
              <a-input v-model="queryParams.legalPerson" placeholder="请输入法人代表" allow-clear @press-enter="handleSearch" />
            </a-form-item>
            <a-form-item field="province" label="所在省份">
              <a-input v-model="queryParams.province" placeholder="请输入省份" allow-clear @press-enter="handleSearch" />
            </a-form-item>
            <a-form-item field="city" label="所在城市">
              <a-input v-model="queryParams.city" placeholder="请输入城市" allow-clear @press-enter="handleSearch" />
            </a-form-item>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <!-- ════════════════════════════════════════════════
         列表舞台
         ════════════════════════════════════════════════ -->
    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button type="primary" @click="handleAdd()">
          <template #icon><icon-plus /></template>
          新增机构
        </a-button>
        <a-popover trigger="click" position="br"
          :content-style="{ padding: '0', background: 'transparent', boxShadow: 'none', border: 'none' }">
          <a-button class="table-column-setting-btn">
            <template #icon><icon-settings /></template>
            列设置
          </a-button>
          <template #content>
            <BusinessTableColumnSetting
              :items="columnSettingItems"
              :drag-state="dragState"
              @toggle-visible="toggleColumnVisible"
              @move="moveColumn"
              @toggle-fixed="toggleColumnFixed"
              @drag-start="handleDragStart"
              @drag-over="handleDragOver"
              @drop="handleDrop"
              @drag-end="handleDragEnd"
              @reset="resetColumns"
            />
          </template>
        </a-popover>
      </template>

      <a-table
        :key="tableResetKey"
        :data="pagedTableData"
        :loading="loading"
        :bordered="false"
        :pagination="false"
        row-key="id"
        :expanded-keys="expandedKeys"
        @expand="handleExpandChange"
        size="small"
        :columns="visibleColumns"
        :scroll="{ x: 1300, y: '100%' }"
        :scrollbar="false"
        sticky-header
        column-resizable
        @column-resize="handleColumnResize"
        @row-dblclick="handleRowDblClick"
      >
        <!--
          ══════════════════════════════════════════════════════
          自定义列头：每列标题旁加放大镜搜索图标（与授权治理一致）
          ══════════════════════════════════════════════════════
        -->
        <template #th-orgName>
          <TableColumnSearch title="机构名称" v-model="columnFilters['orgName']" />
        </template>
        <template #th-orgCode>
          <TableColumnSearch title="机构编码" v-model="columnFilters['orgCode']" />
        </template>
        <template #th-orgType>
          <TableColumnSearch title="机构类型" v-model="columnFilters['orgType']" />
        </template>
        <template #th-leader>
          <TableColumnSearch title="负责人" v-model="columnFilters['leader']" />
        </template>
        <template #th-phone>
          <TableColumnSearch title="联系电话" v-model="columnFilters['phone']" />
        </template>
        <template #th-dataIsolation>
          <TableColumnSearch title="数据隔离" v-model="columnFilters['dataIsolation']" />
        </template>
        <template #th-sort>
          <TableColumnSearch title="排序" v-model="columnFilters['sort']" />
        </template>
        <template #th-status>
          <TableColumnSearch title="状态" v-model="columnFilters['status']" />
        </template>
        <template #th-createTime>
          <TableColumnSearch title="创建时间" v-model="columnFilters['createTime']" />
        </template>
        <template #th-creditCode>
          <TableColumnSearch title="统一社会信用代码" v-model="columnFilters['creditCode']" />
        </template>
        <template #th-legalPerson>
          <TableColumnSearch title="法定代表人" v-model="columnFilters['legalPerson']" />
        </template>
        <template #th-registeredCapital>
          <TableColumnSearch title="注册资本" v-model="columnFilters['registeredCapital']" />
        </template>
        <template #th-establishDate>
          <TableColumnSearch title="成立日期" v-model="columnFilters['establishDate']" />
        </template>
        <template #th-email>
          <TableColumnSearch title="邮箱" v-model="columnFilters['email']" />
        </template>
        <template #th-address>
          <TableColumnSearch title="地址" v-model="columnFilters['address']" />
        </template>
        <template #th-businessScope>
          <TableColumnSearch title="经营范围" v-model="columnFilters['businessScope']" />
        </template>
        <template #th-remark>
          <TableColumnSearch title="备注" v-model="columnFilters['remark']" />
        </template>

        <template #orgType="{ record }">
          <a-tag size="small" :color="orgTypeColor(record.orgType)">{{ orgTypeLabel(record.orgType) }}</a-tag>
        </template>
        <template #dataIsolation="{ record }">
          <a-tag size="small" :color="isolationColor(record.dataIsolation)">{{ isolationLabel(record.dataIsolation) }}</a-tag>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
        </template>
        <template #actions="{ record }">
          <div class="table-row-actions" @click.stop @dblclick.stop>
            <a-button type="primary" size="mini" class="table-action-btn table-action-btn--primary" @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-button size="mini" class="table-action-btn table-action-btn--danger" @click="confirmDelete(record)">
              <template #icon><icon-delete /></template>
              删除
            </a-button>
            <a-dropdown trigger="click" position="br">
              <a-button size="mini" class="table-action-btn table-action-btn--more">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption @click="handleAdd(record.id)">
                  <template #icon><icon-plus /></template>
                  新增子机构
                </a-doption>
                <a-doption @click="openShareDialog(record)">
                  <template #icon><icon-share-external /></template>
                  数据共享配置
                </a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>

      <!-- 底部统计栏：左侧统计信息 + 右侧树控制 & 分页 -->
      <div class="biz-table-footer">
        <div class="biz-table-footer__stats">
          <span class="biz-table-footer__total">共 <b>{{ totalOrgCount }}</b> 条</span>
          <a-divider direction="vertical" />
          <span>顶级 <b>{{ topLevelCount }}</b> 条</span>
          <a-divider direction="vertical" />
          <span class="stat-normal">正常 <b>{{ activeCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-disabled">停用 <b>{{ disabledCount }}</b></span>
        </div>
        <div class="biz-table-footer__actions">
          <a-button-group size="mini">
            <a-button @click="expandAll">
              <template #icon><icon-expand /></template>
              全部展开
            </a-button>
            <a-button @click="collapseAll">
              <template #icon><icon-shrink /></template>
              全部收缩
            </a-button>
          </a-button-group>
          <a-divider direction="vertical" style="margin: 0 8px;" />
          <span class="pagination-label">每页</span>
          <a-select v-model="pageSize" size="mini" style="width: 80px;" @change="handlePageSizeChange">
            <a-option :value="10">10 条</a-option>
            <a-option :value="20">20 条</a-option>
            <a-option :value="50">50 条</a-option>
            <a-option :value="0">全部</a-option>
          </a-select>
          <a-pagination
            v-if="pageSize > 0"
            v-model:current="currentPage"
            :total="topLevelCount"
            :page-size="pageSize"
            size="mini"
            :show-total="false"
            :show-page-size="false"
            style="margin-left: 8px;"
            @change="handlePageChange"
          />
        </div>
      </div>
    </GovernanceListStage>

    <!-- 数据共享配置弹窗 -->
    <BmlModal v-model:visible="shareDialogVisible" :title="shareDialogTitle" :width="780" :height="520" :min-width="580" :min-height="380">
      <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
        <span style="font-size: 13px; color: var(--color-text-2);">配置本机构数据对其他机构的共享规则</span>
        <a-button type="primary" size="small" @click="handleAddShare">
          <template #icon><icon-plus /></template>
          新增共享规则
        </a-button>
      </div>
      <a-table :data="shareTableData" :loading="shareLoading" :bordered="false" :pagination="false" row-key="id" size="small" :scroll="{ y: '100%' }" :scrollbar="true" sticky-header>
        <template #columns>
          <a-table-column title="目标机构" data-index="targetOrgId" :width="160">
            <template #cell="{ record }">
              {{ getOrgNameById(record.targetOrgId) || record.targetOrgId }}
            </template>
          </a-table-column>
          <a-table-column title="共享类型" :width="110" align="center">
            <template #cell="{ record }">
              <a-tag size="small" :color="record.shareType === 1 ? 'arcoblue' : 'gold'">{{ record.shareType === 1 ? '全模块' : '指定模块' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="模块编码" data-index="moduleCode" :width="130" />
          <a-table-column title="权限" :width="80" align="center">
            <template #cell="{ record }">
              <a-tag size="small" :color="record.permission === 1 ? 'green' : 'orange'">{{ record.permission === 1 ? '只读' : '读写' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="过期时间" data-index="expireTime" :width="160" />
          <a-table-column title="操作" :width="170" align="center">
            <template #cell="{ record }">
              <a-button type="text" size="mini" status="danger" @click="confirmDeleteShare(record.id)">删除</a-button>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </BmlModal>

    <!-- 新增共享规则弹窗 -->
    <BmlModal v-model:visible="shareFormVisible" title="新增共享规则" :width="500" :height="420" :min-width="400" :min-height="340">
      <a-form :model="shareFormData" ref="shareFormRef" layout="vertical">
        <a-form-item field="targetOrgId" label="目标机构" required>
          <a-tree-select v-model="shareFormData.targetOrgId" :data="orgTreeOptions" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" placeholder="请选择目标机构" allow-clear />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="shareType" label="共享类型" required>
              <a-select v-model="shareFormData.shareType" placeholder="请选择">
                <a-option :value="1">全模块共享</a-option>
                <a-option :value="2">指定模块共享</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="permission" label="权限级别" required>
              <a-select v-model="shareFormData.permission" placeholder="请选择">
                <a-option :value="1">只读</a-option>
                <a-option :value="2">读写</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="shareFormData.shareType === 2" field="moduleCode" label="模块编码">
          <a-input v-model="shareFormData.moduleCode" placeholder="多个用英文逗号分隔，如 finance,inventory" />
        </a-form-item>
        <a-form-item field="expireTime" label="过期时间">
          <a-date-picker v-model="shareFormData.expireTime" show-time placeholder="留空表示永不过期" style="width: 100%;" />
        </a-form-item>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="shareFormData.remark" placeholder="备注" :auto-size="{ minRows: 2, maxRows: 3 }" />
        </a-form-item>
      </a-form>
      <template #header-actions>
        <a-button @click="shareFormVisible = false">取消</a-button>
        <a-button type="primary" :loading="shareSubmitting" @click="handleSubmitShare">确定</a-button>
      </template>
    </BmlModal>

    <!-- 新增/编辑/查看弹窗 -->
    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="820" :height="640" :min-width="640" :min-height="480">
      <a-form :model="formData" ref="formRef" :rules="formReadonly ? undefined : formRules" layout="vertical" :disabled="formReadonly">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <a-tab-pane key="basic" title="基本信息">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="parentId" label="上级机构">
                  <a-tree-select v-model="formData.parentId" :data="orgTreeOptions"
                    :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
                    placeholder="请选择上级机构" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="orgName" label="机构名称" required>
                  <a-input v-model="formData.orgName" placeholder="请输入机构名称" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="orgCode" label="机构编码" required>
                  <a-input v-model="formData.orgCode" placeholder="请输入机构编码" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="orgType" label="机构类型" required>
                  <a-select v-model="formData.orgType" placeholder="请选择机构类型">
                    <a-option v-for="opt in ORG_TYPE_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item field="dataIsolation" label="数据隔离模式" required>
                  <a-select v-model="formData.dataIsolation" placeholder="请选择">
                    <a-option v-for="iso in ISOLATION_OPTIONS" :key="iso.value" :value="iso.value">
                      <span style="font-weight: 500;">{{ iso.label }}</span>
                      <span style="color: var(--color-text-3); font-size: 12px; margin-left: 6px;">{{ iso.desc }}</span>
                    </a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="leader" label="负责人">
                  <a-input v-model="formData.leader" placeholder="请输入负责人" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="sort" label="排序">
                  <a-input-number v-model="formData.sort" :min="0" placeholder="排序" style="width: 100%;" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="status" label="状态">
                  <a-select v-model="formData.status" placeholder="请选择">
                    <a-option :value="1">正常</a-option>
                    <a-option :value="0">停用</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item field="remark" label="备注">
                  <a-textarea v-model="formData.remark" placeholder="请输入备注" :auto-size="{ minRows: 2, maxRows: 4 }" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>

          <a-tab-pane key="business" title="工商信息">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="creditCode" label="统一社会信用代码">
                  <a-input v-model="formData.creditCode" placeholder="18位信用代码" :max-length="18" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="legalPerson" label="法定代表人">
                  <a-input v-model="formData.legalPerson" placeholder="请输入法定代表人" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="registeredCapital" label="注册资本（万元）">
                  <a-input-number v-model="formData.registeredCapital" :min="0" :precision="2" placeholder="注册资本" style="width: 100%;" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="establishDate" label="成立日期">
                  <a-date-picker v-model="formData.establishDate" placeholder="请选择成立日期" style="width: 100%;" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item field="businessScope" label="经营范围">
                  <a-textarea v-model="formData.businessScope" placeholder="请输入经营范围" :auto-size="{ minRows: 3, maxRows: 6 }" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>

          <a-tab-pane key="contact" title="联系与地址">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="phone" label="联系电话">
                  <a-input v-model="formData.phone" placeholder="请输入联系电话" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="email" label="邮箱">
                  <a-input v-model="formData.email" placeholder="请输入邮箱" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item field="province" label="省份">
                  <a-input v-model="formData.province" placeholder="省份" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item field="city" label="城市">
                  <a-input v-model="formData.city" placeholder="城市" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item field="district" label="区县">
                  <a-input v-model="formData.district" placeholder="区县" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item field="address" label="详细地址">
                  <a-input v-model="formData.address" placeholder="请输入详细地址" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>
        </a-tabs>
      </a-form>
      <template #header-actions>
        <a-button @click="dialogVisible = false">{{ formReadonly ? '关闭' : '取消' }}</a-button>
        <a-button v-if="!formReadonly" type="primary" :loading="submitting" @click="handleSubmit">确定</a-button>
      </template>
    </BmlModal>
  </div>
</template>

<script lang="ts" setup>
/**
 * 机构管理页面
 * defineOptions({ name: 'SystemOrg' }) 是 keep-alive 缓存的关键。
 */
defineOptions({ name: 'SystemOrg' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconDelete, IconMore, IconSettings, IconUp, IconDown, IconShareExternal, IconExpand, IconShrink } from '@arco-design/web-vue/es/icon';
import { fetchOrgList, createOrg, updateOrg, deleteOrg, fetchOrgShareList, createOrgShare, deleteOrgShare, type OrgVO, type OrgForm, type OrgQuery, type OrgDataShareVO, type OrgDataShareForm } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import TableColumnSearch from '../../../../components/common/TableColumnSearch.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';
import { useButtonPermission } from '../../../../composables/useButtonPermission';

const ORG_TYPE_OPTIONS = [
  { value: 1, label: '集团', color: 'red' },
  { value: 2, label: '公司', color: 'arcoblue' },
  { value: 3, label: '分公司', color: 'cyan' },
  { value: 4, label: '子公司', color: 'purple' },
  { value: 5, label: '办事处', color: 'green' },
  { value: 6, label: '事业部', color: 'gold' },
];
const orgTypeLabel = (t: number) => ORG_TYPE_OPTIONS.find(o => o.value === t)?.label || '未知';
const orgTypeColor = (t: number) => ORG_TYPE_OPTIONS.find(o => o.value === t)?.color || 'gray';

const ISOLATION_OPTIONS = [
  { value: 0, label: '共享', desc: '上级机构可查看所有下级机构数据', color: 'arcoblue' },
  { value: 1, label: '完全隔离', desc: '各机构数据完全独立，上下级均不可互查', color: 'orangered' },
  { value: 2, label: '汇总共享', desc: '上级仅可查看下级的汇总统计，不可查看明细', color: 'gold' },
  { value: 3, label: '同级互通', desc: '同一父机构下的兄弟机构可互查数据', color: 'green' },
  { value: 4, label: '按模块隔离', desc: '部分业务模块隔离，部分共享（需配合模块配置）', color: 'purple' },
];
const isolationLabel = (v: number) => ISOLATION_OPTIONS.find(o => o.value === v)?.label || '未知';
const isolationColor = (v: number) => ISOLATION_OPTIONS.find(o => o.value === v)?.color || 'gray';

/**
 * 机构列默认配置（与授权治理列管理模式一致）：
 * ────────────────────────────────────────────────
 * 默认显示方案：核心标识 → 运营状态 → 归属信息 → 低频扩展字段
 * - 机构名称：默认固定在左侧（fixed: 'left'）
 * - 常用字段默认显示（visible: true）
 * - 扩展字段默认隐藏（visible: false），可通过列设置开启
 * - 操作列锁定在右侧（locked: true, fixed: 'right'）
 */
/**
 * 列头搜索筛选条件（与授权治理 columnFilters 一致）。
 * key 与列的 dataIndex 对应，值为搜索关键词。
 */
const columnFilters = reactive<Record<string, string>>({
  orgName: '', orgCode: '', orgType: '', leader: '', phone: '',
  dataIsolation: '', sort: '', status: '', createTime: '',
  creditCode: '', legalPerson: '', registeredCapital: '', establishDate: '',
  email: '', address: '', businessScope: '', remark: '',
});

const defaultColumns: BusinessTableColumn[] = [
  /* ── 核心标识（默认显示） ── */
  { key: 'orgName',       title: '机构名称',       dataIndex: 'orgName',       width: 220, visible: true, fixed: 'left', sortable: true, titleSlotName: 'th-orgName' },
  { key: 'orgCode',       title: '机构编码',       dataIndex: 'orgCode',       width: 130, visible: true, sortable: true, titleSlotName: 'th-orgCode' },
  { key: 'orgType',       title: '机构类型',       slotName: 'orgType',        width: 100, visible: true, align: 'center', sortable: true, titleSlotName: 'th-orgType' },
  { key: 'leader',        title: '负责人',         dataIndex: 'leader',        width: 100, visible: true, sortable: true, titleSlotName: 'th-leader' },
  { key: 'phone',         title: '联系电话',       dataIndex: 'phone',         width: 130, visible: true, sortable: true, titleSlotName: 'th-phone' },
  { key: 'dataIsolation', title: '数据隔离',       slotName: 'dataIsolation',  width: 110, visible: true, align: 'center', sortable: true, titleSlotName: 'th-dataIsolation' },
  { key: 'sort',          title: '排序',           dataIndex: 'sort',          width: 70,  visible: true, align: 'center', sortable: true, titleSlotName: 'th-sort' },
  { key: 'status',        title: '状态',           slotName: 'status',         width: 80,  visible: true, align: 'center', sortable: true, titleSlotName: 'th-status' },
  { key: 'createTime',    title: '创建时间',       dataIndex: 'createTime',    width: 170, visible: true, sortable: true, titleSlotName: 'th-createTime' },
  /* ── 扩展字段（默认隐藏） ── */
  { key: 'creditCode',       title: '统一社会信用代码', dataIndex: 'creditCode',       width: 200, visible: false, sortable: true, titleSlotName: 'th-creditCode' },
  { key: 'legalPerson',      title: '法定代表人',       dataIndex: 'legalPerson',      width: 120, visible: false, sortable: true, titleSlotName: 'th-legalPerson' },
  { key: 'registeredCapital', title: '注册资本',        dataIndex: 'registeredCapital', width: 120, visible: false, align: 'right', sortable: true, titleSlotName: 'th-registeredCapital' },
  { key: 'establishDate',    title: '成立日期',         dataIndex: 'establishDate',    width: 120, visible: false, sortable: true, titleSlotName: 'th-establishDate' },
  { key: 'email',            title: '邮箱',             dataIndex: 'email',            width: 180, visible: false, sortable: true, titleSlotName: 'th-email' },
  { key: 'address',          title: '地址',             dataIndex: 'address',          width: 260, visible: false, ellipsis: true, sortable: true, titleSlotName: 'th-address' },
  { key: 'businessScope',    title: '经营范围',         dataIndex: 'businessScope',    width: 260, visible: false, ellipsis: true, sortable: true, titleSlotName: 'th-businessScope' },
  { key: 'remark',           title: '备注',             dataIndex: 'remark',           width: 200, visible: false, ellipsis: true, sortable: true, titleSlotName: 'th-remark' },
  /* ── 操作列（锁定） ── */
  { key: 'actions', title: '操作', slotName: 'actions', width: 170, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const {
  visibleColumns, columnSettingItems, dragState, tableResetKey,
  handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed,
  handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns,
} = useBusinessTableColumns('system-org', defaultColumns);

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');
const queryExpanded = ref(false);

const loading = ref(false);
const tableData = ref<OrgVO[]>([]);

/** 递归收集树所有节点 ID，用于默认展开全部行 */
const collectAllKeys = (nodes: OrgVO[]): number[] =>
  nodes.reduce<number[]>((keys, n) => {
    keys.push(n.id);
    if (n.children?.length) keys.push(...collectAllKeys(n.children));
    return keys;
  }, []);
const expandedKeys = ref<number[]>([]);

/** 递归统计树形数据总条数 */
const countTreeNodes = (nodes: OrgVO[]): number =>
  nodes.reduce((sum, n) => sum + 1 + (n.children ? countTreeNodes(n.children) : 0), 0);
const totalOrgCount = computed(() => countTreeNodes(tableData.value));

/** 递归统计树形数据中指定状态的节点数量 */
const countByStatus = (nodes: OrgVO[], status: number): number =>
  nodes.reduce((sum, n) => sum + (n.status === status ? 1 : 0) + (n.children ? countByStatus(n.children, status) : 0), 0);

/** 顶级机构数量（树的根节点数） */
const topLevelCount = computed(() => tableData.value.length);
/** 正常状态机构数量 */
const activeCount = computed(() => countByStatus(tableData.value, 1));
/** 停用状态机构数量 */
const disabledCount = computed(() => countByStatus(tableData.value, 0));

/* ══════ 树形分页（按顶级节点分页，保持每棵子树完整） ══════ */
const pageSize = ref(10);
const currentPage = ref(1);

/** 分页后的表格数据：按顶级节点切片，pageSize 为 0 时显示全部 */
const pagedTableData = computed(() => {
  if (pageSize.value === 0) return tableData.value;
  const start = (currentPage.value - 1) * pageSize.value;
  return tableData.value.slice(start, start + pageSize.value);
});

/** 全部展开 */
const expandAll = () => { expandedKeys.value = collectAllKeys(pagedTableData.value); };
/** 全部收缩 */
const collapseAll = () => { expandedKeys.value = []; };

/**
 * 树形表格展开/收缩事件处理
 * 使用受控 :expanded-keys 时必须手动维护展开状态
 */
const handleExpandChange = (rowKey: number) => {
  const idx = expandedKeys.value.indexOf(rowKey);
  if (idx >= 0) { expandedKeys.value.splice(idx, 1); }
  else { expandedKeys.value.push(rowKey); }
};

/** 切换每页条数时重置到第一页 */
const handlePageSizeChange = () => {
  currentPage.value = 1;
  expandedKeys.value = collectAllKeys(pagedTableData.value);
};
/** 翻页后自动展开当前页所有节点 */
const handlePageChange = () => {
  expandedKeys.value = collectAllKeys(pagedTableData.value);
};

const dialogVisible = ref(false);
const dialogTitle = ref('新增机构');
const formRef = ref();

/** 表单只读模式：双击行无编辑权限时以查看模式打开 */
const formReadonly = ref(false);

/* 按钮级权限检查 */
const { hasPermission } = useButtonPermission();
/** 是否拥有机构编辑权限 */
const canEditOrg = computed(() => hasPermission('system:org:edit'));

const queryParams = reactive<OrgQuery>({
  orgName: '', orgCode: '', orgType: undefined, status: undefined,
  dataIsolation: undefined, leader: '', phone: '',
  creditCode: '', legalPerson: '', province: '', city: '',
});

const defaultForm = (): OrgForm => ({
  id: undefined, parentId: 0, orgName: '', orgCode: '', orgType: 2,
  creditCode: '', legalPerson: '', registeredCapital: undefined,
  establishDate: '', sort: 0, leader: '', phone: '', email: '',
  province: '', city: '', district: '', address: '',
  businessScope: '', status: 1, remark: '', dataIsolation: 0
});
const formData = reactive<OrgForm>(defaultForm());

const formRules = {
  orgName: [{ required: true, message: '请输入机构名称' }],
  orgCode: [{ required: true, message: '请输入机构编码' }],
  orgType: [{ required: true, message: '请选择机构类型' }],
  dataIsolation: [{ required: true, message: '请选择数据隔离模式' }],
};

const orgTreeOptions = computed(() => {
  const root = {
    id: 0, parentId: -1, orgName: '顶级机构', orgCode: '', orgType: 0,
    creditCode: '', legalPerson: '', registeredCapital: 0, establishDate: '',
    sort: 0, leader: '', phone: '', email: '', province: '', city: '', district: '',
    address: '', businessScope: '', status: 1, remark: '', dataIsolation: 0,
    createTime: '', children: tableData.value
  } as OrgVO;
  return [root];
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchOrgList(queryParams) as any;
    tableData.value = res.data || [];
    // 重新加载数据后重置到第一页
    currentPage.value = 1;
    // 数据加载完成后默认展开当前页所有行
    expandedKeys.value = collectAllKeys(pagedTableData.value);
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

const handleSearch = () => { loadData(); };
const handleReset = () => {
  queryParams.orgName = ''; queryParams.orgCode = '';
  queryParams.orgType = undefined; queryParams.status = undefined;
  queryParams.dataIsolation = undefined; queryParams.leader = '';
  queryParams.phone = ''; queryParams.creditCode = '';
  queryParams.legalPerson = ''; queryParams.province = '';
  queryParams.city = '';
  loadData();
};

const handleAdd = (parentId?: number) => {
  formReadonly.value = false;
  dialogTitle.value = '新增机构';
  Object.assign(formData, defaultForm());
  if (parentId !== undefined) formData.parentId = parentId;
  dialogVisible.value = true;
};

const handleEdit = (row: OrgVO) => {
  formReadonly.value = false;
  dialogTitle.value = '编辑机构';
  Object.assign(formData, {
    id: row.id, parentId: row.parentId, orgName: row.orgName, orgCode: row.orgCode,
    orgType: row.orgType, creditCode: row.creditCode, legalPerson: row.legalPerson,
    registeredCapital: row.registeredCapital, establishDate: row.establishDate,
    sort: row.sort, leader: row.leader, phone: row.phone, email: row.email,
    province: row.province, city: row.city, district: row.district,
    address: row.address, businessScope: row.businessScope,
    status: row.status, remark: row.remark, dataIsolation: row.dataIsolation
  });
  dialogVisible.value = true;
};

/**
 * 双击行打开详情：
 * - 有编辑权限 → 打开编辑弹窗（与操作列“编辑”按钮一致）
 * - 无编辑权限 → 以只读查看模式打开，表单禁用、隐藏确定按钮
 */
const handleRowDblClick = (record: OrgVO) => {
  if (canEditOrg.value) {
    handleEdit(record);
  } else {
    formReadonly.value = true;
    dialogTitle.value = '查看机构';
    Object.assign(formData, {
      id: record.id, parentId: record.parentId, orgName: record.orgName, orgCode: record.orgCode,
      orgType: record.orgType, creditCode: record.creditCode, legalPerson: record.legalPerson,
      registeredCapital: record.registeredCapital, establishDate: record.establishDate,
      sort: record.sort, leader: record.leader, phone: record.phone, email: record.email,
      province: record.province, city: record.city, district: record.district,
      address: record.address, businessScope: record.businessScope,
      status: record.status, remark: record.remark, dataIsolation: record.dataIsolation
    });
    dialogVisible.value = true;
  }
};

const submitting = ref(false);
const handleSubmit = async () => {
  try {
    const errors = await formRef.value?.validate();
    if (errors) return;
    submitting.value = true;
    if (formData.id) {
      await updateOrg(formData);
      Message.success('修改成功');
    } else {
      await createOrg(formData);
      Message.success('新增成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const confirmDelete = (record: OrgVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确认删除机构「${record.orgName}」吗？`,
    okButtonProps: { status: 'danger' },
    onOk: () => handleDelete(record.id),
  });
};

const handleDelete = async (id: number) => {
  try {
    const res = await deleteOrg(id) as any;
    const msg: string = res?.message || res?.msg || '';
    if (msg.includes('存在子机构')) {
      Message.warning('存在子机构，不允许删除');
      return;
    }
    Message.success('删除成功');
    loadData();
  } catch (err: any) {
    const msg: string = err?.response?.data?.message || err?.response?.data?.msg || err?.message || '';
    if (msg.includes('存在子机构')) {
      Message.warning('存在子机构，不允许删除');
    }
  }
};

// ── 数据共享配置 ──
const shareDialogVisible = ref(false);
const shareDialogTitle = ref('数据共享配置');
const shareLoading = ref(false);
const shareTableData = ref<OrgDataShareVO[]>([]);
const currentShareOrgId = ref<number | null>(null);
const shareFormVisible = ref(false);
const shareFormRef = ref();
const shareSubmitting = ref(false);

const defaultShareForm = (): OrgDataShareForm => ({
  id: undefined, sourceOrgId: undefined, targetOrgId: undefined,
  shareType: 1, moduleCode: '', permission: 1, status: 1,
  expireTime: undefined, remark: ''
});
const shareFormData = reactive<OrgDataShareForm>(defaultShareForm());

const getOrgNameById = (id: number): string => {
  const find = (nodes: OrgVO[]): string => {
    for (const node of nodes) {
      if (node.id === id) return node.orgName;
      if (node.children?.length) {
        const found = find(node.children);
        if (found) return found;
      }
    }
    return '';
  };
  return find(tableData.value);
};

const openShareDialog = async (record: OrgVO) => {
  currentShareOrgId.value = record.id;
  shareDialogTitle.value = `数据共享配置 — ${record.orgName}`;
  shareDialogVisible.value = true;
  await loadShareData();
};

const loadShareData = async () => {
  if (!currentShareOrgId.value) return;
  shareLoading.value = true;
  try {
    const res = await fetchOrgShareList(currentShareOrgId.value) as any;
    shareTableData.value = res.data || [];
  } catch { shareTableData.value = []; }
  finally { shareLoading.value = false; }
};

const handleAddShare = () => {
  Object.assign(shareFormData, defaultShareForm());
  shareFormData.sourceOrgId = currentShareOrgId.value ?? undefined;
  shareFormVisible.value = true;
};

const handleSubmitShare = async () => {
  try {
    shareSubmitting.value = true;
    await createOrgShare(shareFormData);
    Message.success('新增共享规则成功');
    shareFormVisible.value = false;
    await loadShareData();
  } catch { /* keep dialog open */ }
  finally { shareSubmitting.value = false; }
};

const confirmDeleteShare = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确认删除该共享规则吗？',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try { await deleteOrgShare(id); Message.success('删除成功'); await loadShareData(); }
      catch { /* ignore */ }
    }
  });
};

onMounted(() => { loadData(); });
</script>

<style scoped>
.form-tabs :deep(.arco-tabs-content) {
  padding-top: 12px;
}
</style>

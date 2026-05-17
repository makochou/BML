<template>
  <div class="page-wrapper">
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
          <a-form-item field="username" label="账号">
            <a-input v-model="queryParams.username" placeholder="请输入账号" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="phone" label="手机号">
            <a-input v-model="queryParams.phone" placeholder="请输入手机号" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="status" label="状态">
            <a-select v-model="queryParams.status" placeholder="全部" allow-clear @change="handleSearch">
              <a-option :value="1">正常</a-option>
              <a-option :value="0">停用</a-option>
              <a-option :value="2">锁定</a-option>
            </a-select>
          </a-form-item>
        </div>

        <!-- 次要字段（展开时显示，4 列网格） -->
        <transition name="query-expand">
          <div v-if="queryExpanded" class="biz-query-form-extra">
            <a-form-item field="nickname" label="用户名">
              <a-input v-model="queryParams.nickname" placeholder="请输入用户名" allow-clear @press-enter="handleSearch" />
            </a-form-item>
            <a-form-item field="orgId" label="所属机构">
              <a-tree-select v-model="queryParams.orgId" :data="orgTreeData"
                :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
                placeholder="全部机构" allow-clear @change="handleSearch" />
            </a-form-item>
            <a-form-item field="deptId" label="所属部门">
              <a-tree-select v-model="queryParams.deptId" :data="deptTreeData"
                :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
                placeholder="全部部门" allow-clear @change="handleSearch" />
            </a-form-item>
            <a-form-item field="email" label="邮箱">
              <a-input v-model="queryParams.email" placeholder="请输入邮箱" allow-clear @press-enter="handleSearch" />
            </a-form-item>
            <a-form-item field="gender" label="性别">
              <a-select v-model="queryParams.gender" placeholder="全部" allow-clear @change="handleSearch">
                <a-option :value="1">男</a-option>
                <a-option :value="2">女</a-option>
                <a-option :value="0">未知</a-option>
              </a-select>
            </a-form-item>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button type="primary" v-if="hasPermission('system:user:add')" @click="handleAdd">
          <template #icon><icon-plus /></template>
          新增用户
        </a-button>
        <a-button v-if="hasPermission('system:user:assignPerms')" :disabled="!activeRowId" @click="openUserPermissionFromToolbar">
          <template #icon><icon-safe /></template>
          功能授权
        </a-button>
        <a-popover trigger="click" position="br"
          :content-style="{ padding: '0', background: 'transparent', boxShadow: 'none', border: 'none' }">
          <a-button class="table-column-setting-btn">
            <template #icon><icon-settings /></template>
            列设置
          </a-button>
          <template #content>
            <BusinessTableColumnSetting :items="columnSettingItems" :drag-state="dragState"
              @toggle-visible="toggleColumnVisible" @move="moveColumn" @toggle-fixed="toggleColumnFixed"
              @drag-start="handleDragStart" @drag-over="handleDragOver" @drop="handleDrop"
              @drag-end="handleDragEnd" @reset="resetColumns" />
          </template>
        </a-popover>
      </template>
      <a-table :key="tableResetKey" :data="filteredData" :loading="loading" :bordered="false" :pagination="false"
        row-key="id" stripe size="small" :scroll="{ x: '100%', y: '100%' }" :scrollbar="false"
        sticky-header :columns="visibleColumns" :column-resizable="{ mode: 'fixed' }" ref="tableRef" :style="tableStyle" :row-class="getRowClass" @row-click="handleRowClick" @column-resize="handleColumnResize" @row-dblclick="handleRowDblClick">
        <!-- 自定义列头：每列标题旁加放大镜搜索图标（与授权治理一致） -->
        <template #th-username><TableColumnSearch title="账号" v-model="columnFilters['username']" /></template>
        <template #th-nickname><TableColumnSearch title="用户名" v-model="columnFilters['nickname']" /></template>
        <template #th-employeeNo><TableColumnSearch title="工号" v-model="columnFilters['employeeNo']" /></template>
        <template #th-orgName><TableColumnSearch title="所属机构" v-model="columnFilters['orgName']" /></template>
        <template #th-deptName><TableColumnSearch title="部门" v-model="columnFilters['deptName']" /></template>
        <template #th-postName><TableColumnSearch title="岗位" v-model="columnFilters['postName']" /></template>
        <template #th-roleNames><TableColumnSearch title="角色" v-model="columnFilters['roleNames']" /></template>
        <template #th-phone><TableColumnSearch title="手机号" v-model="columnFilters['phone']" /></template>
        <template #th-status><TableColumnSearch title="状态" v-model="columnFilters['status']" /></template>
        <template #th-createTime><TableColumnSearch title="创建时间" v-model="columnFilters['createTime']" /></template>
        <template #th-entryDate><TableColumnSearch title="入职日期" v-model="columnFilters['entryDate']" /></template>
        <template #th-email><TableColumnSearch title="邮箱" v-model="columnFilters['email']" /></template>
        <template #th-loginIp><TableColumnSearch title="最后登录IP" v-model="columnFilters['loginIp']" /></template>
        <template #th-loginDate><TableColumnSearch title="最后登录时间" v-model="columnFilters['loginDate']" /></template>
        <template #th-remark><TableColumnSearch title="备注" v-model="columnFilters['remark']" /></template>

        <template #employeeNo="{ record }">
          {{ record.employeeNo || '—' }}
        </template>
        <template #roleNames="{ record }">
          <template v-if="record.roleNames && record.roleNames.length">
            <a-tag v-for="name in record.roleNames" :key="name" size="small" color="arcoblue" style="margin: 1px 2px;">{{ name }}</a-tag>
          </template>
          <span v-else class="text-placeholder">—</span>
        </template>
        <template #status="{ record }">
          <a-tag :color="USER_STATUS_MAP[record.status]?.color" size="small">{{ USER_STATUS_MAP[record.status]?.label }}</a-tag>
        </template>
        
        <template #createBy="{ record }">
          <UserNameCell :user-id="record.createBy" />
        </template>
        <template #updateBy="{ record }">
          <UserNameCell :user-id="record.updateBy" />
        </template>
        <template #actions="{ record }">
          <div class="table-row-actions" @click.stop @dblclick.stop>
            <a-button type="primary" size="mini" class="table-action-btn table-action-btn--primary" v-if="hasPermission('system:user:edit')" @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-button size="mini" class="table-action-btn table-action-btn--danger" v-if="hasPermission('system:user:remove')" @click="confirmDelete(record.id)">
              <template #icon><icon-delete /></template>
              删除
            </a-button>
            <a-dropdown v-if="hasPermission('system:user:reset') || hasPermission('system:user:dataScope')" trigger="click" position="br">
              <a-button size="mini" class="table-action-btn table-action-btn--more">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption v-if="hasPermission('system:user:reset')" @click="openResetPwd(record)">
                  <template #icon><icon-lock /></template>
                  重置密码
                </a-doption>
                <a-doption v-if="hasPermission('system:user:dataScope')" @click="openUserDataScope(record)">
                  <template #icon><icon-safe /></template>
                  个人数据权限
                </a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>
      <!-- 底部统计栏：左侧统计信息 + 右侧分页 -->
      <div class="biz-table-footer">
        <div class="biz-table-footer__stats">
          <span class="biz-table-footer__total">共 <b>{{ pagination.total }}</b> 条</span>
          <a-divider direction="vertical" />
          <span class="stat-normal">正常 <b>{{ activeCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-disabled">停用 <b>{{ disabledCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-locked">锁定 <b>{{ lockedCount }}</b></span>
        </div>
        <div class="biz-table-footer__actions">
          <a-pagination v-model:current="pagination.current" v-model:page-size="pagination.pageSize"
            :total="pagination.total" show-total show-page-size size="small"
            @change="handlePageChange" @page-size-change="handlePageSizeChange" />
        </div>
      </div>
    </GovernanceListStage>

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="740" :height="620" :min-width="560" :min-height="440">
      <template #header-extra>
        <AuditInfoFooter :data="formData" />
      </template>
      <a-form :model="formData" ref="formRef" :rules="formReadonly ? undefined : formRules" layout="vertical" :disabled="formReadonly">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <a-tab-pane key="basic" title="账号信息">
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:user:field:username')" :span="12">
                <a-form-item field="username" label="账号">
                  <a-input v-model="formData.username" placeholder="请输入账号" />
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:user:field:nickname')" :span="12">
                <a-form-item field="nickname" label="用户名">
                  <a-input v-model="formData.nickname" placeholder="请输入用户名" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row v-if="!formData.id" :gutter="16">
              <a-col :span="12">
                <a-form-item field="password" label="密码">
                  <a-input-password v-model="formData.password" placeholder="请输入密码" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:user:field:phone')" :span="12">
                <a-form-item field="phone" label="手机号">
                  <a-input v-model="formData.phone" placeholder="请输入手机号" />
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:user:field:email')" :span="12">
                <a-form-item field="email" label="邮箱">
                  <a-input v-model="formData.email" placeholder="请输入邮箱" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:user:field:gender')" :span="12">
                <a-form-item field="gender" label="性别">
                  <a-select v-model="formData.gender" placeholder="请选择">
                    <a-option :value="0">未知</a-option>
                    <a-option :value="1">男</a-option>
                    <a-option :value="2">女</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:user:field:status')" :span="12">
                <a-form-item field="status" label="状态">
                  <a-select v-model="formData.status" placeholder="请选择">
                    <a-option :value="1">正常</a-option>
                    <a-option :value="0">停用</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:user:field:roleIds')" :span="12">
                <a-form-item field="roleIds" label="角色">
                  <a-select v-model="formData.roleIds" placeholder="请选择角色" multiple allow-clear>
                    <a-option v-for="role in roleOptions" :key="role.id" :value="role.id">{{ role.roleName }}</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:user:field:remark')" :span="12">
                <a-form-item field="remark" label="备注">
                  <a-input v-model="formData.remark" placeholder="请输入备注" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>
          <a-tab-pane key="org" title="组织与岗位">
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:user:field:orgId')" :span="12">
                <a-form-item field="orgId" label="所属机构">
                  <a-tree-select v-model="formData.orgId" :data="orgTreeData"
                    :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
                    placeholder="请选择所属机构" allow-clear />
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:user:field:deptId')" :span="12">
                <a-form-item field="deptId" label="所属部门">
                  <a-tree-select v-model="formData.deptId" :data="deptTreeData"
                    :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
                    placeholder="请选择所属部门" allow-clear />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:user:field:postId')" :span="12">
                <a-form-item field="postId" label="岗位">
                  <a-select v-model="formData.postId" placeholder="请选择岗位" allow-clear>
                    <a-option v-for="p in postOptions" :key="p.id" :value="p.id">{{ p.postName }}</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:user:field:employeeNo')" :span="12">
                <a-form-item field="employeeNo" label="工号">
                  <a-input v-model="formData.employeeNo" placeholder="请输入工号" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:user:field:superiorId')" :span="12">
                <a-form-item field="superiorId" label="直属上级">
                  <a-select v-model="formData.superiorId" placeholder="请选择直属上级" allow-clear filterable>
                    <a-option v-for="u in allUserOptions" :key="u.id" :value="u.id" :disabled="u.id === formData.id">{{ u.nickname || u.username }}{{ u.employeeNo ? ` (${u.employeeNo})` : '' }}</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:user:field:entryDate')" :span="12">
                <a-form-item field="entryDate" label="入职日期">
                  <a-date-picker v-model="formData.entryDate" placeholder="请选择入职日期" style="width: 100%;" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>
        </a-tabs>
      </a-form>
      <template #header-actions>
        <a-button v-if="formData.id && !formReadonly" status="warning" @click="resetPwdVisible = true">重置密码</a-button>
        <a-button @click="dialogVisible = false">{{ formReadonly ? '关闭' : '取消' }}</a-button>
        <a-button v-if="!formReadonly" type="primary" :loading="submitting" @click="handleSubmit">确定</a-button>
      </template>
    </BmlModal>

    <a-modal v-model:visible="resetPwdVisible" title="重置密码" :width="400"
      @ok="handleResetPassword" @cancel="resetPwdVisible = false">
      <a-form layout="vertical">
        <a-form-item label="新密码">
          <a-input-password v-model="newPassword" placeholder="请输入新密码" allow-clear />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 用户个人数据权限配置弹窗 -->
    <BmlModal v-model:visible="userDsDialogVisible" :title="userDsDialogTitle" :width="560" :height="480" :min-width="440" :min-height="360">
      <a-form :model="userDsForm" layout="vertical">
        <a-form-item field="dataScope" label="数据范围">
          <a-select v-model="userDsForm.dataScope" placeholder="请选择数据范围">
            <a-option v-for="ds in USER_DS_OPTIONS" :key="ds.value" :value="ds.value">{{ ds.label }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="userDsForm.dataScope === 7" field="customOrgIds" label="自定义机构范围">
          <a-input v-model="userDsForm.customOrgIds" placeholder="机构ID，多个用英文逗号分隔" />
        </a-form-item>
        <a-form-item v-if="userDsForm.dataScope === 7" field="customDeptIds" label="自定义部门范围">
          <a-input v-model="userDsForm.customDeptIds" placeholder="部门ID，多个用英文逗号分隔" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="status" label="状态">
              <a-select v-model="userDsForm.status" placeholder="请选择">
                <a-option :value="1">生效</a-option>
                <a-option :value="0">停用</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="expireTime" label="过期时间">
              <a-date-picker v-model="userDsForm.expireTime" show-time placeholder="留空表示永不过期" style="width: 100%;" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="userDsForm.remark" placeholder="记录授权原因" :auto-size="{ minRows: 2, maxRows: 3 }" />
        </a-form-item>
      </a-form>
      <template #header-actions>
        <a-button v-if="userDsForm.id" status="danger" @click="handleDeleteUserDs">删除配置</a-button>
        <a-button @click="userDsDialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="userDsSubmitting" @click="handleSubmitUserDs">保存</a-button>
      </template>
    </BmlModal>

    <!-- 用户个人功能授权弹窗（三面板权限分配，与角色授权一致） -->
    <UserPermissionDialog
      v-model:visible="userPermDialogVisible"
      :user-id="currentPermUserId"
      :user-name="currentPermUserName"
      @saved="loadData"
    />
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'SystemUser' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconMore, IconSettings, IconUp, IconDown, IconLock, IconDelete, IconSafe } from '@arco-design/web-vue/es/icon';
import {
  fetchUserPage, fetchUserDetail, createUser, updateUser, deleteUser, resetUserPassword,
  fetchRoleList, fetchOrgList, fetchDeptList, fetchPostList,
  fetchUserDataScope, saveUserDataScope, deleteUserDataScope,
  type UserVO, type UserForm, type RoleVO, type OrgVO, type DeptVO, type PostVO, type UserDataScopeForm
} from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import UserPermissionDialog from './UserPermissionDialog.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import TableColumnSearch from '../../../../components/common/TableColumnSearch.vue';
import AuditInfoFooter from '../../../../components/common/AuditInfoFooter.vue';
import UserNameCell from '../../../../components/common/UserNameCell.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import { useColumnFilter, resetColumnFilters } from '../../../../composables/useColumnFilter';
import { useTableRowHighlight } from '../../../../composables/useTableRowHighlight';

const USER_STATUS_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '正常', color: 'green' },
  0: { label: '停用', color: 'red' },
  2: { label: '锁定', color: 'orange' },
};

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');
const queryExpanded = ref(false);

/**
 * 行点击选中高亮（使用通用 composable）
 * ──────────────────────────────────
 * handleRowClick + getRowClass 由 useTableRowHighlight 提供，
 * 选中行自动附加 .bml-row-active 类名，底色跟随主题色。
 * 全局样式定义在 business-system.css 第 24 节。
 */
const { activeRowId, handleRowClick, getRowClass } = useTableRowHighlight();

/**
 * 用户列默认配置（与授权治理列管理模式一致）：
 * - 账号：默认固定在左侧（fixed: 'left'）
 * - 常用字段默认显示，扩展字段默认隐藏
 */
/** 列头搜索筛选条件 */
const columnFilters = reactive<Record<string, string>>({
  username: '', nickname: '', employeeNo: '', orgName: '', deptName: '', postName: '',
  roleNames: '', phone: '', status: '', createTime: '', entryDate: '', email: '',
  loginIp: '', loginDate: '', remark: '',
});

const defaultColumns: BusinessTableColumn[] = [
  /* ── 核心标识（默认显示） ── */
  { key: 'username',   title: '账号',     dataIndex: 'username',   width: 120, visible: true, fixed: 'left', sortable: true, titleSlotName: 'th-username', permission: 'system:user:field:username' },
  { key: 'nickname',   title: '用户名',   dataIndex: 'nickname',   width: 120, visible: true, sortable: true, titleSlotName: 'th-nickname', permission: 'system:user:field:nickname' },
  { key: 'employeeNo', title: '工号',     slotName: 'employeeNo',  width: 100, visible: true, sortable: true, titleSlotName: 'th-employeeNo', permission: 'system:user:field:employeeNo' },
  { key: 'orgName',    title: '所属机构', dataIndex: 'orgName',    width: 150, visible: true, sortable: true, titleSlotName: 'th-orgName', permission: 'system:user:field:orgId' },
  { key: 'deptName',   title: '部门',     dataIndex: 'deptName',   width: 120, visible: true, sortable: true, titleSlotName: 'th-deptName', permission: 'system:user:field:deptId' },
  { key: 'postName',   title: '岗位',     dataIndex: 'postName',   width: 110, visible: true, sortable: true, titleSlotName: 'th-postName', permission: 'system:user:field:postId' },
  { key: 'roleNames',  title: '角色',     slotName: 'roleNames',   width: 150, visible: true, sortable: true, titleSlotName: 'th-roleNames', permission: 'system:user:field:roleIds' },
  { key: 'phone',      title: '手机号',   dataIndex: 'phone',      width: 140, visible: true, sortable: true, titleSlotName: 'th-phone', permission: 'system:user:field:phone' },
  { key: 'status',     title: '状态',     slotName: 'status',      width: 90,  visible: true, align: 'center', sortable: true, titleSlotName: 'th-status' },
  { key: 'createTime', title: '创建时间', dataIndex: 'createTime', width: 180, visible: true, sortable: true, titleSlotName: 'th-createTime' },
  { key: 'createBy',  title: '创建人',   dataIndex: 'createBy', slotName: 'createBy',  width: 100, visible: false, sortable: true },
  { key: 'updateTime', title: '修改时间', dataIndex: 'updateTime', width: 180, visible: false, sortable: true },
  { key: 'updateBy',  title: '修改人',   dataIndex: 'updateBy', slotName: 'updateBy',  width: 100, visible: false, sortable: true },
  /* ── 扩展字段（默认隐藏） ── */
  { key: 'entryDate', title: '入职日期', dataIndex: 'entryDate', width: 120, visible: false, sortable: true, titleSlotName: 'th-entryDate', permission: 'system:user:field:entryDate' },
  { key: 'email',     title: '邮箱',     dataIndex: 'email',     width: 180, visible: false, sortable: true, titleSlotName: 'th-email', permission: 'system:user:field:email' },
  { key: 'loginIp',   title: '最后登录IP', dataIndex: 'loginIp', width: 140, visible: false, sortable: true, titleSlotName: 'th-loginIp' },
  { key: 'loginDate', title: '最后登录时间', dataIndex: 'loginDate', width: 170, visible: false, sortable: true, titleSlotName: 'th-loginDate' },
  { key: 'remark',    title: '备注',     dataIndex: 'remark',    width: 200, visible: false, ellipsis: true, sortable: true, titleSlotName: 'th-remark', permission: 'system:user:field:remark' },
  /* ── 操作列（锁定） ── */
  { key: 'actions', title: '操作', slotName: 'actions', width: 170, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const {
  visibleColumns, columnSettingItems, dragState, tableResetKey, scrollX, tableStyle, tableRef,
  handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed,
  handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns,
} = useBusinessTableColumns('system-user', defaultColumns);

const loading = ref(false);
const tableData = ref<UserVO[]>([]);

/** 列头搜索值格式化器：将非文本字段转换为可搜索的展示文本 */
const columnFilterFormatters: Record<string, (val: any) => string> = {
  status: (val) => USER_STATUS_MAP[val]?.label || '',
  roleNames: (val) => Array.isArray(val) ? val.join(',') : '',
};
/** 列头搜索过滤后的表格数据 */
const { filteredData } = useColumnFilter(tableData, columnFilters, columnFilterFormatters);

const roleOptions = ref<RoleVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const deptTreeData = ref<DeptVO[]>([]);
const postOptions = ref<PostVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增用户');
const formRef = ref();

/** 表单只读模式 */
const formReadonly = ref(false);
const { hasPermission } = useButtonPermission();
const canEditUser = computed(() => hasPermission('system:user:edit'));
const pagination = reactive({ current: 1, pageSize: 20, total: 0 });

/** 当前页正常状态用户数量（列头筛选后） */
const activeCount = computed(() => filteredData.value.filter(r => r.status === 1).length);
/** 当前页停用状态用户数量（列头筛选后） */
const disabledCount = computed(() => filteredData.value.filter(r => r.status === 0).length);
/** 当前页锁定状态用户数量（列头筛选后） */
const lockedCount = computed(() => filteredData.value.filter(r => r.status === 2).length);

const queryParams = reactive({
  username: '', phone: '', status: undefined as number | undefined,
  orgId: undefined as number | undefined, nickname: '',
  deptId: undefined as number | undefined, email: '',
  gender: undefined as number | undefined
});
const defaultForm = (): UserForm => ({
  id: undefined, username: '', nickname: '', password: '', phone: '', email: '',
  gender: 0, status: 1, orgId: undefined, deptId: undefined, postId: undefined,
  superiorId: undefined, employeeNo: '', entryDate: '', roleIds: [], remark: ''
});
const formData = reactive<UserForm>(defaultForm());
const formRules = {
  username: [
    { required: true, message: '请输入账号' },
    { maxLength: 30, message: '账号长度不能超过30个字符' },
  ],
  nickname: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }],
};
const resetPwdVisible = ref(false);
const newPassword = ref('');
const currentResetUserId = ref<number | undefined>(undefined);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchUserPage({ ...queryParams, pageNum: pagination.current, pageSize: pagination.pageSize }) as any;
    tableData.value = res.data?.records || [];
    pagination.total = res.data?.total || 0;
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};
const loadRoles = async () => { try { const r = await fetchRoleList() as any; roleOptions.value = r.data || []; } catch { roleOptions.value = []; } };
const loadOrgTree = async () => { try { const r = await fetchOrgList({}) as any; orgTreeData.value = r.data || []; } catch { orgTreeData.value = []; } };
const loadDeptTree = async () => { try { const r = await fetchDeptList({}) as any; deptTreeData.value = r.data || []; } catch { deptTreeData.value = []; } };
const loadPosts = async () => { try { const r = await fetchPostList({}) as any; postOptions.value = r.data || []; } catch { postOptions.value = []; } };

const handleSearch = () => { pagination.current = 1; loadData(); };
const handleReset = () => {
  queryParams.username = ''; queryParams.phone = ''; queryParams.status = undefined;
  queryParams.orgId = undefined; queryParams.nickname = '';
  queryParams.deptId = undefined; queryParams.email = ''; queryParams.gender = undefined;
  resetColumnFilters(columnFilters);
  pagination.current = 1; loadData();
};
const handlePageChange = (page: number) => { pagination.current = page; loadData(); };
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; loadData(); };

const handleAdd = () => { formReadonly.value = false; dialogTitle.value = '新增用户'; Object.assign(formData, defaultForm()); dialogVisible.value = true; };

const handleEdit = async (row: UserVO) => {
  formReadonly.value = false;
  dialogTitle.value = '编辑用户';
  Object.assign(formData, { id: row.id, username: row.username, nickname: row.nickname, password: '', phone: row.phone, email: row.email, gender: row.gender, status: row.status, orgId: row.orgId || undefined, deptId: row.deptId || undefined, postId: row.postId || undefined, employeeNo: row.employeeNo || '', entryDate: row.entryDate || '', roleIds: row.roleIds || [], remark: row.remark, createTime: row.createTime, createBy: row.createBy, updateTime: row.updateTime, updateBy: row.updateBy });
  try { const r = await fetchUserDetail(row.id) as any; formData.roleIds = r.data?.roleIds || []; } catch { /* keep */ }
  dialogVisible.value = true;
};

/** 双击行：有编辑权限则编辑，否则查看 */
const handleRowDblClick = async (record: UserVO) => {
  if (canEditUser.value) {
    await handleEdit(record);
  } else {
    formReadonly.value = true;
    dialogTitle.value = '查看用户';
    Object.assign(formData, { id: record.id, username: record.username, nickname: record.nickname, password: '', phone: record.phone, email: record.email, gender: record.gender, status: record.status, orgId: record.orgId || undefined, deptId: record.deptId || undefined, postId: record.postId || undefined, employeeNo: record.employeeNo || '', entryDate: record.entryDate || '', roleIds: record.roleIds || [], remark: record.remark, createTime: record.createTime, createBy: record.createBy, updateTime: record.updateTime, updateBy: record.updateBy });
    try { const r = await fetchUserDetail(record.id) as any; formData.roleIds = r.data?.roleIds || []; } catch { /* keep */ }
    dialogVisible.value = true;
  }
};

const submitting = ref(false);
const handleSubmit = async () => {
  try {
    const errors = await formRef.value?.validate();
    if (errors) return;
    submitting.value = true;
    if (formData.id) { await updateUser(formData); Message.success('修改成功'); }
    else { await createUser(formData); Message.success('新增成功'); }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => { try { await deleteUser(id); Message.success('删除成功'); loadData(); } catch { /* ignore */ } };
const confirmDelete = (id: number) => { Modal.confirm({ title: '确认删除', content: '确认删除该用户吗？', okButtonProps: { status: 'danger' }, onOk: () => handleDelete(id) }); };

const openResetPwd = (record: UserVO) => { currentResetUserId.value = record.id; newPassword.value = ''; resetPwdVisible.value = true; };
const handleResetPassword = async () => {
  const uid = currentResetUserId.value || formData.id;
  if (!uid || !newPassword.value) return;
  try { await resetUserPassword(uid, newPassword.value); Message.success('密码重置成功'); resetPwdVisible.value = false; newPassword.value = ''; } catch { /* ignore */ }
};

// ── 用户列表（用于上级选择） ──
const allUserOptions = ref<UserVO[]>([]);
const loadAllUsers = async () => {
  try {
    const r = await fetchUserPage({ pageNum: 1, pageSize: 9999 }) as any;
    allUserOptions.value = r.data?.records || [];
  } catch { allUserOptions.value = []; }
};

// ── 用户个人数据权限配置 ──
const USER_DS_OPTIONS = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '所在机构及下级' },
  { value: 3, label: '仅本机构' },
  { value: 4, label: '所在部门及下级' },
  { value: 5, label: '仅本部门' },
  { value: 6, label: '仅本人' },
  { value: 7, label: '自定义' },
  { value: 8, label: '本人及下属' },
];

const userDsDialogVisible = ref(false);
const userDsDialogTitle = ref('个人数据权限配置');
const userDsSubmitting = ref(false);
const currentDsUserId = ref<number | undefined>(undefined);
const defaultUserDsForm = (): UserDataScopeForm => ({
  id: undefined, userId: undefined, dataScope: 6, customOrgIds: '',
  customDeptIds: '', status: 1, expireTime: undefined, remark: ''
});
const userDsForm = reactive<UserDataScopeForm>(defaultUserDsForm());

/* ──────────────────── 用户个人功能授权 ──────────────────── */
/** 用户功能授权弹窗可见状态 */
const userPermDialogVisible = ref(false);
/** 当前授权的用户 ID */
const currentPermUserId = ref<number | undefined>(undefined);
/** 当前授权的用户名称（弹窗标题用） */
const currentPermUserName = ref('');

/** 从操作列下拉菜单打开用户功能授权弹窗 */
const openUserPermission = (record: UserVO) => {
  currentPermUserId.value = record.id;
  currentPermUserName.value = record.nickname || record.username;
  userPermDialogVisible.value = true;
};

/**
 * 从工具栏按钮打开用户功能授权弹窗（需先选中表格行）。
 * 通过 activeRowId 获取当前选中行，从 tableData 中查找对应用户信息。
 */
const openUserPermissionFromToolbar = () => {
  if (!activeRowId.value) {
    Message.warning('请先点击选中一条用户记录');
    return;
  }
  const record = filteredData.value.find(r => r.id === activeRowId.value);
  if (!record) {
    Message.warning('未找到选中的用户记录');
    return;
  }
  openUserPermission(record);
};

const openUserDataScope = async (record: UserVO) => {
  currentDsUserId.value = record.id;
  userDsDialogTitle.value = `个人数据权限 — ${record.nickname || record.username}`;
  Object.assign(userDsForm, defaultUserDsForm());
  userDsForm.userId = record.id;
  try {
    const r = await fetchUserDataScope(record.id) as any;
    if (r.data) {
      Object.assign(userDsForm, {
        id: r.data.id, userId: r.data.userId, dataScope: r.data.dataScope,
        customOrgIds: r.data.customOrgIds || '', customDeptIds: r.data.customDeptIds || '',
        status: r.data.status, expireTime: r.data.expireTime || undefined, remark: r.data.remark || ''
      });
    }
  } catch { /* no existing config, use defaults */ }
  userDsDialogVisible.value = true;
};

const handleSubmitUserDs = async () => {
  try {
    userDsSubmitting.value = true;
    await saveUserDataScope(userDsForm);
    Message.success('保存成功');
    userDsDialogVisible.value = false;
  } catch { /* keep dialog */ }
  finally { userDsSubmitting.value = false; }
};

const handleDeleteUserDs = async () => {
  if (!currentDsUserId.value) return;
  Modal.confirm({
    title: '确认删除',
    content: '确认删除该用户的个人数据权限配置吗？删除后将回退到角色数据权限。',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        await deleteUserDataScope(currentDsUserId.value!);
        Message.success('删除成功');
        userDsDialogVisible.value = false;
      } catch { /* ignore */ }
    }
  });
};

onMounted(() => { loadData(); loadRoles(); loadOrgTree(); loadDeptTree(); loadPosts(); loadAllUsers(); });
</script>

<style scoped>
.form-tabs :deep(.arco-tabs-content) { padding-top: 12px; }
</style>

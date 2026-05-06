<template>
  <!-- ======================================================================
       个人中心页面 —— BML 业务系统
       ======================================================================
       功能：
         1. 展示当前登录用户的详细信息（账号、用户名、机构、部门、岗位、角色等）
         2. 允许修改账号（最少 5 个字符）和用户名
         3. 允许修改登录密码（需验证旧密码）
       设计风格：
         与中台管理 Vision Pro 毛玻璃风格保持一致，卡片布局 + 渐变装饰。
       ====================================================================== -->
  <div class="profile-page">
    <!-- ── 顶部用户卡片 ── -->
    <div class="profile-hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-avatar">
          <a-avatar :size="72" class="avatar-gradient">
            <icon-user :size="32" />
          </a-avatar>
        </div>
        <div class="hero-info">
          <h2 class="hero-name">{{ profileData.nickname || profileData.username || '加载中...' }}</h2>
          <div class="hero-meta">
            <span v-if="profileData.orgName" class="meta-tag">
              <icon-apps :size="12" /> {{ profileData.orgName }}
            </span>
            <span v-if="profileData.deptName" class="meta-tag">
              <icon-branch :size="12" /> {{ profileData.deptName }}
            </span>
            <span v-if="profileData.postName" class="meta-tag">
              <icon-idcard :size="12" /> {{ profileData.postName }}
            </span>
            <span v-if="profileData.roleNames && profileData.roleNames.length" class="meta-tag">
              <icon-safe :size="12" /> {{ profileData.roleNames.join('、') }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 信息卡片网格 ── -->
    <div class="profile-grid">
      <!-- 基本信息卡片 -->
      <div class="profile-card">
        <div class="card-header">
          <div class="card-icon card-icon--blue"><icon-user /></div>
          <h3 class="card-title">基本信息</h3>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="info-label">账号</span>
            <span class="info-value">{{ profileData.username || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ profileData.nickname || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">手机号</span>
            <span class="info-value">{{ profileData.phone || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">邮箱</span>
            <span class="info-value">{{ profileData.email || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">性别</span>
            <span class="info-value">{{ GENDER_MAP[profileData.gender ?? -1] || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">工号</span>
            <span class="info-value">{{ profileData.employeeNo || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 组织信息卡片 -->
      <div class="profile-card">
        <div class="card-header">
          <div class="card-icon card-icon--green"><icon-apps /></div>
          <h3 class="card-title">组织信息</h3>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="info-label">所属机构</span>
            <span class="info-value">{{ profileData.orgName || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">所属部门</span>
            <span class="info-value">{{ profileData.deptName || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">岗位</span>
            <span class="info-value">{{ profileData.postName || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">角色</span>
            <span class="info-value">{{ profileData.roleNames?.join('、') || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">入职日期</span>
            <span class="info-value">{{ profileData.entryDate || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ profileData.createTime || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 修改信息卡片 -->
      <div class="profile-card">
        <div class="card-header">
          <div class="card-icon card-icon--purple"><icon-edit /></div>
          <h3 class="card-title">修改个人信息</h3>
        </div>
        <div class="card-body">
          <a-form :model="editForm" ref="editFormRef" :rules="editFormRules" layout="vertical" class="profile-form">
            <a-form-item field="username" label="账号">
              <a-input v-model="editForm.username" placeholder="请输入账号（至少5个字符）" />
            </a-form-item>
            <a-form-item field="nickname" label="用户名">
              <a-input v-model="editForm.nickname" placeholder="请输入用户名" />
            </a-form-item>
            <div class="form-actions">
              <a-button type="primary" :loading="editSubmitting" @click="handleUpdateProfile">
                <template #icon><icon-check /></template>
                保存修改
              </a-button>
            </div>
          </a-form>
        </div>
      </div>

      <!-- 修改密码卡片 -->
      <div class="profile-card">
        <div class="card-header">
          <div class="card-icon card-icon--orange"><icon-lock /></div>
          <h3 class="card-title">修改密码</h3>
        </div>
        <div class="card-body">
          <a-form :model="pwdForm" ref="pwdFormRef" :rules="pwdFormRules" layout="vertical" class="profile-form">
            <a-form-item field="oldPassword" label="当前密码">
              <a-input-password v-model="pwdForm.oldPassword" placeholder="请输入当前密码" />
            </a-form-item>
            <a-form-item field="newPassword" label="新密码">
              <a-input-password v-model="pwdForm.newPassword" placeholder="请输入新密码（6~30个字符）" />
            </a-form-item>
            <a-form-item field="confirmPassword" label="确认新密码">
              <a-input-password v-model="pwdForm.confirmPassword" placeholder="请再次输入新密码" />
            </a-form-item>
            <div class="form-actions">
              <a-button type="primary" status="warning" :loading="pwdSubmitting" @click="handleChangePassword">
                <template #icon><icon-lock /></template>
                修改密码
              </a-button>
            </div>
          </a-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
/**
 * 个人中心页面
 * <p>
 * 展示当前登录用户的详细信息，提供修改账号/用户名和修改密码的功能。
 * 路由路径：/profile
 * </p>
 */
defineOptions({ name: 'BusinessProfile' });

import { ref, reactive, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import {
  IconUser, IconApps, IconBranch, IconIdcard, IconSafe,
  IconEdit, IconLock, IconCheck
} from '@arco-design/web-vue/es/icon';
import {
  fetchProfile, updateProfile, changePassword,
  type UserVO, type UpdateProfileForm, type ChangePasswordForm
} from '../../../api/system';

/** 性别映射 */
const GENDER_MAP: Record<number, string> = { 0: '未知', 1: '男', 2: '女' };

/** 用户详细信息 */
const profileData = reactive<Partial<UserVO>>({});

/** 加载个人信息 */
const loadProfile = async () => {
  try {
    const res = await fetchProfile() as any;
    if (res.data) {
      Object.assign(profileData, res.data);
      /* 同步到编辑表单 */
      editForm.username = res.data.username || '';
      editForm.nickname = res.data.nickname || '';
    }
  } catch { /* ignore */ }
};

// ── 修改个人信息 ──
const editFormRef = ref();
const editSubmitting = ref(false);
const editForm = reactive<UpdateProfileForm>({ username: '', nickname: '' });
const editFormRules = {
  username: [
    { required: true, message: '请输入账号' },
    { minLength: 5, message: '账号长度不能少于5个字符' },
    { maxLength: 30, message: '账号长度不能超过30个字符' },
  ],
  nickname: [
    { required: true, message: '请输入用户名' },
    { minLength: 2, message: '用户名长度不能少于2个字符' },
    { maxLength: 30, message: '用户名长度不能超过30个字符' },
  ],
};

/** 提交修改个人信息 */
const handleUpdateProfile = async () => {
  try {
    const errors = await editFormRef.value?.validate();
    if (errors) return;
    editSubmitting.value = true;
    await updateProfile(editForm);
    Message.success('个人信息修改成功');
    await loadProfile();
  } catch { /* keep */ }
  finally { editSubmitting.value = false; }
};

// ── 修改密码 ──
const pwdFormRef = ref();
const pwdSubmitting = ref(false);
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });
const pwdFormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { minLength: 6, message: '新密码长度不能少于6个字符' },
    { maxLength: 30, message: '新密码长度不能超过30个字符' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    {
      validator: (value: string, cb: (msg?: string) => void) => {
        if (value !== pwdForm.newPassword) { cb('两次输入的密码不一致'); }
        else { cb(); }
      }
    },
  ],
};

/** 提交修改密码 */
const handleChangePassword = async () => {
  try {
    const errors = await pwdFormRef.value?.validate();
    if (errors) return;
    pwdSubmitting.value = true;
    const data: ChangePasswordForm = {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    };
    await changePassword(data);
    Message.success('密码修改成功，请使用新密码重新登录');
    /* 清空密码表单 */
    pwdForm.oldPassword = '';
    pwdForm.newPassword = '';
    pwdForm.confirmPassword = '';
  } catch { /* keep */ }
  finally { pwdSubmitting.value = false; }
};

onMounted(() => { loadProfile(); });
</script>

<style scoped>
/* ═══════════════════════════════════════════════════
   页面容器
   ═══════════════════════════════════════════════════ */
.profile-page {
  padding: 20px 24px 32px;
  max-width: 1200px;
  margin: 0 auto;
}

/* ═══════════════════════════════════════════════════
   顶部用户卡片 — 渐变背景 + 毛玻璃
   ═══════════════════════════════════════════════════ */
.profile-hero {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
  padding: 32px 36px;
  min-height: 140px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #165dff 0%, #722ed1 50%, #4f46e5 100%);
  z-index: 0;
}

.hero-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(255, 255, 255, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 30%, rgba(255, 255, 255, 0.1) 0%, transparent 40%);
}

.hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-gradient {
  background: rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.4);
  color: #fff !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.hero-name {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 0.5px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

/* ═══════════════════════════════════════════════════
   信息卡片网格
   ═══════════════════════════════════════════════════ */
.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

@media (max-width: 900px) {
  .profile-grid { grid-template-columns: 1fr; }
}

.profile-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px) saturate(180%);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.04),
    0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  transition: all 0.3s ease;
}

.profile-card:hover {
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.06),
    0 2px 6px rgba(0, 0, 0, 0.03);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 22px 0;
}

.card-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  flex-shrink: 0;
}

.card-icon--blue { background: linear-gradient(135deg, #165dff, #3c7eff); box-shadow: 0 4px 12px rgba(22, 93, 255, 0.25); }
.card-icon--green { background: linear-gradient(135deg, #059669, #10b981); box-shadow: 0 4px 12px rgba(5, 150, 105, 0.25); }
.card-icon--purple { background: linear-gradient(135deg, #722ed1, #9254de); box-shadow: 0 4px 12px rgba(114, 46, 209, 0.25); }
.card-icon--orange { background: linear-gradient(135deg, #d97706, #f59e0b); box-shadow: 0 4px 12px rgba(217, 119, 6, 0.25); }

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
  margin: 0;
}

.card-body {
  padding: 16px 22px 20px;
}

/* ═══════════════════════════════════════════════════
   信息行
   ═══════════════════════════════════════════════════ */
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: #86909c;
  font-weight: 500;
  flex-shrink: 0;
  min-width: 70px;
}

.info-value {
  font-size: 13px;
  color: #1d2129;
  font-weight: 500;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ═══════════════════════════════════════════════════
   表单区域
   ═══════════════════════════════════════════════════ */
.profile-form :deep(.arco-form-item) {
  margin-bottom: 16px;
}

.profile-form :deep(.arco-form-item-label-col) {
  margin-bottom: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

/* ═══════════════════════════════════════════════════
   暗色主题适配
   ═══════════════════════════════════════════════════ */
body[arco-theme='dark'] .profile-card {
  background: rgba(40, 40, 42, 0.85);
  border-color: rgba(255, 255, 255, 0.06);
}

body[arco-theme='dark'] .card-title { color: #f2f3f5; }
body[arco-theme='dark'] .info-label { color: #86909c; }
body[arco-theme='dark'] .info-value { color: #e5e6eb; }
body[arco-theme='dark'] .info-row { border-bottom-color: rgba(255, 255, 255, 0.06); }
</style>

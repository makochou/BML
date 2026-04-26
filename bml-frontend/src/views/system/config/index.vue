<template>
  <!-- ======================================================================
       中台管理 — 系统配置页面（全屏铺满）
       ======================================================================
       设计特点：
         1. 铺满整个内容区域，现代卡片网格布局
         2. 顶部 Banner 状态栏（验证码开关 + 实时状态指示）
         3. 品牌文案编辑区（标题、副标题、描述，实时保存到数据库）
         4. 三列品牌图片管理卡片（登录背景、登录框背景、Favicon）
         5. 每张卡片显示推荐尺寸、格式提示
         6. 大面积图片预览 + 悬浮操作层（替换 / 删除）
       ====================================================================== -->
  <div class="sc-page">
    <a-spin :loading="loading" class="sc-page__spin">

      <!-- ════════════════════════════════════════════════
           顶部状态 Banner：验证码开关
           ════════════════════════════════════════════════ -->
      <div class="sc-banner">
        <div class="sc-banner__left">
          <div class="sc-banner__icon-wrap">
            <icon-safe :size="22" />
          </div>
          <div class="sc-banner__info">
            <h3 class="sc-banner__title">图形验证码</h3>
            <p class="sc-banner__desc">
              开启后，业务系统登录页将展示图形验证码，有效防止暴力破解
            </p>
          </div>
        </div>
        <div class="sc-banner__right">
          <a-switch
            v-model="captchaEnabled"
            :before-change="saveCaptchaSwitch"
            type="round"
            checked-color="#00b42a"
          >
            <template #checked>已开启</template>
            <template #unchecked>已关闭</template>
          </a-switch>
        </div>
      </div>

      <!-- ════════════════════════════════════════════════
           空闲超时时长配置
           ════════════════════════════════════════════════ -->
      <div class="sc-banner sc-banner--idle">
        <div class="sc-banner__left">
          <div class="sc-banner__icon-wrap sc-banner__icon-wrap--blue">
            <icon-clock-circle :size="22" />
          </div>
          <div class="sc-banner__info">
            <h3 class="sc-banner__title">空闲自动登出</h3>
            <p class="sc-banner__desc">
              业务系统用户在指定时间内无任何操作（鼠标移动、键盘输入、点击等）将自动退出登录，设为 0 表示不限制
            </p>
          </div>
        </div>
        <div class="sc-banner__right sc-banner__right--idle">
          <div class="idle-time-picker">
            <a-input-number
              v-model="idleHours"
              :min="0"
              :max="23"
              :step="1"
              :style="{ width: '90px' }"
              placeholder="0"
            >
              <template #suffix>时</template>
            </a-input-number>
            <span class="idle-time-sep">:</span>
            <a-input-number
              v-model="idleMinutes"
              :min="0"
              :max="59"
              :step="5"
              :style="{ width: '90px' }"
              placeholder="0"
            >
              <template #suffix>分</template>
            </a-input-number>
          </div>
          <a-button type="primary" size="small" :loading="idleTimeoutSaving" @click="saveIdleTimeout">
            保存
          </a-button>
        </div>
      </div>

      <!-- ════════════════════════════════════════════════
           登录页品牌文案编辑
           ════════════════════════════════════════════════ -->
      <div class="sc-brand-editor">
        <div class="sc-brand-editor__header">
          <div class="sc-brand-editor__icon-wrap">
            <icon-edit :size="22" />
          </div>
          <div class="sc-brand-editor__info">
            <h3 class="sc-banner__title">登录页品牌文案</h3>
            <p class="sc-banner__desc">
              编辑业务系统登录页左侧品牌展示区的标题、副标题和描述文字
            </p>
          </div>
        </div>
        <div class="sc-brand-editor__body">
          <div class="sc-brand-editor__field">
            <label class="sc-brand-editor__label">品牌标题</label>
            <a-input v-model="brandTitle" placeholder="BML" allow-clear :max-length="20" show-word-limit />
          </div>
          <div class="sc-brand-editor__field">
            <label class="sc-brand-editor__label">品牌副标题</label>
            <a-input v-model="brandSlogan" placeholder="智慧企业管理平台" allow-clear :max-length="60" show-word-limit />
          </div>
          <div class="sc-brand-editor__field">
            <label class="sc-brand-editor__label">品牌描述</label>
            <a-input v-model="brandDesc" placeholder="统一身份认证 · 权限精细管控 · 流程高效协同 · 数据驱动决策" allow-clear :max-length="80" show-word-limit />
          </div>
          <div class="sc-brand-editor__actions">
            <a-button type="primary" :loading="brandSaving" @click="saveBrandText">
              <icon-check :size="14" /> 保存文案
            </a-button>
          </div>
        </div>
      </div>

      <!-- ════════════════════════════════════════════════
           品牌图片管理网格
           ════════════════════════════════════════════════ -->
      <div class="sc-grid">
        <!-- ── 登录页背景图 ── -->
        <div class="sc-card">
          <div class="sc-card__header">
            <div class="sc-card__icon sc-card__icon--blue">
              <icon-image :size="18" />
            </div>
            <div>
              <h4 class="sc-card__title">登录页背景图</h4>
              <p class="sc-card__hint">
                显示在登录页左侧 60% 区域，推荐尺寸 <strong>1920 × 1080</strong>，
                支持 JPG / PNG / WebP 格式
              </p>
            </div>
          </div>
          <!-- 未设置：整个虚线区域可点击上传 -->
          <a-upload
            v-if="!loginBgUrl"
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            @change="onLoginBgChange"
          >
            <template #upload-button>
              <div class="sc-card__preview sc-card__preview--empty sc-card__preview--clickable">
                <div class="sc-card__empty">
                  <icon-image :size="40" class="sc-card__empty-icon" />
                  <span>暂未设置</span>
                  <span class="sc-card__empty-hint">将使用默认渐变背景</span>
                  <span class="sc-card__upload-cta"><icon-upload :size="12" /> 点击上传图片</span>
                </div>
              </div>
            </template>
          </a-upload>
          <!-- 已设置：显示预览 + 悬浮操作层 -->
          <div v-else class="sc-card__preview">
            <img :src="loginBgUrl" alt="登录页背景" class="sc-card__img" />
            <div class="sc-card__overlay">
              <a-upload
                :auto-upload="false"
                :show-file-list="false"
                accept="image/*"
                @change="onLoginBgChange"
              >
                <template #upload-button>
                  <a-button type="outline" size="small" class="sc-card__action-btn">
                    <icon-swap :size="14" /> 替换
                  </a-button>
                </template>
              </a-upload>
              <a-popconfirm content="确定删除此图片？将恢复默认背景" @ok="handleDeleteBranding('loginBg')">
                <a-button type="outline" status="danger" size="small" class="sc-card__action-btn">
                  <icon-delete :size="14" /> 删除
                </a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>

        <!-- ── 侧边栏 Logo 图片 ── -->
        <div class="sc-card">
          <div class="sc-card__header">
            <div class="sc-card__icon sc-card__icon--purple">
              <icon-layers :size="18" />
            </div>
            <div>
              <h4 class="sc-card__title">侧边栏 Logo 图片</h4>
              <p class="sc-card__hint">
                显示在业务系统左侧导航栏顶部，推荐尺寸 <strong>200 × 60</strong>，
                支持 PNG / SVG / WebP 透明背景格式效果更佳
              </p>
            </div>
          </div>
          <a-upload
            v-if="!sidebarLogoUrl"
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            @change="onSidebarLogoChange"
          >
            <template #upload-button>
              <div class="sc-card__preview sc-card__preview--empty sc-card__preview--clickable">
                <div class="sc-card__empty">
                  <icon-layers :size="40" class="sc-card__empty-icon" />
                  <span>暂未设置</span>
                  <span class="sc-card__empty-hint">将显示默认渐变文字 Logo</span>
                  <span class="sc-card__upload-cta"><icon-upload :size="12" /> 点击上传图片</span>
                </div>
              </div>
            </template>
          </a-upload>
          <div v-else class="sc-card__preview">
            <img :src="sidebarLogoUrl" alt="侧边栏 Logo" class="sc-card__img" />
            <div class="sc-card__overlay">
              <a-upload
                :auto-upload="false"
                :show-file-list="false"
                accept="image/*"
                @change="onSidebarLogoChange"
              >
                <template #upload-button>
                  <a-button type="outline" size="small" class="sc-card__action-btn">
                    <icon-swap :size="14" /> 替换
                  </a-button>
                </template>
              </a-upload>
              <a-popconfirm content="确定删除此图片？将恢复默认文字 Logo" @ok="handleDeleteBranding('sidebarLogo')">
                <a-button type="outline" status="danger" size="small" class="sc-card__action-btn">
                  <icon-delete :size="14" /> 删除
                </a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>

        <!-- ── Favicon ── -->
        <div class="sc-card">
          <div class="sc-card__header">
            <div class="sc-card__icon sc-card__icon--orange">
              <icon-apps :size="18" />
            </div>
            <div>
              <h4 class="sc-card__title">浏览器标签图标</h4>
              <p class="sc-card__hint">
                浏览器标签页 Favicon，推荐尺寸 <strong>64 × 64</strong> 或 <strong>128 × 128</strong>，
                支持 SVG / ICO / PNG 格式
              </p>
            </div>
          </div>
          <a-upload
            v-if="!faviconUrl"
            :auto-upload="false"
            :show-file-list="false"
            accept=".svg,.ico,.png,.jpg,.jpeg,.gif,.webp"
            @change="onFaviconChange"
          >
            <template #upload-button>
              <div class="sc-card__preview sc-card__preview--favicon sc-card__preview--empty sc-card__preview--clickable">
                <div class="sc-card__empty">
                  <icon-apps :size="40" class="sc-card__empty-icon" />
                  <span>暂未设置</span>
                  <span class="sc-card__empty-hint">将使用系统默认图标</span>
                  <span class="sc-card__upload-cta"><icon-upload :size="12" /> 点击上传图标</span>
                </div>
              </div>
            </template>
          </a-upload>
          <div v-else class="sc-card__preview sc-card__preview--favicon">
            <img :src="faviconUrl" alt="Favicon" class="sc-card__img sc-card__img--favicon" />
            <div class="sc-card__overlay">
              <a-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".svg,.ico,.png,.jpg,.jpeg,.gif,.webp"
                @change="onFaviconChange"
              >
                <template #upload-button>
                  <a-button type="outline" size="small" class="sc-card__action-btn">
                    <icon-swap :size="14" /> 替换
                  </a-button>
                </template>
              </a-upload>
              <a-popconfirm content="确定删除此图标？将恢复默认" @ok="handleDeleteBranding('favicon')">
                <a-button type="outline" status="danger" size="small" class="sc-card__action-btn">
                  <icon-delete :size="14" /> 删除
                </a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>
      </div>

    </a-spin>
  </div>
</template>

<script lang="ts" setup>
/**
 * 中台管理 — 系统配置页面
 * <p>
 * 铺满内容区域的现代化系统配置管理界面。
 * 功能包括：
 *   1. 验证码开关实时切换（Banner 状态栏）
 *   2. 品牌图片管理（登录背景、登录框背景、Favicon）
 *   3. 大面积图片预览 + 悬浮操作层（替换 / 删除）
 *   4. 每个图片区域显示推荐尺寸和格式提示
 * 所有配置项实时保存到后端 sys_config 表。
 * </p>
 *
 * 重要说明：
 *   defineOptions({ name: 'SystemConfig' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'SystemConfig' });

import { ref, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import {
  IconSafe, IconImage, IconLayers, IconApps,
  IconUpload, IconDelete, IconSwap, IconEdit, IconCheck, IconClockCircle
} from '@arco-design/web-vue/es/icon';
import {
  fetchLoginConfig,
  batchUpdateConfig,
  uploadBrandingImage,
  deleteBrandingImage
} from '../../../api/config';

/** 页面加载状态 */
const loading = ref(false);

// ═══════════════════════════════════════════════════════
// 配置状态（与 sys_config 表字段一一对应）
// ═══════════════════════════════════════════════════════
const captchaEnabled = ref(false);
/** 空闲超时 — 小时部分（UI 显示用） */
const idleHours = ref(0);
/** 空闲超时 — 分钟部分（UI 显示用） */
const idleMinutes = ref(30);
const idleTimeoutSaving = ref(false);
const loginBgUrl = ref('');
const sidebarLogoUrl = ref('');
const faviconUrl = ref('');

/** 品牌文案编辑状态 */
const brandTitle = ref('');
const brandSlogan = ref('');
const brandDesc = ref('');
const brandSaving = ref(false);

/**
 * 加载登录页相关配置
 * 从后端 GET /system/config/login 接口获取 sys.login.* 前缀的全部配置项
 */
const loadConfig = async () => {
  loading.value = true;
  try {
    const res = await fetchLoginConfig() as any;
    const config = res.data || {};
    captchaEnabled.value = config['sys.login.captchaEnabled'] === 'true';
    // 将后端存储的总分钟数拆分为 时 + 分 显示
    const totalMinutes = parseInt(config['sys.login.idleTimeout'] || '30', 10) || 30;
    idleHours.value = Math.floor(totalMinutes / 60);
    idleMinutes.value = totalMinutes % 60;
    loginBgUrl.value = config['sys.login.bgImage'] || '';
    sidebarLogoUrl.value = config['sys.sidebar.logo'] || '';
    faviconUrl.value = config['sys.login.favicon'] || '';
    brandTitle.value = config['sys.login.brandTitle'] || '';
    brandSlogan.value = config['sys.login.brandSlogan'] || '';
    brandDesc.value = config['sys.login.brandDesc'] || '';
  } catch {
    Message.error('加载配置失败');
  } finally {
    loading.value = false;
  }
};

/**
 * 切换验证码开关
 * 使用 a-switch 的 beforeChange 回调，返回 Promise<boolean>
 * 成功返回 true 允许切换，失败返回 false 回滚状态
 */
const saveCaptchaSwitch = (): Promise<boolean> => {
  const newValue = !captchaEnabled.value;
  return batchUpdateConfig({
    'sys.login.captchaEnabled': String(newValue)
  }).then(() => {
    Message.success(newValue ? '验证码已开启' : '验证码已关闭');
    return true;
  }).catch(() => {
    Message.error('保存失败');
    return false;
  });
};

/**
 * 通用品牌图片上传处理。
 * <p>
 * 从 Arco Upload 的 change 回调中提取原始 File 对象，
 * 调用后端接口上传并将返回的 URL 写入对应的响应式变量。
 * </p>
 *
 * @param type     图片类型标识（loginBg / sidebarLogo / favicon）
 * @param fileList Arco Upload change 回调的第一个参数（文件列表，此处不使用）
 * @param fileItem Arco Upload change 回调的第二个参数（当前操作的文件项）
 */
const handleUploadBranding = async (type: string, _fileList: any, fileItem: any) => {
  const rawFile = fileItem?.file;
  if (!rawFile) return;

  try {
    const res = await uploadBrandingImage(type, rawFile) as any;
    const url = res.data?.url || '';
    Message.success('上传成功');

    // 更新本地状态
    if (type === 'loginBg') loginBgUrl.value = url;
    else if (type === 'sidebarLogo') sidebarLogoUrl.value = url;
    else if (type === 'favicon') faviconUrl.value = url;
  } catch {
    Message.error('上传失败');
  }
};

/**
 * 三个上传区域的 change 事件处理器。
 * 将 Arco Upload 的 (fileList, fileItem) 参数转发给通用上传方法。
 * 注意：不能在 Vue 模板中使用 TypeScript 类型注解（如 `_: any`），
 * 否则 Vue 模板编译器可能无法正确解析，导致事件绑定失败。
 */
const onLoginBgChange = (fileList: any, fileItem: any) => handleUploadBranding('loginBg', fileList, fileItem);
const onSidebarLogoChange = (fileList: any, fileItem: any) => handleUploadBranding('sidebarLogo', fileList, fileItem);
const onFaviconChange = (fileList: any, fileItem: any) => handleUploadBranding('favicon', fileList, fileItem);

/**
 * 删除品牌图片（恢复系统默认）
 * @param type 图片类型标识
 */
const handleDeleteBranding = async (type: string) => {
  try {
    await deleteBrandingImage(type);
    Message.success('已恢复默认');

    if (type === 'loginBg') loginBgUrl.value = '';
    else if (type === 'sidebarLogo') sidebarLogoUrl.value = '';
    else if (type === 'favicon') faviconUrl.value = '';
  } catch {
    Message.error('删除失败');
  }
};

/**
 * 保存品牌文案
 * 将品牌标题、副标题、描述批量写入 sys_config 表
 */
const saveBrandText = async () => {
  brandSaving.value = true;
  try {
    await batchUpdateConfig({
      'sys.login.brandTitle': brandTitle.value,
      'sys.login.brandSlogan': brandSlogan.value,
      'sys.login.brandDesc': brandDesc.value
    });
    Message.success('品牌文案已保存');
  } catch {
    Message.error('保存失败');
  } finally {
    brandSaving.value = false;
  }
};

/**
 * 保存空闲超时时长
 * 将 时+分 合并为总分钟数写入 sys_config 表，前台业务系统下次加载时生效
 */
const saveIdleTimeout = async () => {
  const totalMinutes = (idleHours.value || 0) * 60 + (idleMinutes.value || 0);
  idleTimeoutSaving.value = true;
  try {
    await batchUpdateConfig({
      'sys.login.idleTimeout': String(totalMinutes)
    });
    // 构造可读的提示文案
    if (totalMinutes <= 0) {
      Message.success('已关闭空闲超时限制');
    } else {
      const h = Math.floor(totalMinutes / 60);
      const m = totalMinutes % 60;
      const parts: string[] = [];
      if (h > 0) parts.push(`${h} 小时`);
      if (m > 0) parts.push(`${m} 分钟`);
      Message.success(`空闲超时已设为 ${parts.join(' ')}`);
    }
  } catch {
    Message.error('保存失败');
  } finally {
    idleTimeoutSaving.value = false;
  }
};

onMounted(() => {
  loadConfig();
});
</script>

<style scoped>
/* ══════════════════════════════════════════════════════
   页面容器 — 铺满内容区域
   ══════════════════════════════════════════════════════ */
.sc-page {
  height: 100%;
  padding: 20px 24px 24px;
  overflow-y: auto;
  background: linear-gradient(180deg, #f0f2f8 0%, #f7f8fa 100%);
}
.sc-page__spin {
  display: block;
  width: 100%;
}

/* ══════════════════════════════════════════════════════
   顶部状态 Banner：验证码开关
   ══════════════════════════════════════════════════════ */
.sc-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
  transition: box-shadow 0.3s;
}
.sc-banner:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}
.sc-banner__left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.sc-banner__icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #00b42a 0%, #23c343 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sc-banner__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--bml-text-primary, #1d2129);
  margin: 0 0 2px;
}
.sc-banner__desc {
  font-size: 13px;
  color: var(--bml-text-tertiary, #86909c);
  margin: 0;
  line-height: 1.4;
}

/* ── 空闲超时 Banner 图标渐变色 ── */
.sc-banner__icon-wrap--blue {
  background: linear-gradient(135deg, #165dff 0%, #3c7eff 100%);
}
/* ── 空闲超时 Banner 右侧：时分选择 + 保存按钮横向排列，整体靠右对齐 ── */
.sc-banner__right--idle {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  /* 靠右对齐：配合父容器 justify-content: space-between 自动推到最右侧 */
  margin-left: auto;
}
/* ── 时分选择器容器 ── */
.idle-time-picker {
  display: flex;
  align-items: center;
  gap: 6px;
}

/*
 * 时分输入框内数字靠右对齐。
 * Arco Design InputNumber 的实际输入元素为 .arco-input-number-input，
 * 通过深度选择器穿透 scoped 样式覆盖其 text-align。
 */
.idle-time-picker :deep(.arco-input-number-input) {
  text-align: right;
}
/* ── 时分之间的冒号分隔符 ── */
.idle-time-sep {
  font-size: 18px;
  font-weight: 700;
  color: #86909c;
  line-height: 1;
  user-select: none;
}

/* ══════════════════════════════════════════════════════
   品牌图片管理网格 — 三列自适应
   ══════════════════════════════════════════════════════ */
.sc-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
@media (max-width: 1200px) {
  .sc-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .sc-grid {
    grid-template-columns: 1fr;
  }
  .sc-page {
    padding: 16px;
  }
}

/* ══════════════════════════════════════════════════════
   品牌图片卡片
   ══════════════════════════════════════════════════════ */
.sc-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.3s, transform 0.2s;
}
.sc-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

/* ── 卡片头部 ── */
.sc-card__header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 20px 20px 0;
}
.sc-card__icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}
.sc-card__icon--blue {
  background: linear-gradient(135deg, #165dff 0%, #3c7eff 100%);
}
.sc-card__icon--purple {
  background: linear-gradient(135deg, #722ed1 0%, #a855f7 100%);
}
.sc-card__icon--orange {
  background: linear-gradient(135deg, #ff7d00 0%, #f7ba1e 100%);
}
.sc-card__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--bml-text-primary, #1d2129);
  margin: 0 0 4px;
  line-height: 1.4;
}
.sc-card__hint {
  font-size: 12px;
  color: var(--bml-text-tertiary, #86909c);
  margin: 0;
  line-height: 1.5;
}
.sc-card__hint strong {
  color: var(--bml-text-secondary, #4e5969);
  font-weight: 600;
}

/* ── 图片预览区 ── */
.sc-card__preview {
  position: relative;
  margin: 16px 20px 0;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: #f2f3f5;
  border: 1px solid #e5e6eb;
}
.sc-card__preview--favicon {
  aspect-ratio: 1 / 1;
  max-width: 180px;
  margin-left: auto;
  margin-right: auto;
}
.sc-card__preview--empty {
  border: 2px dashed #c9cdd4;
  background: #fafbfc;
}
.sc-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.sc-card__img--favicon {
  object-fit: contain;
  padding: 20px;
}

/* ── 空状态 ── */
.sc-card__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 6px;
  color: #c9cdd4;
  font-size: 13px;
}
.sc-card__empty-icon {
  color: #c9cdd4;
}
.sc-card__empty-hint {
  font-size: 11px;
  color: #c9cdd4;
}
/* 上传引导文字（点击上传图片/图标） */
.sc-card__upload-cta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  padding: 4px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #165dff;
  background: rgba(22, 93, 255, 0.06);
  border-radius: 20px;
  transition: background 0.2s, color 0.2s;
}
/* 可点击空状态：悬浮时高亮 + 手型光标 */
.sc-card__preview--clickable {
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.sc-card__preview--clickable:hover {
  border-color: #165dff;
  background: rgba(22, 93, 255, 0.02);
}
.sc-card__preview--clickable:hover .sc-card__upload-cta {
  background: rgba(22, 93, 255, 0.1);
  color: #0e42d2;
}

/* ── 悬浮操作层 ── */
.sc-card__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  opacity: 0;
  transition: opacity 0.25s ease;
}
.sc-card__preview:hover .sc-card__overlay {
  opacity: 1;
}
.sc-card__action-btn {
  color: #fff !important;
  border-color: rgba(255, 255, 255, 0.6) !important;
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(8px);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.sc-card__action-btn:hover {
  background: rgba(255, 255, 255, 0.25) !important;
  border-color: #fff !important;
}

/* .sc-card__footer 已移除 —— 空状态时整个预览区域即为上传触发器 */

/* ══════════════════════════════════════════════════════
   登录页品牌文案编辑区
   ══════════════════════════════════════════════════════ */
.sc-brand-editor {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
  transition: box-shadow 0.3s;
}
.sc-brand-editor:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}
.sc-brand-editor__header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 28px;
  border-bottom: 1px solid #f0f0f5;
}
.sc-brand-editor__icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sc-brand-editor__body {
  padding: 20px 28px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sc-brand-editor__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sc-brand-editor__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--bml-text-secondary, #4e5969);
}
.sc-brand-editor__actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}
</style>

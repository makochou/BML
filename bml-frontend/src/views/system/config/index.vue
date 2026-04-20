<template>
  <!-- ======================================================================
       中台管理 — 系统配置页面（全屏铺满）
       ======================================================================
       设计特点：
         1. 铺满整个内容区域，现代卡片网格布局
         2. 顶部 Banner 状态栏（验证码开关 + 实时状态指示）
         3. 三列品牌图片管理卡片（登录背景、登录框背景、Favicon）
         4. 每张卡片显示推荐尺寸、格式提示
         5. 大面积图片预览 + 悬浮操作层（替换 / 删除）
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
          <div class="sc-card__preview" :class="{ 'sc-card__preview--empty': !loginBgUrl }">
            <img v-if="loginBgUrl" :src="loginBgUrl" alt="登录页背景" class="sc-card__img" />
            <div v-else class="sc-card__empty">
              <icon-image :size="40" class="sc-card__empty-icon" />
              <span>暂未设置</span>
              <span class="sc-card__empty-hint">将使用默认渐变背景</span>
            </div>
            <!-- 悬浮操作层 -->
            <div v-if="loginBgUrl" class="sc-card__overlay">
              <a-upload
                :auto-upload="false"
                :show-file-list="false"
                accept="image/*"
                @change="(_: any, file: any) => handleUploadBranding('loginBg', file)"
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
          <!-- 未设置时的上传入口 -->
          <div v-if="!loginBgUrl" class="sc-card__footer">
            <a-upload
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              @change="(_: any, file: any) => handleUploadBranding('loginBg', file)"
            >
              <template #upload-button>
                <a-button type="primary" size="small" long>
                  <icon-upload :size="14" style="margin-right: 6px;" /> 上传背景图
                </a-button>
              </template>
            </a-upload>
          </div>
        </div>

        <!-- ── 登录框背景图 ── -->
        <div class="sc-card">
          <div class="sc-card__header">
            <div class="sc-card__icon sc-card__icon--purple">
              <icon-layers :size="18" />
            </div>
            <div>
              <h4 class="sc-card__title">登录框背景图</h4>
              <p class="sc-card__hint">
                显示在登录页右侧登录表单顶部，推荐尺寸 <strong>800 × 500</strong>，
                支持 JPG / PNG / WebP 格式
              </p>
            </div>
          </div>
          <div class="sc-card__preview" :class="{ 'sc-card__preview--empty': !cardBgUrl }">
            <img v-if="cardBgUrl" :src="cardBgUrl" alt="登录框背景" class="sc-card__img" />
            <div v-else class="sc-card__empty">
              <icon-layers :size="40" class="sc-card__empty-icon" />
              <span>暂未设置</span>
              <span class="sc-card__empty-hint">将使用默认毛玻璃效果</span>
            </div>
            <div v-if="cardBgUrl" class="sc-card__overlay">
              <a-upload
                :auto-upload="false"
                :show-file-list="false"
                accept="image/*"
                @change="(_: any, file: any) => handleUploadBranding('loginCardBg', file)"
              >
                <template #upload-button>
                  <a-button type="outline" size="small" class="sc-card__action-btn">
                    <icon-swap :size="14" /> 替换
                  </a-button>
                </template>
              </a-upload>
              <a-popconfirm content="确定删除此图片？将恢复默认效果" @ok="handleDeleteBranding('loginCardBg')">
                <a-button type="outline" status="danger" size="small" class="sc-card__action-btn">
                  <icon-delete :size="14" /> 删除
                </a-button>
              </a-popconfirm>
            </div>
          </div>
          <div v-if="!cardBgUrl" class="sc-card__footer">
            <a-upload
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              @change="(_: any, file: any) => handleUploadBranding('loginCardBg', file)"
            >
              <template #upload-button>
                <a-button type="primary" size="small" long>
                  <icon-upload :size="14" style="margin-right: 6px;" /> 上传背景图
                </a-button>
              </template>
            </a-upload>
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
          <div class="sc-card__preview sc-card__preview--favicon" :class="{ 'sc-card__preview--empty': !faviconUrl }">
            <img v-if="faviconUrl" :src="faviconUrl" alt="Favicon" class="sc-card__img sc-card__img--favicon" />
            <div v-else class="sc-card__empty">
              <icon-apps :size="40" class="sc-card__empty-icon" />
              <span>暂未设置</span>
              <span class="sc-card__empty-hint">将使用系统默认图标</span>
            </div>
            <div v-if="faviconUrl" class="sc-card__overlay">
              <a-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".svg,.ico,.png,.jpg,.jpeg,.gif,.webp"
                @change="(_: any, file: any) => handleUploadBranding('favicon', file)"
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
          <div v-if="!faviconUrl" class="sc-card__footer">
            <a-upload
              :auto-upload="false"
              :show-file-list="false"
              accept=".svg,.ico,.png,.jpg,.jpeg,.gif,.webp"
              @change="(_: any, file: any) => handleUploadBranding('favicon', file)"
            >
              <template #upload-button>
                <a-button type="primary" size="small" long>
                  <icon-upload :size="14" style="margin-right: 6px;" /> 上传图标
                </a-button>
              </template>
            </a-upload>
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
 */
import { ref, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import {
  IconSafe, IconImage, IconLayers, IconApps,
  IconUpload, IconDelete, IconSwap
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
const loginBgUrl = ref('');
const cardBgUrl = ref('');
const faviconUrl = ref('');

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
    loginBgUrl.value = config['sys.login.bgImage'] || '';
    cardBgUrl.value = config['sys.login.cardBgImage'] || '';
    faviconUrl.value = config['sys.login.favicon'] || '';
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
 * 上传品牌图片
 * @param type  图片类型标识（loginBg / loginCardBg / favicon）
 * @param file  Arco Upload 组件回传的文件对象
 */
const handleUploadBranding = async (type: string, file: any) => {
  const rawFile = file?.file;
  if (!rawFile) return;

  try {
    const res = await uploadBrandingImage(type, rawFile) as any;
    const url = res.data?.url || '';
    Message.success('上传成功');

    // 更新本地状态
    if (type === 'loginBg') loginBgUrl.value = url;
    else if (type === 'loginCardBg') cardBgUrl.value = url;
    else if (type === 'favicon') faviconUrl.value = url;
  } catch {
    Message.error('上传失败');
  }
};

/**
 * 删除品牌图片（恢复系统默认）
 * @param type 图片类型标识
 */
const handleDeleteBranding = async (type: string) => {
  try {
    await deleteBrandingImage(type);
    Message.success('已恢复默认');

    if (type === 'loginBg') loginBgUrl.value = '';
    else if (type === 'loginCardBg') cardBgUrl.value = '';
    else if (type === 'favicon') faviconUrl.value = '';
  } catch {
    Message.error('删除失败');
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

/* ── 卡片底部操作栏（未设置图片时显示） ── */
.sc-card__footer {
  padding: 16px 20px 20px;
}
</style>

<template>
  <div class="workplace-container">
    <!-- 欢迎头部 (Sunrise Gradient Glass) -->
    <div class="welcome-header">
      <div class="welcome-bg-animate"></div>
      <div class="welcome-glass-overlay"></div>
      
      <div class="welcome-content">
        <div class="avatar-ring">
           <img src="https://p1-arco.byteimg.com/tos-cn-i-uwbnlip3yd/3ee5f13fb09879ecb5185e440cef6eb9.png~tplv-uwbnlip3yd-webp.webp" class="user-avatar" />
           <div class="online-indicator"></div>
        </div>
        <div class="welcome-text">
           <h2 class="welcome-title">早安，管理员</h2>
           <p class="welcome-desc">
              <span>今天也是元气满满的一天！</span>
              <span class="weather-pill">
                  <icon-sun class="weather-icon" /> 26℃ 晴朗
              </span>
           </p>
        </div>
      </div>
      
      <div class="extra-content">
         <div class="stat-item">
            <div class="stat-icon-wrapper blue-glow"><icon-apps /></div>
            <div class="stat-info">
                <div class="stat-title">项目</div>
                <div class="stat-value">12</div>
            </div>
         </div>
         <div class="glass-divider"></div>
         <div class="stat-item">
            <div class="stat-icon-wrapper purple-glow"><icon-check-circle /></div>
            <div class="stat-info">
                <div class="stat-title">待办</div>
                <div class="stat-value">3 / 15</div>
            </div>
         </div>
         <div class="glass-divider"></div>
         <div class="stat-item">
            <div class="stat-icon-wrapper orange-glow"><icon-email /></div>
            <div class="stat-info">
                <div class="stat-title">消息</div>
                <div class="stat-value">2</div>
            </div>
         </div>
      </div>
    </div>

    <!-- 核心指标区 (SaaS 风格) -->
    <div class="stats-overview">
       <div class="overview-card blue-theme">
          <div class="card-icon"><icon-command /></div>
          <div class="card-info">
             <div class="card-label">总接口数</div>
             <div class="card-num">1,024</div>
             <div class="card-trend">周同比 12% <icon-caret-up /></div>
          </div>
          <div class="card-bg-shape"></div>
       </div>
       <div class="overview-card purple-theme">
          <div class="card-icon"><icon-bar-chart /></div>
          <div class="card-info">
             <div class="card-label">今日调用</div>
             <div class="card-num">89,203</div>
             <div class="card-trend">日同比 5% <icon-caret-up /></div>
          </div>
          <div class="card-bg-shape"></div>
       </div>
       <div class="overview-card green-theme">
          <div class="card-icon"><icon-check-circle /></div>
          <div class="card-info">
             <div class="card-label">成功率</div>
             <div class="card-num">99.9%</div>
             <div class="card-trend">运行平稳</div>
          </div>
          <div class="card-bg-shape"></div>
       </div>
       <div class="overview-card orange-theme">
          <div class="card-icon"><icon-thunderbolt /></div>
          <div class="card-info">
             <div class="card-label">平均耗时</div>
             <div class="card-num">45ms</div>
             <div class="card-trend down">优化中 <icon-arrow-down /></div>
          </div>
          <div class="card-bg-shape"></div>
       </div>
    </div>
    
    <div class="content-layout">
      <!-- 左侧主栏 -->
      <div class="left-panel">
         <!-- 流量分析图表 -->
         <div class="panel-box chart-box">
             <div class="panel-header">
                <span class="panel-title">流量访问趋势</span>
                <div class="header-actions">
                   <span class="action-item active">流量</span>
                   <span class="action-item">耗时</span>
                </div>
             </div>
             <div class="chart-content">
                <svg viewBox="0 0 800 280" class="main-chart">
                    <defs>
                        <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stop-color="#165dff" stop-opacity="0.2"/>
                            <stop offset="100%" stop-color="#165dff" stop-opacity="0"/>
                        </linearGradient>
                    </defs>
                    <!-- 网格 -->
                    <line x1="0" y1="50" x2="800" y2="50" stroke="#f2f3f5" />
                    <line x1="0" y1="120" x2="800" y2="120" stroke="#f2f3f5" />
                    <line x1="0" y1="190" x2="800" y2="190" stroke="#f2f3f5" />
                    <line x1="0" y1="260" x2="800" y2="260" stroke="#f2f3f5" />
                    
                    <!-- 曲线 -->
                    <path d="M0,200 Q100,180 200,220 T400,150 T600,100 T800,50" 
                          fill="none" stroke="#165dff" stroke-width="3" stroke-linecap="round" />
                    <!-- 填充 -->
                    <path d="M0,200 Q100,180 200,220 T400,150 T600,100 T800,50 L800,280 L0,280 Z" 
                          fill="url(#areaGradient)" />
                          
                    <!-- 悬停点模拟 -->
                    <circle cx="600" cy="100" r="5" fill="#fff" stroke="#165dff" stroke-width="2" />
                    <rect x="550" y="50" width="100" height="40" rx="4" fill="#000" opacity="0.8" />
                    <text x="600" y="75" text-anchor="middle" fill="#fff" font-size="12">PV: 12,340</text>
                </svg>
             </div>
         </div>

         <!-- 进行中的项目 -->
         <div class="panel-box">
             <div class="panel-header">
                <span class="panel-title">进行中的项目</span>
                <span class="panel-link">全部项目</span>
             </div>
             <div class="project-grid-modern">
                <div class="project-card-modern">
                    <div class="pc-head">
                        <icon-code class="pc-icon blue" />
                        <span class="pc-name">统一用户中心</span>
                    </div>
                    <p class="pc-desc">提供全平台统一身份认证能力。</p>
                    <div class="pc-foot">
                        <span class="pc-team">平台研发部</span>
                        <span class="pc-time">2小时前</span>
                    </div>
                </div>
                <div class="project-card-modern">
                    <div class="pc-head">
                        <icon-cloud class="pc-icon green" />
                        <span class="pc-name">支付结算网关</span>
                    </div>
                    <p class="pc-desc">对接微信支付宝等多渠道支付。</p>
                    <div class="pc-foot">
                        <span class="pc-team">金融业务部</span>
                        <span class="pc-time">5小时前</span>
                    </div>
                </div>
                <div class="project-card-modern">
                    <div class="pc-head">
                        <icon-apps class="pc-icon orange" />
                        <span class="pc-name">低代码平台</span>
                    </div>
                    <p class="pc-desc">快速搭建企业级后台管理应用。</p>
                    <div class="pc-foot">
                        <span class="pc-team">创新实验室</span>
                        <span class="pc-time">昨天</span>
                    </div>
                </div>
                 <div class="project-card-modern">
                    <div class="pc-head">
                        <icon-bug class="pc-icon red" />
                        <span class="pc-name">自动化测试</span>
                    </div>
                    <p class="pc-desc">全链路接口自动化回归测试。</p>
                    <div class="pc-foot">
                        <span class="pc-team">质量保障部</span>
                        <span class="pc-time">3天前</span>
                    </div>
                </div>
             </div>
         </div>
      </div>

      <!-- 右侧副栏 -->
      <div class="right-panel">
         <!-- 快捷操作 -->
         <div class="panel-box quick-action">
             <div class="panel-header"><span class="panel-title">快捷操作</span></div>
             <div class="action-grid">
                <div class="action-item"><icon-plus-circle class="act-icon" /><span>新建应用</span></div>
                <div class="action-item"><icon-import class="act-icon" /><span>导入接口</span></div>
                <div class="action-item"><icon-user-add class="act-icon" /><span>添加成员</span></div>
                <div class="action-item"><icon-settings class="act-icon" /><span>系统设置</span></div>
             </div>
         </div>
         
         <!-- 指数看板 -->
         <div class="panel-box">
             <div class="panel-header"><span class="panel-title">健康指数</span></div>
             <div class="health-chart-box">
                 <svg viewBox="0 0 100 100" class="mini-ring">
                    <circle cx="50" cy="50" r="40" fill="none" stroke="#f2f3f5" stroke-width="8" />
                    <circle cx="50" cy="50" r="40" fill="none" stroke="#165dff" stroke-width="8" stroke-dasharray="251" stroke-dashoffset="20" stroke-linecap="round" transform="rotate(-90 50 50)" />
                 </svg>
                 <div class="ring-data">
                    <span class="ring-val">92</span>
                    <span class="ring-label">分</span>
                 </div>
             </div>
             <div class="health-detail">
                <div class="hd-row"><span>服务可用性</span><span>100%</span></div>
                <div class="hd-row"><span>接口异常率</span><span class="warn">0.1%</span></div>
             </div>
         </div>

         <!-- 动态 -->
         <div class="panel-box">
             <div class="panel-header"><span class="panel-title">动态</span></div>
             <div class="activity-list">
                 <div class="activity-item">
                    <div class="act-avatar"><img src="https://p1-arco.byteimg.com/tos-cn-i-uwbnlip3yd/3ee5f13fb09879ecb5185e440cef6eb9.png~tplv-uwbnlip3yd-webp.webp" /></div>
                    <div class="act-info">
                       <p><b>林东东</b> 发布了 <b>BML v2.3.0</b></p>
                       <span>10分钟前</span>
                    </div>
                 </div>
                 <div class="activity-item">
                    <div class="act-avatar blue-av">API</div>
                    <div class="act-info">
                       <p><b>接口同步</b> 检测到变更</p>
                       <span>30分钟前</span>
                    </div>
                 </div>
                 <div class="activity-item">
                    <div class="act-avatar">U</div>
                    <div class="act-info">
                       <p><b>李四</b> 申请了应用权限</p>
                       <span>1小时前</span>
                    </div>
                 </div>
             </div>
         </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { IconSun, IconCommand, IconBarChart, IconCheckCircle, IconThunderbolt, IconCaretUp, IconArrowDown, IconCode, IconCloud, IconApps, IconBug, IconPlusCircle, IconImport, IconUserAdd, IconSettings, IconEmail } from '@arco-design/web-vue/es/icon';

defineOptions({
  name: 'Dashboard'
});
</script>

<style scoped>
/* 紧凑型全屏布局 */
.workplace-container {
  height: 100%;
  overflow: hidden; /* 禁止整体滚动 */
  display: flex;
  flex-direction: column;
  background: #f2f3f5;
  color: #1d2129;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", sans-serif;
  padding-bottom: 12px;
}

/* =================================================================
   Sunrise Gradient Welcome Header
   ================================================================= */
.welcome-header {
  position: relative;
  height: 120px; /* Taller for impact */
  border-radius: 12px;
  margin: 12px 12px 12px 12px;
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

/* Background Animation */
.welcome-bg-animate {
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(
        -45deg, 
        #ee7752, 
        #e73c7e, 
        #23a6d5, 
        #23d5ab
    );
    background-size: 400% 400%;
    animation: gradientBG 15s ease infinite;
    z-index: 0;
    opacity: 0.1; /* Very subtle colorful tint */
}
.welcome-header::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: #fff; /* Solid white base */
    z-index: -1;
}

/* Frosted Glass Overlay */
.welcome-glass-overlay {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    backdrop-filter: blur(10px);
    z-index: 1;
    background: linear-gradient(90deg, rgba(255,255,255,0.9) 0%, rgba(255,255,255,0.6) 100%);
}

.welcome-content { 
    position: relative; 
    z-index: 2; 
    display: flex; 
    align-items: center; 
    gap: 20px; 
}

/* Avatar Ring */
.avatar-ring {
    position: relative;
    padding: 3px;
    background: linear-gradient(135deg, #165dff, #722ed1);
    border-radius: 50%;
    box-shadow: 0 4px 12px rgba(22, 93, 255, 0.3);
}
.user-avatar { 
    width: 64px; 
    height: 64px; 
    border-radius: 50%; 
    border: 3px solid #fff;
    display: block;
}
.online-indicator {
    position: absolute;
    bottom: 2px;
    right: 2px;
    width: 14px;
    height: 14px;
    background: #00b42a;
    border: 2px solid #fff;
    border-radius: 50%;
    box-shadow: 0 0 0 2px rgba(0, 180, 42, 0.2);
    animation: pulse 2s infinite;
}

@keyframes pulse {
    0% { box-shadow: 0 0 0 0 rgba(0, 180, 42, 0.4); }
    70% { box-shadow: 0 0 0 6px rgba(0, 180, 42, 0); }
    100% { box-shadow: 0 0 0 0 rgba(0, 180, 42, 0); }
}

@keyframes gradientBG {
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
}

/* Text Styling */
.welcome-title { 
    margin: 0 0 8px 0; 
    font-size: 24px; 
    font-weight: 600; 
    color: #1d2129; 
    letter-spacing: 0.5px;
    background: linear-gradient(135deg, #1d2129 0%, #4e5969 100%);
    -webkit-background-clip: text;
    background-clip: text;
}
.welcome-desc { 
    margin: 0; 
    color: #4e5969; 
    font-size: 14px; 
    display: flex; 
    align-items: center; 
    gap: 12px; 
}
.weather-pill {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 2px 8px;
    background: rgba(255, 125, 0, 0.1);
    color: #ff7d00;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
}

/* Right Stats */
.extra-content { 
    position: relative; 
    z-index: 2; 
    display: flex; 
    align-items: center; 
    gap: 24px; 
}
.stat-item {
    display: flex;
    align-items: center;
    gap: 12px;
}
.stat-icon-wrapper {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    transition: all 0.3s;
}
.blue-glow { background: rgba(22, 93, 255, 0.1); color: #165dff; }
.purple-glow { background: rgba(114, 46, 209, 0.1); color: #722ed1; }
.orange-glow { background: rgba(255, 125, 0, 0.1); color: #ff7d00; }

.stat-icon-wrapper:hover {
    transform: scale(1.1);
}

.stat-info { display: flex; flex-direction: column; }
.stat-title { font-size: 12px; color: #86909c; }
.stat-value { font-size: 20px; font-weight: 600; color: #1d2129; font-family: 'DIN Alternate'; line-height: 1.2; }

.glass-divider {
    width: 1px;
    height: 32px;
    background: rgba(0,0,0,0.06);
}

/* 核心指标卡 (Modern SaaS) - 压缩高度 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 0 12px 12px 12px;
  flex-shrink: 0;
}
.overview-card {
  background: #fff; border-radius: 4px; padding: 16px;
  display: flex; align-items: center; gap: 12px;
  position: relative; overflow: hidden;
  border: 1px solid transparent;
  height: 80px;
}
.overview-card:hover { transform: translateY(-2px); box-shadow: 0 4px 10px rgba(0,0,0,0.05); }

.card-icon {
  width: 48px; height: 48px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.card-info { flex: 1; z-index: 1; }
.card-label { font-size: 12px; color: #4e5969; margin-bottom: 2px; }
.card-num { font-size: 24px; font-weight: 600; color: #1d2129; font-family: 'DIN Alternate'; margin-bottom: 0; line-height: 1; }
.card-trend { font-size: 12px; display: flex; align-items: center; gap: 4px; color: #86909c; margin-top: 2px; }




/* 内容布局 - 弹性填充剩余高度 */
.content-layout {
  display: grid;
  grid-template-columns: 2.5fr 1fr;
  gap: 12px;
  padding: 0 12px;
  flex: 1; /* 填充剩余空间 */
  overflow: hidden; /* 内部滚动 */
  min-height: 0; /* 关键：允许 grid item 缩小 */
}
.left-panel, .right-panel {
    display: flex;
    flex-direction: column;
    gap: 12px;
    height: 100%;
    overflow: hidden;
}

/* 面板通用 */
.panel-box {
  background: #fff; border-radius: 4px;
  display: flex; flex-direction: column;
}
.panel-header {
  padding: 12px 16px; border-bottom: 1px solid #f2f3f5;
  display: flex; justify-content: space-between; align-items: center;
  flex-shrink: 0;
}
.panel-title { font-size: 15px; font-weight: 500; color: #1d2129; }
.panel-link { font-size: 12px; color: #165dff; cursor: pointer; }

/* 流量图表 - 占据更多比例 */
.chart-box { flex: 1.6; min-height: 0; }
.chart-content { padding: 12px; flex: 1; min-height: 0; display: flex; align-items: center; } /* 弹性高度 */
.main-chart { width: 100%; height: 100%; }
.header-actions { display: flex; background: #f2f3f5; padding: 2px; border-radius: 2px; }
.action-item {
    font-size: 12px; color: #4e5969; padding: 2px 8px; cursor: pointer; border-radius: 2px;
}
.action-item.active { background: #fff; color: #165dff; font-weight: 500; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }

/* 进行中的项目 - 占据较小比例 */
.left-panel .panel-box:nth-child(2) { flex: 1; min-height: 0; overflow: hidden; }

.project-grid-modern {
    display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; padding: 12px;
    overflow-y: auto; /* 内容过多可滚动 */
    height: 100%;
}
.project-card-modern {
    border: 1px solid #f2f3f5; border-radius: 4px; padding: 12px;
    transition: all 0.2s; cursor: pointer;
    display: flex; flex-direction: column; justify-content: space-between;
}
.project-card-modern:hover { border-color: #165dff; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.pc-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.pc-icon { font-size: 18px; }
.pc-name { font-size: 13px; font-weight: 500; color: #1d2129; }
.pc-desc { font-size: 12px; color: #86909c; line-height: 1.4; height: 34px; overflow: hidden; margin-bottom: 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.pc-foot { display: flex; justify-content: space-between; font-size: 12px; color: #86909c; }

/* 右侧组件布局 */
.right-panel {
    display: grid;
    grid-template-rows: auto 1fr 1.2fr; /* 快捷操作自适应，健康指数1份，动态1.2份 */
}

/* 快捷操作 - 紧凑 */
.quick-action .action-grid {
    display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; padding: 12px;
}
.quick-action .action-item {
    display: flex; flex-direction: row; align-items: center; justify-content: center; gap: 8px;
    background: #f7f8fa; padding: 10px; border-radius: 4px; cursor: pointer; color: #4e5969;
    transition: all 0.2s;
}
.quick-action .action-item:hover { background: #e8f3ff; color: #165dff; }
.quick-action .act-icon { font-size: 16px; margin-bottom: 0; }
.quick-action .action-item span { font-size: 12px; }

/* 健康指数 */
.health-chart-box { padding: 8px 0; text-align: center; position: relative; flex: 1; display: flex; align-items: center; justify-content: center; }
.mini-ring { width: 90px; height: 90px; }
.ring-data {
    position: absolute; top: 0; left: 0; width: 100%; height: 100%;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.ring-val { font-size: 24px; font-weight: 500; color: #1d2129; font-family: 'DIN Alternate'; }
.ring-label { font-size: 12px; color: #86909c; }
.health-detail { padding: 0 16px 16px 16px; }
.hd-row {
    display: flex; justify-content: space-between; font-size: 12px; color: #4e5969; margin-bottom: 4px;
}

/* 动态 */
.right-panel .panel-box:last-child { overflow: hidden; }
.activity-list { padding: 0 16px 16px 16px; overflow-y: auto; height: 100%; }
.activity-item {
    display: flex; gap: 10px; border-bottom: 1px solid #f2f3f5; padding: 10px 0;
}
.activity-item:last-child { border-bottom: none; }
.act-avatar {
    width: 28px; height: 28px; border-radius: 50%; background: #f2f3f5; overflow: hidden;
    display: flex; align-items: center; justify-content: center; font-size: 12px; color: #86909c; flex-shrink: 0;
}
.act-avatar img { width: 100%; height: 100%; }
.blue-av { background: #e8f3ff; color: #165dff; font-weight: 600; }
.act-info { font-size: 12px; color: #1d2129; flex: 1; }
.act-info p { margin: 0 0 2px 0; line-height: 1.4; }
.act-info span { font-size: 12px; color: #86909c; }

@media (max-width: 1200px) {
  .stats-overview { grid-template-columns: 1fr 1fr; }
  .content-layout { grid-template-columns: 1fr; }
}

/* Dark Mode Overrides */
:global(body[arco-theme='dark']) .workplace-container {
    background: #17171a;
    color: #f2f3f5;
}

:global(body[arco-theme='dark']) .welcome-header {
    background: #232324;
    border-bottom: 1px solid #2a2a2b;
}
:global(body[arco-theme='dark']) .welcome-header::before {
    background: #232324;
}
:global(body[arco-theme='dark']) .welcome-glass-overlay {
    background: linear-gradient(90deg, rgba(35,35,36,0.9) 0%, rgba(35,35,36,0.6) 100%);
}
:global(body[arco-theme='dark']) .welcome-title {
    background: linear-gradient(135deg, #f2f3f5 0%, #c9cdd4 100%);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
}
:global(body[arco-theme='dark']) .welcome-desc { color: #86909c; }
:global(body[arco-theme='dark']) .stat-item .stat-value { color: #f2f3f5; }
:global(body[arco-theme='dark']) .stat-title { color: #86909c; }
:global(body[arco-theme='dark']) .divider, 
:global(body[arco-theme='dark']) .glass-divider { background: rgba(255,255,255,0.08); }

/* Dark Mode - Stats Overview */
:global(body[arco-theme='dark']) .overview-card {
    background: #232324;
    border: 1px solid #2a2a2b;
}
:global(body[arco-theme='dark']) .card-num { color: #f2f3f5; }
:global(body[arco-theme='dark']) .card-label { color: #86909c; }

:global(body[arco-theme='dark']) .blue-theme .card-icon { background: rgba(22, 93, 255, 0.15); }
:global(body[arco-theme='dark']) .purple-theme .card-icon { background: rgba(114, 46, 209, 0.15); }
:global(body[arco-theme='dark']) .green-theme .card-icon { background: rgba(0, 180, 42, 0.15); }
:global(body[arco-theme='dark']) .orange-theme .card-icon { background: rgba(255, 125, 0, 0.15); }

/* Dark Mode - Panels */
:global(body[arco-theme='dark']) .panel-box {
    background: #232324;
    border: 1px solid #2a2a2b;
}
:global(body[arco-theme='dark']) .panel-header {
    border-bottom: 1px solid #2a2a2b;
}
:global(body[arco-theme='dark']) .panel-title { color: #f2f3f5; }
:global(body[arco-theme='dark']) .line-x, 
:global(body[arco-theme='dark']) .line-y { stroke: #3a3a3b; }
:global(body[arco-theme='dark']) svg line { stroke: #2a2a2b; }

:global(body[arco-theme='dark']) .header-actions { background: #2a2a2b; }
:global(body[arco-theme='dark']) .action-item { color: #86909c; }
:global(body[arco-theme='dark']) .action-item.active { background: #3a3a3b; color: #165dff; }

:global(body[arco-theme='dark']) .project-card-modern {
    border-color: #2a2a2b;
}
:global(body[arco-theme='dark']) .pc-name { color: #f2f3f5; }
:global(body[arco-theme='dark']) .quick-action .action-item {
    background: #2a2a2b;
    color: #86909c;
}
:global(body[arco-theme='dark']) .quick-action .action-item:hover {
    background: rgba(22, 93, 255, 0.15);
}
:global(body[arco-theme='dark']) .ring-val { color: #f2f3f5; }
:global(body[arco-theme='dark']) .mini-ring circle[stroke="#f2f3f5"] { stroke: #2a2a2b; }

:global(body[arco-theme='dark']) .activity-item { border-bottom: 1px solid #2a2a2b; }
:global(body[arco-theme='dark']) .act-info { color: #c9cdd4; }
:global(body[arco-theme='dark']) .act-avatar { background: #2a2a2b; }


</style>

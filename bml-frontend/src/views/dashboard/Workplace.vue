<template>
  <div class="monitor-dashboard">
    <!-- Star-field / Tech Background -->
    <div class="tech-bg"></div>

    <!-- 顶部状态栏 -->
    <header class="monitor-header">
       <div class="header-left">
          <div class="pulse-ring"></div>
          <h2>服务器物理探针中枢</h2>
       </div>
       <div class="header-right">
          <div class="info-pill" v-if="serverInfo?.net">
             <icon-swap style="margin-right: 4px; color: #165dff"/>
             <span style="color: #86909c">↓</span> <span class="text-green hw-metric">{{ serverInfo.net.rxSpeed }}</span>
             <span style="color: #86909c; margin-left: 8px">↑</span> <span class="hardware-text hw-metric">{{ serverInfo.net.txSpeed }}</span>
             <span style="color: #86909c; margin-left: 8px" title="Total(Rx/Tx)">总计(收/发):</span> <span class="hw-metric" style="color: #1d2129">{{ serverInfo.net.totalRxBytes }} / {{ serverInfo.net.totalTxBytes }}</span>
             <span style="color: #86909c; margin-left: 8px" title="Real-time Active Visitors">访客:</span> <span class="text-green hw-metric">{{ serverInfo.net.tcpConnections != null ? serverInfo.net.tcpConnections : '-' }}</span>
          </div>
          <div class="info-pill" v-else><icon-loading /> 网络嗅探中...</div>
          <div class="info-pill"><icon-desktop/> 探测节点 (内/外): 
             <span class="hardware-text" style="margin-left: 4px">{{ serverInfo?.sys?.computerIp || '解析中...' }}</span> 
             <span style="color: #e5e6eb; margin: 0 6px;">|</span> 
             <span class="text-green" style="font-weight:bold">{{ serverInfo?.sys?.computerWanIp || '测算中...' }}</span>
          </div>
       </div>
    </header>

    <!-- 核心网格布局区 -->
    <div class="monitor-grid">
       
       <!-- 左侧栏：系统与虚拟机详情 -->
       <div class="grid-left">
           <!-- 宿主机信息卡片 -->
           <div class="tech-card glass-panel left-card" style="flex: 1.1">
               <div class="card-accent" style="background: linear-gradient(135deg, #165dff, #00b8d4);"></div>
               <div class="card-title"><icon-computer/> 宿主机运行环境</div>
               <div class="info-list" v-if="serverInfo?.sys">
                   <div class="info-row"><span class="label">服务器名称</span> <span class="value hardware-text">{{ serverInfo.sys.computerName }}</span></div>
                   <div class="info-row"><span class="label">操作系统</span> <span class="value">{{ serverInfo.sys.osName }}</span></div>
                   <div class="info-row"><span class="label">系统架构</span> <span class="value">{{ serverInfo.sys.osArch }}</span></div>
                   <div class="info-row"><span class="label">系统启动时间</span> <span class="value">{{ serverInfo.sys.bootTime || '-' }}</span></div>
                   <div class="info-row"><span class="label">不间断运行</span> <span class="value text-green">{{ serverInfo.sys.upTime || '-' }}</span></div>
                   <div class="info-row"><span class="label">项目根路径</span> <span class="value code-text">{{ serverInfo.sys.userDir }}</span></div>
               </div>
               <div class="loading-state" v-else><icon-loading /> 载入中...</div>
           </div>

           <!-- JVM信息卡片 -->
           <div class="tech-card glass-panel left-card">
               <div class="card-accent" style="background: linear-gradient(135deg, #00b42a, #00d68f);"></div>
               <div class="card-title"><icon-layers/> Runtime 运行时环境</div>
               <div class="info-list" v-if="serverInfo?.jvm">
                   <div class="info-row"><span class="label">Java 版本</span> <span class="value">{{ serverInfo.jvm.version }}</span></div>
                   <div class="info-row"><span class="label">JVM 名称</span> <span class="value">{{ serverInfo.jvm.name }}</span></div>
                   <div class="info-row"><span class="label">启动时间</span> <span class="value">{{ serverInfo.jvm.startTime }}</span></div>
                   <div class="info-row"><span class="label">连续运行</span> <span class="value text-green">{{ serverInfo.jvm.runTime }}</span></div>
                   <div class="info-row"><span class="label">守护进程路径</span> <span class="value code-text">{{ serverInfo.jvm.home }}</span></div>
               </div>
               <div class="loading-state" v-else><icon-loading /> 载入中...</div>
           </div>

       </div>

       <!-- 中间栏：核心算力量表 (图表大区) -->
       <div class="grid-center">
            <div class="tech-card center-panel">
                <div class="center-head">
                    <div style="display: flex; flex-direction: column; gap: 8px; width: 100%;">
                        <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                           <h3>核心物理算力洞察 (Computing Power)</h3>
                           <div class="load-badge" v-if="serverInfo?.cpu && serverInfo.cpu.load1 != null">
                              Load: <span>{{ serverInfo.cpu.load1 }}</span> | <span>{{ serverInfo.cpu.load5 }}</span> | <span>{{ serverInfo.cpu.load15 }}</span>
                           </div>
                        </div>
                        <div>
                            <span class="refresh-tag">3s Refresh</span>
                        </div>
                    </div>
                </div>
                
                <div class="gauge-area">
                    <div class="gauge-wrapper">
                        <v-chart class="gauge-chart" :option="cpuOption" autoresize />
                        <div class="gauge-title">CPU 利用率</div>
                    </div>
                    <div class="gauge-wrapper">
                        <v-chart class="gauge-chart" :option="memOption" autoresize />
                        <div class="gauge-title">物理内存占用</div>
                    </div>
                    <div class="gauge-wrapper">
                        <v-chart class="gauge-chart" :option="jvmOption" autoresize />
                        <div class="gauge-title">JVM 内存占用</div>
                    </div>
                </div>

                <!-- 无缝衔接的内存清理操作栏 -->
                <div class="gc-action-bar">
                    <div class="gc-desc">
                        <div class="gc-icon-bg"><icon-thunderbolt /></div>
                        <div>
                           <span class="gc-title">智能内存回收 (GC)</span>
                           <span class="gc-subtitle">平滑释放过期对象，无损业务运行。</span>
                        </div>
                    </div>
                    <a-button class="tech-gc-btn" type="primary" size="small" shape="round" :loading="gcLoading" @click="handleGcClean">
                        <template #icon><icon-delete /></template>
                        一键清理
                    </a-button>
                </div>

                <div class="cpu-detail-bars" v-if="serverInfo?.cpu">
                    <div class="model-name">芯片组: {{ serverInfo.cpu.cpuModel }} [{{ serverInfo.cpu.cpuNum }} Logical Cores]</div>
                    
                    <div class="bar-item">
                        <div class="bar-labels"><span>系统消耗 (Sys)</span> <span>{{ serverInfo.cpu.sys }}%</span></div>
                        <div class="bar-track"><div class="bar-fill bg-blue" :style="{width: serverInfo.cpu.sys + '%'}"></div></div>
                    </div>
                    <div class="bar-item">
                        <div class="bar-labels"><span>用户进程 (User)</span> <span>{{ serverInfo.cpu.used }}%</span></div>
                        <div class="bar-track"><div class="bar-fill bg-purple" :style="{width: serverInfo.cpu.used + '%'}"></div></div>
                    </div>
                    <div class="bar-item">
                        <div class="bar-labels"><span>IO 阻塞 (Wait)</span> <span>{{ serverInfo.cpu.wait }}%</span></div>
                        <div class="bar-track"><div class="bar-fill bg-orange" :style="{width: serverInfo.cpu.wait + '%'}"></div></div>
                    </div>
                </div>
            </div>
       </div>

       <!-- 右侧栏：集群磁盘阵列 -->
       <div class="grid-right">
           <div class="tech-card glass-panel full-height">
               <div class="card-title"><icon-storage/> 挂载存储池阵列</div>
               
               <div class="disk-list" v-if="serverInfo?.disks">
                   <div class="disk-item" v-for="(disk, idx) in serverInfo.disks" :key="idx">
                       <div class="disk-head">
                           <span class="disk-path">{{ disk.dirName }}</span>
                           <span class="disk-type">{{ disk.sysTypeName }}</span>
                       </div>
                       
                       <div class="disk-progress">
                           <div class="dp-track">
                               <div class="dp-fill" 
                                    :class="disk.usage > 85 ? 'bg-red' : (disk.usage > 60 ? 'bg-orange' : 'bg-cyan')" 
                                    :style="{width: disk.usage + '%'}">
                               </div>
                           </div>
                           <span class="dp-percent" :class="disk.usage > 85 ? 'text-red' : ''">{{ disk.usage }}%</span>
                       </div>
                       
                       <div class="disk-foot">
                           <span>已用: {{ disk.used }} / {{ disk.total }}</span>
                           <span class="disk-io" v-if="disk.readSpeed && disk.readSpeed !== '-'"><icon-swap/> R: {{ disk.readSpeed }} | W: {{ disk.writeSpeed }}</span>
                       </div>
                   </div>
               </div>
               <div class="loading-state" v-else><icon-loading /> 载入中...</div>
           </div>
       </div>

    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconDesktop, IconComputer, IconLayers, IconStorage, IconLoading, IconSwap, IconDelete, IconThunderbolt } from '@arco-design/web-vue/es/icon';
import request from '../../utils/request';
import { use } from 'echarts/core';
import { GaugeChart } from 'echarts/charts';
import type { GaugeSeriesOption } from 'echarts/charts';
import { ToolboxComponent, TooltipComponent } from 'echarts/components';
import type { TooltipComponentOption } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import VChart from 'vue-echarts';

use([GaugeChart, ToolboxComponent, TooltipComponent, CanvasRenderer]);

type ECOption = echarts.ComposeOption<GaugeSeriesOption | TooltipComponentOption>;

defineOptions({ name: 'Dashboard' });

// 服务器状态模型响应数据
const serverInfo = ref<any>(null);

// 提取共用的深黑科技质感图表工厂方法
const createTechGauge = (name: string, color: string): ECOption => ({
  series: [{
    type: 'gauge',
    startAngle: 180,
    endAngle: 0,
    min: 0,
    max: 100,
    splitNumber: 10,
    radius: '110%',
    center: ['50%', '68%'],
    axisLine: {
      lineStyle: {
        width: 12,
        color: [[1, 'rgba(255,255,255,0.05)']]
      }
    },
    progress: {
      show: true,
      width: 12,
      itemStyle: {
        color: color,
        shadowBlur: 8,
        shadowColor: color
      }
    },
    pointer: { show: false },
    axisTick: { show: false },
    splitLine: { show: false },
    axisLabel: { show: false },
    detail: {
      valueAnimation: true,
      offsetCenter: [0, '-5%'],
      fontSize: 22,
      fontWeight: 'bold',
      fontFamily: 'Courier New',
      formatter: '{value}%',
      color: color
    },
    data: [{ value: 0, name: name }],
    title: {
      show: false
    }
  }]
});

const cpuOption = ref<ECOption>(createTechGauge('CPU', '#00d8ff'));
const memOption = ref<ECOption>(createTechGauge('RAM', '#ff2e93'));
const jvmOption = ref<ECOption>(createTechGauge('JVM', '#00ff88'));

let timer: number | undefined;

const fetchMonitorData = async () => {
    try {
        const res: any = await request.get('/system/monitor/server');
        if (res.code === 200) {
            serverInfo.value = res.data;
            // console.log('最新的服务器硬件与网速指标 ->', serverInfo.value);
            
            // 同步 Echarts 表盘动态值
            if (serverInfo.value.cpu) {
               const cpuUsage = serverInfo.value.cpu.free != null 
                 ? parseFloat((100 - serverInfo.value.cpu.free).toFixed(2)) 
                 : 0;
               (cpuOption.value.series as any)[0].data[0].value = cpuUsage;
            }
            if (serverInfo.value.mem) {
               (memOption.value.series as any)[0].data[0].value = serverInfo.value.mem.usage || 0;
            }
            if (serverInfo.value.jvm) {
               (jvmOption.value.series as any)[0].data[0].value = serverInfo.value.jvm.usage || 0;
            }
        }
    } catch (e) {
        console.error('获取监控探针数据异常', e);
    }
};

const gcLoading = ref(false);
const handleGcClean = async () => {
    try {
        gcLoading.value = true;
        const res: any = await request.post('/system/monitor/gc');
        if (res.code === 200) {
            Message.success(res.msg || '清理指令发送成功，由于释放内存需要时间，请留意后续变动。');
            setTimeout(() => {
                fetchMonitorData();
            }, 500); // 略微延迟0.5s强制刷新一次抓取最新数据 
        } else {
            Message.error(res.msg || '清理指令发送失败');
        }
    } catch (error) {
        Message.error('触发垃圾回收失败');
    } finally {
        setTimeout(() => { gcLoading.value = false; }, 1000); // 防止连续快速点击
    }
};

onMounted(() => {
    fetchMonitorData();
    timer = window.setInterval(fetchMonitorData, 3000);
});

onUnmounted(() => {
    if (timer) clearInterval(timer);
});
</script>

<style scoped>
/* ==================== 现代质感重构：亮色容器与网格 ==================== */
.monitor-dashboard {
  position: relative;
  height: calc(100vh - 100px);
  background: #f2f3f5;
  color: #1d2129;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  overflow: hidden;
  padding: 14px 20px;
  border-radius: 8px;
}

/* 动态网格背景特效 (浅色版) */
.tech-bg {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background-image: 
    linear-gradient(rgba(0, 0, 0, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 0, 0, 0.02) 1px, transparent 1px);
  background-size: 30px 30px;
  z-index: 0;
  pointer-events: none;
}

/* ==================== 头部横幅 ==================== */
.monitor-header {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 22px;
  background: linear-gradient(90deg, #165dff, #722ed1);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-weight: 600;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

/* 雷达圆点 */
.pulse-ring {
  width: 12px;
  height: 12px;
  background: #00b42a;
  border-radius: 50%;
  box-shadow: 0 0 8px #00b42a, 0 0 16px #00b42a;
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(0, 180, 42, 0.5); }
  70% { box-shadow: 0 0 0 10px rgba(0, 180, 42, 0); }
  100% { box-shadow: 0 0 0 0 rgba(0, 180, 42, 0); }
}

.header-right {
  display: flex;
  gap: 8px; /* 减小间距 */
  justify-content: flex-end;
  overflow: hidden; /* 防止溢出破坏布局 */
}
.info-pill {
  background: #fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  border: 1px solid rgba(0,0,0,0.05);
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  display: flex;
  align-items: center;
  gap: 4px; /* 减小图标与文字间距 */
  color: #4e5969;
  white-space: nowrap;
  flex-shrink: 1; /* 允许在极端情况下收缩 */
  overflow: hidden;
  text-overflow: ellipsis;
}
.info-pill span {
  color: #165dff;
  font-family: 'Courier New', monospace;
  font-weight: 500;
}

/* ==================== 三列主网格 ==================== */
.monitor-grid {
  position: relative;
  z-index: 10;
  display: flex;
  gap: 16px;
  height: calc(100vh - 170px);
}

.grid-left, .grid-right {
  width: 28%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.left-card {
  flex: 1;
  position: relative;
  overflow: hidden;
}
.card-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 12px 0 0 12px;
}
.grid-center {
  width: 44%;
  display: flex;
  flex-direction: column;
}
.mt-20 { margin-top: 16px; }
.full-height { height: 100%; flex: 1; }

/* 通用玻璃态卡片容器 (亮色版) */
.tech-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 1);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s, box-shadow 0.3s;
}
.tech-card:hover { 
  box-shadow: 0 8px 24px rgba(22, 93, 255, 0.1); 
  border-color: rgba(22, 93, 255, 0.2);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-title svg { color: #165dff; }

/* 列表展示 */
.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  border-bottom: 1px dashed rgba(0,0,0,0.08);
  padding-bottom: 6px;
  font-size: 14px;
}
.info-row .label { color: #86909c; }
.info-row .value { color: #1d2129; text-align: right; max-width: 60%; word-break: break-all; font-weight: 500; }
.hardware-text { font-family: 'Courier New', monospace; font-weight: bold; color: #165dff !important; }
.text-green { color: #00b42a !important; }
.code-text { font-family: monospace; font-size: 12px; color: #ff7d00 !important; background: rgba(255, 125, 0, 0.05); padding: 1px 4px; border-radius: 4px; }

/* ==================== 中心仪表盘区 ==================== */
.center-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid rgba(22, 93, 255, 0.1);
  box-shadow: 0 8px 24px rgba(22, 93, 255, 0.06);
}

.center-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.center-head h3 { margin: 0; color: #1d2129; font-size: 18px; font-weight: 600; white-space: nowrap; }
.refresh-tag { background: rgba(0,180,42,0.1); color: #00b42a; padding: 2px 8px; border-radius: 4px; font-size: 12px; border: 1px solid rgba(0,180,42,0.2); font-weight: 500; white-space: nowrap; }

.gauge-area {
  display: flex;
  justify-content: center;
  gap: 6%;
  margin-top: 15px;
  margin-bottom: 10px;
}
.gauge-wrapper {
  width: 25%;
  position: relative;
  display: flex;
  justify-content: center;
}
.gauge-chart {
  width: 100%;
  aspect-ratio: 5 / 4;
}
.gauge-title {
  position: absolute;
  bottom: 0px;
  font-size: 13px;
  color: #4e5969;
  font-weight: 600;
  letter-spacing: 0;
}

/* 算力槽改版：去除虚线框，改用浅灰色卡片沉浸感 */
.cpu-detail-bars {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 16px 20px;
  margin: 0 16px 16px 16px;
  margin-top: auto;
}
.model-name { font-size: 13px; color: #1d2129; margin-bottom: 14px; font-family: 'Courier New', monospace; font-weight: bold;}
.bar-item { margin-bottom: 10px; }
.bar-item:last-child { margin-bottom: 0; }
.bar-labels { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 6px; color: #1d2129; font-weight: 500;}
.bar-track { width: 100%; height: 6px; background: rgba(0,0,0,0.05); border-radius: 3px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease-out; }
.bg-blue { background: #165dff; box-shadow: 0 0 6px rgba(22,93,255,0.4); }
.bg-purple { background: #722ed1; box-shadow: 0 0 6px rgba(114,46,209,0.4); }
.bg-orange { background: #ff7d00; box-shadow: 0 0 6px rgba(255,125,0,0.4); }

/* ==================== 磁盘列表阵列 ==================== */
.disk-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  padding-right: 4px;
}
.disk-list::-webkit-scrollbar { width: 4px; }
.disk-list::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 4px; }

.disk-item {
  background: #fff;
  border: 1px solid rgba(0,0,0,0.06);
  padding: 14px;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
}
.disk-head { display: flex; justify-content: space-between; margin-bottom: 12px; }
.disk-path { font-weight: bold; color: #1d2129; font-size: 15px; }
.disk-type { background: rgba(22,93,255,0.08); padding: 2px 6px; border-radius: 4px; font-size: 12px; color: #165dff; font-weight: 500; }

.disk-progress { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.dp-track { flex: 1; height: 8px; background: rgba(0,0,0,0.05); border-radius: 4px; overflow: hidden; }
.dp-fill { height: 100%; border-radius: 4px; transition: width 0.3s; }
.bg-cyan { background: #00b42a; box-shadow: 0 0 6px rgba(0,180,42,0.4); }
.bg-red { background: #f53f3f; box-shadow: 0 0 6px rgba(245,63,63,0.4); }
.dp-percent { font-size: 14px; font-weight: bold; width: 40px; text-align: right; color: #1d2129; }
.text-red { color: #f53f3f; }

.disk-foot { display: flex; justify-content: space-between; font-size: 12px; color: #86909c; font-weight: 500; }
.disk-foot .disk-io { color: #165dff; display: flex; align-items: center; gap: 4px; font-family: 'Courier New', monospace; font-weight: bold; }

.loading-state { text-align: center; color: #86909c; padding: 40px 0; font-size: 14px; }

/* 算力与网络负载 */
.load-badge { background: rgba(22,93,255,0.06); border: 1px solid rgba(22,93,255,0.2); padding: 4px 10px; border-radius: 6px; font-size: 13px; color: #4e5969; font-weight: 500; white-space: nowrap; }
.load-badge span { color: #165dff; font-family: 'Courier New', monospace; font-weight: bold; }
.hw-metric { font-family: 'Courier New', monospace; font-weight: bold; min-width: 60px; display: inline-block;}

/* 修改后的内存清理操作栏 - 更精致无边框感 */
.gc-action-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #f4f5f9;
    border-radius: 8px;
    padding: 12px 20px; /* 减小上下 padding 从而减小高度 */
    margin: auto 16px; /* 使用 auto 实现垂直居中 */
    border-left: 4px solid #165dff;
    transition: all 0.3s ease;
}
.gc-action-bar:hover {
    box-shadow: 0 4px 12px rgba(22, 93, 255, 0.08);
}
.gc-desc {
    display: flex;
    align-items: center;
}
.gc-icon-bg {
    background: rgba(22,93,255, 0.1);
    color: #165dff;
    width: 36px; height: 36px;
    border-radius: 8px;
    display: flex; justify-content: center; align-items: center;
    margin-right: 16px;
    font-size: 20px;
}
.gc-title {
    font-size: 15px;
    font-weight: 600;
    color: #1d2129;
    margin-right: 12px;
    display: block;
    margin-bottom: 4px;
}
.gc-subtitle {
    font-size: 13px;
    color: #86909c;
    display: block;
}
.tech-gc-btn {
    font-weight: bold;
    box-shadow: 0 2px 8px rgba(22, 93, 255, 0.15);
}
.tech-gc-btn:hover {
    box-shadow: 0 4px 12px rgba(22, 93, 255, 0.3);
}

/* 响应式调整以防压扁 */
@media screen and (max-width: 1400px) {
    .monitor-grid { flex-direction: column; height: auto; }
    .grid-left, .grid-center, .grid-right { width: 100%; }
    .grid-center { order: -1; margin-bottom: 20px; } /* 算力放最上面 */
}
</style>

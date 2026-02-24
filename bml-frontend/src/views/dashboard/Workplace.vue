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
             <span style="color: #86909c">下行:</span> <span class="text-green" style="font-weight:bold; min-width: 65px">{{ serverInfo.net.rxSpeed }}</span>
             <span style="color: #86909c; margin-left: 8px">上行:</span> <span class="hardware-text" style="font-weight:bold; min-width: 65px">{{ serverInfo.net.txSpeed }}</span>
             <span style="color: #86909c; margin-left: 8px">总计(收起/发送):</span> <span style="font-family: monospace; font-weight: 500; color: #1d2129">{{ serverInfo.net.totalRxBytes }} / {{ serverInfo.net.totalTxBytes }}</span>
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
           <div class="tech-card glass-panel left-card">
               <div class="card-accent" style="background: linear-gradient(135deg, #165dff, #00b8d4);"></div>
               <div class="card-title"><icon-computer/> 宿主机运行环境</div>
               <div class="info-list" v-if="serverInfo?.sys">
                   <div class="info-row"><span class="label">服务器名称</span> <span class="value hardware-text">{{ serverInfo.sys.computerName }}</span></div>
                   <div class="info-row"><span class="label">操作系统</span> <span class="value">{{ serverInfo.sys.osName }}</span></div>
                   <div class="info-row"><span class="label">系统架构</span> <span class="value">{{ serverInfo.sys.osArch }}</span></div>
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
                    <h3>核心物理算力洞察 (Computing Power)</h3>
                    <span class="refresh-tag">3s Refresh</span>
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
                           <span>已用: {{ disk.used }}</span>
                           <span>配额: {{ disk.total }}</span>
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
import { IconWifi, IconDesktop, IconComputer, IconLayers, IconStorage, IconLoading, IconSwap } from '@arco-design/web-vue/es/icon';
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
    radius: '100%',
    center: ['50%', '75%'],
    axisLine: {
      lineStyle: {
        width: 14,
        color: [[1, 'rgba(255,255,255,0.05)']]
      }
    },
    progress: {
      show: true,
      width: 14,
      itemStyle: {
        color: color,
        shadowBlur: 10,
        shadowColor: color
      }
    },
    pointer: { show: false },
    axisTick: { show: false },
    splitLine: { show: false },
    axisLabel: { show: false },
    detail: {
      valueAnimation: true,
      offsetCenter: [0, '5%'],
      fontSize: 24,
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
            console.log('最新的服务器硬件与网速指标 ->', serverInfo.value);
            
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
  gap: 12px;
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
  gap: 6px;
  color: #4e5969;
  white-space: nowrap;
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
.center-head h3 { margin: 0; color: #1d2129; font-size: 18px; font-weight: 600; }
.refresh-tag { background: rgba(0,180,42,0.1); color: #00b42a; padding: 2px 8px; border-radius: 4px; font-size: 12px; border: 1px solid rgba(0,180,42,0.2); font-weight: 500; }

.gauge-area {
  display: flex;
  justify-content: space-between;
  margin-top: -10px;
  margin-bottom: 10px;
}
.gauge-wrapper {
  width: 32%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.gauge-chart {
  width: 100%;
  height: 200px;
}
.gauge-title {
  font-size: 13px;
  color: #4e5969;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin-top: -25px;
  margin-bottom: 10px;
}

/* 算力槽 (亮色适配) */
.cpu-detail-bars {
  background: rgba(22, 93, 255, 0.03);
  border-radius: 8px;
  padding: 12px 16px;
  margin-top: auto;
  border: 1px dashed rgba(22, 93, 255, 0.2);
}
.model-name { font-size: 13px; color: #4e5969; margin-bottom: 12px; font-family: 'Courier New', monospace; font-weight: 500;}
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

.loading-state { text-align: center; color: #86909c; padding: 40px 0; font-size: 14px; }

/* 响应式调整以防压扁 */
@media screen and (max-width: 1400px) {
    .monitor-grid { flex-direction: column; height: auto; }
    .grid-left, .grid-center, .grid-right { width: 100%; }
    .grid-center { order: -1; margin-bottom: 20px; } /* 算力放最上面 */
}
</style>

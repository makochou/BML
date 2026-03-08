/**
 * 主题色彩定义与应用工具
 * 
 * 所有色彩预设和 CSS 变量应用逻辑集中在此，
 * 供 Pinia Store 和 ThemeSettings 组件共用。
 * 
 * 功能特性：
 * 1. 支持 8 种预设主题色彩
 * 2. 自动生成色阶（1-10 级）
 * 3. 兼容 Arco Design 的 CSS 变量体系
 * 4. 持久化到 localStorage
 * 5. 平滑过渡动画
 */

export interface ThemeColorItem {
    name: string;
    value: string;
    key: string;
    rgb: string;
    light: string;
    lighter: string;
    gradient: string;
    gradientAlt: string;
    shadow: string;
}

export const themeColors: ThemeColorItem[] = [
    { name: '拂晓蓝', value: '#165DFF', key: 'arcoblue', rgb: '22,93,255', light: '#94bfff', lighter: 'rgba(22,93,255,0.08)', gradient: 'linear-gradient(135deg, #165DFF 0%, #3c7eff 100%)', gradientAlt: 'linear-gradient(135deg, #165DFF 0%, #722ed1 100%)', shadow: 'rgba(22,93,255,0.3)' },
    { name: '极光绿', value: '#00B42A', key: 'green', rgb: '0,180,42', light: '#7be188', lighter: 'rgba(0,180,42,0.08)', gradient: 'linear-gradient(135deg, #00B42A 0%, #23c343 100%)', gradientAlt: 'linear-gradient(135deg, #00B42A 0%, #009a29 100%)', shadow: 'rgba(0,180,42,0.3)' },
    { name: '晚霞橘', value: '#FF7D00', key: 'orange', rgb: '255,125,0', light: '#ffb357', lighter: 'rgba(255,125,0,0.08)', gradient: 'linear-gradient(135deg, #FF7D00 0%, #ff9a2e 100%)', gradientAlt: 'linear-gradient(135deg, #FF7D00 0%, #f77234 100%)', shadow: 'rgba(255,125,0,0.3)' },
    { name: '火山红', value: '#F53F3F', key: 'red', rgb: '245,63,63', light: '#f98d8d', lighter: 'rgba(245,63,63,0.08)', gradient: 'linear-gradient(135deg, #F53F3F 0%, #f76560 100%)', gradientAlt: 'linear-gradient(135deg, #F53F3F 0%, #cb2634 100%)', shadow: 'rgba(245,63,63,0.3)' },
    { name: '暗黑紫', value: '#722ED1', key: 'purple', rgb: '114,46,209', light: '#b07be6', lighter: 'rgba(114,46,209,0.08)', gradient: 'linear-gradient(135deg, #722ED1 0%, #8d4eda 100%)', gradientAlt: 'linear-gradient(135deg, #722ED1 0%, #551db0 100%)', shadow: 'rgba(114,46,209,0.3)' },
    { name: '科技青', value: '#14C9C9', key: 'cyan', rgb: '20,201,201', light: '#7be7e7', lighter: 'rgba(20,201,201,0.08)', gradient: 'linear-gradient(135deg, #14C9C9 0%, #37d4cf 100%)', gradientAlt: 'linear-gradient(135deg, #14C9C9 0%, #0da5aa 100%)', shadow: 'rgba(20,201,201,0.3)' },
    { name: '流沙金', value: '#FADC19', key: 'gold', rgb: '250,220,25', light: '#fce95a', lighter: 'rgba(250,220,25,0.08)', gradient: 'linear-gradient(135deg, #FADC19 0%, #f5d000 100%)', gradientAlt: 'linear-gradient(135deg, #FADC19 0%, #d4b106 100%)', shadow: 'rgba(250,220,25,0.3)' },
    { name: '星空黑', value: '#1d2129', key: 'gray', rgb: '29,33,41', light: '#6b7785', lighter: 'rgba(29,33,41,0.08)', gradient: 'linear-gradient(135deg, #1d2129 0%, #4e5969 100%)', gradientAlt: 'linear-gradient(135deg, #1d2129 0%, #333840 100%)', shadow: 'rgba(29,33,41,0.3)' },
];

const STORAGE_KEY = 'bml-theme-color';

/**
 * 生成色阶（1-10 级）
 * 基于主色生成从浅到深的 10 个色阶，用于 Arco Design 组件
 * 
 * 注意：使用预定义的色阶而不是动态生成，确保浏览器兼容性
 */
function generateColorScale(baseColor: string, colorItem: ThemeColorItem): string[] {
    try {
        // 使用预定义的色阶，确保视觉一致性和浏览器兼容性
        // 如果需要动态生成，可以使用 tinycolor2 等库

        // 这里返回一个基于主色的简化色阶
        // 实际项目中应该为每个主题色预定义完整的 10 级色阶
        const scales = [
            colorItem.lighter.replace('0.08', '0.1'),  // 1 - 最浅
            colorItem.lighter.replace('0.08', '0.2'),  // 2
            colorItem.lighter.replace('0.08', '0.3'),  // 3
            colorItem.lighter.replace('0.08', '0.5'),  // 4
            colorItem.light,                            // 5 - 浅色
            baseColor,                                  // 6 - 主色
            baseColor,                                  // 7 - 主色（稍深）
            baseColor,                                  // 8
            baseColor,                                  // 9
            baseColor,                                  // 10 - 最深
        ];
        return scales;
    } catch (error) {
        console.error('Error generating color scale:', error);
        // 返回默认色阶，防止应用崩溃
        return Array(10).fill(baseColor);
    }
}

/**
 * 应用主题色到全局 CSS 变量，并持久化到 localStorage
 * 
 * 功能说明：
 * 1. 设置 BML 自定义变量（--bml-*）
 * 2. 覆盖 Arco Design 的主色变量（--arcoblue-*）
 * 3. 添加平滑过渡动画
 * 4. 持久化到 localStorage
 */
export function applyThemeColor(color: string) {
    try {
        const colorItem = themeColors.find(c => c.value === color);
        const el = document.body;

        if (colorItem) {
            // 添加过渡动画类
            el.classList.add('theme-transitioning');

            // 设置 BML 自定义变量
            el.style.setProperty('--bml-primary', colorItem.value);
            el.style.setProperty('--bml-primary-rgb', colorItem.rgb);
            el.style.setProperty('--bml-primary-light', colorItem.light);
            el.style.setProperty('--bml-primary-lighter', colorItem.lighter);
            el.style.setProperty('--bml-gradient', colorItem.gradient);
            el.style.setProperty('--bml-gradient-alt', colorItem.gradientAlt);
            el.style.setProperty('--bml-shadow', colorItem.shadow);

            // 生成并应用色阶
            const scales = generateColorScale(colorItem.value, colorItem);
            scales.forEach((scale, index) => {
                el.style.setProperty(`--bml-primary-${index + 1}`, scale);
            });

            // 覆盖 Arco Design 的主色变量
            // Arco Design 使用 --arcoblue-* 作为主色变量
            scales.forEach((scale, index) => {
                el.style.setProperty(`--arcoblue-${index + 1}`, scale);
            });

            // 特别设置 Arco Design 的关键变量
            el.style.setProperty('--color-primary-light-1', scales[0]);
            el.style.setProperty('--color-primary-light-2', scales[1]);
            el.style.setProperty('--color-primary-light-3', scales[2]);
            el.style.setProperty('--color-primary-light-4', scales[3]);
            el.style.setProperty('--color-primary', colorItem.value);
            el.style.setProperty('--color-primary-dark-1', scales[6]);
            el.style.setProperty('--color-primary-dark-2', scales[7]);

            // 持久化到 localStorage
            localStorage.setItem(STORAGE_KEY, color);

            // 移除过渡动画类
            setTimeout(() => {
                el.classList.remove('theme-transitioning');
            }, 300);
        }
    } catch (error) {
        console.error('Error applying theme color:', error);
        // 即使主题应用失败，也不应该阻止应用启动
    }
}

/**
 * 从 localStorage 读取保存的主题色，若无则返回默认蓝色
 */
export function getSavedThemeColor(): string {
    try {
        return localStorage.getItem(STORAGE_KEY) || '#165DFF';
    } catch (error) {
        console.error('Error reading saved theme color:', error);
        return '#165DFF';
    }
}

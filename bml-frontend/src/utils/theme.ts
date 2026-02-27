/**
 * 主题色彩定义与应用工具
 * 
 * 所有色彩预设和 CSS 变量应用逻辑集中在此，
 * 供 Pinia Store 和 ThemeSettings 组件共用。
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
 * 应用主题色到全局 CSS 变量，并持久化到 localStorage
 */
export function applyThemeColor(color: string) {
    const colorItem = themeColors.find(c => c.value === color);
    const el = document.body;

    if (colorItem) {
        el.style.setProperty('--bml-primary', colorItem.value);
        el.style.setProperty('--bml-primary-rgb', colorItem.rgb);
        el.style.setProperty('--bml-primary-light', colorItem.light);
        el.style.setProperty('--bml-primary-lighter', colorItem.lighter);
        el.style.setProperty('--bml-gradient', colorItem.gradient);
        el.style.setProperty('--bml-gradient-alt', colorItem.gradientAlt);
        el.style.setProperty('--bml-shadow', colorItem.shadow);

        // 兼容 Arco 的 --arcoblue-* 体系
        if (colorItem.key !== 'gray' && colorItem.key !== 'arcoblue') {
            for (let i = 1; i <= 10; i++) {
                el.style.setProperty(`--arcoblue-${i}`, `var(--${colorItem.key}-${i})`);
            }
        } else if (colorItem.key === 'arcoblue') {
            for (let i = 1; i <= 10; i++) {
                el.style.removeProperty(`--arcoblue-${i}`);
            }
        } else {
            el.style.setProperty('--arcoblue-6', color);
        }

        // 持久化
        localStorage.setItem(STORAGE_KEY, color);
    }
}

/**
 * 从 localStorage 读取保存的主题色，若无则返回默认蓝色
 */
export function getSavedThemeColor(): string {
    return localStorage.getItem(STORAGE_KEY) || '#165DFF';
}

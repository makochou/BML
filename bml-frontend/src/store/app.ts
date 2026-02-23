import { defineStore } from 'pinia';

export interface AppState {
    theme: 'light' | 'dark';
    colorWeek: boolean;
    navbar: boolean;
    menu: boolean;
    hideMenu: boolean;
    menuCollapse: boolean;
    footer: boolean;
    themeColor: string;
    headerTheme: 'light' | 'dark' | 'primary' | 'transparent';
    sidebarTheme: 'light' | 'dark' | 'white' | 'primary';
    settingsVisible: boolean;
}

export const useAppStore = defineStore('app', {
    state: (): AppState => ({
        theme: 'light',
        colorWeek: false,
        navbar: true,
        menu: true,
        hideMenu: false,
        menuCollapse: false,
        footer: true,
        themeColor: '#165DFF',
        headerTheme: 'transparent',
        sidebarTheme: 'white',
        settingsVisible: false,
    }),
    actions: {
        toggleTheme(dark: boolean) {
            if (dark) {
                this.theme = 'dark';
                document.body.setAttribute('arco-theme', 'dark');
            } else {
                this.theme = 'light';
                document.body.removeAttribute('arco-theme');
            }
        },
        toggleSettings(visible: boolean) {
            this.settingsVisible = visible;
        },
        toggleColorWeek(weak: boolean) {
            this.colorWeek = weak;
            if (weak) {
                document.documentElement.style.filter = 'invert(80%)';
            } else {
                document.documentElement.style.filter = 'none';
            }
        },
        updateSettings(partial: Partial<AppState>) {
            this.$patch(partial);
        }
    },
});

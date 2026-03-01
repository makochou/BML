/*
 * V1.6.1 API账号管理菜单收口完善
 * ---------------------------------------
 * 1. 幂等修正“API账号管理”菜单定义，确保历史环境字段一致
 * 2. 额外按组件与路径隐藏旧入口，覆盖早期测试数据或人工录入差异
 */

UPDATE sys_menu
SET menu_name = 'API账号管理',
    perms = 'api:account:list',
    parent_id = 0,
    menu_type = 'C',
    path = 'api/account',
    component = 'api/ApiAccountManage',
    icon = 'apps',
    sort = 3,
    status = 1,
    visible = 1
WHERE id = 234;

UPDATE sys_menu
SET visible = 0,
    status = 0
WHERE id IN (231, 232, 233)
   OR component IN ('api/ApiList', 'api/ApiDebug', 'app/AppList')
   OR path IN ('api/list', 'api/debug', 'app');

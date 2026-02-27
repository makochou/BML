UPDATE sys_menu
SET perms = 'system:user:remove'
WHERE perms = 'system:user:delete';

UPDATE sys_menu
SET perms = 'system:role:remove'
WHERE perms = 'system:role:delete';

DROP TABLE IF EXISTS bml_api_access;
DROP TABLE IF EXISTS bml_api_app;
DROP TABLE IF EXISTS bml_api_info;
DROP TABLE IF EXISTS bml_api_group;

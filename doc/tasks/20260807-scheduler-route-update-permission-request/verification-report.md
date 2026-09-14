# 验证报告：给排产员增加工艺路线更新权限

## Result

PASS

## Evidence

- 本地默认库存在两个 `排产员/mes_scheduler`：`tenant_id=1 role_id=910233` 与 `tenant_id=122 role_id=910287`。
- `tenant_id=1 role_id=910233` 已有效拥有 `mes:pro-route:update / menu_id=5723`，且未拥有 `mes:pro-route:delete / menu_id=5724`。
- 用户确认后已对 `tenant_id=122 / package_id=113 / role_id=910287` 执行正式授权。
- MySQL 事务写入结果：`package_rows=1`、`inserted_role_menu_rows=1`、`active_role_menu_id=912265`。
- `tenant_id=122 / package_id=113` 已包含 `5723 / mes:pro-route:update`，仍不包含 `5724 / mes:pro-route:delete`。
- `tenant_id=122 role_id=910287` 已有效拥有 `mes:pro-route:update / menu_id=5723`，且未拥有 `mes:pro-route:delete / menu_id=5724`。
- `zhaojie / user_id=913324 / tenant_id=122` 通过排产员角色获得 `mes:pro-route:update` 有效权限。
- `system_user_role` 未变更：行数 `2452`，SHA-256 `e6b15b053587951d665bdc51acfb8b97615313fa7b5c4dba668af6bf9f9a1804`。
- Redis DB0/DB1 相关权限缓存键扫描无命中；精确删除 `permission_menu_ids:mes:pro-route:update`、`menu_role_ids:5723`、`user_role_ids:913324` 返回 `0`，未清全库缓存。

## Remaining

- 若用户当前仍处于旧登录态，需要退出后重新登录以刷新前端权限信息。

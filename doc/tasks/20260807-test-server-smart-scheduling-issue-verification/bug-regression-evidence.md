# Bug Regression Evidence: zhaojie 工艺路线编辑按钮缺失

## Bug Summary

测试服务器账号 `zhaojie` 已绑定排产员角色，但打开工艺路线列表后“操作”列为空，看不到“编辑”按钮。

## Expected Behavior

正式权限合同 `20260728-scheduler-route-flow-list-permission` 要求启用的 `排产员/mes_scheduler` 有效拥有 `5723/mes:pro-route:update` 和 `5730/mes:pro-route:version-query`；前端应显示“产品/编辑/版本”，但不显示“删除”。

## Reproduction

- 真实页面：用户在测试服务器以 `zhaojie` 登录后打开工艺路线列表，截图显示 4 条路线的“操作”列全部为空。
- 只读后置核验：在测试服务器 MySQL 中按 `system_users -> system_user_role -> system_role -> system_role_menu -> system_menu` 核对用户、角色和菜单绑定。

RED: `zhaojie` 真实页面截图 + 测试库只读角色菜单查询 -> FAIL，`mes_scheduler` 的 `5723/mes:pro-route:update` 绑定为 `deleted=1`，`5730` 菜单不存在。

## Root Cause

- `zhaojie` 用户和 `mes_scheduler` 角色绑定均正常、启用且未删除。
- 测试库没有应用 `20260716_mes_route_version_permission_menu`，因此 `5730` 菜单不存在。
- 测试库没有应用 `20260728_mes_scheduler_route_flow_list_permission`，因此历史软删除的 `5723` 绑定没有恢复。
- 这是测试环境正式权限迁移缺失，不是前端条件判断错误，也不是账号未绑定排产员角色。

## Regression Test

仓库已有聚焦迁移合同测试：

- `IntRuoyiBackend/script/tests/test_mes_scheduler_route_flow_list_permission_sql.py`
- `IntRuoyiBackend/script/tests/test_mes_route_version_permission_menu_sql.py`

本轮没有改生产代码或迁移文件，不新增重复测试。

## GREEN

GREEN: BLOCKED，当前请求仅授权测试服务器问题核对，没有授权执行远端数据库迁移、角色菜单写入或权限缓存失效。

## Verification

- 用户、角色和角色绑定：PASS，均启用且未删除。
- 页面与角色菜单失败状态一致性：PASS，截图操作列为空，数据库中 `5723` 为软删除。
- 修复后真实页面回归：BLOCKED，等待测试服务器写入授权。

## Risk And Regression Scope

- 正式修复应只恢复排产员 `5723/update` 与 `5730/version-query`。
- 不得授予 `5724/delete`，不得扩大到其它角色或其它菜单。
- 权限恢复后必须清理 `zhaojie` 的精确权限缓存并重新登录验证，不能只凭数据库行宣称页面恢复。

## Blockers And Follow-up

- 阻塞：缺少用户对测试服务器数据迁移的明确写入授权。
- 获得授权后：备份精确权限行，执行两条现有正式迁移，核对影响范围，刷新精确缓存，并用 `zhaojie` 真实页面完成 GREEN 验证。

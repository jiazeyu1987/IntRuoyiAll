# Database Schema Evidence

## Data Change Goal And Affected Entities

- Goal: 在测试服 `tenant_id=1/芋道源码` 为 `wangsiyu(id=910250)` 绑定 `wenkong_no_download(910417)`，恢复文控入口且不授予下载权限。
- Affected entities: `system_user_role` 新增一条任务自有绑定行；只读核对 `system_users`、`system_role`、`system_role_menu`、`system_menu`、`dcc_file_category_permission_rule`、`dcc_directory_access_rule`、`system_entitlement_grant`。

## Database Engine And Migration Tool

- Engine: 测试服 MySQL `8.0.39`，库 `ruoyi-vue-pro`。
- Execution: SSH 到 `172.30.30.58` 后通过 `intruoyi-mysql` 容器内 MySQL CLI 执行任务 SQL。

## Schema, Migration, Fixture, Seed, Index, Or Constraint Changes

- Schema changes: None.
- Data changes: `change.sql` 单事务插入 `system_user_role(user_id=910250, role_id=910417, tenant_id=1)`。
- Fixture/seed/index/constraint changes: None.

## Data Safety Analysis

- 目标用户写前唯一且启用：`wangsiyu(id=910250, tenant_id=1, status=0, deleted=0)`。
- 候选角色写前唯一且启用：`wenkong_no_download(id=910417, status=0, deleted=0)`。
- 候选角色覆盖根菜单：`6800/900218/990200` 共 `3` 个。
- 候选角色菜单数：`10`；危险菜单权限计数：`0`。
- 用户直接、角色、岗位、部门链的类别/目录下载规则计数均为 `0`。
- 不执行全库 Redis 清理；只删除目标用户精确缓存键。

## Rollback Or Recovery Plan

- `rollback.sql` 只软删除由本任务 `creator='codex-20260807-wangsiyu-wenkong-no-download'` 创建的目标绑定。
- 回滚后同样只清理 `user_role_ids:910250` / `user_role_ids::910250` 精确缓存并重新运行 `verify.sql`。

## BDD Scenarios

- BDD: wangsiyu safe DCC menu binding -> Given 测试服芋道源码 `wangsiyu` 当前只有审批中心入口角色且看不到文控, When 绑定已有 `wenkong_no_download` 并清理该用户精确角色缓存, Then 该用户重新登录后应获得文控中心、电子签名、基础数据入口，同时没有 DCC 下载放行来源。

## RED Command And Expected Failure

- RED: 上一轮只读诊断 -> FAIL，expected reason：测试服 `wangsiyu` 有效角色只有 `approval_center_entry(910295)`，DCC 权限数为 `0`，`user_role_ids:910250` 也只缓存 `910295`。

## GREEN Command And Passing Result

- GREEN: 写前门禁核对 -> PASS，目标用户唯一启用、候选角色唯一启用、三个根菜单覆盖完整、危险菜单权限计数 `0`、下载规则旁路计数均为 `0`。
- GREEN: `change.sql` 测试服 MySQL 单事务执行 -> PASS，返回 `COMMITTED 910250 910417 4236`。
- GREEN: 精确 Redis 缓存清理 -> PASS，DB 1 `user_role_ids:910250` 删除 `1`，DB 0 无目标键；未执行全库清理。
- GREEN: 修正后 `verify.sql` -> PASS，`ACTIVE_ROLE` 包含 `approval_center_entry(910295)` 与 `wenkong_no_download(910417)`；`ROOT_MENU` 包含 `6800/900218/990200`；`NO_DOWNLOAD_RULE_COUNTS` 八项均为 `0`；`ACTIVE_DYNAMIC_ENTITLEMENT_COUNT=0`。
- GREEN: `rollback.sql` 语法验证 -> PASS，未执行业务回滚。

## Migration Verification

- `system_user_role` 新增任务自有绑定行 `id=4236`。
- `system_role`、`system_role_menu`、`system_menu` 未修改。
- `dcc_file_category_permission_rule`、`dcc_directory_access_rule` 未新增下载规则。
- Redis 仅删除目标用户精确缓存，用户重新登录后生效。

## Blockers

- None.

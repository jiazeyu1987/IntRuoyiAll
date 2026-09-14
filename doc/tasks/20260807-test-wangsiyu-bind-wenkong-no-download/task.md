# 测试服 wangsiyu 绑定文控无下载角色

## Task Goal

按用户要求在测试服务器 `172.30.30.58` 的芋道源码租户中，为 `wangsiyu` 绑定文控入口；执行范围限定为已有安全角色 `wenkong_no_download(910417)`，不授予下载、目录管理、访问规则管理或类别管理权限。

## Milestones

- [x] 读取服务器、数据库、登录、备份回滚、PowerShell 编码和 DCC 菜单恢复门禁。
- [x] 建立任务记录、回滚脚本、变更脚本和只读复验脚本。
- [x] 写前核对目标用户唯一、候选角色安全、无下载规则旁路。
- [x] 单事务绑定 `wangsiyu -> wenkong_no_download`。
- [x] 只清理 `wangsiyu` 精确用户角色缓存并复验。
- [x] 记录最终验证结果。

## Expected Verification

- MySQL 只读验证：`wangsiyu(id=910250, tenant_id=1)` 有效角色包含 `approval_center_entry` 与 `wenkong_no_download`。
- 菜单验证：三个根菜单 `6800 文控中心`、`900218 电子签名`、`990200 基础数据` 均能通过 `wenkong_no_download` 解析。
- 无下载验证：候选角色危险权限计数、角色/用户/岗位/部门类别下载规则和目录下载规则均为 `0`。
- Redis 验证：仅删除 `user_role_ids:910250` / `user_role_ids::910250` 精确候选键，不执行全库清理。

## Data Safety

- 数据库引擎：测试服 MySQL `8.0.39`，业务库 `ruoyi-vue-pro`，目标表 `system_user_role`。
- 变更类型：插入一条任务自有用户角色绑定行；不改角色定义、不改角色菜单、不改其它用户。
- 回滚路径：执行 `rollback.sql` 仅软删除由本任务 `creator='codex-20260807-wangsiyu-wenkong-no-download'` 创建的目标绑定。
- 缓存路径：只删除目标用户精确角色缓存键，不清理全库权限缓存。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，绑定缺失的正式用户角色并保留无下载边界。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260807-test-wangsiyu-bind-wenkong-no-download/change.sql
- doc/tasks/20260807-test-wangsiyu-bind-wenkong-no-download/verify.sql
- doc/tasks/20260807-test-wangsiyu-bind-wenkong-no-download/rollback.sql
- doc/tasks/20260807-test-wangsiyu-bind-wenkong-no-download/database-schema-evidence.md

## Final Result

已在测试服 `tenant_id=1/芋道源码` 为 `wangsiyu(id=910250)` 新增任务自有绑定 `wenkong_no_download(910417)`，新增 `system_user_role.id=4236`。已精确删除 Redis DB 1 的 `user_role_ids:910250`，未清理全库缓存。只读复验证明三个根菜单 `6800/900218/990200` 均由 `wenkong_no_download` 解析，角色危险权限计数为 `0`，角色/用户/岗位/部门链下载规则计数均为 `0`。

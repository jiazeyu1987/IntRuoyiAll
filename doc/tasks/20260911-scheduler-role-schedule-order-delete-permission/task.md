# 排产员增加排产工单删除权限

## Task Goal

新增正式前向权限迁移，为活动 `mes_scheduler/排产员` 角色授予 `mes:pro-schedule-order:delete`，并提供只读 target preflight、幂等与回滚验证；最终仅应用测试服并用 `zhaojie` fresh 登录验收，不执行删除。

## Milestones

1. [completed] 记录数据库、权限与 worktree 前置门禁，读取真实表结构和现有迁移模式。
2. [completed] 新增失败合同测试，锁定目标角色、唯一菜单、幂等写入、非目标隔离和回滚要求。
3. [completed] 实现前向迁移与 target preflight，完成 GREEN、回归与迁移策略验证。
4. [pending] 提交并推送任务分支，应用测试服并完成 Playwright 真实页面验收。
5. [pending] 融合主线、清理 worktree 和槽位，完成任务记录。

## Expected Verification

- 静态合同 RED -> GREEN。
- MySQL 8 隔离场景覆盖首次执行、重复执行、缺角色、重复角色、缺菜单、重复菜单与精确回滚。
- 发布迁移策略和 target preflight 合同通过。
- 测试服写入前后仅目标角色菜单绑定增加，用户角色和非目标角色权限哈希不变。
- `zhaojie` fresh 登录权限响应包含目标 permission，页面显示删除按钮但不点击。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；新增前向迁移，不篡改已发布迁移，不做一次性手工授权。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 角色按 `tenant_id + code/name`、菜单按 `permission` 解析，缺失或重复必须 fail fast。
- 只新增 `mes_scheduler -> mes:pro-schedule-order:delete` 活动绑定，不改变其它角色、用户角色或套餐。
- 迁移必须幂等；回滚仅删除本迁移新增且可归属的绑定。
- 测试服写入与 E2E 前必须记录 experience preflight PASS；禁止正式服、审查服和实际删除操作。
- 分支 worktree 位于允许目录，已登记 `int_main slot=42`，本任务不启动本地前后端。

## Current Status

in_progress

## Cleanup Keep

- doc/tasks/20260911-scheduler-role-schedule-order-delete-permission/rollback.sql

# 本机压力泵全工序权限补齐

## Task Goal

在本机 `int_main` 运行态中确认并执行 `20260803_mes_frontline_pressure_pump_all_process_permission.sql`，然后给当前登录账号所属启用角色分配 `mes:pro-feedback:frontline-pressure-pump:all-processes` 权限，解决账号 1 因缺权限落到岗位 14 工作站绑定链路的问题。

## Milestones

- [x] 核对本机运行态和缺失权限现象。
- [x] 只读核对目标库表结构、当前账号、角色、菜单和授权现状。
- [x] 执行权限迁移 SQL 并补齐当前账号所属启用角色授权。
- [x] 清理权限缓存并验证登录权限响应命中目标权限。

## Expected Verification

- 本机后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- `system_menu.id=900450` 存在，权限为 `mes:pro-feedback:frontline-pressure-pump:all-processes`。
- 当前登录账号所属启用角色至少一个拥有菜单 `900450`。
- 登录态 `/system/auth/get-permission-info` 的 `permissions` 包含目标权限。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否；只补正式菜单权限和角色授权，不放宽岗位/工作站绑定。
- `是否从根因和长期维护角度解决`：是；按既有正式迁移和权限模型补齐数据。
- `是否存在临时补丁或绕过`：否；不硬编码业务流程结果，不绕过后端权限判断。

## Current Status

completed

本机目标库已执行权限迁移，当前登录账号 userId=1 在 tenantId=1 下的 27 个启用所属角色已全部获得菜单 900450，登录权限响应已命中 `mes:pro-feedback:frontline-pressure-pump:all-processes`。

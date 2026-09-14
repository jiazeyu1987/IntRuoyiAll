# 给排产员增加工艺路线更新权限

## Task Goal

按用户要求核对并确保 `排产员/mes_scheduler` 角色拥有正式菜单权限 `mes:pro-route:update`，用于闭环处理同步工单阻断记录中的工艺维护入口。

## Milestones

- [x] 建立任务记录并核对数据库/权限门禁
- [x] 只读核对目标角色、菜单和当前授权状态
- [x] 如缺失则执行最小正式授权；如已存在则记录 no-op
- [x] 复核权限边界和验证证据

## Expected Verification

- `system_menu.permission='mes:pro-route:update'` 存在且启用、未删除。
- `system_role` 中 `排产员/mes_scheduler` 目标角色唯一或可按租户边界明确解析。
- `system_role_menu` 对目标角色和目标菜单有效绑定为 `deleted=0`。
- 不授予 `mes:pro-route:delete`，不修改 `system_user_role`，不跨非目标租户误绑。

## Applicable Gates

- `docs/database-rules.md#系统角色菜单授权 tenant 1 admin 门禁` 与跨环境角色权限同步门禁：按 `tenant_id + role.code + menu.permission` 稳定键核对。
- `docs/powershell-encoding.md`：SQL 和中文角色名使用 UTF-8 安全路径。
- `database-schema-delivery`：记录数据变更目标、回滚/恢复计划、BDD、RED/GREEN 与数据安全分析。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过正式角色菜单绑定授权，不改前端、不绕过后端鉴权。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

已按用户确认对 `tenant_id=122 / 测试租户` 执行最小正式授权：将 `5723 / mes:pro-route:update` 加入 `package_id=113`，并给 `role_id=910287 / 排产员 / mes_scheduler` 新增有效角色菜单绑定。已复核未授予 `5724 / mes:pro-route:delete`，未修改 `system_user_role`，并执行精确 Redis 权限缓存键清理。Cleanup apply 已完成，仅删除本任务中间 `database-schema-evidence.md`，保留核心任务记录。

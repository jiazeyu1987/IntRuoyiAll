# 给 admin 赋予生产组长权限角色

## Task Goal

在本地 `ruoyi-vue-pro` 数据库中，将已确认的生产组长权限角色授予同租户的 `admin` 账号，确保不跨租户误绑、不引入默认成功或降级路径。

## Milestones

- [x] 确认 `admin` 账号、目标角色和 `system_user_role` 表结构。
- [x] 记录 RED 查询，证明授权写入前缺少目标绑定或已存在则不做重复写入。
- [x] 执行最小幂等授权写入。
- [x] 复核 `admin` 角色绑定和生产组长权限菜单可解析。
- [x] 更新验证与收尾状态。

## Expected Verification

- 只读查询确认 `admin` 与目标生产组长角色属于同一租户。
- RED 查询确认写入前是否缺少 `system_user_role` 绑定。
- GREEN 查询确认写入后 `admin` 已拥有目标角色及生产组长权限点。
- 写入 SQL 必须幂等，不能新增角色、菜单或跨租户绑定。

## Current Status

ready_for_closeout

## Applicable Gates

- `docs/database-rules.md#系统角色菜单授权 tenant 1 admin 门禁`：本任务涉及 `system_role`、`system_role_menu`、`system_user_role`，写入前先确认用户、角色、权限菜单和租户边界；本次为租户 122 同租户绑定，不通过前端隐藏或 super admin 绕过。
- `docs/powershell-encoding.md`：Docker MySQL 命令使用 `--default-character-set=utf8mb4`，查询中文角色和菜单名后复核输出。
- `docs/task-closeout-rules.md`：实现和验证完成后先标记 `ready_for_closeout`，再记录验证证据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过正式角色绑定表授权，不改前端、不绕过权限校验。
- `是否存在临时补丁或绕过`：否。

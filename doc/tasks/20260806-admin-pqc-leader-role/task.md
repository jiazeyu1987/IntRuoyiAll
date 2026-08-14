# Admin 赋予 PQC组长权限角色

## Task Goal

- 在本机 `芋道源码` 租户中，将现有 `PQC组长权限角色` 赋予 `admin` 用户。
- 若 `admin` 已拥有该角色，不重复写入。
- 只修改正式 `system_user_role` 授权链路，不扩大其它角色或菜单范围。

## Milestones

- [x] 创建任务目录和初始任务文档。
- [x] 只读核对 `admin` 用户、`PQC组长权限角色` 和现有绑定。
- [x] 执行最小范围 admin 用户角色绑定。
- [x] 复核绑定结果、重复绑定和回滚边界。

## Expected Verification

- tenant `1` 存在有效 `admin` 用户。
- tenant `1` 存在有效 `PQC组长权限角色`，角色编码为 `pqc_leader_permission`。
- `admin` 用户拥有该角色。
- 对 `admin + PQC组长权限角色` 的有效绑定计数为 `1`。

## Experience Gate

- 已读取 `docs/experience-index.md`，匹配 `system_role` / `system_user_role` / tenant 1 admin 授权门禁。
- 适用门禁：`docs/database-rules.md#系统角色菜单授权-tenant-1-admin-门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用正式用户角色绑定链路。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

- 本机数据库授权、独立复核、证据校验和任务清理已完成。
- 收尾清理保留 `task.md`、`execution-log.md`、`verification-report.md`，删除临时 `database-schema-evidence.md`。

# DCC 动作权限角色拆分

## Task Goal

将 DCC 文控“查看/预览、下载、培训/阅读确认、正式下发”拆成尽可能独立的权限角色，并在本机测试租户中用幂等 SQL 完成最小安全授权，避免培训对象或下发人员被顺带授予下载、管理等无关能力。

## Milestones

1. 核对 DCC 菜单权限、类别动作权限枚举和本地库表结构。
2. 记录 BDD 与 RED，证明独立角色未完整落位。
3. 编写并执行幂等 SQL：独立角色、菜单绑定、类别动作规则和目标账号绑定。
4. 刷新相关账号权限缓存，并只读核验角色、菜单、类别规则和账号绑定。
5. 输出 verification-report.md，记录保留的旧混合角色风险与后续可选收敛项。

## Expected Verification

- 四个独立角色存在：查看、下载、培训、下发。
- 查看角色只绑定查询/预览菜单和 `VIEW` 类别动作；下载角色只绑定下载菜单和 `DOWNLOAD` 类别动作；培训角色只绑定 `dcc:controlled-file:training:mine`；下发角色只绑定 `DISTRIBUTE` 类别动作。
- 当前 DCC 账号 `wangsiyu` 获得查看+下发；培训对象账号获得培训角色；下载角色不自动授予培训或下发对象。
- 不删除旧混合角色，不修改文件状态、培训完成状态、确认时间或发布状态。

## Current Status

ready_for_closeout

## Verification Summary

- SQL 已执行通过：新增/启用 `dcc_action_view_independent`、`dcc_action_download_independent`、`dcc_action_training_independent`、`dcc_action_distribute_independent` 四个独立角色，角色 ID 分别为 `910432/910433/910434/910435`。
- 查看角色只绑定 `6807:dcc:controlled-file:query:controlled-file/browser` 与 `6810:dcc:controlled-file:preview`；未绑定重复的 `controlled-file/approval-tasks` 查询入口。
- 下载角色只绑定 `6811:dcc:controlled-file:download`；培训角色只绑定 `980121:dcc:controlled-file:training:mine`；下发角色不绑定菜单，只绑定类别动作 `DISTRIBUTE`。
- 类别 `906104 / 其他` 已新增独立类别动作规则：查看=`VIEW`、下载=`DOWNLOAD`、下发=`DISTRIBUTE`。
- `wangsiyu` 已绑定查看+下发；9 个培训对象已绑定培训；培训对象未获得新下载角色。
- Redis 权限缓存已按本次角色、菜单、用户和权限键精确刷新；活跃用户缓存已重建并包含新角色。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过角色和类别动作规则拆分权限边界，不用前端隐藏或接口绕过代替授权模型。
- `是否存在临时补丁或绕过`：否。当前仅做非破坏性新增/绑定；旧混合角色是否移除需另行确认，避免破坏既有业务。

## Applicable Gates

- 数据库 schema 门禁：写入前核对 `system_role`、`system_menu`、`system_role_menu`、`system_user_role`、`dcc_file_category_permission_rule` 结构。
- 临时表排序规则门禁：SQL 会话已显式 `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci`；预检临时表也声明 `utf8mb4_unicode_ci`，不修改库表默认排序规则。
- 权限数据安全门禁：不删除旧角色、不改业务状态、不授予 admin 绕过，不扩大到远端环境。

## Cleanup Keep

- doc/tasks/20260803-dcc-action-role-split/task.md
- doc/tasks/20260803-dcc-action-role-split/execution-log.md
- doc/tasks/20260803-dcc-action-role-split/verification-report.md
- doc/tasks/20260803-dcc-action-role-split/database-schema-evidence.md
- doc/tasks/20260803-dcc-action-role-split/role-split.sql

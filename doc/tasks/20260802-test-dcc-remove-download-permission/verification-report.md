# Verification Report

## Scope

- Target environment: test server `172.30.30.58`.
- Target tenant/account scope: `芋道源码 / zhaohaichen / 赵海辰` and `芋道源码 / wangsiyu / 王思雨`.
- Change type: account-level permission data update.

## Results

- PASS: 测试服 `system_user_role` 中三条目标绑定已标记删除：`zhaohaichen/doc_control`、`wangsiyu/doc_control`、`wangsiyu/wenkong_download`。
- PASS: 变更仅命中目标账号角色绑定；未修改 `system_role`、`system_menu`、`system_role_menu`、`dcc_file_category_permission_rule` 或 `dcc_directory_access_rule`。
- PASS: 最终复验显示 `admin effective_can_download=1`，`zhaohaichen effective_can_download=0`，`wangsiyu effective_can_download=0`。
- PASS: 最终全量清单中剩余可下载账号为 `admin`、`aoteman`、`showroomeditor`、`showroomsupervisor`、`showroomviewer`、`yingtai`；`zhaohaichen` 与 `wangsiyu` 已移除。
- PASS: Redis 用户角色缓存精确删除命令已执行；目标 key 当时不存在，返回 `0`。
- PASS: 任务审计记录提交 `b07378ca9` 已推送到 `origin/codex/20260802-test-dcc-download-permission`，远端 refs 核对为 `b07378ca94bba9f22652500eb69f92578cb5f95d`。
- PASS: 2026-08-03 00:17:19 +08:00 复验通过；测试服健康 `UP`，`zhaohaichen` 与 `wangsiyu` 均为 `effective_can_download=0`，`admin` 仍为 `effective_can_download=1`。
- PASS: 2026-08-03 00:17:19 +08:00 全量复验通过；剩余可下载账号清单不包含 `zhaohaichen` 与 `wangsiyu`。
- BLOCKED CLOSEOUT: `task-closeout-cleanup` preview 无删除项，但主工作区 `E:\IntRuoyi` 有并行脏改动，不能执行 linked worktree ff-only merge 与 worktree 删除。

## Notes

- 本次采用账号级角色绑定移除，而非修改共享角色或菜单定义，避免影响其他账号。
- 由于后端下载判定中目录管理权限会直接放行下载，`zhaohaichen` 的有效下载能力必须通过移除其 `doc_control` 角色绑定解除。

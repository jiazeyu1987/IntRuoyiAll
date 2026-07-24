# 生产管理菜单乱码修复

## 任务目标

修复生产管理页签下子菜单显示为 `???????` 的问题，确保菜单可见文案为规范简体中文，并保留现有菜单结构、权限与路由行为。

## 里程碑

1. 已完成：确认前端仓库状态、最近任务已完成，并建立本任务记录。
2. 已完成：扫描前端文本来源，定位生产管理菜单乱码来源。
3. 已完成：通过失败回归测试复现乱码问题。
4. 已完成：修复菜单文案并运行目标验证与回归验证。
5. 已完成：更新任务证据，准备提交本任务改动。

## 预期验证

- `python C:\Users\BJB110\.codex\skills\clear-frontend-copy\scripts\scan_frontend_copy.py --root D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --format markdown`
- 生产管理相关菜单文本不再包含 `???????`。
- 回归测试先失败后通过，覆盖生产管理子菜单中文文案。
- 受影响前端检查命令通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，优先定位真实文案来源并修复源数据或源代码。
- `是否存在临时补丁或绕过`：否。

## 当前状态

已完成：生产管理子菜单 `5590`、`5580` 已从问号乱码修复为 `排产员工作台`、`排产工单池`；`生产工单` 页面金蝶同步提示残留乱码已修复。

## 完成记录

- 根因：本机 `system_menu` 中 `id=5590`、`id=5580` 的 `name` 字段被历史执行链路写成问号；对应源码 SQL 文件本身为正确 UTF-8。另有 `src/views/mes/pro/workorder/index.vue` 残留金蝶同步提示问号乱码。
- 修复：通过 `scripts/repair-production-menu-garbled-copy.sql` fail-fast 校验父菜单、路由和组件后，仅更新 `5590` 与 `5580` 的菜单名；前端页面提示改为 `金蝶工单同步完成，新增 ... 个，跳过 ... 个`。
- 验证：静态回归、DB 回归、真实 Playwright 页面复核均通过。
- 收尾：task-closeout-cleanup 预览为 `ready`，无删除项、无阻塞。
- 残留风险：`npm run ts:check` 在加大 Node 堆内存后仍失败，但失败点在未由本任务修改的 `src/views/mes/pro/batchrecordtemplate/index.vue` 签名单元格相关属性缺失，不属于本次菜单乱码修复范围。

## 阻塞与风险

- 全量 `ts:check` 当前受既有 eDHR 签名单元格开发改动阻塞；本次直接相关验证已通过。

## Cleanup Keep

- `doc/tasks/20260610-production-menu-garbled-copy/bug-regression-evidence.md`
- `doc/tasks/20260610-production-menu-garbled-copy/scripts/verify-production-menu-copy-db.mjs`
- `doc/tasks/20260610-production-menu-garbled-copy/scripts/repair-production-menu-garbled-copy.sql`
- `doc/tasks/20260610-production-menu-garbled-copy/artifacts/production-menu-after-fix.png`

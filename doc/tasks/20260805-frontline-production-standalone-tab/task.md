# 一线生产独立页签

## Task Goal

将 eDHR 批次执行内部 tab 中的“生产填写”提取为独立可见页签，页签名称为“一线生产”；批次执行页面内部 tab 不再显示“生产填写”，并确保本机默认 admin 账号可在动态菜单中看到该页签。

## Milestones

- [x] M0 - 读取前端、数据库菜单、E2E、登录、运行态、编码和收尾规则。
- [x] M1 - 建立 BDD 与 RED 静态合同，证明当前实现仍把生产填写放在内部 tab 且菜单未暴露“一线生产”。
- [x] M2 - 修改前端路由、页面 tab 和动态菜单 SQL，使“一线生产”成为独立可见页签。
- [x] M3 - 更新 admin 可见性和菜单顺序静态/真实 E2E 合同。
- [x] M4 - 运行目标验证并记录证据。
- [ ] M5 - 收尾、经验沉淀、提交并推送。（验证与 cleanup 已完成；提交/推送被并行任务未合并冲突阻塞）

## Expected Verification

- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `node tests/e2e/mes-edhr-qa-menu-static.spec.js`
- `python -X utf8 -m pytest script/tests/test_mes_edhr_qa_menu_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-frontline-production-standalone-tab/frontend-feature-evidence.md`
- 如本机 `8081/48081` 运行态可用：使用默认本机 `芋道源码/admin` 真实页面验证“一线生产”菜单可见。

## Current Status

blocked - admin 可见性验证已通过，提交/推送被并行任务未合并冲突阻塞。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；从正式动态菜单、路由元数据和内部 tab 结构同步调整。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 动态菜单页签重命名门禁：同步 `system_menu.name/path/component/component_name/permission`、前端 route title、真实 E2E 入口等待文本和 admin/租户菜单绑定。
- 数据库菜单权限门禁：动态菜单交付必须核对组件文件、菜单路径、组件名、权限、租户套餐、角色菜单和登录后权限响应。
- E2E admin 可见性门禁：使用 Playwright 操作真实本机前端；API 只可作为辅助，不得替代页面可见性。
- 严格无 fallback：不得用隐藏路由、前端硬编码标题或权限静默降级掩盖动态菜单缺失。

## Commit / Push Blocker

- `git status --short --branch --untracked-files=all` 显示当前分支存在并行任务未合并冲突与已暂存的非本任务改动。
- 未合并冲突文件包括 PQC 聚合/复核相关 Java、测试文件，以及 `docs/powershell-memory.md`。
- 按同文件并行改动和提交推送门禁，本任务不能在未解决这些共享索引冲突前提交或推送，也不能回滚或代为合并非本任务改动。

## Final Verification Summary

- Static production split contract: PASS.
- Dynamic menu SQL/static contract: PASS.
- SQL migration policy gate for `20260804_mes_edhr_qa_menu.sql` and dependency chain: PASS.
- Local DB migration: `system_menu.id=900437` is `一线生产`; admin role bindings count is 3.
- Real admin visibility: PASS via `芋道源码/admin`, visible menu order `批记录表单 -> QA -> 生产组长 -> 一线生产 -> PQC组长 -> 批次执行`.
- Evidence validator: PASS before cleanup; temporary `frontend-feature-evidence.md` was deleted by task-closeout after its summary was copied into retained reports.
- Cleanup: preview/apply PASS, deleted only task-local temporary evidence and gate JSON files.

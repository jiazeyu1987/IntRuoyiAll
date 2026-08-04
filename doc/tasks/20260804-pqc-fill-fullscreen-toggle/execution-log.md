# Execution Log

## User Intent

用户使用 PQC 账号登录系统进入 `PQC填写` 页面时，页面顶部按钮默认不显示“主页”，而是显示“最大化”。点击“最大化”后页面全屏最大化，按钮切换成“主页”；点击“主页”后退出全屏恢复。最大化后的视觉布局需与用户截图一致。

## Preconditions And Rule Reads

- Read `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`.
- Read `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`.
- Read `docs\task-closeout-rules.md`.
- Read `docs\frontend-development.md`.
- Read `docs\e2e-rules.md`.
- Read `docs\powershell-encoding.md`.
- Read `docs\engineering\technology-stack-routing.md`.
- Read `docs\experience-index.md`.
- Read matching PQC experience gate in `docs\backend-development.md`.
- Git status before implementation showed many unrelated dirty files and branch already ahead of `origin/int_main`; this task must keep edits scoped to PQC fullscreen files and avoid mixing unrelated existing changes.

## BDD

- BDD: PQC填写默认最大化入口 -> Given PQC账号进入 PQC填写页面, When 页面首次渲染顶部操作区, Then 操作按钮显示“最大化”且不显示“主页”作为默认入口。
- BDD: PQC填写进入全屏 -> Given PQC填写页面显示“最大化”, When 点击最大化按钮, Then 页面请求浏览器全屏并应用最大化样式，按钮文案切换为“主页”。
- BDD: PQC填写退出全屏 -> Given PQC填写页面处于最大化状态且按钮显示“主页”, When 点击主页按钮, Then 页面退出浏览器全屏并恢复普通布局，按钮文案切回“最大化”。

## RED / GREEN / REGRESSION

- RED: `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> FAIL, expected reason: `fullscreen state must be applied to the operator panel that also owns PQC pickers and dialogs`，当前 PQC 模板仍硬编码 `@click="handleHome">主页</button>` 且没有 PQC fullscreen 状态。
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> PASS.
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-html-alignment-static.spec.cjs` -> PASS.
- REGRESSION BLOCKED: `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> FAIL before this task's PQC assertions, expected unrelated reason: `eDHR batch tabs must include 历史批记录`; current `EdhrBatchRecordTabs.vue` snapshot in workspace does not render that tab.
- GREEN: `workdir=E:\IntRuoyi; git diff --check -- <task-owned files>` -> PASS with only CRLF normalization warnings for existing frontend files.
- GREEN: `workdir=E:\IntRuoyi; python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-pqc-fill-fullscreen-toggle/frontend-feature-evidence.md` -> PASS.

## Milestone Updates

- Located target entry from router and tests: `BatchPqcFillPage.vue` renders `FrontlineFixedTemplatePanel mode="pqc"`.
- Located screenshot UI block in `FrontlineFixedTemplatePanel.vue` under `data-frontline-pqc-operator`.
- Added `edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` to lock default “最大化”, fullscreen “主页”, browser fullscreen lifecycle, and screenshot-like full-width proportions.
- Updated PQC block in `FrontlineFixedTemplatePanel.vue` to render `pqcFullscreenActionText`, request fullscreen on the outer panel, and keep picker/dialog overlays inside the fullscreen subtree.
- Updated adjacent fill-tabs static contract so PQC no longer requires the old hard-coded `主页` button before fullscreen.
- Ran `project-experience-consolidation` check; existing `docs/frontend-development.md#Element Plus 全屏弹框挂载门禁` already covers the reusable fullscreen subtree rule, so no long-term experience document was created or modified.

## Blockers

- Formal closeout remains blocked by unrelated dirty workspace and branch state: initial `git status --short --branch` showed many pre-existing dirty files and `int_main...origin/int_main [ahead 14]`; final status still shows many unrelated dirty files and `int_main...origin/int_main [ahead 16]`.
- Adjacent fill-tabs regression command remains blocked by pre-existing `EdhrBatchRecordTabs.vue` content missing “历史批记录”; this was not changed by this task.

# Execution Log

## User Intent

用户反馈受控浏览列表中“追溯”和“签核”点击后跳转页面一样；要求追溯只显示追溯信息，签核只显示签核信息，避免多余内容分散用户注意力。

## Preconditions

- 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`。
- 已使用 `bug-regression-fix-loop` 与 `frontend-feature-delivery` 技能，并读取对应 contract。
- 既有脏工作区基线提交：`6f5f52814 chore: baseline dirty worktree before trace signature split`。
- 基线后仍有并行残余：`IntRuoyiFronted/package.json`、`IntRuoyiFronted/tests/e2e/dcc-upload-browser-tab-cache-static.spec.js`；本任务不修改这些文件。
- 共享分支并发基线提交门禁命中：任务相关代码/测试改动已被共享提交吸收，后续只记录证据并选择性修改本任务文档与前端经验文档，不 amend/reset/rewrite。

## BDD Scenarios

- BDD: 追溯入口仅展示追溯信息 -> Given 用户在受控浏览列表点击某文件“追溯”；When 进入 DCC 详情追溯页；Then 页面展示生命周期、版本历史、分发、培训、受控打印等追溯区块，不展示签核追溯和签名留痕区块。
- BDD: 签核入口仅展示签核信息 -> Given 用户在受控浏览列表点击同一文件“签核”；When 进入 DCC 签核页面；Then 页面展示签核追溯和签名留痕区块，不展示项目联动、受控浏览落位、分发、培训、受控打印等非签核区块。

## RED / GREEN

- RED: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> FAIL，旧实现缺少 `ControlledFileTraceabilityScope`、`traceScope`、追溯/签核入口 scope 参数和详情页区块分面。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-detail-signature-view-mode-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-detail-signature-evidence-nonblocking-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-controlled-file-detail-retired-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Verification Evidence

- 追溯入口现在调用 `openControlledFileTraceability(router, route, id, 'browser', 'trace')`，签核入口调用 `openControlledFileTraceability(router, route, id, 'browser', 'signature')`。
- 路由构造写入 `traceScope`，详情页通过 `resolveControlledFileTraceabilityScope` 解析 `trace/signature`。
- 详情页使用 `showLifecycleTraceSections` 和 `showSignatureTraceSections` 分别控制生命周期追溯区块与签核/签名留痕区块；签核页不加载生命周期专用辅助数据，追溯页不加载签核证据。
- 2026-08-03 当前 HEAD 复跑定向静态契约与 `pnpm ts:check` 均 PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260803-dcc-trace-signature-scope-split\verification-report.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260803-dcc-trace-signature-scope-split\verification-report.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-dcc-trace-signature-scope-split --mode preview` -> `status: ready`，keep 三份核心任务记录，delete/blocked/warnings 均为 `<none>`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-dcc-trace-signature-scope-split --mode apply` -> `status: applied`，deleted_paths 为 `<none>`。
- 项目经验沉淀：已在 `docs/frontend-development.md#前端同路由多入口分面门禁` 增加复用详情路由时的 scope 分面门禁，并在 `docs/experience-index.md` 增加关键词路由；`rg -n "同路由多入口分面|traceScope"` 可定位。

## Shared Branch Commit Evidence

- `6f5f52814 chore: baseline dirty worktree before trace signature split`：任务启动前脏工作区基线。
- `3d49c8713 chore: baseline dirty worktree before dcc preview all types fix`：包含本任务入口 scope、路由 helper、详情分面与初始任务文档。
- `a79a8c31d chore: baseline frontend backend DCC preview updates`：包含详情页分面相关后续并发吸收。
- `4293607a1 chore: baseline residual docs before DCC onboarding merge`：包含相邻静态契约更新。
- `527dea09f chore: baseline residual concurrent updates before trace split closeout`：收尾前并发残余基线；未作为本任务实现提交声明。
- 这些提交均为共享分支基线/并发提交，不能伪装成本任务独立实现提交；本任务记录异常并保留验证证据。

## Blockers

- 当前工作区仍有非本任务修改：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineDeviceAccountContextServiceImpl.java`。
- 当前分支 `int_main...origin/int_main [ahead 8]`，领先提交含多个共享基线/并发任务提交；未获得明确授权前不能把它们作为本任务推送成果。
- 因上述共享分支状态，本任务保持 `ready_for_closeout`，不标记 `completed`。

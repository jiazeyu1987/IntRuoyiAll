# Verification Report

## Scope

- 修复 DCC 受控浏览行操作中“追溯”和“签核”复用同一详情页面范围的问题。
- 追溯页仅保留生命周期、版本、分发、培训、受控打印等追溯区块。
- 签核页仅保留签核追溯和签名留痕区块。
- 非目标范围：未修改后端接口、权限模型、审批流程或真实数据。

## Bug Summary

- 受控浏览列表“追溯”和“签核”入口打开同一追溯详情内容，导致用户在签核入口看到生命周期追溯等无关内容，在追溯入口也混入签核/签名留痕内容。

## Expected Behavior

- 点击“追溯”只显示生命周期、版本、分发、培训、受控打印等追溯信息。
- 点击“签核”只显示签核追溯和签名留痕信息。

## Reproduction

- Reproduction: `node tests/e2e/dcc-traceability-ux-static.spec.js` 在旧实现上 FAIL，证明两个入口缺少显式 scope，详情页也缺少区块分面。

## Root Cause

- 两个行操作都复用同一个 traceability URL 和同一个详情页默认范围，路由中没有表达 `trace/signature` 入口意图，详情页也没有按入口意图正向隔离区块和辅助加载。

## Feature Goal

- 将 DCC 受控浏览的追溯和签核入口拆成显式分面，在不改变后端接口和审批权限的前提下，让页面只展示当前入口需要的信息。

## Acceptance Criteria

- Acceptance: 追溯入口 URL 必须携带 `traceScope=trace`，签核入口 URL 必须携带 `traceScope=signature`。
- Acceptance: 追溯分面显示生命周期追溯区块并隐藏签核/签名留痕区块。
- Acceptance: 签核分面显示签核/签名留痕区块并隐藏生命周期、分发、培训、受控打印等非签核区块。

## BDD

- BDD: 追溯入口仅展示追溯信息 -> Given 用户在受控浏览列表点击某文件“追溯”；When 进入 DCC 详情追溯页；Then 页面展示生命周期、版本历史、分发、培训、受控打印等追溯区块，不展示签核追溯和签名留痕区块。
- BDD: 签核入口仅展示签核信息 -> Given 用户在受控浏览列表点击同一文件“签核”；When 进入 DCC 签核页面；Then 页面展示签核追溯和签名留痕区块，不展示项目联动、受控浏览落位、分发、培训、受控打印等非签核区块。

## Implementation Evidence

- `presentation.ts` 新增 `ControlledFileTraceabilityScope = 'trace' | 'signature'`，并在追溯 URL 中写入 `traceScope`。
- `viewer-navigation.ts` 将追溯打开 helper 扩展为显式 scope 参数，默认仍为 `trace`。
- `browser/index.vue` 将“追溯”与“签核”分别传入 `trace` 与 `signature`。
- `detail/index.vue` 解析 `traceScope`，用 `showLifecycleTraceSections` / `showSignatureTraceSections` 正向控制区块可见性，并避免加载非当前分面的辅助数据。
- `dcc-traceability-ux-static.spec.js` 覆盖入口 scope、query 写入、详情分面和非目标区块隔离。

## RED / GREEN Evidence

- RED: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> FAIL，旧实现缺少显式 scope 和详情区块分面。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-detail-signature-view-mode-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-detail-signature-evidence-nonblocking-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-controlled-file-detail-retired-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Regression Risk

- 风险集中在 DCC 详情页不同入口复用同一组件；已用静态契约锁定 route query、入口 helper、详情分面和相邻签核证据加载行为。
- 未运行真实 Playwright 页面 E2E；本轮未启动服务、未改数据、未触发写入路径。

## Closeout Status

- 实现和验证完成，任务状态为 `ready_for_closeout`。
- Cleanup Preview: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-dcc-trace-signature-scope-split --mode preview` -> `status: ready`，保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项、阻塞项或警告。
- Cleanup Apply: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-dcc-trace-signature-scope-split --mode apply` -> `status: applied`，无删除项。
- Evidence Validators: bug regression validator 与 frontend feature validator 均 PASS。
- 当前共享分支领先 `origin/int_main` 且存在非本任务后端脏改动，按共享分支并发基线门禁未标记 `completed`，未推送。

## Blockers

- 完成状态和推送被共享分支状态阻塞：当前分支领先 `origin/int_main` 且工作区存在非本任务后端脏改动；未获得明确授权前不能把并发提交作为本任务成果推送。

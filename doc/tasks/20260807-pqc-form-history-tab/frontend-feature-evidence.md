# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 在 PQC 组长工作台新增“历史表单”页签，仅展示审核通过的 PQC 表单历史，并显示审核通过人、审核通过时间。
- Non-goal: 不重构生产报工列池，不改变 PQC 管理页签的复核/修改能力，不新增 mock 或前端本地过滤。

## Requirements And Acceptance

- AC1: PQC 模块页签每个重复区域都在“看板”同级新增“历史表单”。
- AC2: 历史表单复用正式 PQC 管理列表接口，并强制 `submissionReviewStatus=APPROVED`。
- AC3: 历史表单显示 PQC 管理基础字段以及 `审核通过人`、`审核通过时间`。
- AC4: 历史表单只读，只保留详情操作。

## UI Entry Points And Owned Files

- Entry: `PqcLeaderWorkbenchPage.vue` 传入 `show-pqc-module-tabs=true` 后渲染 `TeamLeaderWorkbenchPage.vue`。
- Owned implementation: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- Owned contract adaptation: `IntRuoyiFronted/tests/e2e/team-leader-production-report-history-tab-static.spec.cjs`。
- Target RED/GREEN contract: `IntRuoyiFronted/tests/e2e/pqc-leader-form-history-tab-static.spec.cjs`。

## API Contracts And Data States

- Frontend VO: `submissionReviewLeaderUserName?: string` 已存在。
- Backend mapper/read model/response/service: 已返回并复制 `submissionReviewLeaderUserName`。
- History state: `isPqcFormHistoryTab` 时查询参数固定为 `APPROVED`，不允许复核或修改。

## BDD Scenarios

- `BDD: PQC历史表单只展示审核通过记录 -> Given PQC组长打开工作台 / When 切换到“历史表单”tab / Then 页面必须使用正式 PQC 管理列表接口并携带 submissionReviewStatus=APPROVED，只展示审核通过记录。`
- `BDD: PQC历史表单展示审核上下文 -> Given 一条 PQC 表单已审核通过 / When 历史表单列表渲染该记录 / Then 列表显示 PQC管理基本字段，并显示审核通过人姓名与审核通过时间。`
- `BDD: PQC历史表单保持只读 -> Given 用户查看历史表单 / When 行记录已审核通过 / Then 行操作只允许查看详情，不得出现复核或复核修改入口。`
- `BDD: PQC管理保留复核能力 -> Given 用户停留在“PQC管理”tab / When 列表包含待复核或退回记录 / Then 原有详情、复核、复核修改能力保持不变。`

## Verification

- RED: `node IntRuoyiFronted\tests\e2e\pqc-leader-form-history-tab-static.spec.cjs` -> FAIL，历史页签数量 `0 !== 4`。
- GREEN: `node IntRuoyiFronted\tests\e2e\pqc-leader-form-history-tab-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-history-tab-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check` -> PASS。

## UX And State Checks

- Loading/error state: 复用现有 `getSubmissionList` loading 与 `loadError` 处理，不新增吞异常或 fallback。
- Empty state: 复用正式列表空数据表现。
- Permission/read-only: `canReviewSubmission` 与 `canCorrectSubmission` 同时排除生产报工历史和 PQC 历史。
- Accessibility/stable anchors: 新增 `data-pqc-leader-module-tab-history`、`data-pqc-leader-history-approved-by`、`data-pqc-leader-history-approved-at`。

## Blockers

- `team-leader-production-report-payload-columns-static.spec.cjs` 仍失败在既存生产报工默认列池断言，未纳入本次 PQC 历史表单改动范围。

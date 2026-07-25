# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: 删除 eDHR 批次详情右侧独立 `填写人 / 提交时间` 元信息红框。
- Non-goal: 不改变单据卡片、打开填写、管理员接管、特殊节点附件或后端接口契约。

## Requirements And Acceptance

- REQ-1: 右侧栏不得渲染 `edhr-batch-detail__primary-fill-meta`。
- REQ-2: 源码不得保留仅服务该红框的 `primaryFormFillMetaItems` / `showPrimaryFormFillMeta` 逻辑。
- REQ-3: 右侧单据卡片内填写人展示和打开填写入口必须保留。

## UI Entry Points And Owned Files

- Route/UI: eDHR 批次执行详情页右侧当前工序栏。
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`。
- Tests: `IntRuoyiFronted/tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` and related eDHR static contracts.

## API Contracts And Data States

- API contract unchanged。
- Data states unchanged; this task only removes an extra presentation block.

## BDD Scenarios

- BDD: 隐藏右侧填写元信息红框 -> Given 用户打开 eDHR 批次执行详情页并查看右侧当前工序单据列表, When 右侧栏渲染当前工序单据卡片, Then 不渲染独立的 `填写人 / 提交时间` 元信息块，单据卡片自身的填写人、阻断原因和打开填写入口保持可见。

## RED / GREEN

- RED: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> FAIL，命中现有 `class="edhr-batch-detail__primary-fill-meta"`。
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-review-summary-right-rail-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-edhr-batch-review-signoff-summary-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-fill-direct-navigation-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS。

## Verification

- PASS: Target red-box removal contract passed.
- PASS: Related right-rail and form-card filler contracts passed.
- PASS: `git diff --check` completed with CRLF warnings only and no whitespace errors.

## Responsive / Accessibility / States

- Responsive: 删除额外块后右侧栏高度减少；卡片现有紧凑样式不变。
- Accessibility: 删除 `aria-label="表单填写元信息"`，保留右侧栏和卡片操作语义。
- Loading/empty/error/permission: 无数据链路变化；静态契约覆盖卡片、阻断原因和按钮保留。

## Blockers

- `node tests/e2e/edhr-ordinary-process-fill-only-static.spec.js` 仍因既有 `ExecutionPage.vue` 提交处理包含“请选择审核/批准人”失败，和本次红框删除无关。
- 当前工作区存在其他任务持续写入的非自有后端、E2E 和任务文档改动；不纳入本任务改动。

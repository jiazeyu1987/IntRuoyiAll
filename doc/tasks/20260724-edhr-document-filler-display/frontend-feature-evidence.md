# Frontend Feature Evidence

## Feature Goal and Non-Goals

- Goal: eDHR 批次执行详情页右侧每个单据卡片显示该单据的填写人。
- Non-goals: 不修改后端接口契约；不新增 mock 数据；不改变填写、提交、审核、打开填写流程。

## Requirements and Acceptance IDs

- REQ-1: 主生产表卡片显示填写人。
- REQ-2: 动态表单卡片显示填写人。
- REQ-3: 缺少填写人时显式显示“未配置”，不通过当前用户、创建人或兜底成功值推断。

## UI Entry Points, Routes, Components, and Owned Files

- Route: eDHR 批次执行详情页。
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`。
- Test: `IntRuoyiFronted/tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js`。
- Adjacent regression tests: `edhr-batch-companion-forms-right-panel-static.spec.js`, `edhr-batch-admin-filler-visibility-static.spec.js`, `edhr-review-summary-right-rail-static.spec.js`。

## API Contracts and Data States

- Data state: 复用现有批次执行详情加载到的每个任务 `fillableUsers`。
- API contract: 不新增、删除或改名接口字段。

## BDD Scenarios

- BDD: 右侧每个单据卡片显示填写人 -> Given 用户打开 eDHR 批次执行详情页并看到主生产表和动态表单卡片, When 单据存在真实填写人或责任填写人信息, Then 每个单据卡片都必须在卡片内显示“填写人”及对应姓名，不能只在底部汇总显示。
- BDD: 缺少单据填写人时显式展示未配置 -> Given 单据卡片缺少真实填写人和责任填写人信息, When 用户查看右侧单据列表, Then 该单据卡片必须显示“填写人 未配置”，不能空白或推断为当前用户。

## RED Command and Expected Failure

- RED: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> FAIL, expected reason: `右侧每张单据卡片必须显示填写人元信息。`

## GREEN Command and Passing Result

- GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-review-summary-right-rail-static.spec.js` -> PASS。
- `pnpm ts:check` -> FAIL in unrelated `src/views/dcc/controlled-file/browser/index.vue` existing ID type mismatch; no eDHR type error was reported before the unrelated blocker.

## Responsive, Accessibility, Loading, Empty, Error, and Permission Checks

- Responsive: 新增填写人行使用紧凑两列网格和 ellipsis，避免长姓名撑破 260px 右侧栏。
- Accessibility: 单据卡片仍保留现有 `role="button"`、键盘选择和 `title` 完整姓名提示。
- Loading: 不改变现有详情加载态。
- Empty: 单据无 `fillableUsers` 时明确显示 `未配置`。
- Error: 不吞异常、不新增静默降级。
- Permission: 不改变 `OPEN_FORM`、跳过、管理员接管等现有动作权限判断。

## E2E or Component Verification Path

- Static contract test first; real E2E not run because this change is a local display contract and no test tenant/login credential was requested for a write-path E2E.

## Blockers and Follow-up Skills

- `docs/experience-index.md` is missing at workspace root; recorded as non-blocking for this low-risk local UI display fix.
- Broader frontend typecheck currently blocked by unrelated DCC controlled-file browser type mismatches.

# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 在测试管理列表“操作”列增加行级“执行”按钮。
- Goal: 点击行级“执行”时，只针对当前测试项发起执行。
- Goal: 保持现有批量“顺序执行 / 并行执行”、修改、删除、执行记录刷新逻辑不变。
- Non-goal: 不修改后端 API、Runner 协议、租户权限、真实执行策略或测试项数据结构。

## Requirements And Acceptance

- AC-1: 每个测试项公共操作列显示紧凑 inline “执行”动作。
- AC-2: “执行”按钮使用 `system:codex-test:execute` 权限控制。
- AC-3: 未选择测试租户、当前行无 ID 或已有执行请求进行中时，行级“执行”不可用。
- AC-4: 行级“执行”调用现有 `/system/codex-test-execution/start`，`caseIds` 只包含当前行测试项 ID。
- AC-5: 行级“执行”使用当前测试项的 `defaultExecutionMode`。

## UI Entry And Owned Files

- Entry: `系统管理 > 测试管理`。
- Component: `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`。
- Static contract: `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`。
- Task records: `doc/tasks/20260725-codex-test-row-execute-button/`。

## API Contracts And Data States

- Reused existing `CodexTestApi.startCodexTestExecution` API.
- Request payload shape remains `{ targetTenantId, executionMode, caseIds }`。
- Row execution payload uses `caseIds: [caseId]` and `executionMode: row.defaultExecutionMode`。
- No fallback, mock success, API contract change, or silent downgrade was introduced.

## BDD Scenarios

- BDD: 单项执行按钮 -> Given 测试管理列表存在多个测试项 / When 用户点击某一行操作列的“执行”按钮 / Then 系统只以该行测试项 ID 调用执行接口，不依赖复选框已选集合，也不影响其他行。

## RED And GREEN Evidence

- RED: `node tests/e2e/system-codex-test-management-static.spec.js` -> FAIL, expected reason: `startSingleCaseExecution` missing.
- GREEN: `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/system-codex-test-management-real.e2e.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS, exit code 0。

## UI State Checks

- Responsive: Reused existing Element Plus table operation-column pattern; width increased to `220` for three inline actions.
- Accessibility: Button text is visible as `执行`; disabled state is explicit when prerequisites are missing.
- Loading: Reuses existing `executeLoading` while a start request is in progress.
- Empty: No empty-state behavior changed.
- Error: Request failures still surface through `showRequestError(error, '执行失败')`。
- Permission: Row action uses existing `system:codex-test:execute` permission.

## E2E Or Component Verification Path

- Static contract verifies the row-level execution function and payload shape.
- Real E2E script syntax check verifies the page test script remains parseable.
- Full TypeScript check verifies the new row handler type against `CodexTestCaseTableRow`。
- True Runner execution was not run because this task only adds the UI entry; Runner preconditions remain governed by `docs/e2e-rules.md#codex-runner-自动测试门禁`。

## Blockers And Follow-Up Skills

- Blockers: None for this frontend slice.
- Follow-up skills: None required.

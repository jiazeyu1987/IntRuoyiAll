# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 工艺路线流转关系图选择“表单槽位”时，在工序节点显示附加表单数量；无附加表单工序完全隐藏徽标和未绑定提示。
- Non-goals: 不改后端接口、不改工艺路线保存契约、不新增 mock 数据、不扩大到批次详情页或其它页面。

## Requirements And Acceptance

- R1: 仅当 `selectedProcessDetailFieldKey === FORM_SLOT_AGGREGATE_FIELD_KEY` 且附加表单数量大于 0 时显示数量徽标。
- R2: 附加表单数量只统计 `formBindings.formTemplateId > 0` 且 `formSlotType !== 'MAIN'` 的记录，不统计批记录表单或 legacy `batchRecordReports`。
- R3: 表单槽位零绑定返回无状态，不显示红色未绑定边框。
- R4: 其它配置项节点红绿状态不变。

## UI Entry Points And Owned Files

- Entry: `MesProRouteEdit` -> `RouteFormContent` -> `RouteFlowGraphDesigner` 的“流转关系图”页签。
- Owned source: `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`
- Owned tests: `IntRuoyiFronted/tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js`, `IntRuoyiFronted/tests/e2e/mes-route-flow-binding-border-static.spec.js`

## API Contracts And Data States

- Existing data: `ProRouteFlowProcessConfigVO.formBindings` for the badge count; `batchRecordReports` remains only for existing batch-record/legacy detail paths.
- No new API fields or backend changes.

## BDD Scenarios

- BDD: 有附加表单工序显示数量 -> Given 用户在工艺路线流转关系图选择“表单槽位”，And 某工序存在非 `MAIN` 的有效 `formBindings`，When 关系图渲染该工序节点，Then 节点黄框位置显示附加表单数量。
- BDD: 仅批记录表单工序完全隐藏 -> Given 用户选择“表单槽位”，And 某工序只有 `MAIN` 批记录表单或 legacy `batchRecordReports`，When 关系图渲染该工序节点，Then 不显示数量徽标、不显示 `0`、不显示红色未绑定边框。
- BDD: 其它配置项状态保持 -> Given 用户选择其它配置项，When 关系图渲染节点，Then 仍沿用现有已绑定/未绑定红绿状态口径。

## RED Command And Expected Failure

- RED: `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> FAIL before implementation because node badge template/helper was missing.
- RED: `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> FAIL after requirement correction because old helper still counted `MAIN` / legacy records.

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-route-flow-binding-border-static.spec.js` -> PASS.
- GREEN: `pnpm e2e:mes:route-flow-node-text-center:static` -> PASS.
- GREEN: earlier main-workspace `pnpm ts:check` -> PASS before later unrelated `system/codex-test-management` changes.
- GREEN: clean detached staged-patch `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS.
- GREEN: Real readonly Playwright probe -> PASS, route `RT000028`, badge count `1`, no MES write requests.

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Accessibility: badge uses `aria-label` and `title` with `已绑定 N 个表单`。
- Empty state: count = 0 hides badge and `missing` state for form slots.
- Error/fallback: no swallowed errors and no fallback data source introduced.
- Permissions: no permission behavior changed.

## E2E Or Component Verification Path

- Static contracts plus isolated clean-worktree type check; local runtime was available, so a read-only Playwright probe verified the badge on `http://localhost:8081`.

## Blockers And Follow-Up Skills

- No blockers. Project experience consolidation and task closeout cleanup completed.

# Execution Log

## User Intent

- 用户确认“检验规则”不需要独立配置，红框位置增加“是否需要末检” switch；首检、上午巡检、下午巡检固定需要。

## BDD

- BDD: QA 规程固定常规检验类型 -> Given QA 规程配置页已打开, When 用户查看页签和检验项目工具栏, Then 不再显示“检验规则”页签，首检、上午巡检、下午巡检内部固定为 required=true。
- BDD: 末检由工具栏 switch 控制 -> Given 用户在“工序检验方法与抽样方案”工具栏操作“是否需要末检”, When 保存或发布 QA 规程, Then `FINAL` 规则的 required 值与 switch 一致，并继续生成 `finalInspectionApplicable` 与不适用依据校验。

## Command Log

- 2026-08-06: Read `frontend-feature-delivery` skill and `references/frontend-contract.md`.
- 2026-08-06: Read `docs/frontend-development.md`, `docs/e2e-rules.md`, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, and `docs/powershell-memory.md`.
- 2026-08-06: Read `docs/experience-index.md` after creating task directory; applicable gates copied into `task.md`.
- 2026-08-06: Read `task-closeout-cleanup` skill, closeout reference, and `project-experience-consolidation` skill.

## TDD Evidence

- RED: `node tests/e2e/qa-regulation-final-inspection-switch-static.spec.cjs` -> FAIL, expected reason: old QA navigation still included `{ label: '检验规则', name: 'rules' }`.
- GREEN: `node tests/e2e/qa-regulation-final-inspection-switch-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: `git diff --check -- <task-owned paths>` -> PASS.
- REGRESSION BLOCKED: `pnpm ts:check` -> FAIL in unrelated `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue` lines 611, 647, 665, 683, 701, 746, 770 because several `resolvePqc...` helper functions are absent from that page instance; no errors reported for `QaRegulationPage.vue` in this output.

## Implementation Notes

- Removed the standalone rules tab/table and rule-specific table-column state from `QaRegulationPage.vue`.
- Added `data-qa-regulation-final-inspection-switch` in the inspection-item toolbar with `finalInspectionRequired` bound to the formal `FINAL` rule.
- Kept `data-qa-regulation-final-not-applicable-reason` when switch is off so `finalInspectionNotApplicableReason` remains user-editable and save/publish validation remains traceable.
- Added normalization so non-FINAL rules are always `required: true` when loaded into local QA rule state.

## Current Notes

- Workspace already contains many unrelated modified and untracked files; current task touched only QA regulation page, focused QA static contracts, and this task directory.
- `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue` and `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` already had concurrent unrelated diffs before this task; those preexisting hunks were preserved.

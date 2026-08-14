# Feature

- Goal: PQC 管理提交列表以结构化列反映一线 PQC 填写数据，参数明细不再重复逐件样本值。
- Non-goal: 不改后端提交接口、不阻止超限参数提交、不恢复审核副本/过程检验汇集/复核判定列。
- Entry point: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue` 的 PQC 提交列表。
- Owned files: `TeamLeaderWorkbenchPage.vue`、`pqc-leader-list-fill-form-parity-static.spec.js`、任务文档。

## Acceptance

- 参数明细按检验项展示冻结标准、上下限、设备、设备编号、检验方法和判定上下文。
- 逐件/样本值独立展示一线填写的 30 件样本值，并继续对超出上下限的数字标红提醒。
- 旧列“一线PQC表单”“审核副本”“过程检验汇集”“复核判定”不得重新出现在默认列配置或表格中。

## BDD

- BDD: PQC 参数明细不重复样本值 -> Given 一线 PQC 提交包含长度、压力、外观 30 件样本 / When PQC 管理列表渲染提交行 / Then 参数明细只按检验项展示配置上下文，逐件/样本值才展示每件样本并标红超限值。

## TDD

- RED: `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js` -> FAIL, expected because `resolvePqcParameterItems` still duplicated sample values and lacked equipment context.
- GREEN: `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js` -> PASS.

## Verification

- `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js` -> PASS.
- `node tests\e2e\pqc-submission-structured-columns-static.spec.js` -> PASS.
- `node tests\e2e\pqc-leader-item-snapshot-static.spec.js` -> PASS.
- `node tests\e2e\pqc-leader-standard-list-template-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js IntRuoyiFronted/tests/e2e/pqc-submission-structured-columns-static.spec.js doc/tasks/20260806-pqc-leader-structured-submission-columns` -> PASS.

## Blockers

- 当前分支 `int_main` 落后 `origin/int_main` 11 个提交，且工作区存在并行脏改动；本轮不做合并、提交或推送。

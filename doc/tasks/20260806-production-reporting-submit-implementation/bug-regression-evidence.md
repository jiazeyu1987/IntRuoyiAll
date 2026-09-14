# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: 生产组长报工表虽然模板渲染层对 PQC 专属列加了 `activeLeaderTab === 'PQC'`，但显示字段设置仍使用共享默认列池，生产组长可看到 PQC 专属字段配置。
- Expected: 生产组长报工表和显示字段设置不出现 `PQC提交内容`、检验类型/轮次、过程检验汇集等 PQC 内容；PQC 组长表继续保留这些列。

## Reproduction Command Or Path

- Reproduction: `node tests\e2e\team-leader-production-report-payload-columns-static.spec.cjs`.

## Root Cause

- Root cause: `TeamLeaderWorkbenchPage.vue` 使用单一 `submissionDefaultColumns` 和单一列配置控制器承载生产/PQC 两类组长报工表，导致 PQC 专属列进入生产组长的列设置池。

## Regression Test Added Or Updated

- Updated: `IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs` now asserts production and PQC report columns use separate default column pools and active column control.

## RED Command And Expected Failure

- RED: `node tests\e2e\team-leader-production-report-payload-columns-static.spec.cjs` -> FAIL, expected reason: missing `productionSubmissionDefaultColumns` / `pqcSubmissionDefaultColumns`, proving production/PQC columns were not isolated.

## GREEN Command And Passing Result

- GREEN: `node tests\e2e\team-leader-production-report-payload-columns-static.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\team-leader-workbench-static.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\frontline-production-submit-payload-detail-static.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\team-leader-production-report-abnormal-parameter-static.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\frontline-formal-submit-static.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\team-leader-report-allocation-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- Verification: targeted production/PQC column contract, adjacent team-leader static contracts, frontline submit static contracts, report allocation static contract, and TypeScript check passed.

## Risk And Regression Scope

- Scope: `TeamLeaderWorkbenchPage.vue` report table column definitions and user column settings only.
- Risk: Existing saved column preferences under the old shared key are intentionally not reused for the split production/PQC column pools, so each leader context starts from its formal default column set.
- No fallback, mock, silent downgrade, or API contract change was introduced.

## Blockers And Follow-Up Actions

- No blocker remains for static/type verification.
- Real write-type E2E remains outside this correction because task-owned runtime, tenant/account, signature, production order, and process configuration fixtures were not established.

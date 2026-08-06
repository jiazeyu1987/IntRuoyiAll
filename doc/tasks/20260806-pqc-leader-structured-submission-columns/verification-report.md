# Verification Report

## Summary

- Result: PQC leader submission list now uses structured submitted-data columns instead of a single 一线PQC表单 column.
- Removed columns: 一线PQC表单, 审核副本, 过程检验汇集, 复核判定.
- Added/kept structured coverage: 生产工单, 产品, 检验类型/轮次, 检验项, 检验数量, 损耗数量, 损耗明细, 设备, 设备编号, 接收标准, 检验方法, 检验判定, 参数明细, 逐件/样本值, 不良说明.
- Parameter detail split: 参数明细 now summarizes each inspection item's frozen standard/limits/equipment/method/judgement context; 逐件/样本值 alone lists the 30 submitted sample values.
- Out-of-range parameter/sample values remain red warnings and do not block submission.

## Commands

- `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js` -> PASS.
- `node tests\e2e\pqc-submission-structured-columns-static.spec.js` -> PASS.
- `node tests\e2e\pqc-leader-item-snapshot-static.spec.js` -> PASS.
- `node tests\e2e\pqc-leader-standard-list-template-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260806-pqc-leader-structured-submission-columns\frontend-feature-evidence.md` -> PASS.
- `node --check tests\e2e\team-leader-workbench-real-flow.e2e.js` -> PASS.
- `node tests\e2e\role-requirement-matrix-real-flow.e2e.js --check` -> BLOCKED, missing 37 real-flow prerequisites such as task tenant/accounts/signatures/order/route/QA regulation data.
- `Invoke-WebRequest http://127.0.0.1:8081/` -> `FRONTEND_HTTP=200`.
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `BACKEND_HEALTH=UP`.
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js IntRuoyiFronted/tests/e2e/pqc-submission-structured-columns-static.spec.js doc/tasks/20260806-pqc-leader-structured-submission-columns` -> PASS.

## Independent Verification Result

- PASS: Static contracts, TypeScript, evidence validator, local frontend availability, local backend health, and real-flow script syntax checks passed.
- BLOCKED: Full real Playwright business-flow verification is not claimable without the required task-owned tenant, users, signatures, work order, route, transfer, and QA regulation fixtures.

## Git Closeout Blocker

- Current branch `int_main` is behind `origin/int_main` by 11 commits and the workspace has parallel dirty changes.
- This turn intentionally avoided merge, commit, and push to avoid mixing unrelated work.

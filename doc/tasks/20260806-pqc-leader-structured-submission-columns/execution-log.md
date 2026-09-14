# Execution Log

## User Intent

- 用户明确要求：删除“一线PQC表单”整段列，改为结构化提交数据；同时删除“审核副本”“过程检验汇集”“复核判定”列，因为追溯、汇集、复核判定不在这个列表里展示。
- 需要分析并补齐结构化字段，保证 PQC 一线填写数据不遗漏。
- 用户继续后，按当前示例数据修正列表表达：参数明细不能重复逐件样本值，逐件/样本值列负责展示一线填写的 30 件数据和超限标红。

## Command / Rule Evidence

- Read: `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`.
- Read: `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`.
- Read: `docs\task-closeout-rules.md`.
- Read: `docs\frontend-development.md`.
- Read: `docs\powershell-encoding.md`.
- Read: `docs\experience-index.md` PQC/list related entries.
- Read: `C:\Users\BJB110\.codex\skills\independent-verification-gate\SKILL.md`.
- Read: `docs\e2e-rules.md`, `docs\local-runtime.md`, `docs\login-access.md`, `docs\worktree-restrictions.md` before checking real E2E/runtime readiness.

## BDD Evidence

- BDD: PQC 列表移除整段表单列 -> Given PQC 组长查看提交列表 / When 一线提交记录包含多个检验项 / Then 列表不得显示“一线PQC表单”整段列，而应按结构化列展示提交事实。
- BDD: PQC 列表删除非本列表职责列 -> Given PQC 管理列表只用于查看提交数据和操作 / When 渲染列头 / Then 不显示审核副本、过程检验汇集、复核判定列。
- BDD: PQC 结构化字段不遗漏 -> Given 一线 PQC 提交包含工单、产品、检验项、设备、标准、方法、判定、数量、损耗、不良说明和样本值 / When PQC 组长查看列表 / Then 每类数据都有独立结构化列或现有结构化列承载，超限样本值标红提醒。
- BDD: PQC 参数明细不重复样本值 -> Given 一线 PQC 提交包含长度、压力、外观 30 件样本 / When PQC 管理列表渲染提交行 / Then 参数明细只按检验项展示配置上下文，逐件/样本值才展示每件样本并标红超限值。

## Field Coverage Analysis

- 已有结构化列保留：提交时间、PQC检验员、工序、检验数量、损耗数量、损耗明细、产品、检验类型/轮次、设备、参数明细。
- 需要补齐结构化列：生产工单、检验项、设备编号、接收标准、检验方法、检验判定、不良说明、逐件/样本值。
- 删除列：一线PQC表单、审核副本、过程检验汇集、复核判定。
- 本轮职责拆分：参数明细 = 检验项冻结标准/上下限/设备/设备编号/方法/判定上下文；逐件/样本值 = 一线填写样本值及超限标红。

## TDD Evidence

- RED: `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js` -> FAIL, expected because the old table still rendered the removed 一线PQC表单 column.
- GREEN: `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js` -> PASS.
- RED: `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js` -> FAIL, expected because `resolvePqcParameterItems` still duplicated sample values and lacked equipment context.
- GREEN: `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\pqc-submission-structured-columns-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\pqc-leader-item-snapshot-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\pqc-leader-standard-list-template-static.spec.js` -> PASS.
- REGRESSION: `pnpm ts:check` -> PASS.
- REGRESSION: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260806-pqc-leader-structured-submission-columns\frontend-feature-evidence.md` -> PASS.
- REGRESSION: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js IntRuoyiFronted/tests/e2e/pqc-submission-structured-columns-static.spec.js doc/tasks/20260806-pqc-leader-structured-submission-columns` -> PASS.
- INDEPENDENT VERIFY: `node --check tests\e2e\team-leader-workbench-real-flow.e2e.js` -> PASS.
- INDEPENDENT VERIFY: `node tests\e2e\role-requirement-matrix-real-flow.e2e.js --check` -> BLOCKED, expected because 37 required real-flow environment/data prerequisites are not provided.
- INDEPENDENT VERIFY: `Invoke-WebRequest http://127.0.0.1:8081/` -> `FRONTEND_HTTP=200`.
- INDEPENDENT VERIFY: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `BACKEND_HEALTH=UP`.

## Blockers

- 当前分支 `int_main` 比 `origin/int_main` 落后 11 个提交，且工作区已有并行脏改动；本轮只做精确本地改动和验证，不执行合并、提交或推送。
- 真实页面全链路 E2E 仍需任务专用测试租户、多角色账号、签名 ID、生产订单、路线、调拨单、QA 规程版本等 37 项前置；缺前置时按 E2E 规则只能记录 BLOCKED。

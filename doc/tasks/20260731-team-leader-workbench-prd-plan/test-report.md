# Test Report

## Environment Used

- Evaluation mode: phase-gated
- Validation surface: task-defined

## Results

## P1

- PASS.
- Scope verified: 活跃订单加入/移除/查询、班组员工档案、临时工不关联用户系统、工序-员工绑定、班组设备新增、设备报修/禁用/恢复状态、工序-设备绑定、设备参数单位/上下限/默认值、工序异常原因绑定、Controller 权限与登录用户注入、P1 schema contract。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 15, failures: 0, errors: 0, skipped: 0.
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamEmployeeBindingServiceTest,MesProcessDeviceParameterRuleServiceTest,MesDefectReasonCatalogServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 20, failures: 0, errors: 0, skipped: 0.
- Evidence validators: backend API evidence PASS; database schema evidence PASS.
- Remaining scope: P2-P6 are not covered by P1 and remain pending, including employee runtime config read path, FIFO/manual report allocation, order-process completion, formal batch-record backfill, frontend workbench, real E2E, and int_main fusion.

## P2

- PASS.
- Scope verified: 员工端运行态配置接口、临时工无系统用户 ID 切换、员工端设备/参数/不良原因配置驱动、报修/禁用设备过滤、设备参数默认值、其它班组员工不泄漏、Controller 登录用户注入、前端静态合同和类型检查。
- RED evidence: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected `<1>` employees but was `<2>`，运行态配置泄漏其它班组员工。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 1, failures: 0, errors: 0, skipped: 0.
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigControllerTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 7, failures: 0, errors: 0, skipped: 0.
- Command: `pnpm --dir IntRuoyiFronted test e2e:frontline-team-config:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- Remaining scope: P3-P6 are not covered by P2 and remain pending, including FIFO/manual report allocation, order-process completion, formal batch-record backfill, frontend workbench, real E2E, and int_main fusion.

## P3

- PASS.
- Scope verified: 报工确认分配模型、FIFO 自动分配服务、手动分配调整、活跃订单约束、分配总数校验、当前工序剩余数量校验、重复确认阻塞、Controller 登录用户注入、P3 schema contract、前端报工确认静态合同和类型检查。
- RED evidence: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderReportConfirmationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前复核接口不能表达多活跃订单分配、分配行校验和重复确认锁定。
- RED evidence: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderFifoAllocationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前缺少按活跃订单队列、稳定排序和当前工序剩余数量自动拆分的 FIFO 服务。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 15, failures: 0, errors: 0, skipped: 0.
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-report-allocation:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- Remaining scope: P4-P6 are not covered by P3 and remain pending, including order-process completion, formal batch-record backfill, full frontend workbench, real E2E, and int_main fusion.

## P4

- PASS.
- Scope verified: 订单工序累计完成服务、正式批记录绑定读取、字段映射回填、缺正式绑定阻塞、缺字段映射阻塞、已完成工序幂等不重复回填、P4 schema contract、P3/P4 联合回归。
- RED evidence: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 缺 P4 迁移文件且已完成工序会重复触发批记录回填。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 7, failures: 0, errors: 0, skipped: 0.
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 21, failures: 0, errors: 0, skipped: 0.
- Remaining scope: P5-P6 are not covered by P4 and remain pending, including full frontend workbench, real E2E, and int_main fusion.

## P5

- PASS.
- Scope verified: 生产组长页签重构为报工确认工作台和班组配置中心，包含活跃订单、员工档案、设备档案、设备参数、工序员工、工序设备、工序异常关系、活跃订单异常选择器、结构化报工详情、FIFO 自动分配和手动分配表。
- RED evidence: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static` -> FAIL, 页面缺少 `data-team-leader-report-workbench`，仍以旧提交看板 / 异常上报 / 班组维护三页签和 ID 手输表单为主。
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-report-allocation:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted test e2e:frontline-team-config:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- Remaining scope: P6 is not covered by P5 and remains pending, including real Playwright E2E, task closeout, push, and int_main fusion.

## P6

- PASS.
- Scope verified: 真实 Playwright 用户路径已覆盖生产组长配置、员工正式报工、动态 eventId 发现、FIFO 自动分配、组长确认分配、订单工序完成、正式批记录回填和任务自有数据清理；静态合同、前端类型检查和后端回归均通过。
- RED evidence: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static` -> FAIL, 真实 E2E 证据路径依赖 `process.cwd()`，会误写到 `IntRuoyiFronted/doc/tasks/...`。
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:real:check` -> PASS.
- RED evidence: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static` -> FAIL, P6 静态合同新增动态 eventId 要求后，真实 E2E 仍要求外部提前提供包含 eventId 的分配核验路径。
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static` -> PASS, 真实 E2E 可在员工提交后只读发现 eventId，并通过 `__EVENT_ID__` / `{{eventId}}` 占位符或默认 trace endpoint 构造核验路径。
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:real:check` -> PASS.
- Fixture: tenant `122` task-owned `TLW-20260731-` data seeded and read-back verified for work order `980007`, task `980008`, route process `980006`, process `980002`, item `980001`, employee profile `980014`, device `980005`, recordbook `980010`, signature `922734`, formal route-process batch-record binding and `PROCESS_POOL_REPORT` cell-link rule.
- Command: `pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real:check` -> PASS.
- RED evidence: `pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real` -> FAIL, 旧脚本等待瞬时 `复核已提交` toast 超时；已改为等待 `/mes/pro/process-pool/team-leader/submission/allocation/confirm` 响应并断言业务码。
- RED evidence: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getPrefill_skipsProcessPoolReportRulesBecauseTeamLeaderBackfillOwnsThem" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 通用批记录单元格链接预填把 `PROCESS_POOL_REPORT` 当成不支持来源；已修正为跳过并交由生产组长批记录回填服务处理。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getPrefill_skipsProcessPoolReportRulesBecauseTeamLeaderBackfillOwnsThem" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- E2E: `pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real` -> PASS；证据见 `doc/tasks/20260731-team-leader-workbench-prd-plan/p6-real-e2e-evidence.md`，result eventId=`22`。
- Cleanup: post-E2E task-owned residue check -> PASS，`active_order`、`employee_binding`、`process_device`、`parameter_rule`、`defect_reason`、`event`、`feedback`、`allocation`、`completion`、`recordbook_entry` 均为 `0`，设备 `980005` 恢复 `REPAIRING` 且 enabled。
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted test e2e:frontline-formal-submit:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted test e2e:frontline-team-config:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-report-allocation:static` -> PASS.
- Command: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesFrontlineRuntimeConfigControllerTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineRuntimeConfigServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 47, failures: 0, errors: 0, skipped: 0.
- Remaining scope: P6 已完成；后续仅剩任务收尾、提交推送和按门禁融合 `int_main`。

## P6 Resume Recheck

- Command: `pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real` -> PASS after password-only environment injection; `p6-real-e2e-evidence.md` now shows `Status: PASS` and eventId=`23`.
- Cleanup: final task-owned residue check -> PASS，`active_order`、`employee_binding`、`process_device`、`parameter_rule`、`defect_reason`、`event`、`feedback`、`allocation`、`completion`、`recordbook_entry` 均为 `0`，设备 `980005` 恢复 `REPAIRING` 且 enabled。
- Governance: append-only batch-record field audit item for executionId=`1607` remains by design and was not force-deleted.

## int_main Fusion Recheck

- Command: `git merge-base --is-ancestor codex/20260731_shengchanbanzuzhang int_main` -> PASS，feature branch 已被当前 `int_main` 包含。
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static` -> PASS on merged `int_main`.
- Command: `pnpm --dir IntRuoyiFronted test e2e:frontline-formal-submit:static` -> PASS on merged `int_main`.
- Command: `pnpm --dir IntRuoyiFronted test e2e:frontline-team-config:static` -> PASS on merged `int_main`.
- Command: `pnpm --dir IntRuoyiFronted test e2e:team-leader-report-allocation:static` -> PASS on merged `int_main`.
- Command: `pnpm --dir IntRuoyiFronted ts:check` -> PASS on merged `int_main`.
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesFrontlineRuntimeConfigControllerTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineRuntimeConfigServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS on merged `int_main`, tests run: 48, failures: 0, errors: 0, skipped: 0.
- Command: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS on `int_main`, frontend `8081`, backend `48081`.
- Baseline: unrelated serial routes Runner dirty workspace preserved separately as `00df27e68 chore: baseline serial routes runner workspace changes`.

## Final Verdict

- Outcome: P6 real E2E, regression verification, merged `int_main` fusion recheck, and remote push recovery passed; task is completed.

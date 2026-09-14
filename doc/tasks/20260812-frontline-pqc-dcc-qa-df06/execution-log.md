# DF06 Execution Log

## User Intent

继续推进一线 PQC DCC-QA 正式链路中的 DF06：新建活跃订单锁定 DCC/QA 快照，并按 QA 正式规则 key 生成 PQC 任务。

## BDD

- BDD: 新建活跃订单锁定 QA 快照 -> Given 工单的当前生产路线存在正式 route-DCC 关系且 DCC 项目代码下存在已发布 QA 规程, When 班组长把工单加入活跃订单池, Then 新活跃订单写入 dccProjectCodeId、qaRegulationId、qaRegulationVersionId，并在同一事务继续生成工序快照和 PQC 任务。
- BDD: 上午下午巡检不合并 -> Given QA 发布版本包含 FIRST、PATROL_AM、PATROL_PM、FINAL 规则且 PATROL_AM/PATROL_PM 共用 inspectionType=PATROL 检验项目, When 系统生成 PQC 任务, Then 生成四条任务并分别写入 inspectionRuleKey=FIRST/PATROL_AM/PATROL_PM/FINAL。
- BDD: 重新激活保留旧快照 -> Given 旧活跃订单已被移除且已有 DCC/QA 锁定快照, When 用户重新加入同一工单路线版本, Then 系统只恢复原订单，不重新锁定当前 QA 版本，不重建历史任务。

## Evidence

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected missing DF06 implementation. First captured RED from worker was missing active-order QA snapshot getters and inspectionRuleKey; after partial implementation, rerun failed at compile because PqcInspectionRule, parseCanonicalInspectionRules, and new QA identity mapper call were not implemented.
- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, current rerun failed before final fix with 1 assertion mismatch in shouldAddWorkOrderToLeaderActivePoolWithServerResolvedQaRoute after route snapshot fixture changed from legacy QA-derived route process IDs to formal route snapshot IDs.
- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 33, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df06/backend-api-evidence.md -> PASS, Backend API evidence is valid.
- REGRESSION: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesTeamLeaderActiveOrderManualSortTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 39, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- REGRESSION: git diff --check -> PASS.
- REGRESSION: rg -n 'selectEnabledList\(|productMasterId|formBindings|fallback|兼容|兜底|默认成功' DF06 touched production files -> PASS, no narrow forbidden matches.

## Milestone Updates

- 2026-08-13: Took over incomplete DF06 worktree after worker handoff. Confirmed task docs were absent in the worktree and recreated them before continuing implementation.
- 2026-08-13: Implemented DF06 route-DCC based QA lock, active-order DCC/QA snapshot persistence, removed-order snapshot preservation, QA inspectionRuleKey identity, and four canonical task rule keys without mapping QA processes back to MES route processes.
- 2026-08-13: Completed DF06 GREEN, backend evidence validator, touched-test regression, and static no-fallback/no-product-inference scan.

## Blockers

- None currently.

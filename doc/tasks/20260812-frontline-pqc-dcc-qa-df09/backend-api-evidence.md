# Backend API Evidence

## Scope

- Service / read-model scope: MES 一线 PQC task overlay 与一线生产提交候选。
- Owned files: `MesPqcInspectionTaskMapper`, `MesFrontlinePqcTaskOverlay`, `MesFrontlineProductionSubmitCandidate` and matching unit tests.

## API Contract And Data Contract

- PQC overlay must match pending tasks by `activeOrderId`, `regulationVersionId`, `qaProcessId`, `inspectionRuleKey`, and inspection type.
- Missing matching PQC task is reported as `NOT_CREATED`.
- FIRST, PATROL_AM, PATROL_PM, and FINAL remain separate task identities.
- PQC overlay list output is sorted by `businessDate`, FIRST/PATROL_AM/PATROL_PM/FINAL rule order, `roundNo`, and task id.
- Production submit candidate ownership is derived from active-order process snapshot identity.

## Auth, Permissions, Validation, Error Behavior

- No auth or permission contract change in this slice.
- No fallback, compatibility shim, default success, or swallowed exception is introduced.
- No QA process or inspection item filtering is introduced.
- No product/material/route inference is used to create QA ownership.
- No QA process-to-MES route process existence validation is introduced.

## Required Config, Services, Fixtures, Migrations

- No new config.
- No new service dependency.
- No schema or migration change.
- Required fixtures are unit-test-local builders and mapper/service test doubles only.

## BDD Scenarios

- BDD: PQC overlay 精确匹配 -> Given 同一 activeOrderId 下存在不同 regulationVersionId、qaProcessId、inspectionRuleKey 与 inspectionType 的 PQC 任务 When 构建一线 PQC overlay Then 仅匹配 activeOrderId + regulationVersionId + qaProcessId + inspectionRuleKey 完全一致的任务。
- BDD: PQC 任务未创建状态 -> Given active-order process snapshot 有 QA 检验上下文但无匹配 PENDING PQC 任务 When 构建 overlay Then 返回 NOT_CREATED，且不通过默认任务或空成功掩盖未创建状态。
- BDD: PQC 检验类型隔离 -> Given 同一 QA 工序存在 FIRST、PATROL_AM、PATROL_PM、FINAL 任务 When 构建 overlay Then 四类检验分别附着，不合并为一个任务。
- BDD: PQC overlay 稳定业务排序 -> Given DF09 组合结果输入顺序与业务顺序不同 When 构建 overlay 列表 Then 按 businessDate、FIRST/PATROL_AM/PATROL_PM/FINAL 规则顺序、roundNo、taskId 稳定输出。
- BDD: 生产提交候选 snapshot 归属 -> Given active-order process snapshot 只授权部分 routeProcessId/processId When 查询生产提交候选 Then 候选只来自 active-order process snapshot。

## RED Command And Expected Failure

- Command: `cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df09\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- Expected failure: test compilation fails because `MesFrontlinePqcTaskOverlay` does not exist yet.
- Observed result: FAIL with `MesFrontlinePqcTaskOverlay` missing.
- RED: `cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df09\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesFrontlinePqcTaskOverlay` missing.

## GREEN Command And Passing Result

- Command: `cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df09\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- Supervisor RED: same command -> FAIL, expected reason: stable sorting scenario exposed input-order output [1004, 1003, 1001, 1002] instead of [1001, 1002, 1003, 1004].
- Passing result: BUILD SUCCESS; `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.
- GREEN: `cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df09\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.

## Contract Or Integration Verification

- Unit contract verification covers activeOrderId + regulationVersionId + qaProcessId + inspectionRuleKey matching.
- Unit contract verification covers NOT_CREATED when no PENDING task matches.
- Unit contract verification covers FIRST, PATROL_AM, PATROL_PM, and FINAL as separate overlay identities.
- Unit contract verification covers stable business sorting by businessDate, rule order, roundNo, and task id.
- Unit contract verification covers production submit candidates requiring active-order process snapshot membership.

## Observability Touchpoints

- Unit tests assert read-model contract directly.
- No runtime logging or metrics contract change.

## Blockers And Downstream Skill Needs

- None currently.

## Evidence Validator

- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df09/backend-api-evidence.md`: PASS，`Backend API evidence is valid.`

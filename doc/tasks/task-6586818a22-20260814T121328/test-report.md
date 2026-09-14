# Test Report

- Task ID: task-6586818a22-20260814T121328
- Purpose: PRD、开发计划、生产实现和 P1-P4 自动化验收记录。

## Environment Used

- Evaluation mode: blind-first-pass
- Validation surface: real-runtime
- Tools: Maven, pnpm, Playwright
- Initial readable artifacts: prd.md, test-plan.md
- Initial withheld artifacts: execution-log.md, task-state.json
- Initial verdict before withheld inspection: yes
- Workspace: E:\IntRuoyi
- Runtime used: 本地 int_main 源码；Maven/JUnit 数据库与单元测试、Node 静态合同、Vue TypeScript 检查。

## Results

### T1: 未勾选工艺流程不重建工序节点和流程关系

- Covers: P1-AC1
- Result: passed
- Command run: `node tests/e2e/batch-record-word-import-route-candidate-static.spec.js`；最终 Maven 12 条组合回归中的 `generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph`
- Environment proof: 前端静态合同 PASS；后端最终回归 Tests run: 12, Failures: 0, Errors: 0。
- Evidence refs: execution-log.md, verification-report.md
- Notes: 绑定候选沿用原 ACTIVE 的 nodes、edges 和 boundaryEdges。

### T2: 勾选工艺流程并已有路线时提示候选版本

- Covers: P1-AC2, P2-AC3
- Result: passed
- Command run: `node tests/e2e/batch-record-word-import-route-candidate-static.spec.js`；最终 Maven 12 条组合回归中的 `recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3`
- Environment proof: 静态合同 PASS；后端 DRAFT 原位更新测试 PASS。
- Evidence refs: execution-log.md, verification-report.md
- Notes: 页面明确候选发布后生效，后端不创建 V3。

### T3: 候选锁定状态阻断

- Covers: P1-AC3, P2-AC4
- Result: passed
- Command run: `node tests/e2e/batch-record-word-import-route-candidate-static.spec.js`；P3 相邻回归中的 PENDING_APPROVAL、READY_TO_PUBLISH 和候选 ID 漂移定向 Maven 测试
- Environment proof: 前端锁定合同 PASS；后端 4 条候选锁定/漂移回归 PASS。
- Evidence refs: execution-log.md, verification-report.md
- Notes: 锁定候选和预检后漂移均在写入前阻断。

### T4: 无现有路线时创建完整路线

- Covers: P2-AC2
- Result: passed
- Command run: P2 `MesProBatchRecordReportServiceImplDbTest` 新建路线与 DCC 身份定向 Maven 回归
- Environment proof: P2 数据库场景 Tests run: 8, Failures: 0, Errors: 0。
- Evidence refs: execution-log.md, verification-report.md
- Notes: 覆盖路线、工序、流程边界、DCC 正式绑定和初始 ACTIVE。

### T5: 已有路线生成或更新 DRAFT 候选且 ACTIVE 不变

- Covers: P2-AC3, P4-AC2
- Result: passed
- Command run: 最终 Maven 12 条组合回归中的 `recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3` 和发布投影测试类
- Environment proof: 后端最终回归 Tests run: 12, Failures: 0, Errors: 0。
- Evidence refs: execution-log.md, verification-report.md
- Notes: 同源 DRAFT 原位更新，发布前 ACTIVE 快照和正式路线不变。

### T6: 候选节点按 Word 顺序生成

- Covers: P3-AC1
- Result: passed
- Command run: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Environment proof: P3 独立验收 3/3 PASS；最终组合回归再次 PASS。
- Evidence refs: test-report.md#Independent-P3-Verification-2026-08-14, execution-log.md
- Notes: 重复 processId 按 occurrence 映射并保持 Word 顺序。

### T7: 正式批记录表单绑定保留

- Covers: P3-AC2
- Result: passed
- Command run: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Environment proof: 旧绑定 permissionScopeId 701001/701002 和两组冻结 hash 原值断言 PASS。
- Evidence refs: test-report.md#Independent-P3-Verification-2026-08-14, execution-log.md
- Notes: 旧正式绑定仅更新候选节点引用，不重算权限或冻结值。

### T8: formBindings 保留但不替代批记录表单

- Covers: P3-AC3
- Result: passed
- Command run: P3 配置迁移数据库测试；`MesProRouteVersionPublishProjectionServiceImplTest#projectCandidate_shouldRestoreMainBatchRecordAndKeepLossFormInIndependentSlot`
- Environment proof: P3 独立验收 PASS；P4 正向发布测试 PASS。
- Evidence refs: test-report.md#Independent-P3-Verification-2026-08-14, execution-log.md
- Notes: formBindings 独立迁移，不补齐或替代正式批记录表单。

### T9: 工序开始配置保留，工序结束不生成绑定

- Covers: P3-AC4
- Result: passed
- Command run: P3 配置迁移数据库测试；完整 `MesProRouteVersionPublishProjectionServiceImplTest`
- Environment proof: P3 独立验收 PASS；发布投影测试类 9/9 PASS。
- Evidence refs: test-report.md#Independent-P3-Verification-2026-08-14, execution-log.md
- Notes: 两类 START 配置保留，未生成 routeEndBindings/processEndBindings。

### T13: 只导入批记录表单绑定时不重排 flowGraph

- Covers: P1-AC1
- Result: passed
- Command run: `MesProBatchRecordReportServiceImplDbTest#generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph`
- Environment proof: P1 独立后端 DB 1/1 PASS；最终组合回归再次 PASS。
- Evidence refs: test-report.md#Independent-P1-Verification-2026-08-14, execution-log.md
- Notes: 未勾选工艺流程时不重排 flowGraph。

### T10: 配置工序无法唯一映射时失败

- Covers: P3-AC5, P4-AC3
- Result: passed
- Command run: `MesProBatchRecordReportServiceImplDbTest#generateRouteOnlyForUploadedWord_whenConfiguredOccurrenceIsMissing_failsFast`；P4 两条缺快照发布前阻断测试
- Environment proof: P3 独立验收负向路径 PASS；发布投影测试类 9/9 PASS。
- Evidence refs: test-report.md#Independent-P3-Verification-2026-08-14, execution-log.md
- Notes: 映射失败或必要数组缺失均在 ACTIVE 写入前 fail fast。

### T11: 发布候选后正式路线保留三类配置

- Covers: P4-AC1
- Result: passed
- Command run: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProRouteVersionPublishProjectionServiceImplTest test`
- Environment proof: Tests run: 9, Failures: 0, Errors: 0, Skipped: 0。
- Evidence refs: execution-log.md, verification-report.md
- Notes: 正式批记录、formBindings、START 配置分别投影；新工序先建正式身份再建权限范围。

### T12: DCC 与版本并发保护

- Covers: P2-AC1, P2-AC4
- Result: passed
- Command run: P2 `MesProBatchRecordReportServiceImplDbTest` DCC 缺失、冻结 ID 缺失、候选 ID 漂移和锁定状态定向 Maven 回归
- Environment proof: P2 数据库场景 8/8 PASS；治理合同 5/5 PASS。
- Evidence refs: execution-log.md, verification-report.md
- Notes: 缺正式身份或预检后漂移时不解析 Word、不写路线。

## Final Verdict

- Outcome: passed
- Verified acceptance ids: P1-AC1, P1-AC2, P1-AC3, P2-AC1, P2-AC2, P2-AC3, P2-AC4, P3-AC1, P3-AC2, P3-AC3, P3-AC4, P3-AC5, P4-AC1, P4-AC2, P4-AC3
- Blocking prerequisites:
- Summary: P1-P4 的生产实现、数据库/单元测试、前端静态合同和类型检查全部通过；旧正式批记录绑定关系保持不变，新工序发布后建立正式权限关系。

## Open Issues

- 真实浏览器写入 E2E 未执行：缺少已确认的测试租户、账号和任务自有 Word fixture；未使用 mock、API-only 或直接 SQL 替代。

## Independent P1 Verification (2026-08-14)

- Scope: P1 only. The first pass read only `prd.md` and `test-plan.md`; `execution-log.md` and `task-state.json` were not read before the verdict below.
- First-pass verdict: PASS for P1-AC1, P1-AC2 and P1-AC3. This is not a verdict for P2-P4.

### Requirement-to-Evidence Checklist

| P1 acceptance | Result | Independent evidence |
| --- | --- | --- |
| P1-AC1: no flow rebuild when "工艺流程" is unchecked | PASS | The page computes `routeFlowRebuildRequested` only from `selectedOptions.length`; a binding-only request is explicitly separate. The backend DB test creates an ACTIVE graph containing nodes, normal edges and START/END boundary edges, then asserts the resulting DRAFT binding candidate has exactly the same `flowGraph`. |
| P1-AC2: checked flow shows candidate/publish-later semantics and sends frozen IDs | PASS | Static contract verifies the candidate confirmation text and the frozen `expectedRouteId`, `expectedRouteVersionId`, and `expectedRouteCandidateVersionId` payload fields. |
| P1-AC3: locked candidate blocks import | PASS | Static contract verifies `PENDING_APPROVAL` and `READY_TO_PUBLISH` are lock states and disables flow selection. Independent source inspection also confirms submit confirmation exits with the locked-state message before any upgrade confirmation. |

### Commands and Results

- PASS: `node tests/e2e/batch-record-word-import-route-candidate-static.spec.js`
- PASS: `node tests/e2e/batch-record-word-import-production-upgrade-dedupe-static.spec.js`
- PASS: `node tests/e2e/batch-record-word-import-dcc-identity-static.spec.cjs`
- PASS: `pnpm ts:check`
- PASS: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Surefire result: 1 test run, 0 failures, 0 errors, 0 skipped; report timestamp 2026-08-14 18:11:39.
  - The reactor compilation was lengthy but had no Maven target contention. The completed report is `IntRuoyiBackend/yudao-module-mes/target/surefire-reports/TEST-cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImplDbTest.xml`.

### Boundaries and Remaining Work

- P1 is independently verified only at backend DB/static-contract/type-check level. No real-browser write E2E was run because P1's planned primary evidence is static/backend and no P1-specific confirmed test tenant fixture was supplied to this verifier.
- P2, P3 and P4 remain unverified by this P1 gate. The pre-existing `not_run` entries above remain their historical planning status and are not a substitute for their later phase evidence.

## Independent P3 Verification (2026-08-14)

- Scope: P3 only. The blind first pass read only `prd.md` and `test-plan.md`; it did not read `execution-log.md`, `task-state.json`, or this report before reaching the verdict.
- First-pass verdict: PASS for P3-AC1 through P3-AC5. This is not a publish-projection (P4) verdict.

### Requirement-to-Evidence Checklist

| P3 acceptance | Result | Independent evidence |
| --- | --- | --- |
| P3-AC1: Word order and publishable node identity | PASS | The candidate builder counts each `processId` occurrence and resolves the matching ACTIVE occurrence. The DB test uses a repeated process and asserts Word order `混合 -> 包装 -> 混合`, with the first and second occurrences mapped to their distinct original `routeProcessId` values. |
| P3-AC2: formal batch-record binding is retained independently | PASS | The DB test retains `old-mixing-1` and `old-mixing-2` from `batchRecordReports`, confirms Word report values are not substituted, and checks the remapped formal report identity/hash fields. The implementation requires `batchRecordReports` and `formBindings` as separate ACTIVE snapshot arrays. |
| P3-AC3: form slots are retained without becoming batch records | PASS | The same test preserves form-template IDs `8101`, `8201`, and `8102` in `formBindings`, maps each slot to its candidate `routeProcessId`, and separately asserts formal batch-record bindings. |
| P3-AC4: START configuration retained; no END business binding | PASS | The test compares `routeStartProductionLeaders` and `batchRecordAttachmentOwners` exactly with ACTIVE, retains only START/END boundary edges, and asserts that neither `routeEndBindings` nor `processEndBindings` is generated. |
| P3-AC5: unmappable configured occurrence fails fast | PASS | The negative DB test removes a configured old process from the Word sequence, receives a route/process/occurrence-specific exception, creates no candidate, and leaves ACTIVE snapshot unchanged. |

### Commands and Results

- PASS: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations+generateRouteOnlyForUploadedWord_whenConfiguredOccurrenceIsMissing_failsFast+recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Surefire result: 3 tests run, 0 failures, 0 errors, 0 skipped; elapsed 68.67 seconds.
- PASS: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRouteGovernanceContractTest,MesProBatchRecordRouteCandidateGovernanceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Regression result: 5 tests run, 0 failures, 0 errors, 0 skipped; Maven build success at 2026-08-14T22:09:42+08:00.

### Boundaries and Remaining Work

- ACTIVE is checked unchanged in both the positive migration and unmappable-config negative paths. Candidate publication and runtime read selection are P4 concerns and were not accepted by this P3 gate.
- No real-browser E2E was run: P3's acceptance criteria concern route snapshot construction and its explicit fail-fast behavior; the task did not supply a confirmed tenant/account and task-owned route fixture for a write-path browser run. It was not replaced with a mock or API-only success claim.

## P4 Verification And Final Regression (2026-08-15)

- Scope: P4 publish projection plus final P1-P4 regression. This section supersedes the historical `not_run`/`pending` planning entries above for automated verification status.
- Verdict: PASS for P4-AC1 through P4-AC3 at unit, database, static-contract and TypeScript levels.

### Requirement-to-Evidence Checklist

| P4 acceptance | Result | Verification evidence |
| --- | --- | --- |
| P4-AC1: published ACTIVE keeps the three configuration chains and formal identities correct | PASS | Positive projection tests restore formal `batchRecordReports`, keep `formBindings` independent, preserve START arrays, retain old permission/hash values, and create a formal scope for new client references. |
| P4-AC2: candidate does not affect ACTIVE before publish | PASS | Database tests confirm binding-only candidates reuse the ACTIVE flow graph and a route rebuild updates the same DRAFT without creating V3 or mutating ACTIVE. |
| P4-AC3: incomplete Word snapshot fails before live mutation | PASS | Negative projection tests reject missing `formBindings` or START arrays and verify no live-route mapper interaction occurs. |

### Commands And Results

- PASS: P4 executor subagent targeted combination, 7 tests run, 0 failures, 0 errors, 0 skipped.
- PASS: P4 executor subagent full `MesProRouteVersionPublishProjectionServiceImplTest`, 9 tests run, 0 failures, 0 errors, 0 skipped.
- PASS: final backend regression `MesProBatchRecordReportServiceImplDbTest` selected P1/P3 cases plus full publish projection class, 12 tests run, 0 failures, 0 errors, 0 skipped; finished 2026-08-15 04:05:48 +08:00.
- PASS: all three Word-import frontend static contracts.
- PASS: `pnpm ts:check`.

### Final Verdict

- Outcome: pass for automated P1-P4 verification.
- Verified acceptance ids: P1-AC1, P1-AC2, P1-AC3, P2-AC1, P2-AC2, P2-AC3, P2-AC4, P3-AC1, P3-AC2, P3-AC3, P3-AC4, P3-AC5, P4-AC1, P4-AC2, P4-AC3.
- Remaining environment prerequisite: real-browser write E2E was not run because no confirmed test tenant, account, and task-owned Word fixture were supplied. This is not replaced by mocks, API-only writes, direct SQL, or a default-success claim.

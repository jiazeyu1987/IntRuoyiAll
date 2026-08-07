# Execution Log

## 2026-08-07 Task Start

- 用户意图：让表单模板使用批记录表单的 Word 解析能力，并完成设计、开发和验证。
- 实施边界：共享纯 Word 结构解析；不合并业务 HTTP 接口，不改变权限、审批、版本、路线、产品绑定或 Jimu 业务语义。
- 经验门禁：真实 DOC + 合成表格双重验证；禁止模板/文件名特例；Maven 使用 reactor `-am`；状态文件串行更新；共享分支每阶段复核并发提交。
- 脏工作区基线提交：`6b3a6b816bcb881c1c2345b7674738ad38fa7303`（`chore: baseline concurrent changes before shared word parser`）。提交文件：`MesFrontlineDeviceAccountContextServiceImpl.java`、`MesTeamLeaderActiveOrderServiceTest.java`、`doc/tasks/20260807-form-template-import-dialog-layout/{task.md,execution-log.md,frontend-feature-evidence.md}`。提交后仍有并发任务新增改动，均与当前共享解析器目标文件分离并保持未暂存。
- BDD/TDD、规划、实现、独立测试和收尾证据待后续里程碑逐项记录。

## 2026-08-07 M1 Planning Gate

- Planner revision 1 resolved the shared-boundary defect: shared output is business-neutral raw Word structure; `MesProBatchRecordSharedPageTitleRules`, title decisions and `splitTemplates` remain in the MES adapter.
- `request-analysis.md` and `prd.md` approved with stable `AC-01` through `AC-16`.
- `dev-plan.md` and `test-plan.md` approved with sequential tasks `T1` through `T9`, three strict RED/GREEN gates, mandatory real `.doc` coverage and independent verification.
- Pre-execution fixture check: `pressure-pump-record.doc` exists and is readable, size `905800` bytes.
- Environment coordination: another task is currently running Maven for `yudao-module-mes` in `E:\IntRuoyi\IntRuoyiBackend`; current task will not overlap writes to the same reactor targets and will not terminate that process.

## 2026-08-07 T1 Ownership Inventory Gate

- Changed paths: `MesProBatchRecordWordParserOwnershipContractTest.java` and `shared-word-parser-ownership.json`; no production source or POM was modified by T1.
- Ownership result: the contract classifies all `39` fields across `MesProBatchRecordParsedCell`, `MesProBatchRecordParsedTable` and `MesProBatchRecordDocumentFrame`, all `86` non-synthetic `MesProBatchRecordDocParser` methods, and `6` external MES-only semantics as exactly `SHARED_RAW_STRUCTURE` or `MES_ADAPTER_SEMANTICS`. It contains no `UNKNOWN`, `MIXED` or default classification.
- MES boundary: `MesProBatchRecordSharedPageTitleRules`, `extractTemplateTitle`, `splitTemplates`, batch-specific title/split/grid normalization, route recognition, Jimu generation, `sourceSplitIndex`, `fillable`, `visualBlank`, `reviewedCellRule`, `cellRuleSource`, `placeholder`, `inputType` and `routeBSource` remain explicitly owned by the MES adapter.
- Fixture check: `src/test/resources/fixtures/pressure-pump-record.doc` is readable and non-empty (`905800` bytes); the contract test performs the same mandatory classpath check without assumptions.
- Environment: Maven ran on Eclipse Adoptium JDK `21.0.10.7` while the reactor compiler target remained Java `17`; T1 did not change the runtime or compiler configuration.
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordWordParserOwnershipContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` from `IntRuoyiBackend`.
- Attempt 1: `FAIL` before the T1 test because concurrent task source `MesTeamLeaderActiveOrderRow` was absent when MES test compilation started; the failing reference was `MesProcessPoolTeamLeaderControllerTest.java:57`. The concurrent task later created that source file.
- Attempt 2: started only after the observed MES target writer cleared, but two unrelated Maven processes began writing the same MES target during this run. The ownership build then remained in `WindowsNativeDispatcher.CreateFile0` while javac wrote MES classes for about `1998` seconds; only this task's hung Maven process was interrupted after the unrelated writers ended. No Surefire result was produced.
- Attempt 3: started after all observed backend Maven writers cleared, but the Maven session terminated after reaching `yudao-module-infra` with exit code `0` and no MES Surefire report. This incomplete reactor output is not counted as a passing test.
- Covered acceptance intent: `AC-03`, `AC-12`, `AC-16`. Static JSON parsing and `git diff --check` pass for the two T1 product paths, but the required Maven ownership gate remains unverified.
- TDD note: T1 is a pre-migration planning/contract gate and does not require a production-code RED.
- Blocker: shared-reactor target contention and abnormal Windows target file I/O prevented a complete, isolated Maven result. Impact: T1 is not complete and T2 must not start until the exact command produces a Surefire PASS; no fallback or alternate verification command was used.
- Supervisor decision: use a task-isolated Git worktree under the mandated `D:\IntRuoyiWorktree\` root for all remaining builds. This changes only execution isolation, not test commands or acceptance criteria; T1 remains incomplete until the exact Maven command reaches Surefire and passes there.

## 2026-08-07 Build Isolation

- Created worktree `D:\IntRuoyiWorktree\shared-word-parser-implementation` on branch `codex/shared-word-parser-implementation` from `int_main` commit `7d6a1a53aa3bfb3abcabfba4e1ff4ff921e2d3bf`.
- Absolute target validation passed: the worktree is a child of the mandated `D:\IntRuoyiWorktree\` root.
- Reserved `int_main` runtime slot `9` through `scripts/runtime/reserve-worktree-slot.ps1`: frontend `8090`, backend `48090`. No frontend or backend service is started for this task.
- Copied only the current task-owned `task.md`, `execution-log.md` and `task-state.json` deltas into the isolated worktree. T1 contract/test files and planning artifacts were already present in the branch baseline.
- Isolation objective: Maven commands now write only this worktree's `target` directories, so unrelated builds in `E:\IntRuoyi\IntRuoyiBackend` cannot invalidate Surefire evidence.

## 2026-08-07 T1 Isolated Re-verification

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordWordParserOwnershipContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, the ownership JSON declared `canProjectRowsToVisualGrid(List,int)` while reflection proved the current parser signature is `canProjectRowsToVisualGrid(List,int,int)`.
- Fix: corrected only the ownership resource signature; no production source or test assertion was weakened.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordWordParserOwnershipContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- Surefire evidence: `MesProBatchRecordWordParserOwnershipContractTest` ran `1` test with `0` failures, `0` errors and `0` skipped in `0.255 s`; reactor build succeeded.
- Static evidence: the ownership test contains no JUnit assumptions; targeted `git diff --check` passed.
- T1 result: completed. T2 is unblocked.

## 2026-08-07 T2 Shared Parser Contract RED

- BDD: BPM and MES share canonical structural parsing -> Given the same supported Word source / When either business adapter invokes the shared parser / Then both receive the same deterministic raw document structure before applying business semantics.
- BDD: Shared parser stays business neutral -> Given the shared parser module / When its dependencies and source are inspected / Then it contains no BPM, MES, persistence, Flowable, Jimu, batch-title, route, fillable or template-specific dependency or token.
- BDD: Invalid Word fails explicitly -> Given an empty, unsupported, corrupt, content-empty or structurally invalid Word source / When canonical parsing is requested / Then parsing fails with the corresponding typed failure code and redacted diagnostics without fallback.
- BDD: Real and synthetic fixtures prevent structural regression -> Given the tracked `pressure-pump-record.doc` and deterministic synthetic `.docx` / When canonical parsing runs repeatedly / Then DOC support is mandatory and paragraphs, frames, merges, geometry, style and diagnostics remain deterministic.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, the shared Word parser module POM is intentionally absent; Surefire ran 1 test with 1 expected failure and 0 errors/skips.
- Contract sources added before production implementation: canonical DOC/DOCX structure tests, diagnostics allowlist/hash/redaction tests, typed fail-fast error tests, and the module dependency/source neutrality test.
- Boundary staging correction: T2/T3 validates creation and neutrality of the shared module while preserving `BPM -> MES` prohibition; assertions that BPM and MES directly depend on the shared module are deferred to their adapter migration stages, matching the approved task dependency graph.
- Static evidence: targeted `git diff --check` passed; test sources contain no JUnit assumptions, absolute workspace path, filename-driven parser branch, mock success, fallback or downgraded assertion.
- Independent T2 review: not approved. It identified insufficient real-DOC assertions, geometry assertions that accepted arbitrary positive values, incomplete shared dependency/DTO neutrality checks, insufficient diagnostics redaction probes, missing corrupt `.doc`, and a fixture filename that could permit filename-specific behavior.
- Remediation: strengthened real-DOC paragraphs/frame/text/count/span/index/geometry/style assertions and filename invariance; replaced geometry lower bounds with exact synthetic values; added an exact public-record field/profile allowlist; changed shared POM validation to an exact dependency allowlist; expanded forbidden business tokens and diagnostics/error redaction probes; added corrupt `.doc` coverage. Production implementation remains absent while these contracts are revised.
- Remediation verification: targeted `git diff --check` passed. Static scans found no JUnit assumptions, absolute workspace paths, mock success or fallback; the tracked fixture path is used only to read bytes, while parser commands use opaque filenames and assert structure is filename-invariant.
- T2 result: completed after review remediation. T3 may create the module and production parser; BPM/MES adapter dependency assertions remain explicitly assigned to T5/T7/T8 because neither adapter is permitted to depend on shared before its migration stage.

## 2026-08-07 T3 Shared Canonical Parser

- Implemented the reactor module, immutable public records, single canonical profile, typed failure contract, deterministic redacted diagnostics, and business-neutral DOC/DOCX raw structure parser. BPM and MES production sources/POMs remain unchanged in this stage.
- RED: `mvn -pl yudao-module-word-parser -am "-Dtest=SharedWordDocumentParserTest,SharedWordParserDiagnosticsTest,SharedWordParserErrorContractTest,SharedWordParserPublicModelContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, the first production run exposed a real OOXML color conversion defect: schema `fill` was a byte array and rendered as an object identity instead of canonical hexadecimal `D9EAF7`; 9 tests ran with 1 failure and 0 errors/skips.
- Fix: convert OOXML byte-array fill values with deterministic hexadecimal encoding; no test assertion or expected value was changed.
- GREEN: `mvn -pl yudao-module-word-parser -am "-Dtest=SharedWordDocumentParserTest,SharedWordParserDiagnosticsTest,SharedWordParserErrorContractTest,SharedWordParserPublicModelContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- Shared Surefire evidence: 9 tests, 0 failures, 0 errors, 0 skipped. The mandatory real DOC case executed and passed together with exact synthetic DOCX geometry/style, immutable model allowlist, diagnostics and all typed failure cases.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest,MesProBatchRecordWordParserOwnershipContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- Boundary/ownership Surefire evidence: 2 tests, 0 failures, 0 errors, 0 skipped; shared dependencies are limited to POI plus test scope, source business-token scan is clean, BPM still does not depend on MES, and the migration ownership inventory remains exact.
- Static evidence: targeted `git diff --check` passed; shared production source scan found no BPM/MES/Flowable/Jimu/transaction/batch/template business token. T3 result: completed.

## 2026-08-07 T4 MES Equivalence RED

- BDD: MES real DOC output remains fully equivalent -> Given the mandatory tracked pressure-pump DOC and its migration-before snapshot / When MES parsing runs repeatedly before and after shared-parser migration / Then the complete ordered table, source/split index, title, row/column, cell, geometry, style and document-frame snapshot is identical with no skip.
- BDD: MES synthetic DOCX output remains fully equivalent -> Given a deterministic business-neutral DOCX containing paragraphs, frame, horizontal/vertical merges, exact dimensions, borders, diagonal, font and alignment / When MES parses it before and after migration / Then the complete ordered MES snapshot is identical without filename or template rules.
- Frozen legacy baselines: real DOC SHA-256 `7ba180f4392f05680539956eeb206070446f1a7924f656f378459ca24c544ee0` across 15 final tables; synthetic DOCX SHA-256 `73b0a8a568bfb178811af36d5d3b75952071a6420c6afa2d03fb56df9b3cd0a1` across 1 final table. The canonical snapshot serializer includes every current table, frame and cell field in fixed order.
- Existing tracked pressure-pump tests now hard-fail on a missing fixture instead of using JUnit assumptions; unrelated developer-local sample tests retain their existing skip behavior and are outside this task's mandatory fixture path.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordSharedParserEquivalenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, both frozen legacy snapshot tests passed while the adapter contract failed with `ClassNotFoundException` because MES does not yet depend on or constructor-inject `SharedWordDocumentParser`; 3 tests ran, 0 failures, 1 expected error, 0 skipped.
- T4 result: completed. No MES production source or POM was modified before this RED.

## 2026-08-07 T5 MES Shared Parser Adapter

- BDD: Route C normalized DOCX uses the canonical shared parser -> Given a Route C source that has been normalized to DOCX / When Route C recognition parses the normalized bytes / Then it invokes `SharedWordDocumentParser` exactly once with the normalized bytes, `.docx`, the normalized source name and `STRUCTURAL_CANONICAL`, while retaining only Route C title, split and grid business semantics locally.
- BDD: MES import rejects every typed shared-parser failure without side effects -> Given empty, unsupported, corrupt, structurally invalid or content-empty Word input / When the real `MesProBatchRecordReportServiceImpl.importPilotDoc` entry processes it / Then MES returns the exact existing business error, invokes the shared parser at most once, and performs no report, definition, version, migration, Jimu, product, route or route-binding write.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRouteCRecognizerTest,MesProBatchRecordReportServiceParserFailureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 6 expected MES test-compilation errors prove Route C has no constructor accepting `SharedWordDocumentParser` and therefore cannot satisfy the canonical delegation contract; all 24 prerequisite reactor modules passed before the MES compile gate.

- BDD: MES keeps its complete parsed-document contract -> Given the tracked real DOC and deterministic synthetic DOCX migration baselines / When the MES adapter invokes the shared canonical parser / Then every ordered table, frame, cell, merge, geometry, style, title, split index and MES business field remains byte-for-byte snapshot equivalent.
- BDD: MES owns only batch-record semantics after migration -> Given the migrated MES adapter / When its declared helpers and imports are inspected / Then shared raw POI parsing helpers are absent while MES title, split, route and grid semantics remain locally owned.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordDocParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, the first migrated implementation exposed exact legacy-contract differences in paragraph-frame null coordinates and DOCX vertical-merge logical indices.
- Fix: made raw cell coordinates nullable for paragraph-frame cells, mapped shared frames with null table coordinates, and rebased DOCX table logical indices to the exact legacy non-follower sequence inside the MES adapter. No filename, form, product or route special case was introduced.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordDocParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 17 tests with 0 failures, 0 errors and 0 skipped; real DOC hash `7ba180f4392f05680539956eeb206070446f1a7924f656f378459ca24c544ee0` and synthetic DOCX hash `73b0a8a568bfb178811af36d5d3b75952071a6420c6afa2d03fb56df9b3cd0a1` are unchanged.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordRouteARecognizerTest,MesProBatchRecordRouteBRecognizerTest,MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteERecognizerTest,MesProBatchRecordRouteFRecognizerTest" test` -> PASS, 36 tests with 0 failures/errors and 1 pre-existing optional developer-local sample skip; the mandatory tracked real-DOC equivalence path has 0 skips.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordWordParserOwnershipContractTest" test` -> FAIL, the pre-migration ownership inventory still required shared raw POI helpers to remain declared on the MES parser.
- Fix: converted the ownership inventory into a bidirectional migration gate: exact MES-owned declared helpers must remain and every shared-owned legacy raw helper must be absent.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordWordParserOwnershipContractTest" test` -> PASS, 1 test with 0 failures, 0 errors and 0 skipped.
- Regression: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordJimuReportGatewayImplTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordReportControllerTest" test` reached Surefire. The 159 non-database report/controller/gateway/layout tests passed; the database suite reported 3 failures and 17 errors, all caused by the unrelated pre-existing H2 schema mismatch `dcc_project_code.product_master_id` missing while the DCC mapper selects that column.
- Blocker handling: the DCC schema/mapper is outside this task's shared-parser ownership and was not changed, replaced or bypassed. This remains explicit T9 integration evidence and does not block the independent BPM adapter implementation.
- T5 result: completed. MES production parsing now delegates raw DOC/DOCX structure to the shared parser while preserving MES business semantics and legacy output exactly.

## 2026-08-07 T6 BPM Shared Adapter RED

- BDD: BPM recognition delegates to the shared canonical parser -> Given an ordered shared raw document / When the form-template recognizer runs / Then it forwards the original source with `STRUCTURAL_CANONICAL`, consumes paragraphs before table cells, ignores header/footer frames, and keeps the existing BPM label/code/type/required/deduplication/length/300-field rules.
- BDD: Typed shared parsing failures never persist a template -> Given corrupt, content-empty or structurally invalid Word input / When form-template import invokes recognition / Then the runtime returns the exact `FR-10` BPM business error with a stable shared failure reason and performs no template insert/update or approval submission.
- BDD: Source validation remains before parsing -> Given an empty or unsupported source / When form-template import is requested / Then the existing precise source error is raised before invoking the recognizer or persistence.
- Test setup: added only a test-scope BPM dependency on the shared module so the new tests can express the intended constructor and typed contract before production migration.
- RED: `mvn -pl yudao-module-bpm -am "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL at BPM test compilation with 6 expected errors because `DefaultWordFormTemplateRecognizer` still has only the legacy no-argument POI implementation and does not accept `SharedWordDocumentParser`; all 19 prerequisite reactor modules succeeded.
- T6 result: completed with a stable implementation-specific RED. T7 is unblocked.

## 2026-08-07 T7 BPM Shared Parser Adapter

- Implementation: replaced the BPM recognizer's direct HWPF/XWPF traversal with constructor-injected `SharedWordDocumentParser`, `WordParseProfile.STRUCTURAL_CANONICAL`, and ordered raw paragraph/table-cell mapping. Existing BPM clean-blank, deduplication, 80-character filter, code/type/required inference and 300-field limit remain BPM-owned.
- Failure semantics: typed shared failures preserve both the exact `FR-10` BPM business error and their stable failure-code names; an empty recognized label set maps to `NO_RECOGNIZABLE_FIELD_LABELS`; unexpected runtime failures propagate instead of being swallowed or converted to default success.
- Dependency/configuration: BPM now directly depends on the shared module and no longer declares direct POI dependencies. BPM and MES each expose only their business adapter bean and privately construct the same `DefaultSharedWordDocumentParser`, avoiding cross-business bean ownership and duplicate `SharedWordDocumentParser` beans.
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests with 0 failures, 0 errors and 0 skipped.
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest,FormCenterRuntimeContractTest,FormTemplateLifecycleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 28 tests with 0 failures, 0 errors and 0 skipped.
- Static evidence: BPM/MES migrated adapter sources contain no POI/HWPF/XWPF/WordExtractor token; production sources contain no no-argument adapter construction; targeted `git diff --check` passed with only existing line-ending conversion warnings.
- T7 result: completed. T8 cross-module contracts and integration regression are unblocked.

## 2026-08-07 T8 Cross-Module Contracts And Integration Regression

- Static/API contract: added `shared-word-parser-api-contract-static.spec.js`, which isolates the three frontend API methods and proves their original upload URLs remain independent with no cross-business retry, catch or fallback branch.
- Boundary contract: strengthened direct dependency checks for BPM/MES -> shared, forbids BPM direct POI parser dependencies and raw POI traversal in both adapters, and verifies both adapters use `STRUCTURAL_CANONICAL`.
- Bean isolation: added a two-configuration Spring context test proving exactly one BPM recognizer bean, one MES parser bean and zero exposed `SharedWordDocumentParser` beans, preventing cross-business ownership and duplicate-bean ambiguity.
- Controller contract: extended MES reflection coverage for `/upload-extra-slot` and its `file`, `batchRecordName` and `formSlotType` parameter names; existing BPM import and MES recognize-uploaded contracts remain unchanged.
- GREEN: `node tests/e2e/shared-word-parser-api-contract-static.spec.js` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest,MesProBatchRecordWordParserOwnershipContractTest,MesProBatchRecordReportControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests with 0 failures/errors/skips.
- GREEN: `mvn -pl "yudao-module-word-parser,yudao-module-bpm,yudao-module-mes" -am "-Dtest=SharedWordDocumentParserTest,SharedWordParserDiagnosticsTest,SharedWordParserErrorContractTest,DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest,FormCenterRuntimeContractTest,MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordDocParserTest,MesProBatchRecordReportControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, shared 8 + BPM 23 + MES 26 = 57 tests with 0 failures/errors/skips.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, final boundary and Spring context isolation 2 tests with 0 failures/errors/skips.
- GREEN: `mvn -pl yudao-module-word-parser "-Dtest=SharedWordDocumentParserTest,SharedWordParserDiagnosticsTest,SharedWordParserErrorContractTest,SharedWordParserPublicModelContractTest" test` -> PASS, 9 tests with 0 failures/errors/skips.
- GREEN: final MES route regression -> PASS, 36 tests with 0 failures/errors and 1 pre-existing optional developer-local sample skip; mandatory tracked fixture tests are not skipped.
- GREEN: final MES non-database report regression -> PASS, 159 tests with 0 failures/errors/skips.
- GREEN: backend API evidence validator -> PASS, `Backend API evidence is valid.`
- Static evidence: targeted `git diff --check` passed with line-ending warnings only; required shared/BPM/MES tests contain no JUnit assumption. T8 result: completed.

## 2026-08-07 T8 Corrective FR-10 Error Mapping Loop

- BDD: BPM preserves the approved business error for every shared parser failure -> Given any of the five typed shared parser failure conditions / When the BPM recognizer and import lifecycle handle the failure / Then they expose the exact `FR-10` BPM error and persist no template.
- BDD: MES preserves the approved business error for every shared parser failure -> Given any of the five typed shared parser failure conditions / When the MES adapter handles the failure / Then it throws the exact `FR-10` MES error instead of a generic parse error.
- RED: `mvn -pl yudao-module-bpm -am "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest,FormTemplateLifecycleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because `FormTemplateRecognition` does not yet carry a business failure code and has no typed failure factory; all 19 prerequisite reactor modules succeeded before the BPM test-compilation failure.
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest,FormTemplateLifecycleServiceTest" test` -> PASS, 20 tests with 0 failures, 0 errors and 0 skipped; corrupt Word now remains `TEMPLATE_SOURCE_INVALID`, unsupported remains `TEMPLATE_SOURCE_TYPE_UNSUPPORTED`, and structure/content recognition failures remain `TEMPLATE_RECOGNITION_FAILED` through both import lifecycles.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDocParserTest" test` -> FAIL, 15 tests ran and the new five-condition adapter contract proved `EMPTY_SOURCE` incorrectly returned generic parse error `1040509004` instead of `PRO_BATCH_RECORD_REPORT_FILE_EMPTY` `1040509001`.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDocParserTest" test` -> PASS, 15 tests with 0 failures, 0 errors and 0 skipped; all five shared failure conditions map to the exact MES business errors from `FR-10`.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordDocParserTest" test` -> PASS, 19 tests with 0 failures, 0 errors and 0 skipped; both adapters produced identical canonical structural content and source diagnostics from the mandatory real DOC while keeping their independent original-filename diagnostics.
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest,FormTemplateLifecycleServiceTest,FormCenterRuntimeContractTest" test` -> PASS, 29 tests with 0 failures, 0 errors and 0 skipped.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=SharedWordParserModuleBoundaryTest,MesProBatchRecordWordParserOwnershipContractTest,MesProBatchRecordReportControllerTest,MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordDocParserTest" test` -> PASS, 31 tests with 0 failures, 0 errors and 0 skipped.
- GREEN: frontend static API contract and backend API evidence validator both passed; `git diff --check` completed with line-ending warnings only.
- T8 corrective result: completed. Exact FR-10 mappings, no-persistence behavior and same-real-DOC canonical consumption are now explicit regression contracts. T9 independent verification is unblocked.

## 2026-08-07 Project Experience Consolidation

- `project-experience-consolidation` routed the reusable lesson to the existing `docs/system/shared-word-template-parser-design.md`; no new long-term document was created.
- Added a preflight gate requiring typed business errors to survive intermediate result objects and requiring real-source canonical equality to compare structural content/source hash while keeping adapter-specific `fileNameHash` independent.
- Added matching keywords to `docs/experience-index.md` so future shared-parser and adapter error-mapping tasks resolve this gate before implementation.

## 2026-08-07 T8 Corrective Global Raw-Parser Boundary

- BDD: All BPM and MES production Word paths use the canonical shared parser -> Given the BPM form-template runtime and MES batch-record-report production source trees / When the module boundary contract scans every Java source and the three known business Word adapters / Then no raw HWPF/XWPF document traversal exists, every adapter explicitly uses `SharedWordDocumentParser` with `STRUCTURAL_CANONICAL`, and Spring registers Route C without exposing a shared-parser bean.
- RED: independent T9 Q12 current-source static scan -> FAIL, `SharedWordParserModuleBoundaryTest` inspected only `DefaultWordFormTemplateRecognizer` and `MesProBatchRecordDocParser`, so the runnable Route C XWPF parser escaped the automated boundary despite AC-12 requiring global uniqueness.
- Verification attempt: `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest,MesProBatchRecordRouteCRecognizerTest,MesProBatchRecordReportServiceParserFailureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` under Java `17.0.20` -> BLOCKED before Surefire. MES test compilation fails at unchanged baseline `MesProBatchRecordExecutionFieldResponsibilityMapperTest.java:246` because Java 17 `List` has no `getFirst()` method.
- Blocker handling: the unrelated baseline test was not modified, Java 21 was not substituted, stale compiled tests were not accepted, and test compilation was not skipped. T8 remains blocked until the Java 17 test prerequisite is corrected and the unchanged command reaches Surefire.

## 2026-08-07 T5 Corrective AC-09 And AC-12

- Implementation: removed Route C's executable XWPF document/table/row/cell traversal and injected `SharedWordDocumentParser`; normalized DOCX bytes now enter exactly one `WordParseCommand` with `.docx`, the normalized source name and `WordParseProfile.STRUCTURAL_CANONICAL`. Route C still owns title extraction, template splitting, header normalization, wide-grid compression and its legacy font, alignment, width and height defaults.
- Configuration: `MesProBatchRecordWordParserConfiguration` explicitly constructs the canonical parser for both the normal MES adapter and Route C without exposing a cross-business `SharedWordDocumentParser` bean.
- Service contract: added five isolated tests through the real `MesProBatchRecordReportServiceImpl.importPilotDoc` entry. Empty and unsupported files stop before parsing; corrupt, invalid-table and no-content failures invoke the shared parser exactly once. Every case asserts the exact MES error and zero report, definition, version, migration, approval-event, Jimu, product, route and route-binding interactions.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRouteCRecognizerTest,MesProBatchRecordReportServiceParserFailureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 12 tests with 0 failures, 0 errors and 0 skipped (`7` Route C plus `5` service failure contracts).
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest,MesProBatchRecordWordParserOwnershipContractTest,MesProBatchRecordDocParserTest,MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordRouteCRecognizerTest,MesProBatchRecordReportServiceParserFailureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 34 tests with 0 failures, 0 errors and 0 skipped.
- Static evidence: Route C production source contains no `org.apache.poi`, `XWPFDocument`, `document.getTables()`, `HWPF` or `WordExtractor` token and contains both `SharedWordDocumentParser` and `WordParseProfile.STRUCTURAL_CANONICAL`; targeted `git diff --check` passed with line-ending conversion warnings only.
- T5 corrective result: implementation and executor verification are complete for `AC-09` and `AC-12`; the milestone is ready for independent tester review. The unrelated DCC H2 prerequisite was not invoked or modified.

## 2026-08-07 Authorized Corrective Prerequisites

- User authorization: continue with two narrowly scoped prerequisite repairs: Java 17 test compilation compatibility and MES H2 DCC schema alignment. Production Word parser behavior, production database schema, API URLs, permissions and no-fallback policy remain unchanged.
- BDD: Java 17 boundary regression can reach Surefire -> Given the existing T8 boundary command runs on Java `17.0.20` / When Maven compiles MES tests / Then no baseline test may use Java 21-only `List.getFirst()` and the mapped boundary tests must execute in Surefire.
- BDD: MES DCC H2 fixture matches the mapper contract -> Given the MES DB regression bootstraps `yudao-module-mes/src/test/resources/sql/create_tables.sql` and the DCC mapper selects `dcc_project_code.product_master_id` / When the fixture is loaded and report DB tests run / Then the column exists as nullable `BIGINT`, the query executes, and no production schema/data is changed.
- RED: T8 boundary command under Java `17.0.20` -> FAIL, testCompile stops at `MesProBatchRecordExecutionFieldResponsibilityMapperTest.java:246` because Java 17 `List` has no `getFirst()`; Surefire is not reached.
- RED: MES report DB regression -> FAIL, H2 reports missing `dcc_project_code.product_master_id`; approximately twenty DB service cases are blocked by the same test-fixture/schema mismatch.
- Corrective scope rule: T10 may modify only the baseline Java test expression; T11 may modify only the MES H2 fixture and its schema contract evidence/test. No Java 21, stale class, skipped test, alternate datasource, mock success or production schema workaround is permitted.

## 2026-08-07 T10 Java 17 Test Baseline Correction

- Implementation: replaced the Java 21-only `tenantInterceptors.getFirst()` call in `MesProBatchRecordExecutionFieldResponsibilityMapperTest` with Java 17-equivalent `tenantInterceptors.get(0)`. The assertion target and collection cardinality assertion remain unchanged.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest,MesProBatchRecordRouteCRecognizerTest,MesProBatchRecordReportServiceParserFailureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` under Java `17.0.20` -> PASS, 14 tests with 0 failures, 0 errors and 0 skipped.
- Result: T10 completed. The previous testCompile blocker is cleared without switching to Java 21, skipping tests, changing Maven compiler settings or touching production code.

## 2026-08-07 T11 MES H2 DCC Schema Fixture Alignment

- Schema evidence: DCC module test schema already defines `dcc_project_code.product_master_id BIGINT NULL`; `DccProjectCodeDO` contains `productMasterId`; runtime migration `20260803_dcc_product_onboarding_flow.sql` adds the same production column. MES H2 fixture was the only missing schema surface.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesBatchRecordBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 1 test reached Surefire and failed with `Missing column dcc_project_code.product_master_id in test schema`.
- Implementation: added nullable `product_master_id` to the MES H2 `dcc_project_code` fixture and kept the explicit schema contract assertion in `MesBatchRecordBaseSchemaTest`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesBatchRecordBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test with 0 failures, 0 errors and 0 skipped.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 110 tests with 0 failures, 0 errors and 0 skipped; the prior DCC H2 missing-column blocker is cleared.
- Result: T11 completed. This changed only MES test fixture/contract/evidence and did not modify production DCC schema, runtime data, mapper behavior or parser logic.

## 2026-08-07 T8 Corrective Combined Regression

- GREEN: `mvn -pl "yudao-module-word-parser,yudao-module-bpm,yudao-module-mes" -am "-Dtest=SharedWordDocumentParserTest,SharedWordParserDiagnosticsTest,SharedWordParserErrorContractTest,DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest,FormCenterRuntimeContractTest,MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordDocParserTest,MesProBatchRecordReportControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, shared 8 + BPM 23 + MES 28 = 59 tests with 0 failures, 0 errors and 0 skipped.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRouteARecognizerTest,MesProBatchRecordRouteBRecognizerTest,MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteERecognizerTest,MesProBatchRecordRouteFRecognizerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 36 tests with 0 failures, 0 errors and 1 pre-existing optional developer-local sample skip. Mandatory tracked fixture and route regression cases are not skipped.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordJimuReportGatewayImplTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordReportControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 269 tests with 0 failures, 0 errors and 0 skipped.
- GREEN: `node tests\e2e\shared-word-parser-api-contract-static.spec.js` from `IntRuoyiFronted` -> PASS, `shared Word parser keeps all three business API contracts independent`.
- GREEN: backend API evidence validator -> PASS, `Backend API evidence is valid.`
- GREEN: database schema evidence validator -> PASS, `Database schema evidence is valid.`
- GREEN: `git diff --check` -> PASS with line-ending conversion warnings only and no whitespace error.
- GREEN: untracked task-owned file whitespace check using `git -c core.autocrlf=false diff --no-index --check -- NUL <file>` -> PASS for 26 files.
- Surefire XML audit: shared/BPM/MES/report/DB/controller/route/parser-failure/schema reports all show 0 failures and 0 errors; the only skip remains the optional Route B developer-local sample and is not used as mandatory evidence.
- T8 result: completed. The previous Java 17 and DCC H2 blockers are cleared without Java/runtime downgrade, stale class use, skip, mock success, fallback parser, production schema change or API behavior change.

## 2026-08-07 T9 Independent Verification

- Independent decision: GO / PASS. `test-report.md` now records `PASS=16`, `FAIL=0`, `BLOCKED=0` for `AC-01` through `AC-16`.
- `AC-08` and `AC-15` are now cleared by the T11 schema contract, 110-test DB regression, 269-test report/Jimu/DB/controller regression and combined Java 17 reactor verification.
- `AC-09` and `AC-12` remain cleared by the Route C shared-parser migration, five-condition MES service failure suite, BPM FR-10 mapping tests and production static scans.
- No fallback decision: no old parser fallback, no broad exception-to-success branch, no API cross-retry, no skipped required fixture, no mock success and no production DB/schema workaround were introduced.
- Verification report: `verification-report.md` archives the validator PASS results and final gate summary so temporary evidence files can be safely considered for cleanup.
- Current status: ready_for_closeout. Remaining work is task-owned cleanup preview/apply and final status archival; Git commit/push is not performed unless explicitly requested under the current project policy.

## 2026-08-07 Closeout Cleanup

- Experience consolidation: updated existing long-term documents instead of creating new ones. Added Java 17 test API baseline guidance to `docs/powershell-memory.md`; expanded DCC project-code MDM schema fixture guidance in `docs/database-rules.md`; added routing keywords in `docs/experience-index.md`.
- Cleanup preview 1: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260807-shared-word-parser-implementation --mode preview` -> BLOCKED because linked-worktree closeout would require ff-only merge into `int_main`, the main worktree `E:\IntRuoyi` is dirty, and the current project Git policy does not authorize implicit commit/merge/push.
- Cleanup preview 2: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260807-shared-word-parser-implementation --mode preview --worktree-closeout off` -> READY. Keep set: `task.md`, `execution-log.md`, `test-report.md`, `verification-report.md`; delete set: task-local planning/intermediate evidence files.
- Cleanup apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260807-shared-word-parser-implementation --mode apply --worktree-closeout off` -> APPLIED. Deleted task-local intermediate files: `backend-api-evidence.md`, `database-schema-evidence.md`, `dev-plan.md`, `prd.md`, `request-analysis.md`, `task-state.json`, `test-plan.md`.
- Preserved records: `task.md`, `execution-log.md`, `test-report.md`, `verification-report.md`.
- User follow-up: 用户发送“继续”，按上一轮最终提示解释为授权继续任务分支集成收尾。
- Git/worktree preflight: re-read `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`, `docs/powershell-memory.md` and `docs/task-closeout-rules.md`.
- Branch runtime guard: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for `codex/shared-word-parser-implementation/int_main`, frontend `8090`, backend `48090`.
- Main worktree gate: `git -C E:\IntRuoyi status --short --branch` shows `int_main...origin/int_main [ahead 4]` plus many unrelated dirty ERP/MES/frontend/task changes. No baseline, merge, reset, cleanup or worktree deletion was performed against the main worktree.
- Implementation commit: `b8817ffd8 feat: share canonical word parser across BPM and MES`; staged implementation/code/test diff passed `git diff --cached --check` before commit and branch-runtime-port guard passed during commit hook.
- Closeout commit scope: this surviving task documentation plus `docs/database-rules.md`, `docs/experience-index.md`, `docs/powershell-memory.md`, and `docs/system/shared-word-template-parser-design.md`. It intentionally does not touch main-worktree unrelated changes.
- Final status: completed; current task branch is ready to push. `int_main` merge and linked worktree removal remain blocked until the main worktree is clean or the user separately authorizes handling its unrelated dirty state.

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

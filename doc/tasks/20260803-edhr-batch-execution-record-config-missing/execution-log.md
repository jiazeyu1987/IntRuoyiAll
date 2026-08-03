# Execution Log

## User Intent

- 用户反馈：`eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录`。
- 初始判断：问题命中 eDHR 批次执行的正式批记录配置来源链路，需用 BDD + RED/GREEN 修复，禁止用 `formBindings`、默认 `MAIN`、工序开始配置或前端文案替代正式批记录绑定。

## Rule And Experience Reads

- Read: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- Read: `docs\task-closeout-rules.md`
- Read: `docs\backend-development.md`
- Read: `docs\frontend-development.md`
- Read: `docs\e2e-rules.md`
- Read: `docs\experience-index.md`
- Applicable gate: 工艺路线三类配置术语契约；eDHR 批次任务配置来源门禁；eDHR 管理员主区域已提交内容门禁。

## BDD

- BDD: 历史批次提交内容读取使用正式批记录配置 -> Given 历史 eDHR 批次的冻结快照缺少完整 `flowGraph.nodes` 或 `batchUseConfigs` 但当前 BATCH 工序配置完整覆盖任务工序, When 批记录管理员读取 `review-timeline` 或批次详情提交内容, Then 后端应使用正式当前 BATCH 配置恢复批记录任务上下文并返回可读内容, And 不得用 `formBindings`、默认 `MAIN`、工序开始配置或空绑定替代正式逐工序批记录表单。
- BDD: 正式批记录配置确实缺失时 fail fast -> Given 冻结快照和当前 BATCH 工序配置都无法完整覆盖批次任务工序, When 批次执行读取任务上下文, Then 后端应返回明确缺失配置错误, And 不得静默返回空任务、默认成功或 mock 内容。

## Command Log

- Command intent: `git status --short --branch` -> observed existing unrelated dirty files before this task; will preserve and avoid unrelated edits.
- Command intent: `rg` search for target error/config keys -> located backend error constant, `MesProEdhrBatchExecutionServiceImpl`, existing `MesProEdhrBatchExecutionServiceTest`, and related eDHR gates.

## RED / Implementation / Verification

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected recovered `taskTotal=6` but actual was `4`, proving historical batches with incomplete frozen task config did not recover formal route batch record tasks.
- Implementation: updated `MesProEdhrBatchExecutionServiceImpl#getReviewTimeline` to run `syncIfActive(batch)` under `@Transactional(rollbackFor = Exception.class)` before reading timeline tasks, then reselect the latest batch and tasks. This reuses the existing formal route task recovery path and preserves fail-fast behavior when neither current BATCH config nor frozen config can provide formal batch record bindings.
- Regression: added `MesProEdhrBatchExecutionServiceTest#getReviewTimeline_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete`; the test inserts a historical special-only batch with incomplete frozen `flowGraph.nodes` / `batchUseConfigs`, keeps formal current BATCH process config, calls `getReviewTimeline`, and asserts six task events plus persisted route batch record report IDs from the formal per-process bindings.
- Verification attempt: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED, task-owned Maven PID stayed in javac class-file close with no fresh Surefire report; process was stopped after diagnostic sampling.
- Verification attempt: reran the same `-am` target with local JVM resource limits -> BLOCKED, upstream reactor stalled in Maven filesystem/dependency scanning before reaching `yudao-module-mes`; no fresh Surefire report was produced.
- Verification attempt: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> FAIL before tests, compile blocker in `MesProScheduleCalendarServiceImpl.java:[448,87]` because `MesProScheduleCalendarDayDetailRespVO.LineDetailItem.LineDetailItemBuilder` class file was inaccessible/missing.
- Diagnostic: source `MesProScheduleCalendarDayDetailRespVO.LineDetailItem` already has `@Builder`, so the compile blocker appears tied to stale/corrupt target/generated class output rather than this task's eDHR source change.
- Verification attempt: `mvn -pl yudao-module-mes clean ... test` -> BLOCKED, Maven clean stuck in documented Windows `java.io.WinNTFileSystem.delete0` target deletion path; stopped only the task-owned Maven JVM after thread dump.
- Verification attempt: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete+getDetail_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> BLOCKED, Maven/JDK exited with code `-1` shortly after `javac` started compiling MES sources and produced no new Surefire report.
- Static verification: `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java` -> PASS; only Git CRLF normalization warnings.
- Evidence validation: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260803-edhr-batch-execution-record-config-missing\bug-regression-evidence.md` -> PASS. The evidence file explicitly marks GREEN as blocked and does not claim a passing JUnit.
- Experience consolidation: checked `project-experience-consolidation`; existing `docs\powershell-memory.md#Maven 目标目录文件系统异常门禁` already covers Maven `target` corruption / `WinNTFileSystem.delete0` handling, and that document currently has unrelated dirty edits, so no additional long-term memory write was made.

## Blockers

- Required GREEN is not available yet because local `yudao-module-mes` Maven verification is blocked by build-output/JDK filesystem issues before Surefire can execute the eDHR tests.
- No implementation commit/push performed because required verification did not pass and the workspace also contains unrelated dirty changes from other tasks.

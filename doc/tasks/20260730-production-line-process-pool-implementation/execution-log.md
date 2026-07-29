# Execution Log

## 2026-07-30 Bootstrap

- Task id: 20260730-production-line-process-pool-implementation
- User intent: 启动 6 个子 agent，分别在 6 个 worktree 实现和验证 F1/F2/F3/F4/F7/F8，主线程 review 后融合进 `int_main`。
- Rules read: `docs/worktree-restrictions.md`, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, `docs/powershell-memory.md`, `docs/backend-development.md`, `docs/frontend-development.md`, `docs/e2e-rules.md`, `docs/database-rules.md`, `docs/branch-runtime-ports.md`, `docs/local-runtime.md`, `docs/login-access.md`.
- Experience index: `docs/experience-index.md` exists. Applicable gates copied into `task.md`: worktree, PowerShell, backend, frontend, database, E2E, no-fallback, batch-record terminology.
- BDD: 生产一线报工工序池 21 条门禁 -> Given 已放行验收文档和当前 `int_main` 代码；When 6 个功能点分别实现验证并融合；Then R01-R21 全部由代码、测试和主线程 review 证据证明。
- Current git state before task docs: `## int_main...origin/int_main`, clean.
- Current worktree evidence: `git worktree list` shows existing worktrees under `D:\IntRuoyiWorktree`; new worktree names must avoid collisions.
- Port registry evidence: `D:\IntRuoyiWorktree\.ports\worktree-ports.json` exists; current active `int_main` slots leave limited free runtime slots, so new worktrees will not start services until slots are safely reserved.
- Command note: initial UTF-8 validation used Bash heredoc syntax and failed in PowerShell with `Missing file specification after redirection operator`; command was corrected to PowerShell here-string piped to `python -X utf8 -`.
- Verification: `python -X utf8 -` UTF-8 task-doc read -> PASS, `TASK_DOCS_UTF8_OK`.
- Verification: `git diff --check -- doc\tasks\20260730-production-line-process-pool-implementation` -> PASS.

## 2026-07-30 F7 FIFO Execution

- Task role: F7 execution agent for branch `codex/20260730-ppool-f7-fifo` in `D:\IntRuoyiWorktree\20260730-ppool-f7-fifo`.
- User intent: 实现并验证 F7 生产工单 FIFO 分配基础逻辑；优先覆盖 T15-T17；只修改 `IntRuoyiBackend/yudao-module-mes`、`IntRuoyiBackend/sql/mysql` 和本 `execution-log.md`。
- Rules read: `AGENTS.md`, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, `docs/powershell-memory.md`, `docs/backend-development.md`, `docs/database-rules.md`, `docs/acceptance/production-line-process-pool/bdd-scenarios.md`, `docs/acceptance/production-line-process-pool/tdd-plan.md`, `doc/tasks/20260730-production-line-process-pool-implementation/dev-plan.md`, `doc/tasks/20260730-production-line-process-pool-implementation/test-plan.md`.
- Experience gate: `docs/experience-index.md` exists; matched gates are already covered by `docs/powershell-memory.md#PowerShell Maven -D 参数引号门禁`, `docs/backend-development.md#2026-07-25 Maven Reactor 兄弟模块验证门禁`, and `docs/task-closeout-rules.md#验收范围变更门禁`.
- Git preflight: `git status --short --branch` -> `## codex/20260730-ppool-f7-fifo`, clean; `git branch --show-current` -> `codex/20260730-ppool-f7-fifo`; `origin` fetch/push -> `https://github.com/jiazeyu1987/IntRuoyiAll.git`.
- BDD: F7 生产工单按计划开始时间 FIFO 分配 -> Given 工序池存在可分配数量片段且候选目标均为生产工单；When 执行 FIFO 分配；Then 只按生产工单 `plannedStartTime ASC` 分配，不读取或写入排产目标字段。
- BDD: F7 缺少计划开始时间阻塞 -> Given 候选生产工单缺少 `plannedStartTime`；When 执行 FIFO 分配；Then 本次分配整体失败且不写入分配明细，不 fallback 到创建时间、工单号、当前时间或排产字段。
- BDD: F7 分配明细和片段锁定 -> Given 工序池事件产生多个数量片段；When FIFO 分配到生产工单；Then 明细可追溯来源工序池、来源提交事件、来源数量片段、目标生产工单、目标工序和数量；已分配片段禁止修改影响数量、质量状态或可分配状态的原始字段。
- BDD: F7 多人多次提交累计完成 -> Given 多名员工多次提交独立事件数量；When 计算生产工单目标工序完成状态；Then 按数量累计判定完成，不要求同一员工提交。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest#shouldAllocateWorkOrdersByPlannedStartTime,MesProcessPoolFifoAllocationServiceTest#shouldBlockWhenPlannedStartTimeIsMissing" test` -> FAIL, reactor sibling modules had no matching tests and required `-Dsurefire.failIfNoSpecifiedTests=false`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest#shouldAllocateWorkOrdersByPlannedStartTime,MesProcessPoolFifoAllocationServiceTest#shouldBlockWhenPlannedStartTimeIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected missing F7 service/model/mapper classes.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest#shouldPersistAllocationLinesFromEventFragmentsToWorkOrder,MesProcessPoolAllocatedFragmentLockTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected missing F7 allocation detail and allocated-fragment lock classes.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolCompletionCalculatorTest,MesProcessPoolFifoAllocationConcurrencyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected missing F7 cumulative-completion and allocation concurrency classes.
- Implementation: 新增 F7 FIFO 分配服务、分配命令/结果/目标生产工单/来源数量片段模型、分配明细 DO/Mapper、累计完成计算器、片段原始字段锁定枚举和 MySQL/H2 分配明细表契约。
- Scope guard: 生产代码未引入前端、排产系统、工单号/创建时间/当前时间 fallback；计划开始时间缺失或并列均 fail-fast。
- F1 dependency note: 当前 worktree 未包含 F1 正式工序池事件/数量片段持久模型；F7 以服务入参和分配明细契约实现基础逻辑，后续融合 F1 时需把真实片段行纳入同一事务锁定。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest#shouldAllocateWorkOrdersByPlannedStartTime,MesProcessPoolFifoAllocationServiceTest#shouldBlockWhenPlannedStartTimeIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN attempt: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest#shouldPersistAllocationLinesFromEventFragmentsToWorkOrder,MesProcessPoolAllocatedFragmentLockTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, Mockito strict unused stub exposed that non-allocation-affecting lock path did not read allocation count; adjusted service to read count once and only throw for affecting fields.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest#shouldPersistAllocationLinesFromEventFragmentsToWorkOrder,MesProcessPoolAllocatedFragmentLockTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolCompletionCalculatorTest,MesProcessPoolFifoAllocationConcurrencyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest,MesProcessPoolAllocatedFragmentLockTest,MesProcessPoolCompletionCalculatorTest,MesProcessPoolFifoAllocationConcurrencyTest,MesProcessPoolFifoAllocationSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.
- REGRESSION: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest#shouldBlockWhenPlannedStartTimeTieNeedsSecondarySortRule" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test; protects the no secondary FIFO fallback contract.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest,MesProcessPoolAllocatedFragmentLockTest,MesProcessPoolCompletionCalculatorTest,MesProcessPoolFifoAllocationConcurrencyTest,MesProcessPoolFifoAllocationSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests.
- REGRESSION: `git diff --check` -> PASS; Git only reported LF-to-CRLF working-copy warnings.
- Experience consolidation: `project-experience-consolidation` skill read; existing `docs/powershell-memory.md` and `docs/backend-development.md` already cover the recurring Maven/worktree gates used by this task, and no new long-term experience document was changed under the F7 write-scope restriction.
- Current status: ready_for_closeout; implementation and scoped verification passed, commit/push pending.

## 2026-07-30 F7 Main Review Fixes

- Review fix: FIFO allocation table name changed to `mes_pro_process_pool_fifo_allocation_line` so it aligns with the F1 dedicated process pool table namespace.
- Verification: `rg -n "mes_process_pool_fifo_allocation_line" IntRuoyiBackend` -> no matches after the table-name fix.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationSchemaTest,MesProcessPoolFifoAllocationServiceTest,MesProcessPoolFifoAllocationConcurrencyTest,MesProcessPoolCompletionCalculatorTest,MesProcessPoolAllocatedFragmentLockTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests.
- Blocker note: `python -m pytest IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` could not run in this F7 worktree because the F7 branch was cut from pre-F1 baseline and does not contain the F1 pytest file. Mainline merge verification must rerun SQL contracts after F1+F7 are together.

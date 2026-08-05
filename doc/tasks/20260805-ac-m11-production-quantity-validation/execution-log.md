# AC-M11 生产报工数量与损耗边界校验执行日志

## User Intent

- 用户要求继续修复岗位需求矩阵中从系统代码分析发现的不符合项。
- 本轮选择 AC-M11 的窄切片：生产员工正式报工数量与损耗边界校验。

## BDD / TDD

- BDD: 拒绝损耗大于产出 -> Given 生产员工提交产出数量 10 且损耗数量 11, When 后端处理正式生产报工, Then 服务端拒绝提交且不得用 0 合格数量截断后继续生成报工/批记录/过程池事件。
- BDD: 拒绝负数数量 -> Given 生产员工提交负数产出或负数损耗, When 后端处理正式生产报工, Then 服务端拒绝提交并暴露真实校验错误。
- BDD: 合法损耗形成合格数量 -> Given 生产员工提交产出数量 10 且损耗数量 3, When 后端拆分生产报工 payload, Then 合格数量为 7 且损耗数量为 3。

## Gate Evidence

- 2026-08-05: 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/experience-index.md`。
- 2026-08-05: 已读取技能 `bug-regression-fix-loop`、`backend-api-delivery`、`bdd-tdd-acceptance-planner` 及其引用契约。

## Milestone Log

- 2026-08-05: 创建任务目录和最小任务文档，当前状态为 `in_progress`。
- 2026-08-05: 复核 `MesProFrontlineFeedbackSubmitServiceImpl`、`MesProFrontlineFeedbackPayloadSplitter`、`MesProFrontlineFeedbackPayloadReqVO`、`MesProFrontlineFeedbackSubmitServiceTest` 和 `MesProFrontlineFeedbackPayloadSplitterTest`，确认原缺口为提交服务未校验 `loss <= output`、拆分器用 `.max(BigDecimal.ZERO)` 截断合格数量。
- 2026-08-05: 新增 `MesProFrontlineFeedbackSubmitServiceTest` 负向用例，覆盖损耗大于产出、负数产出、负数损耗，要求在授权、幂等查询和写入前 fail-fast。
- 2026-08-05: 修复 `MesProFrontlineFeedbackSubmitServiceImpl`，新增 `validateProductionQuantity`；修复 `MesProFrontlineFeedbackPayloadSplitter`，移除合格数量 0 截断。
- 2026-08-05: 更新矩阵分析 `doc/tasks/20260805-job-matrix-compliance/non-compliance-analysis.md`，将 AC-M11 标记为“数量/损耗边界代码级已修复”，但整体仍不完全符合。
- 2026-08-05: 复核 `git log --oneline -5` / `git show --name-status --oneline 5e0acef75`，确认共享分支并发基线提交 `5e0acef75 chore: preserve concurrent workspace before AC-M18 closeout docs` 已吸收本切片部分文件（含 AC-M11 任务文档与后端提交服务变更）并混有其它任务文件；本任务不改写历史、不宽泛暂存，后续提交需按共享分支并发基线提交门禁处理。

## Verification Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL。关键失败：`lossQuantity=11.000`、`outputQuantity=10.000` 时仍调用 `feedbackService.createFeedback(...)`，并生成 `qualifiedQuantity=0`；负数数量也未在校验阶段 fail-fast。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，9 tests, 0 failures, 0 errors。
- FINAL GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2026-08-05 15:20:16，9 tests, 0 failures, 0 errors。
- DIFF CHECK: `git diff --check -- <AC-M11 task-owned paths>` -> PASS；仅提示 LF/CRLF warning，无 whitespace 错误。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-m11-production-quantity-validation --mode preview` -> READY，keep 3，delete/blocked/warnings 均为 `<none>`。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-m11-production-quantity-validation --mode apply` -> APPLIED，无删除项。
- Timeout handling: 一次 RED 重跑在 120s 超时，确认 PID 49916/37744 属于本任务 Maven 后仅停止该残留，未触碰其它 worktree Maven 或运行态进程。
- Experience consolidation: 已按 `project-experience-consolidation` 检查，经验点“共享分支并发基线提交吸收当前任务改动”已由 `docs/powershell-memory.md#共享分支并发基线提交门禁` 和 `docs/experience-index.md` 覆盖，本轮不新增长期经验文档。

## Blockers

- 无 AC-M11 完成门禁阻塞。共享工作区仍有其它任务脏改动，本切片不宽泛暂存、提交或回滚。

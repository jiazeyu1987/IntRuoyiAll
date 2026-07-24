# Execution Log

## User Intent

用户确认继续处理：工艺路线里绑定的损耗单配置了填写人，但批次详情右侧单据卡片仍显示未配置。要求继续修复。

## Evidence Before Fix

- Runtime read-only query: `EDHRB-1784855561493` 的路线版本 `routeVersionId=358` 中，粗洗/精洗/清洗工序绑定的“损耗单”均配置 `candidateSourceNames=["张可莹（zhangkeying）"]`、`candidateSourceIds=[152]`。
- Runtime read-only query: 同一批次详情接口中 3 个“损耗单”任务 `fillableUsers=[]`。

## BDD

BDD: 动态表单任务显示工艺路线绑定填写人 -> Given 工艺路线工序绑定的损耗单配置了填写人, When 用户打开批次执行详情, Then 对应损耗单任务 `fillableUsers` 必须返回该配置人员，供右侧单据卡片显示。

BDD: 主生产表任务填写人逻辑不被破坏 -> Given 主生产表或已有工作任务已有填写人来源, When 批次执行详情组装任务列表, Then 仍优先使用既有工作任务或任务分配规则解析填写人，不被动态表单绑定回填覆盖。

## Milestone Updates

- M1 completed: `toResp(...)` 原本只从有效填写/返工工作任务和工序任务分配规则解析 `fillableUsers`，未读取动态表单槽位发布后同步出的 `MesProEdhrProcessFormPermissionRuleDO`。
- M2 completed: 已新增并校准 `detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`。该测试使用冻结路线快照、动态表单 `formBindingKey`、发布后同步的 `FILL/USERS/152` 表单权限规则，断言响应返回 `张可莹（zhangkeying）`。
- RED: `mvn '-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated' surefire:test` -> FAIL，`expected: <[152]> but was: <[]>`。
- M3 completed: `MesProEdhrBatchExecutionServiceImpl` 新增动态表单填表规则映射，详情组装优先级为有效填写/返工工作任务 -> 表单权限规则 -> 工序填写分配规则；表单权限规则按 `batchRecordReportId` 优先、否则 `formBindingKey` 查询，候选人解析复用 `MesProEdhrCandidateResolver`，不引入前端推断或兜底。
- GREEN: `mvn '-Dmaven.compiler.useIncrementalCompilation=false' -DskipTests compile` -> PASS。
- GREEN: `javac @target\javac-edhr.args` -> PASS。
- GREEN: `mvn '-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated' '-Dsurefire.useManifestOnlyJar=false' surefire:test` -> PASS，3 tests, 0 failures, 0 errors。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260724-edhr-route-form-filler-backfill\bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260724-edhr-route-form-filler-backfill\backend-api-evidence.md` -> PASS。
- M4 regression re-check: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated test` -> PASS，3 tests, 0 failures, 0 errors，current HEAD verified at 2026-07-24 17:17 +08:00。
- Closeout completed: `task-closeout-cleanup` preview/apply 已通过，无删除项、无 blocker、无 warning，保留 `task.md`、`execution-log.md`、`verification-report.md`、`backend-api-evidence.md`、`bug-regression-evidence.md`。
- Experience consolidation completed: 已将“eDHR 详情回填门禁”合并到 `docs/backend-development.md`，并更新 `docs/experience-index.md` 关键词索引。
- Dirty worktree baseline completed: `git commit -m "工作区: 保存 route form filler 收尾后续脏区基线"` -> PASS，commit `16892129`。

## Commit Evidence

### Dirty Worktree Baseline Commit `16892129`

```text
A	IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/codextest/CodexTestCaseServiceImplTest.java
A	IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/codextest/CodexTestExecutionServiceImplTest.java
A	IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/codextest/CodexTestRunnerServiceImplTest.java
M	doc/tasks/20260724-edhr-route-form-filler-backfill/backend-api-evidence.md
M	doc/tasks/enforce-commit-push-policy/execution-log.md
M	doc/tasks/enforce-commit-push-policy/task.md
M	doc/tasks/enforce-commit-push-policy/verification-report.md
```

### Final Closeout Record

- This update records final task status and verification evidence before the final closeout record commit and required `git push origin int_main`.

## Current Blocker

- None for the task-owned verification scope. Full module `testCompile` remains blocked by unrelated legacy WM/MD tests referencing missing classes.

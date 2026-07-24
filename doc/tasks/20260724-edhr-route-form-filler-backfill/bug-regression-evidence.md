# Bug Regression Evidence

## Bug Summary and Expected Behavior

- Bug: 工艺路线工序绑定的损耗单已配置填写人，但批次执行详情返回的损耗单任务 `fillableUsers` 为空，导致右侧单据卡片显示未配置。
- Expected: 动态表单任务应返回发布路线同步出的表单填表权限规则填写人。

## Reproduction Command or Path

- Runtime read-only API query confirmed `EDHRB-1784855561493` route config has `张可莹（zhangkeying）` while task `fillableUsers=[]`。

## Root Cause

- 批次详情组装只解析有效填写/返工工作任务和工序任务分配规则，没有读取动态表单槽位发布后同步出的 `MesProEdhrProcessFormPermissionRuleDO`。因此已配置 `formBindingKey + FILL` 填写人的动态表单任务在没有活动工作任务时返回空 `fillableUsers`。

## Regression Test Added or Updated

- `MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`
- 场景使用冻结路线动态表单绑定和发布后同步出的表单填表规则 `FILL/USERS/152`，断言批次详情返回用户 ID `152` 和姓名 `张可莹（zhangkeying）`。
- 同组回归覆盖 `detailTask_includesFillableUsersFromActiveFillWorkTask` 与 `detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated`，确认优先级未破坏。

## RED Command and Expected Failure

- RED: 运行时复现显示路线绑定配置 `USERS/152`，但批次详情对应损耗单任务 `fillableUsers=[]`。
- RED: `mvn '-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated' surefire:test`（模块目录）-> FAIL, `expected: <[152]> but was: <[]>`。

## GREEN Command and Passing Result

- GREEN: `mvn '-Dmaven.compiler.useIncrementalCompilation=false' -DskipTests compile` -> PASS。
- GREEN: `javac @target\javac-edhr.args` -> PASS，隔离编译目标测试类。
- GREEN: `mvn '-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated' '-Dsurefire.useManifestOnlyJar=false' surefire:test` -> PASS, 3 tests。

## Verification

- Target regression verifies dynamic form task `fillableUsers` includes `152 / 张可莹（zhangkeying）`。
- Adjacent regressions verify active fill work task candidates and route-process assignment rule behavior remain intact.
- Full module `testCompile` is blocked by unrelated legacy WM/MD tests referencing missing classes; not used as this task's GREEN evidence.

## Risk and Regression Scope

- Must not alter existing main batch-record task fillable-user priority.
- Must not infer filler from current login user, creator, updater, or generic owner role.

## Blockers and Follow-up Actions

- No task-owned blocker remains.
- Separate cleanup needed outside this task: repair or exclude unrelated legacy WM/MD tests that block full `testCompile`.

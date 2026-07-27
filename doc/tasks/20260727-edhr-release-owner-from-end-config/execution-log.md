# 执行日志

## User Intent

修复放行负责人没有从工艺路线“工序结束 > 放行责任人”解析的问题，使工作台展示、电子签名授权和放行审批任务统一使用路线级 `RELEASE_APPROVE` 配置。

## Scope And Preconditions

- 后端范围：`yudao-module-mes` 工作台响应与放行服务授权。
- 前端范围：`BatchExecutionDetailPage.vue` 及其批次执行响应类型。
- 不新增数据库迁移，不修改既有路线配置数据，不引入 fallback 或吞异常。
- 当前工作区已有其他任务脏改动：`IntRuoyiBackend/script/deploy/publish-int-ruoyi.ps1`、`IntRuoyiBackend/script/tests/test_publish_int_ruoyi_to_test_tooling.py`、`doc/tasks/20260727-codex-runner-token-invalid/`、`doc/tasks/20260727-route-version-batch-record-snapshot/`；本任务不修改、不纳入本任务提交。
- 并发任务于 `2026-07-27 18:41:23 +08:00` 创建并推送基线提交 `f18927b9`，该提交包含本任务当时的核心后端、前端、测试和任务文档改动；它不是本任务独立实现提交。

## BDD Scenarios

BDD: 路线配置具体用户时返回放行负责人 -> Given 批次绑定的工艺路线配置了 `RELEASE_APPROVE` 具体用户 When 查询批次工作台 Then `releaseSummary.releaseOwnerConfigured` 为真且 `releaseOwnerLabel` 为该用户昵称。

BDD: 路线配置权限角色时返回角色放行说明 -> Given 批次绑定的工艺路线配置了 `RELEASE_APPROVE` 权限角色 When 查询批次工作台 Then `releaseSummary.releaseOwnerSourceType` 为 `ROLE_GROUP` 且 `releaseOwnerLabel` 显示角色名称并说明角色成员均可放行。

BDD: 放行负责人可以完成电子签名放行 -> Given 当前用户是 `RELEASE_APPROVE` 候选人且预检通过 When 提交带有效密码的放行签名 Then 放行成功并记录当前用户为签名/审批人。

BDD: 关闭负责人不能越权放行 -> Given 路线只配置 `CLOSE` 负责人而未配置 `RELEASE_APPROVE` When 关闭负责人提交放行签名 Then 请求 fail-fast 为放行负责人无效。

BDD: 缺失或无效放行配置继续 fail-fast -> Given `RELEASE_APPROVE` 配置缺失或候选人无效 When 查询/提交放行 Then 返回明确未配置或负责人无效错误，不回退为关闭负责人或执行人。

BDD: 放行阶段展示路线级负责人 -> Given 工作台响应包含 `releaseSummary.releaseOwnerLabel` When 打开批次详情的放行预检或审批节点 Then 右侧负责人展示该字段且不使用 `stageOwnerRole` 静默替代。

## TDD Sequence

1. 后端先补工作台响应和放行授权失败测试，预期现有实现因读取 `CLOSE` 或缺少字段而失败。
2. 前端先补静态契约测试，预期现有实现因放行阶段读取 `stageOwnerRole` 而失败。
3. 实现最小后端共享解析入口及响应字段。
4. 实现前端类型和放行阶段展示。
5. 运行目标测试、模块编译、静态契约和真实页面验证。

## Verification Evidence

### RED

- RED: backend target JUnit -> FAIL, `WorkbenchReleaseSummary` did not expose `getReleaseOwnerConfigured`, `getReleaseOwnerSourceType`, or `getReleaseOwnerLabel`.
- RED: frontend static contract -> FAIL, `batchExecution.ts` did not contain `releaseOwnerConfigured`.

### GREEN

- GREEN: backend target Maven -> PASS, 9 tests covering USER, ROLE_GROUP, role-member release, non-owner rejection, close-owner-only rejection, and password paths.
- GREEN: frontend static contracts -> PASS, `edhr-release-owner-label-static` and `edhr-release-screenshot-action-buttons-static`.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: route configuration static contracts -> PASS, `mes-route-flow-end-release-owner-static` and `mes-route-flow-release-owner-candidate-static`.

### REGRESSION

- REGRESSION: `CLOSE` owner remains isolated to close authorization; close-owner-only submit fails before password validation.
- REGRESSION: added `submitRejectsWhenRouteReleaseRoleHasNoEnabledMembers` to lock fail-fast behavior for an invalid empty `ROLE_GROUP` candidate pool.
- REGRESSION: non-release stage owner display path still uses the existing stage owner logic.
- REGRESSION: release-stage missing owner data now displays `放行责任人未配置` instead of a generic owner-role fallback.
- REGRESSION: blank user nicknames and blank role names now fail fast instead of falling back to numeric IDs.
- REGRESSION: no DB migration, route data rewrite, fallback, or swallowed exception introduced.

### Verification Retry

- RETRY: final Maven target command including `submitRejectsWhenRouteReleaseRoleHasNoEnabledMembers` -> TIMEOUT, because multiple unrelated Maven builds were concurrently active in `E:\IntRuoyi\IntRuoyiBackend`; no test failure was produced.
- RETRY: final `pnpm ts:check` after the explicit missing-owner label refinement -> TIMEOUT while unrelated `vue-tsc` processes were active; the task static contracts passed after the refinement.
- GREEN: isolated `javac` compilation of the final workbench service source and the new release-service regression test -> PASS.
- GREEN: database read-only verification -> `route_id=922119 / RT000028 / 球囊扩张压力泵` has enabled `RELEASE_APPROVE` rule `9000253153`, source `USER`, user `1 / admin / 瑛泰管理员`; latest batch `900000000881` is bound to the same route and rule.
- CLEANUP: stopped only the exact Maven and `vue-tsc` child processes created by this task's timed-out retries; shared `8081/48081` services and unrelated build processes were not stopped.
- CLEANUP: removed the fixed task-owned isolated compilation directory and Java argument files from `%TEMP%`.

## Blockers

- RESOLVED: the stale `48081` Jar was replaced with a validated Jar built from latest `origin/int_main`, and real Playwright verification now passes.
- RESOLVED: the final Maven target rerun including the empty-role candidate-pool case passes with 10 tests, 0 failures, and 0 errors.
- REMAINING CLOSEOUT: the core implementation is already pushed in `f18927b9`, but the current main workspace contains unrelated concurrent task changes. Final evidence commit/push must wait for a safe Git closeout window and must not include those files.

## Safe Rebuild And Restart

- SOURCE: current development is integrated in `int_main`, not pending in a feature worktree. Commit `f18927b9` is an ancestor of latest `origin/int_main`.
- BUILD ISOLATION: created detached build worktree `D:\IntRuoyiWorktree\20260727-edhr-release-owner-build` at `a9dfaf9e7f9338dce43ca6955d79f1a6e6c291e2`; no runtime service was started from that worktree.
- GREEN: isolated Maven target regression including `submitRejectsWhenRouteReleaseRoleHasNoEnabledMembers` -> PASS, 10 tests, 0 failures, 0 errors.
- GREEN: `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS.
- ARTIFACT: built `yudao-server-exec.jar` size `500646666`, SHA256 `7A3F2A015A0816D9F6876DBAAE4D99DB1619F7C5011E79E0EF7D72AE43A7DA0C`.
- ROLLBACK: old Jar SHA256 `6B86DCDAC11258E897F2F168C3F43E0E425D7F4D8CF8B93BCCC1C2169BD8921C` was copied to `E:\IntRuoyi\output\runtime\int_main\backend-release-owner-before-20260727-195336.jar` and hash-verified before stopping the service.
- STOP: confirmed PID `54560` owned the `48081` listener and PID `58680` used the same `E:\IntRuoyi` Jar command. Both exited normally; no force stop was used and `48081` was confirmed free before replacement.
- START: new backend PID `61040` loads `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`; target hash matches the isolated build artifact.
- GREEN: `http://127.0.0.1:48081/actuator/health` -> HTTP 200, `{"status":"UP"}`.

## Real Runtime Verification

- LOGIN PREFLIGHT: the first cold-start login completed backend permission loading after the frontend timeout; retrying the same authorized `芋道源码/admin` path after warm-up passed. No account, tenant, port, or backend was switched.
- GREEN: official login preflight opened `/mes/pro/feedback/edhr-batch-execution/detail?id=900000000881`.
- GREEN: Playwright selected the visible `99 放行` node and displayed `当前放行负责人：瑛泰管理员`.
- GREEN: the release view contained neither `放行责任人未配置` nor the generic `执行人` owner fallback.
- GREEN: authenticated workbench response for batch `900000000881` returned HTTP 200, business code `0`, `releaseOwnerConfigured=true`, `releaseOwnerSourceType=USER`, and `releaseOwnerLabel=瑛泰管理员`.
- SCOPE: verification was read-only; it did not run precheck, release, reject, upload, skip, complete, or any MES write action.

# Backend API Evidence

## Scope

- `CodexTestCaseServiceImpl` 对测试项 `project` 分类的保存校验。
- 合法项目集合从三项扩展为四项，新增 `工艺路线`。

## Contract

- `project` 允许：`智能排产`、`文控`、`批记录`、`工艺路线`。
- 其它项目值继续返回 `CODEX_TEST_RESULT_SCHEMA_INVALID`，不返回默认成功。
- 既有方法项、检查点、状态和执行模式校验保持不变。

## BDD

- BDD: 工艺路线项目可保存 -> Given 合法测试项请求包含 `project=工艺路线`，When 调用创建服务，Then 测试项和检查点成功落库。
- BDD: 未知项目仍拒绝 -> Given 请求包含未登记项目，When 调用创建服务，Then 返回项目枚举校验错误且不写入测试项。

## Validation

- 保存校验入口：`CodexTestCaseServiceImpl#createCase` / `updateCase`。
- 合法项目集合：`CodexTestConstants.CASE_PROJECTS`。
- OpenAPI 保存 VO 说明已同步为 `智能排产/文控/批记录/工艺路线`。

## RED / GREEN

- RED: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 `project=工艺路线` 场景被旧项目枚举拒绝，未知项目错误文案仍按旧枚举断言。
- GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，5 tests，0 failures，0 errors。
- REGRESSION: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS，5 tests，0 failures，0 errors；本机 Java 21 fork 模式存在 surefire/native resource 超时风险，因此目标回归用非 fork 方式复核。

## Verification

- 真实前端写入后，同一登录会话只读调用 `/admin-api/system/codex-test-case/get`，确认 4 个 `工艺路线` 测试项均为 `SEQUENTIAL`、`parallelSafe=false`、`ENABLE`、每项 4 个检查点。
- E2E 摘要：`doc/tasks/20260726-codex-test-process-route-case/artifacts/process-route-codex-test-items-summary.json`，status=`PASS`。

## Blockers

- 无。

# Backend API Evidence

## Scope

- 测试管理测试项接口如需新增项目字段，需保持分页查询、详情和列表契约一致。

## Contract

- `CodexTestCaseSaveReqVO.project` 为必填，值只能是 `智能排产`、`文控`、`批记录`。
- `CodexTestCaseRespVO.project` 在详情与分页响应中返回。
- `CodexTestCasePageReqVO.project` 用于分页精确过滤。

## BDD

- BDD: 测试项保存校验项目枚举 -> Given 管理员保存测试项, When 项目不是 `智能排产`、`文控` 或 `批记录`, Then 后端返回结构化业务错误且不落库。

## Verification

- `mvn -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

## Blockers

- 无后端实现阻塞；提交/推送收尾受工作区既有脏改动阻塞。

## Evidence

- Endpoint scope: `/system/codex-test-case/page`、`/get`、`/create`、`/update` 的测试项数据契约。
- Data contract: `CodexTestCaseDO.project`、`CodexTestCaseRespVO.project`、`CodexTestCaseSaveReqVO.project`、`CodexTestCasePageReqVO.project`。
- Validation: 保存时 `project` 必须为 `智能排产`、`文控` 或 `批记录`，否则抛出 `CODEX_TEST_RESULT_SCHEMA_INVALID`，不兜底。
- Query behavior: 分页 Mapper 支持按 `project` 精确过滤。
- RED: `CodexTestCaseServiceImplTest` 新增项目断言后，生产字段缺失会编译/测试失败。
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests passed。
- Observability: 非法项目以现有业务异常返回，不吞异常、不默认成功。

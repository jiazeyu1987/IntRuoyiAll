# Backend API Evidence

## Scope

- Endpoint/service scope: DCC 项目代码全局分配候选分页接口的请求与响应类型。
- Data contract: Page request 包含 `keyword`；response 包含文件身份、当前项目代码身份、可选状态与禁用原因。
- Auth/permissions: 复用现有控制器权限，不在本任务修改。
- Validation/error behavior: 复用现有分页、服务校验与 fail-fast 行为，不增加 fallback。
- Required migration/config/service: 无新增迁移、配置或服务。

## BDD

- BDD: Clean worktree compiles formal assignment candidate contract -> Given existing consumers reference the two candidate DTOs, When the DCC reactor compiles and runs the focused test, Then the request and response contracts resolve without changing authorization or service behavior.
- BDD: Scope stays narrow -> Given the user authorized only the missing DTO prerequisite, When the patch is reviewed, Then exactly the two DTO source files plus task evidence are changed.

## TDD

- RED: `mvn.cmd -pl yudao-module-dcc -am "-DskipITs" "-Dtest=DccProjectCodeAssignmentServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- RED result: FAIL（退出码 1，2026-08-15 03:38:32 +08:00）；DCC 主源码编译报告 15 个 `cannot find symbol`，缺失类型仅为 `DccProjectCodeAssignmentCandidatePageReqVO` 与 `DccProjectCodeAssignmentCandidateRespVO`，未到达 Surefire。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-DskipITs" "-Dtest=DccProjectCodeAssignmentServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS（退出码 0，2026-08-15 03:42:39 +08:00）；Surefire 实际运行 13 项，Failures 0、Errors 0、Skipped 0。

## Contract Verification

- Page request: `@Data`、`@EqualsAndHashCode(callSuper = true)`、extends `PageParam`、仅 `String keyword`。
- Response: `@Data`，精确字段为 `id/masterId/fileName/fileNumber/versionNo/status/currentProjectCodeId/currentProjectName/currentProjectCode/selectable/disabledReason`。
- 现有 controller、service、mapper、service test 均继续引用同一 package 下两个正式 DTO；未新增第二套模型或适配层。
- REGRESSION: `mvn.cmd -pl yudao-module-dcc -am "-DskipITs" "-Dtest=DccProjectCodeAssignmentServiceImplTest,DccControlledFileMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS（退出码 0，2026-08-15 03:46:32 +08:00）；共 23 项，Failures 0、Errors 0、Skipped 0。

## Observability

- 无新增运行态日志或指标；编译和契约测试是本切片证据。

## Blockers

- 代码、目标测试、回归、技能 validator 与端口 guard 无 blocker。
- 主管预建且禁止本 executor 编辑的 `task.md` 第 39 行存在 `new blank line at EOF`；主管提交前需修正并复跑 staged `git diff --check`。本 executor 拥有的 6 个文件逐文件 whitespace check 全部通过。

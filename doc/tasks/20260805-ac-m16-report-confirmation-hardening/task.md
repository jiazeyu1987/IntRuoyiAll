# AC-M16 生产报工确认链路加固

## Task Goal

修复 `AC-M16 | 生产班组长 | 确认员工报工` 的代码级不符合项：生产报工通过必须进入分配确认链路，退回后不得继续分配，重复终态不得产生第二条有效复核/分配事实，并补齐对应后端回归与 schema 约束。

## Milestones

- [x] M1 建立任务文档、BDD/TDD 计划和并行基线证据
- [x] M2 补充失败回归测试，证明当前实现可绕过分配或退回后继续分配
- [x] M3 修复后端服务校验与必要错误码，保持 fail-fast
- [x] M4 补充/修正 schema 约束与测试 fixture
- [ ] M5 运行目标 Maven 回归和 evidence validators
- [ ] M6 收尾记录、cleanup preview/apply、提交并推送

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesTeamLeaderReportConfirmationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-ac-m16-report-confirmation-hardening/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-ac-m16-report-confirmation-hardening/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260805-ac-m16-report-confirmation-hardening/database-schema-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-m16-report-confirmation-hardening --mode preview`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-m16-report-confirmation-hardening --mode apply`

## Current Status

in_progress

实现代码、回归测试和 schema 迁移已完成；定向 javac + JUnit Console GREEN 通过。标准 Maven 完成门禁因并行 Maven 构建/本地仓库占用多次超时，提交/推送与 cleanup 尚未执行。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务按 AC-M16 正式链路 fail-fast，不允许通用复核绕过生产分配确认。
- `是否从根因和长期维护角度解决`：是。修复服务入口终态一致性、生产角色/事件类型边界和 schema 唯一约束。
- `是否存在临时补丁或绕过`：否。

## Parallel Workspace Baseline

- 本任务开始时工作区存在多批并行文档/源码改动，已按项目规则做独立基线提交并保留文件边界。
- 已观察基线提交：`fc5e98ffe`、`fdf1b49d8`、`c7a713c03`、`5702e9d59`、`b01682b49`、`e7c27613e`、`f8c1a38f7`、`ca181206a`。
- 当前仍可能有并行任务继续写入非 AC-M16 文件；本任务只选择性修改 AC-M16 相关服务、测试、SQL 和任务文档。

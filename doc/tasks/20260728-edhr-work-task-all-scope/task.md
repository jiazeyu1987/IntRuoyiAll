# 20260728 eDHR 工作任务 ALL 责任范围修复

## Task Goal

修复创建 eDHR 批次执行时报错 `eDHR 工作任务责任范围快照无效：scopeKey=ALL` 的问题，确保普通整表填写人规则能生成可追溯责任范围快照。

## Milestones

- [x] 定位 `scopeKey=ALL` 责任范围快照生成与校验链路
- [x] 增加 RED 回归测试覆盖普通填写人规则无显式范围时的创建失败
- [x] 实现正式整表填写范围生成，不引入 fallback 或吞异常
- [x] 运行目标 Maven 测试并记录 GREEN/REGRESSION

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260728-edhr-work-task-all-scope/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260728-edhr-work-task-all-scope/backend-api-evidence.md`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是让 ALL 规则在保存/运行态拥有正式责任范围快照。
- `是否存在临时补丁或绕过`：否。

## Verification Result

- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Closeout pending: 当前分支存在非本任务本地提交领先 `origin/int_main`，最终 push/完成状态需在不混入无关任务风险后处理。

## 经验门禁

### eDHR 详情回填门禁

- Trigger: eDHR、批次详情、动态表单、工艺路线绑定、填写人、`fillableUsers`、配置页有值但详情接口为空。
- Preflight check: 同时核对配置接口/表中的来源字段、执行任务快照字段、详情接口组装链路和既有优先级，不得只改前端显示文案。
- Blocker: 详情任务没有可追溯绑定 ID、快照字段或正式规则来源时，必须补齐后端数据链路；不得从当前登录人、创建人、更新人或角色 ID 推断填写人。
- Verification: 新增后端回归测试覆盖路线绑定配置填写人场景，并同时跑相邻优先级测试。
- Forbidden action: 禁止前端文案掩盖、角色/部门 ID 当用户 ID、空列表兜底。
- Evidence: `docs/backend-development.md#edhr-详情回填门禁`。

### eDHR 批次任务配置来源门禁

- Trigger: eDHR 批次执行、路线发布快照、`routeSnapshotJson`、`batchUseConfigs`、当前路线配置缺失或陈旧绑定。
- Preflight check: 新建批次前检查当前 BATCH 工序配置、绑定归属和发布版本快照完整性。
- Blocker: 当前 BATCH 工序配置存在时必须使用当前配置并严格校验绑定归属；不得静默回退发布快照。
- Verification: 覆盖当前配置优先、整体缺失才用已发布快照、陈旧绑定 fail-fast。
- Forbidden action: 禁止把发布快照作为通用 fallback，禁止用空绑定或默认 MAIN 掩盖当前配置损坏。
- Evidence: `docs/backend-development.md#edhr-批次任务配置来源门禁`。

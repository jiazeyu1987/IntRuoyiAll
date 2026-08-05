# AC-M20 PQC 组长复核闭环修复

## Task Goal

修复 AC-M20 “PQC 组长确认 PQC 检验单”代码不符合项：PQC 事件必须由 PQC 组长复核，复核通过必须闭环到正式 PQC 检验任务 `CONFIRMED` 状态，退回必须保留可审计原因，前端必须按复核终态约束操作入口，并补齐结构化审计与并发/重复防护证据。

## Milestones

- [x] 记录 AC-M20 BDD/TDD、门禁和已知运行态限制
- [x] 后端 RED：补 PQC 角色硬校验、任务确认状态、退回原因、审计字段、重复终态约束测试
- [x] 后端 GREEN：实施最小服务、DO、Mapper、迁移和测试修复
- [x] 前端 RED/GREEN：补状态门禁与退回原因校验的静态或单元验证
- [x] 运行定向验证并记录剩余 blocker

## Expected Verification

- `mvn -pl yudao-module-mes "-DskipTests" compile`
- `mvn -pl yudao-module-mes -am -Pmes-ac-m20-pqc-review-targeted-tests "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderSchemaTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests\e2e\team-leader-pqc-review-gate-static.spec.js`
- `pnpm ts:check`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`
- `git diff --check`
- 本任务 worktree 运行态：后端 `http://127.0.0.1:48083/actuator/health` 返回 `UP`，前端 `http://127.0.0.1:8083/` 返回 `200`。
- 真实写入型 Playwright E2E 需要正式 `RRM_*` 测试租户、账号、签名 ID、生产订单、工艺路线、调拨/发货/补料/退料、批记录报表和 QA 规程版本前置；缺失时记录为 E2E BLOCKED，不用 API-only、静态扫描或 admin 基线数据冒充真实 E2E。

## Applicable Gates

- BDD/TDD：生产行为先 RED 后 GREEN，不能只做静态修补。
- Strict no-fallback：缺正式 PQC 任务、缺 PQC 角色、缺退回原因时 fail fast，不返回默认成功。
- PQC 项目级检验快照门禁：PQC 组长复核不能从 raw payload 或前端文案推断正式检验事实。
- 前端静态契约隔离：如全量 `ts:check` 存在无关 blocker，使用任务专用最小契约证明当前行为。
- Worktree 运行态限制：当前任务已预约 `int_main slot 2`，只能使用成对 `8083/48083` 运行态；不得切换到主工作区 `8081/48081` 或其它 worktree 端口。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；在服务层、持久化审计、状态流转和前端入口同时收紧。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

2026-08-05 复验结论：AC-M20 代码级修复已通过后端编译、聚焦 JUnit、前端静态契约、前端类型检查、迁移门禁、diff 检查和本任务运行态健康检查；但真实写入型 Playwright E2E 缺少任务专用 `RRM_*` 租户、账号、签名 ID 与业务数据前置，不能标记为最终 `ACCEPTED`。

# TDD 执行顺序

## Purpose and Scope

规定二阶段生产代码的严格 RED/GREEN 顺序，防止先实现后补测试或用页面结果掩盖事务、权限与幂等缺口。

## Evidence Reviewed

- `prd.md`、`development-plan.md`、`test-plan.md`。
- 当前 DCC 后端和前端测试模式。

## TDD Sequence

1. P1 先写 migration/schema/唯一键测试，再写发布事务原子性、重复发布和正反向快照测试。
2. P2 先写负责人缺失、转派越权、决定 CAS、零版本副作用和开放大版本冲突测试。
3. P3 先写收件人去重、幂等消息、部分失败、SENT 禁止重放和通知不授权测试。
4. 前端先写工作台、详情、管理页和 Long ID 静态合同，再实现页面。
5. 每阶段 GREEN 后跑相邻生命周期回归；最终才进入运行库和 Playwright。

## RED Commands

- `mvn -o -pl yudao-module-dcc -Dtest=DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest test`
- `mvn -o -pl yudao-module-dcc -Dtest=DccRelatedFileImpactAssessmentServiceTest test`
- `mvn -o -pl yudao-module-dcc -Dtest=DccPublicationNotificationServiceTest test`
- `node tests/e2e/dcc-release-impact-workbench-static.spec.js`

## Expected Failures

- 发布后续 schema、批次和快照不存在。
- 当前关系服务不能同时解析正向和反向关系并按 Master 去重。
- 当前消息业务类型没有发布批次级幂等键和失败重试合同。
- 当前工作台没有影响评估入口。

## GREEN Commands

- 运行 P1-P3 新增测试及 `DccControlledFileFinalizationServiceImplTest`、`DccControlledFileWorkflowServiceImplTest`。
- 运行新 migration 的 pytest 静态合同和完整 dependsOn 闭包门禁。
- 运行 DCC 前端静态合同和 relaxed `vue-tsc`。
- 运行现有新文件生命周期组合回归，确保 A/1、A/2、B/1 行为不变。

## Refactor Checks

- finalization 只依赖一个后续账本端口。
- 权限继续复用现有 OWNER/VIEW 服务。
- 通知只通过系统幂等消息 API。
- 列表一对多在后端聚合，前端不去重。
- 所有 Long ID 保持字符串。

## Evidence Log Template

`BDD: <scenario> -> Given/When/Then`

`RED: <command> -> FAIL, <expected reason>`

`GREEN: <command> -> PASS, <count>`

`REGRESSION: <command> -> PASS/BLOCKED, <scope>`

## Test Blockers

- 缺依赖、schema、账号、入口或权限时停止，不新增 fallback。
- 独立 tester 未通过时不得推进下一阶段。

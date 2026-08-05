# AC-M18 更新生产订单进度修复

## Task Goal

修复 AC-M18“系统更新生产订单进度”不完全符合项：班组长确认分配后必须同步正式排产工单工序进度和工单汇总，ERP 产品数量保持不变；缺失/非正数系数、超目标数量和并发超额更新必须被阻塞。

## Milestones

- [ ] 建立 AC-M18 可观察 BDD 场景和 RED 回归测试。
- [ ] 修复班组长确认分配后的正式排产工单进度同步。
- [ ] 修复正式排产进度同步的超目标 fail-fast 行为。
- [ ] 运行定向后端验证并记录 GREEN/REGRESSION 证据。
- [ ] 补齐技能 evidence、验证报告和收尾状态。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesProScheduleOrderProgressServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-ac-m18-progress-repair/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-ac-m18-progress-repair/backend-api-evidence.md`

## Current Status

in_progress

已创建任务证据目录，准备补充 RED 测试。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划通过正式排产工单进度链路与 fail-fast 校验修复根因。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- BDD + Strict TDD：生产代码变更前必须先记录 BDD 并跑出 RED。
- No fallback：缺少正式进度、目标量或系数前置条件时必须 fail-fast，不得默认成功或静默降级。
- Backend API Delivery：服务行为、校验、持久化和错误行为必须有定向后端测试证据。
- Maven Reactor：MES 模块验证必须使用 `-pl yudao-module-mes -am` 覆盖兄弟模块依赖。

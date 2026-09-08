# 临时权限管理失控完整功能核查

## Task Goal

验证 1.10「临时权限管理失控」在当前系统中的完整闭环程度，重点覆盖真实前端入口、临时授权生命周期、到期提醒、到期自动撤销、审查归集、审计可追踪和定时任务前置条件。

## Milestones

1. 前置核查：确认本地运行态、页面入口、测试命令、Playwright 可用性和当前代码来源。
2. 静态与合同核查：确认后端接口、SQL 种子、菜单权限、前端页面/API 与测试覆盖。
3. 真实 E2E 可行性核查：确认是否可以通过真实前端完成申请、审批、提醒可见、到期撤销与审查列表。
4. 结果报告：输出 PASS / BLOCKED / GAP 结论和下一步最小修复建议。

## Expected Verification

- `npx --version`
- 端口与 health 前置检查：8081 / 48081
- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_system_temporary_role_grant_sql.py -q`
- `mvn.cmd -pl yudao-module-system '-Dtest=TemporaryRoleGrantServiceImplTest,PermissionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `node IntRuoyiFronted\tests\e2e\system-temporary-role-grant-static.spec.js`
- 必要时执行真实 Playwright 页面核查；若运行态、入口、权限或测试数据不满足，记录 BLOCKED，不用 API-only 冒充通过。

## Current Status

completed：静态合同、SQL 合同、后端目标回归和真实前端 E2E 均已完成。真实 E2E 使用 `admin` 登录、从菜单进入“临时角色授权”，通过页面完成临时授权申请、审批、到期前提醒任务触发、站内信提醒可见、异常逾期归集、到期回收任务触发、已过期状态和审计记录核验；DB 仅用于最终只读核验临时授权未写入长期 `system_user_role`。task-closeout-cleanup preview/apply 已通过，仅保留正式任务记录和最终 PASS JSON 证据；主干基线提交已先推送，本任务收尾记录可单独提交并推送。

## BDD Scenarios

BDD: 真实前端申请审批闭环 -> Given 管理员通过真实前端进入临时角色授权页面 When 创建并审批一个任务自有临时授权 Then 页面列表显示授权状态、生效时间、截止时间和审计记录。
BDD: 到期前提醒真实可见 -> Given 临时授权将在提醒窗口内到期 When 正式提醒任务通过系统入口执行 Then 收件人本人登录站内信页面能看到目标提醒。
BDD: 到期自动撤销真实生效 -> Given 临时授权已过期 When 正式到期回收任务执行 Then 被授权用户权限失效，列表显示已过期，审计记录包含 EXPIRE。
BDD: 审查归集可操作 -> Given 存在仍有效、即将到期和异常逾期授权 When 管理员查看临时授权页面 Then 统计卡片和审查分类筛选能区分三类状态。

## Design Constraints

- E2E 必须走真实前端页面，API/DB 只允许只读佐证。
- 不修改 admin 作为被测业务用户；admin 仅用于测试登录和管理操作。
- 不直接写 `system_user_role` 冒充临时授权。
- 运行态不满足当前代码或 SQL 未应用时，停止并记录 BLOCKED。

## Cleanup Keep

- doc/tasks/20260908-temporary-role-grant-full-verification/e2e-artifacts/temporary-role-grant-real-e2e-E2E-20260908-continue-10.json


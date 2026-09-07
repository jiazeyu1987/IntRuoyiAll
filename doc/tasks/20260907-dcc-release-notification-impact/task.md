# DCC 发布通知与关联文件影响评估二阶段规划

## Task Goal

在已完成的新文件 Windchill 生命周期上，规划大版本发布后的关联方通知与关联文件影响评估闭环，使发布结果可通知、影响结论可处理和可追溯，同时不阻塞文件正式生效、不自动替关联文件升版。

## Scope

- 本任务只交付可执行的 PRD、开发计划、测试计划和任务状态。
- 只规划新文件生命周期产生的发布事实，不治理历史文件。

## Non-Scope

- 不实现生产代码、数据库迁移或前端页面。
- 不执行数据库写入、E2E、服务重启、Git 提交或推送。
- 不规划目录模板版本化、历史数据治理或自动升版。

## Milestones

- P1 发布事件与通知对象快照
- P2 关联文件影响评估任务
- P3 站内通知、待办与处理页面
- P4 审计、失败边界和真实验收

## Expected Verification

- 需求变更评估通过
- PRD 结构验证通过
- 开发任务包结构验证通过
- BDD/TDD 测试计划结构验证通过
- 每项验收标准有可执行验证路径

## Blockers

- 本轮已授权启用子 Agent并提交/推送 P1 相关变更。
- 数据库写入、真实 E2E 和 48081 重启未授权，但不阻塞 P1 的静态 schema、服务和测试交付；这些运行态动作留到 P4 当轮授权。
- P1 开始前先把独立的 DCC 关联文件 migration metadata 修复纳入代码基线。

## Current Status

in_progress

规划已通过结构、一致性和主 Agent 复核；生产代码尚未开始，下一步从 P1 发布后续账本与冻结快照执行。

## 设计约束检查

- 发布成功先于通知和影响评估，后两者失败不得回滚已生效文件。
- 后续动作完成不阻塞生效，但后续账本和发布时快照必须随发布事务可靠保存；账本保存失败时发布整体失败。
- 通知对象按发布时快照冻结；后续权限变化不改写历史通知对象。
- 关联文件只生成评估任务，系统不得自动创建新版本或改变其状态。
- 影响结论至少区分无需处理、需要复核、需要升版，并记录责任人、原因和时间。
- 缺少责任人或通知渠道时必须暴露阻塞，不得静默跳过或伪造送达。
- 同一发布事件不得重复生成通知或影响任务。

## Cleanup Keep

- doc/tasks/20260907-dcc-release-notification-impact/task.md
- doc/tasks/20260907-dcc-release-notification-impact/change-request.md
- doc/tasks/20260907-dcc-release-notification-impact/prd.md
- doc/tasks/20260907-dcc-release-notification-impact/user-flows.md
- doc/tasks/20260907-dcc-release-notification-impact/acceptance-criteria.md
- doc/tasks/20260907-dcc-release-notification-impact/development-plan.md
- doc/tasks/20260907-dcc-release-notification-impact/test-plan.md
- doc/tasks/20260907-dcc-release-notification-impact/bdd-scenarios.md
- doc/tasks/20260907-dcc-release-notification-impact/tdd-plan.md
- doc/tasks/20260907-dcc-release-notification-impact/e2e-plan.md
- doc/tasks/20260907-dcc-release-notification-impact/test-data.md
- doc/tasks/20260907-dcc-release-notification-impact/task-state.json
- doc/tasks/20260907-dcc-release-notification-impact/execution-log.md
- doc/tasks/20260907-dcc-release-notification-impact/verification-report.md

# DCC 发布通知与关联文件影响评估

## Task Goal

在已完成的新文件 Windchill 生命周期上，规划大版本发布后的关联方通知与关联文件影响评估闭环，使发布结果可通知、影响结论可处理和可追溯，同时不阻塞文件正式生效、不自动替关联文件升版。

## Scope

- 本任务按 P1-P4 计划交付发布后续账本、影响评估、通知与真实前端闭环。
- 只处理新文件生命周期产生的发布事实，不治理历史文件。

## Non-Scope

- 不治理或补建历史文件数据。
- 不规划目录模板版本化、历史数据治理或自动升版。

## Milestones

- P1 发布事件与通知对象快照：completed
- P2 关联文件影响评估任务：completed
- P3 站内通知、待办与处理页面：completed
- P4 审计、失败边界和真实验收：blocked

## Expected Verification

- 需求变更评估通过
- PRD 结构验证通过
- 开发任务包结构验证通过
- BDD/TDD 测试计划结构验证通过
- 每项验收标准有可执行验证路径

## Blockers

- 当前无 P2 代码或本地验证 blocker。
- 数据库写入、真实 E2E 和 48081 重启未授权；P1/P2 运行库迁移首次/重复执行留到 P4 当轮授权。

## Current Status

blocked

P1 发布后续账本、P2 影响评估任务和 P3 幂等通知及真实前端入口均已通过独立 tester；P4 本地运行态和页面/上传检查通过。继续真实前端审批时，任务文件 `DCC-P4-202609081528-NEW` 在签名证据摘要阶段因对象存储源对象缺失（S3 NoSuchKey 404）返回 HTTP 500。审批、发布、发布后续批次、通知和影响任务闭环因此阻塞，未使用 API 或数据库写入绕过。

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
- doc/tasks/20260907-dcc-release-notification-impact/p4-local-page-e2e.cjs
- doc/tasks/20260907-dcc-release-notification-impact/p4-local-upload-precheck.cjs
- doc/tasks/20260907-dcc-release-notification-impact/p4-local-upload-submit-e2e.cjs

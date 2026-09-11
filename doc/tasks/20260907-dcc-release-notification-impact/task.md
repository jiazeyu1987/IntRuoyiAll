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

completed

P1 发布后续账本、P2 影响评估任务和 P3 幂等通知及真实前端入口均已通过独立 tester；P4 本地运行态和页面/上传检查通过。`DCC-P4-202609081528-NEW` 的源对象实际可读，审批失败的真实根因是运行库缺少 `system_electronic_signature` 表；现有正式 T3/T7/T8 迁移已首次及重复执行成功。当前复测被共享前端中另一任务遗留的未解决 Git 冲突和 Vite 编译错误阻塞，待前端恢复后继续会签、发布、通知和影响任务闭环。

2026-09-09 补充：本轮已修复阻塞标准后端重启的 System/DCC/MES 编译问题，并使用标准脚本重启 `int_main` 后端；`48081` 已监听，`/actuator/health` 返回 HTTP 200。DCC 真实前端审批、发布、通知和影响任务闭环本轮未执行，P4 仍保持 blocked。

2026-09-09 P4 写入闭环补充：已使用真实前端创建并提交新的测试文件 `DCC-P4-20260909P4B`，文件 ID `2054545668044070331`，流程实例 `7fd46866-ac25-11f1-b878-00155dde8c13`。只读核对显示 Flowable 当前任务 `81b33998-ac25-11f1-b878-00155dde8c13` 已分配给 admin，真实详情页可加载当前文件和审批任务，`task-action-readiness` 返回 ready=true；点击“审核通过”后 `/admin-api/dcc/controlled-files/2054545668044070331/approve-task` 返回 HTTP 500。后端日志显示运行库缺少 `gxp_audit_policy_operation` 表；正式迁移文件为 `IntRuoyiBackend/sql/mysql/20260908_gxp_audit_trail_core.sql`。由于执行该迁移属于直接数据库写入，当前 Playwright 写入授权下未执行迁移，P4 仍 blocked。

2026-09-09 GxP 迁移与 P4C 续验补充：已按授权执行 GxP audit 测试库迁移，补齐正式 SQL 与运行态表的 GxP 基础字段和 DCC 签名容量合同，并从 `IntRuoyiBackend/config/gxp-audit-policy.yaml` 导入正式策略登记。使用真实前端重新创建 `DCC-P4-20260909P4C`，完成文控审核和 admin 矩阵评审；当前阻塞在 zhaojie 的矩阵评审，原因是 zhaojie 虽为 BPM 当前任务处理人，但 DCC 文件详情接口返回 `1080000012 Current user cannot access this controlled file`。剩余会签、终批、发布、通知和影响任务闭环未声明通过。

2026-09-09 当前审批人访问修复补充：已修复待审 DCC 文件详情/预览权限，新增“当前 Flowable 运行任务处理人”作为待审详情和待审原文件预览的放行条件，同时保持未发布文件下载拒绝。新增回归用例先 RED 后 GREEN；`DccControlledFileQueryServiceTest` 全类 98 条通过。当前代码尚未重启加载到 `48081`，真实前端 zhaojie 会签、终批、发布、通知和影响任务闭环仍未声明通过。

2026-09-10 P4 最终闭环补充：真实前端已完成 P4D A/1 发布、影响任务重新打开并改判需要升版、创建/关联 P4C B/1、B/1 全审批与独立发布，以及反向影响任务无需升版结论。只读核验确认 P4C A/1 已被取代，P4C B/1 与 P4D A/1 生效，三个发布批次均完成且每批 7/7 通知已发送；P4D/B/1 各五条 DCC 签名均为 VALID + HMAC_SHA256。重启后真实页面只读 E2E 再次 PASS，并保留完整 30 项时间线文本。DCC 相邻 162 项测试和三项前端合同通过；独立 tester 已放行 P4-AC1 至 P4-AC5 和 AC-18。并行 MES 页面导致的全局 `vue-tsc` 红灯记录为仓库级残余问题，不归本任务修改。

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
- doc/tasks/20260907-dcc-release-notification-impact/test-report.md
- doc/tasks/20260907-dcc-release-notification-impact/backend-api-evidence.md
- doc/tasks/20260907-dcc-release-notification-impact/database-schema-evidence.md
- doc/tasks/20260907-dcc-release-notification-impact/frontend-feature-evidence.md
- doc/tasks/20260907-dcc-release-notification-impact/bug-regression-evidence.md
- doc/tasks/20260907-dcc-release-notification-impact/p4-runtime-evidence.md
- doc/tasks/20260907-dcc-release-notification-impact/p4-runtime-preflight.md
- doc/tasks/20260907-dcc-release-notification-impact/p4-local-page-e2e.cjs
- doc/tasks/20260907-dcc-release-notification-impact/p4-local-upload-precheck.cjs
- doc/tasks/20260907-dcc-release-notification-impact/p4-local-upload-submit-e2e.cjs
- doc/tasks/20260907-dcc-release-notification-impact/p4-approval-center-inspect.cjs
- doc/tasks/20260907-dcc-release-notification-impact/p4-approval-publish-e2e.cjs
- doc/tasks/20260907-dcc-release-notification-impact/p4-direct-approve-task-e2e.cjs
- doc/tasks/20260907-dcc-release-notification-impact/output/playwright-p4-local/

## Closeout Evidence

- `task_closeout.py --mode preview` -> PASS, status `ready`, blocked/warnings 均为空；正式任务文档、独立测试报告和最终 Playwright 证据均在 keep 清单。
- `task_closeout.py --mode apply` -> PASS, status `applied`; 删除 5165 个本任务旧 Playwright trace、临时运行日志、临时数据库备份和一次性启动产物。
- 当前仓库是主工作区 `int_main`，不是额外 worktree；无需执行 worktree 合并或删除。
- 最终状态：completed；P1-P4 和 AC-01 至 AC-18 均已完成并通过独立 tester。

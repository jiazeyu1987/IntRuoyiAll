# DCC 源文件独占与哈希证据治理

## Task Goal

在测试环境内，治理 DCC 历史受控文件的源文件独占关系与 SHA-256 证据，为后续 Windchill Revision/Iteration 迁移建立可审计、可重复核验的迁移前置。不得猜测历史事实，不直接进入 Revision/Iteration 表实施。

## Milestones

- [x] M1：完成现状分析与 PRD 评审。
- [x] M2：完成依赖任务图与测试计划评审。
- [x] M3：完成源文件证据治理设计或实现任务。
- [x] M4：完成独立验证与 blocker 复核。
- [x] M5：完成任务收尾清理。

## Expected Verification

- 明确 18,065 条 ownership/hash 缺失记录、43 组共享源文件和 7 条软删除源文件引用的处理边界。
- 源文件归属、源文件位置、SHA-256 与 DCC 受控文件关系均有明确来源；无法确认的记录进入 BLOCKED。
- 不以 `source_file_id` 存在、文件名、最新版本或共享源文件反推历史事实。
- 重新执行只读盘点后，AUTO_MAP 候选数量和剩余 blocker 可解释、可复核。
- 所有变更和验证证据写入任务文档；不提交凭据，不执行未授权的 Git、生产发布或 E2E。

## Current Status

completed

T3、T4、T5 代码与定向回归已完成；T6 Pass 7 已由独立 tester 完成，最新 81 项回归、三类 evidence validator、运行态幂等重试、postflight 和只读盘点均通过。测试库 additive schema、备份恢复、对象存储 smoke 和真实 prepare/confirm/execute 已验证；tenant 1 的 17,607 条 source 引用中 7 条完成、17,600 条阻塞，tenant 122 的 465 条全部阻塞。最新运行包已通过标准 30 模块 package/restart，48081 健康检查 UP，7/7 postflight 通过。M4/M5 已完成；Git 基线与任务实现提交已完成，待提交最终收尾记录并推送。全量历史仍受 AUTO_MAP=0、ownership/hash、平台状态及历史关联 blocker 约束，业务阶段保持 `NO-GO`。

## 设计约束检查

- 当前范围仅限测试环境；不得影响 `int_main` 现有运行服务或并行任务。
- 默认先只读分析和设计；涉及数据库写入、DDL、迁移脚本、服务重启或 Git 提交必须单独获得当轮授权。
- 不引入 fallback、模拟成功、静默吞错或依据弱字段猜测历史关系。
- Planner 只修改 `request-analysis.md` 和 `prd.md`；后续角色按任务边界修改文件。

## Cleanup Keep

- doc/tasks/20260904-dcc-source-ownership-hash-governance/task.md
- doc/tasks/20260904-dcc-source-ownership-hash-governance/request-analysis.md
- doc/tasks/20260904-dcc-source-ownership-hash-governance/prd.md
- doc/tasks/20260904-dcc-source-ownership-hash-governance/dev-plan.md
- doc/tasks/20260904-dcc-source-ownership-hash-governance/test-plan.md
- doc/tasks/20260904-dcc-source-ownership-hash-governance/task-state.json
- doc/tasks/20260904-dcc-source-ownership-hash-governance/execution-log.md
- doc/tasks/20260904-dcc-source-ownership-hash-governance/test-report.md
- doc/tasks/20260904-dcc-source-ownership-hash-governance/windchill-readonly-inventory.sql
- doc/tasks/20260904-dcc-source-ownership-hash-governance/windchill-inventory-report.md
- doc/tasks/20260904-dcc-source-ownership-hash-governance/verification-report.md
- doc/tasks/20260904-dcc-source-ownership-hash-governance/runtime-backup-evidence.md

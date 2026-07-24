# 系统旧工序 ID 全量审计

## Task Goal

只读排查 MES 后端代码与本机数据库中是否仍存在旧工序 ID、已删除工序 ID 或路线工序关系不一致，并评估对排产、报工、领料和审批链路的影响。

## Milestones

1. 建立工序 ID 相关代码与数据库字段清单。
2. 执行工序与路线工序引用完整性检查。
3. 检查活动任务、排产快照和既有报工的风险。
4. 形成独立验证报告并完成任务记录。

## Expected Verification

- 代码搜索覆盖旧工序 ID 可能参与实时查询的主要服务。
- 数据库检查覆盖所有包含 `process_id` 或 `route_process_id` 的 MES 表。
- 明确区分活动业务、已完成历史数据和纯快照引用。
- 所有查询只读且不输出连接凭据。

## 经验门禁

- PowerShell 和中文文件显式 UTF-8，不使用 `&&`。
- 数据库仅在 `SHOW TABLES` / `DESCRIBE` 后执行参数化只读查询。
- 不修改任何租户数据，不使用 mock、fallback 或静默跳过。
- 对旧 ID 的判断同时验证主工序、路线工序、排产快照和下游使用方式。
- 审计结论必须记录命令、路径、数量和可复核样例。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；识别稳定身份和历史快照之间的边界，并定位仍依赖易变工序 ID 的实时路径。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

审计和收尾已完成。结论为系统仍存在多类旧工序 ID 实时风险；未修改生产代码或数据库。

## Cleanup Candidates

- `doc/tasks/20260710-legacy-process-id-audit/legacy_process_audit.py`

## Verification Summary

- 覆盖本机数据库 721 张表，筛选并检查 35 张非备份 MES 工序引用表。
- 所有审计 SQL 均为只读，查询错误数为 0。
- 当前有效 `mes_pro_route_process.process_id` 未发现指向缺失或已删除工序。
- 仍发现活动排产快照、任务、报工、工位和路线 BOM 使用旧工序 ID，详见 `verification-report.md`。
- `task-closeout-cleanup` 预览和应用均通过，仅保留三份核心任务记录。

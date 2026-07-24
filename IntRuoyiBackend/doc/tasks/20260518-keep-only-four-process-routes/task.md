# Task: 仅保留四条工艺路线

## Goal

将本地 MES 工艺路线数据清理为仅保留以下 4 条路线，其他路线按任务计划停用并删除：

- `ROUTE-XLSX-00001`
- `ROUTE-XLSX-00002`
- `ROUTE-YXN.044.02.1020`
- `ROUTE-YXN.069.001.1001`

## Scope

- 基于真实 MySQL 数据检查当前工艺路线及其外部引用。
- 仅对非保留路线执行先停用再删除的数据治理。
- 保留目标路线及其工序、产品、BOM 绑定不变。
- 记录清理脚本、验证证据与最终状态。

## Previous Task Check

- Previous backend task: `doc/tasks/20260517T220502-schedule-calendar-detail-route-workorder-links/task.md`
- Status before this task: blocked.
- Impact: the paused route-link task did not block this route-pruning work.

## Milestones

- [x] M1: Create the task package and execution log.
- [ ] M2: Record BDD scenarios and RED live-database verification.
- [ ] M3: Implement the minimal route-pruning script.
- [ ] M4: Execute live cleanup and complete GREEN verification.
- [ ] M5: Update closeout evidence and commit task-scoped changes.

## Expected Verification

- `python doc/tasks/20260518-keep-only-four-process-routes/scripts/prune_routes_to_four.py --dry-run`
- `python doc/tasks/20260518-keep-only-four-process-routes/scripts/prune_routes_to_four.py`
- `SELECT code, status, deleted FROM mes_pro_route WHERE tenant_id = 1 ORDER BY code`
- `SELECT route_id, COUNT(*) FROM mes_pro_route_process WHERE deleted = 0 GROUP BY route_id`
- `SELECT route_id, COUNT(*) FROM mes_pro_route_product WHERE deleted = 0 GROUP BY route_id`
- `SELECT route_id, COUNT(*) FROM mes_pro_route_product_bom WHERE deleted = 0 GROUP BY route_id`

## Current Status

Blocked on 2026-05-18 because the user switched to the higher-priority defect `点击从 ERP 同步 BOM 之后，工单详情里有乱码`. This route-pruning task must pause without mixing unrelated production-data cleanup with the current encoding fix.

## Blocker And Impact

- Blocker: the active turn priority changed to the live work-order ERP BOM garbled-text defect.
- Impact: this route-pruning task stops after initial live-data inspection and must resume later from `M2` with its own isolated verification and commit scope.

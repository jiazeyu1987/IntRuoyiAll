# 执行日志：MES 工单清空全部排产阻断 后端实现

BDD: 工单所有排产前置齐备后不再阻断 -> Given 工单涉及的工作站、产线、排班计划与产能前置均已满足 / When 执行排产预览 / Then 系统不再返回该工单的阻断项。

BDD: 仍有缺失时继续暴露真实阻断 -> Given 工单还存在其它缺失前置 / When 执行排产预览 / Then 系统继续返回下一条真实阻断，而不是静默通过。

GREEN: previous-task-check -> PASS，上一后端任务 `20260628-mes-process-line-blocker-fix` 已完成。
GREEN: experience-index-hit -> PASS，已命中并读取 `docs/powershell-memory.md`。
GREEN: experience-index-hit-login -> PASS，已命中并读取 `docs/login-access.md`。
GREEN: experience-preflight -> PASS，已确认本次仅在本机运行态执行真实登录、预览复验与必要主数据修复；后续所有库写入前均先复现当前阻断。
GREEN: login-preflight -> PASS，官方 `login-preflight.mjs` 已真实登录本机 `芋道源码/admin` 并进入 `/mes/pro/task`。
GREEN: preview-recheck -> PASS，使用本机 admin token 真实调用 `/admin-api/mes/pro/auto-schedule/preview`，请求 `scheduleOrderIds=[48] / workOrderIds=[925553] / capacityMode=PLANNED / preserveManualLockedTasks=true`，返回 `blockingIssueCount=0`。
GREEN: warning-scope-check -> PASS，当前 `issues` 仅剩 `MATERIAL_DEMAND`（工单缺少物料需求）与 `LATEST_START`（计划开工时间晚于最晚开工时间）两条 warning，无 blocking issue。
GREEN: readonly-master-data-check -> PASS，`mes_md_workstation(900113-900121)` 均为 `production_line_id=900040`、`status=0`；`mes_md_production_line(900040)` 为启用状态并已绑定 `calendar_plan_id=900030`。

# 执行日志：MES 手动重排今天/明天整天重排

## BDD
- BDD: 应用重排选择今天整天重排 -> Given 已选择可重排排产工单 / When 用户点击应用重排并选择从今天开始 / Then 系统从今天日期锚点重新检查、预览并应用，任务不再按当前剩余分钟切分。
- BDD: 应用重排选择明天整天重排 -> Given 已选择可重排排产工单 / When 用户点击应用重排并选择从明天开始 / Then 系统从明天日期锚点重新检查、预览并应用，生成任务不得早于明天可用班次。
- BDD: 应用前重算发现阻断 -> Given 用户选择今天或明天后排产前检查或预览存在阻断 / When 确认应用 / Then 系统停止写入并展示真实阻断错误。

## Evidence
- GREEN: experience-preflight -> PASS, 已读取 `docs/powershell-memory.md`、`docs/login-access.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，真实 E2E 前将先运行官方登录前置。
- RED: backend replan startTime normalization tests -> FAIL, 旧实现会在手动重排中继续使用传入时刻或模拟当前时间，无法证明今天整天 / 明天整天语义。
- RED: frontend whole-day apply static contract -> FAIL, 旧实现点击应用重排不弹出今天/明天选择，且不会在应用前按所选日期重新执行 preflight + preview。
- GREEN: backend targeted tests -> PASS, `mvn --% -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest#preview_shouldBlockWhenProductionMaterialListMissing+replanPreview_shouldNormalizeRequestStartTimeToWholeDayDate+replanPreview_shouldStartFromTomorrowWholeDayWhenTomorrowDateIsSelected+preview_shouldKeepExactRequestStartTimeForNormalAutoSchedule -Dsurefire.failIfNoSpecifiedTests=false test`。
- GREEN: backend schedule service regression -> PASS, `mvn --% -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；Tests run: 50, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: frontend static contract -> PASS, `node tests\e2e\mes-replan-whole-day-apply-static.spec.js`。
- GREEN: frontend e2e script syntax -> PASS, `node --check tests\e2e\mes-replan-whole-day-apply-real-flow.e2e.js`。
- GREEN: frontend schedule type check -> PASS, `pnpm ts:check:schedule`。
- GREEN: real Playwright E2E -> PASS, 测试租户 `aoteman` 登录 `http://localhost:8081`，选择真实排产工单 `SCH-TESTERPF102B88DA0E7-20260706-0001`，点击“应用重排”分别选择“从今天开始重排”和“从明天开始重排”，请求体分别为 `2026-07-06 00:00:00` 与 `2026-07-07 00:00:00`，preflight、preview、apply 均按所选日期重新发送，apply 携带本次 preview 的 `calendarContextToken`，页面无 console error / page error。
- GREEN: readonly DB verification -> PASS, `SELECT work_order_id, MIN(start_time), MAX(start_time), COUNT(*) FROM mes_pro_task WHERE work_order_id = 925853 AND deleted = 0 GROUP BY work_order_id;` 返回 `work_order_id=925853, earliest_start_time=2026-07-09 11:50:00, latest_start_time=2026-07-14 08:00:00, task_count=53`；最终明天重排后的任务最早开始时间晚于 `2026-07-07 00:00:00`。
- GREEN: material blocker consistency -> PASS, 缺少生产用料清单的工单在 preview 阶段保持 `BLOCKING`，前端应用前停止，不再出现旧预览放行、最终 apply 报 `系统异常` 的不一致。

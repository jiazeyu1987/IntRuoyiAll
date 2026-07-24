# 执行日志

BDD: 班次产能缺失补齐 -> Given 手动重排前置检查提示班次产能缺失, When 使用系统默认数值补齐缺失日期/产线/班次产能, Then 再次检查不应再因班次产能缺失阻断。
GREEN: experience-preflight -> PASS, 已按经验门禁核对 PowerShell UTF-8、真实库字段、测试租户范围；本次写入仅限本机 MySQL 测试租户 122 的实际班次产能。
RED: sql actual capacity gap -> FAIL, mes_pro_capacity_actual 缺少 tenant_id=122、line_id=900042、shift_id=900033 自 2026-07-06 起的实际班次产能；计划产能默认值为 450 分钟。
GREEN: sql fill actual capacity -> PASS, BEFORE_MISSING_COUNT=475，inserted_rows=475，AFTER_MISSING_COUNT=0；样例日期 2026-07-06 至 2026-07-14 均为 450 分钟。
GREEN: real login preflight -> PASS, 使用本机 http://localhost:8081 测试租户/aoteman 真实登录进入 /mes/pro/schedule-order。
GREEN: actual capacity replan preview -> PASS, scheduleOrderId=125, startTime=2026-07-06 00:00:00, capacityMode=ACTUAL；preflightResult=PASS，previewBlockingIssueCount=0，previewGeneratedTaskCount=53；响应中不再出现“班次产能缺失”和 1970-01-01。
GREEN: sql verify actual capacity -> PASS, tenant_id=122 / line_id=900042 / shift_id=900033 自 2026-07-06 至 2027-12-31 实际产能缺失数=0。GREEN: active runtime backend check -> PASS, 8081 前端进程连接 48081 后端；48081 后端运行参数指向 127.0.0.2:23306/ruoyi-vue-pro 当前数据库。
RED: active runtime 1970 actual capacity gap -> FAIL, 计划产能 PLAN_1970_122 已存在 450 分钟，但实际产能 ACTUAL_1970_122 缺失。
GREEN: fill active runtime 1970 actual capacity -> PASS, 新增 tenant_id=122 / line_id=900042 / shift_id=900033 / calendar_date=1970-01-01 00:00:00 / capacity_minutes=450。
GREEN: active runtime replan capacity check -> PASS, 对 startTime=1970-01-01 00:00:00 与 2026-07-06 00:00:00、PLANNED 与 ACTUAL 两种产能口径验证，preflightCode=0、previewCode=0，均不再返回“班次产能缺失”。
BDD: admin 租户班次产能复验 -> Given 用户要求使用芋道源码/admin 测试, When 真实登录进入排产页面并执行重排检查, Then 必须暴露 admin 租户真实阻断并按默认产能补齐最小缺口。
GREEN: experience-preflight -> PASS, 已读取 PowerShell 与登录经验门禁；用户已明确指定本机 芋道源码/admin 复验，后续高风险写入前先核 tenant-id、账号、记录主键、产线/班次/日期缺口。

RED: admin actual capacity gap -> FAIL, 使用本机 `芋道源码/admin` 真实登录进入 `/mes/pro/schedule-order`，页面列表返回 `scheduleOrderId=88`；只读 SQL 确认 `tenant_id=1 / line_id=900040 / shift_id=900031 / AUTO-DAY` 在 `1970-01-01`、`2026-07-06`、`2026-07-07` 存在计划产能 930 分钟但实际产能缺失。
GREEN: admin actual capacity fill -> PASS, `tenant_id=1 / line_id=900040 / shift_id=900031 / AUTO-DAY` 在 `1970-01-01`、`2026-07-06`、`2026-07-07` 已按默认整班 `08:00-23:30 = 930` 分钟补齐实际产能，新增 3 条。
GREEN: admin replan capacity missing verification -> PASS, 使用 `芋道源码/admin` 对 `scheduleOrderId=88` 执行 `PLANNED/ACTUAL` 与 `1970-01-01/2026-07-06/2026-07-07` 六组重排预览，接口均 `code=0` 且 `hasCapacityMissing=false`。
NOTE: admin remaining blocker -> `ACTUAL` 口径仍返回 `BLOCKING:CAPACITY / 产线可用班次产能不足`，这是产能不足而非班次产能缺失；`PLANNED` 口径可生成 47 个任务且无阻断。

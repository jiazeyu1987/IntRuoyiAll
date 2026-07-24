# Execution Log: 工艺排产路线弹窗接入标准列表模板

BDD: 工艺排产路线使用标准列表模板 -> Given 用户打开排产工单的工艺排产路线弹窗 / When 工序列表渲染 / Then 列表使用 `UnifiedListTemplate` 并保留展开报工明细能力。
BDD: 工序列表展示班次状态 -> Given 工序存在白班或夜班排产属性 / When 用户查看工艺排产路线弹窗 / Then 表格展示“班次状态”列，显示白班或夜班。
BDD: 工序列表展示预计完成时间 -> Given 工序存在计划结束时间 / When 用户查看工艺排产路线弹窗 / Then 表格展示“预计完成时间”列并格式化时间。
GREEN: experience-preflight -> PASS，已读取 PowerShell、前端样式、登录/E2E 与项目防错经验门禁；本次只改本机前端源码与测试，E2E 前执行官方登录预检。
RED: `node tests/e2e/mes-schedule-order-process-route-unified-list-static.spec.js` -> FAIL，当前“工艺排产路线”弹窗仍是裸 `el-table`，缺少 `UnifiedListTemplate`、工序列配置、班次状态与预计完成时间列。
GREEN: `node tests/e2e/mes-schedule-order-process-route-unified-list-static.spec.js` -> PASS，弹窗已接入 `UnifiedListTemplate`，列表具有稳定 tableKey、列配置、快速过滤、班次状态和预计完成时间列。
GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS，前端 TypeScript relaxed 检查通过。
GREEN: `node "D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs" --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/schedule-order --target-text 排产工单` -> PASS，官方登录预检已用系统 Chrome 真实登录测试租户并进入目标页。
RED: `node tests/e2e/mes-schedule-order-process-route-unified-list-real.e2e.js` -> FAIL，首个可见排产工单的旧工序快照 `nightShiftEnabled` 为空，无法真实验证班次状态；按无 fallback 原则改为在真实列表中逐行打开“查看”，定位带正式班次字段的工单。
GREEN: `node tests/e2e/mes-schedule-order-process-route-unified-list-real.e2e.js` -> PASS，真实浏览器打开 `/mes/pro/schedule-order`，通过 UI 逐行查看并定位 `scheduleOrderId=134` 的真实工序快照，验证标准列表模板、用户列配置 key、班次状态“白班”、预计完成时间列、分页和无 MES 写请求。

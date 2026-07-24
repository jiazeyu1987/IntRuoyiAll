# Execution Log：排产工单人工完成与未完成筛选

BDD: 排产工单默认只看未完成 -> Given 用户首次进入排产工单页 / When 页面发起分页请求 / Then 默认请求 completionFilter=INCOMPLETE，列表不展示已完成工单。
BDD: 排产员可人工设完成并二次确认 -> Given 某行工单未完成且当前用户具备人工完成权限 / When 用户填写完成原因并确认二次弹窗 / Then 页面调用人工完成接口，刷新后该行显示已完成、100% 并从未完成筛选中消失。
BDD: 已人工完成工单保持真实工序提示 -> Given 某工单已人工完成 / When 用户打开工艺排产路线弹窗 / Then 页面提示该工单列表口径已人工完成，但工序表仍按真实报工展示。
BDD: 撤销已完成仅对撤销权限用户显示 -> Given 某工单已人工完成 / When 当前用户没有撤销权限 / Then 页面不显示撤销已完成按钮。

GREEN: experience-preflight -> PASS，本轮高风险动作限定为本机 8081/48081 真实登录与真实页面验证；已读取登录/PowerShell 门禁，不触碰测试服/正式服。
RED: `@' ... git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 show HEAD:src/views/mes/pro/scheduleorder/index.vue 与 src/api/mes/pro/scheduleorder/index.ts，再断言 completionFilter / manualFinishDialogVisible / manualFinishScheduleOrder / revokeManualFinishScheduleOrder 存在 ... '@ | python -X utf8 -` -> FAIL，旧版 `HEAD` 缺少完成筛选和人工完成交互，`AssertionError: HEAD missing token: completionFilter`。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> PASS。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-manual-finish-static.spec.js` -> PASS。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-manual-finish-real-flow.e2e.js` -> PASS，结果 `status=PASS`、`scheduleOrderId=9`、`workOrderCode=CODexERP20260610B`、`plannerUsername=smokeplan1`、`adminUsername=smokeappr1`。

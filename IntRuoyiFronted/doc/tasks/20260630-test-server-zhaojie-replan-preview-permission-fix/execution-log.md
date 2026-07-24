# Execution Log：测试服 zhaojie 预览重排无权限修复（前端）

- `2026-06-30 任务创建`：建立前端任务文档，目标是把排产工单手动重排入口与 `mes:pro-auto-schedule:replan` 鉴权合同对齐。
- `BDD: 无 replan 权限时不暴露手动重排入口 -> Given 用户缺少 mes:pro-auto-schedule:replan / When 打开排产工单页 / Then 工具栏不显示“手动重排”按钮。`
- `BDD: 无 replan 权限时程序化打开也应 fail fast -> Given URL 或其他动作触发 openReplanDrawer / When 当前用户缺少 mes:pro-auto-schedule:replan / Then 页面提示无手动重排权限且不打开抽屉。`
- `BDD: 有 replan 权限时预览与应用入口一致受控 -> Given 用户拥有 mes:pro-auto-schedule:replan / When 打开手动重排抽屉 / Then “预览重排”和“应用重排”入口继续可见并与后端合同一致。`
- `GREEN: ui-contract-root-cause -> PASS`，已确认当前 `scheduleorder/index.vue` 的“手动重排/预览重排/应用重排”没有绑定 `mes:pro-auto-schedule:replan`，而 `previewReplan/applyReplan` 实际调用的是自动排产 `replan` API。
- `RED: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-usability-static.spec.js -> FAIL`，当前页面尚未对手动重排入口加 `mes:pro-auto-schedule:replan` 门禁。
- `RED: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-pool-static.spec.js -> FAIL`，当前 `openReplanDrawer` 仍允许无权限程序化打开抽屉。
- `CHANGE: src/views/mes/pro/scheduleorder/index.vue`，已为“手动重排”按钮增加 `v-hasPermi="['mes:pro-auto-schedule:replan']"`，并新增 `hasReplanPermission` 计算属性；“预览重排”“应用重排”仅在有权限时渲染，`openReplanDrawer` 对无权限用户直接 warning 并 return。
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-usability-static.spec.js -> PASS`。
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-pool-static.spec.js -> PASS`。

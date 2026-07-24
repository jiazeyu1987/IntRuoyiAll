# 执行日志：20260628-schedule-calendar-past-card-gray

BDD: 历史日期卡片默认呈现淡灰底色 -> Given 用户打开排程日历 / When 某个日期早于今天且不可编辑 / Then 该卡片应显示淡灰色背景，便于与今天及未来日期区分。

BDD: 历史日期卡片悬停时仍保持历史态视觉 -> Given 用户把鼠标移到今天之前的卡片 / When 卡片保持只读状态 / Then 悬停不应回退成普通白底，而应继续保持淡灰色背景。

RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-past-card-gray-static.spec.js` -> FAIL，当前 `.calendar-cell.is-readonly-past` 虽然存在只读 class，但缺少单独的淡灰底色与 hover 保持规则，历史日期仍可能回退到普通白底视觉。

CHANGE: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\task\calendar\index.vue` 为 `.calendar-cell.is-readonly-past` 增加 `background: #f3f4f6;`，并新增 `.calendar-cell.is-readonly-past:hover { background: #f3f4f6; }`；`tests\e2e\mes-pro-schedule-calendar-past-card-gray-static.spec.js` 锁定历史卡片 class、默认底色和 hover 底色约束。

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-past-card-gray-static.spec.js` -> PASS

BLOCKER: `pnpm ts:check` -> FAIL，当前工作区锁文件供应链策略校验因既有 tarball URL mismatch 被拦截，与本次样式改动无关。

BLOCKER: `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> FAIL，暴露既有无关类型错误：`src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue` 的 `recordCategory: "TEMPLATE"` 与 `EdhrRecordCategory` 不兼容。

# 执行日志：排程日历右侧详情/规则页签调整

## 2026-06-24

- `BDD: 默认显示日详情 -> Given 用户进入排程日历 / When 页面初始化 / Then 右侧第一个页签为当前选中日期日详情且默认选中。`
- `BDD: 排程规则进入第二页签 -> Given 用户查看右侧页签 / When 切换到排程规则 / Then 看到保存规则、周末模式、模拟推进、生成未来产能、生成预览和发布排产。`
- `BDD: 日详情随选中日期更新 -> Given 用户点击日历其他日期 / When selectedDate 更新 / Then 第一个页签文案同步为新的 selectedDayTitle。`
- 已读取 `docs/experience-index.md`，本任务命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `RED: node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js -> FAIL, expected reason: 旧结构缺少 activeSidebarTab，右侧仍为上下两个面板且日详情不在第一个 tab。`
- `GREEN: experience-preflight -> PASS，本次真实 Playwright 仅访问本机 http://localhost:8081，使用测试租户 aoteman 只读验证排程日历页签，不操作测试服/正式服，不写入业务数据。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js -> PASS，右侧页签默认 detail，日详情为第一个动态 tab，排程规则为第二个 tab。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js -> PASS，产能生成静态合同未破坏。`
- `GREEN: node scripts/schedule-calendar-inline-shift-editor.test.mjs -> PASS，5 tests passed，日历格内班次编辑回归未破坏。`
- `GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。`
- `GREEN: real-playwright-local-readonly -> PASS，http://localhost:8081 登录 测试租户/aoteman 后进入 /mes/pro/schedule-calendar，默认日详情 tab、排程规则 tab 内容、点击日期后日详情 tab 标题同步均通过。`
- `GREEN: task-closeout-cleanup preview -> PASS，初次预览建议删除 frontend-feature-evidence.md；因该文件是本任务计划与前端交付证据要求的正式产物，已加入 Cleanup Keep 后重新预览。`
- `GREEN: task-closeout-cleanup preview -> PASS，Cleanup Keep 生效后无删除项、无阻塞项、无警告。`
- 任务状态：已完成，等待提交。

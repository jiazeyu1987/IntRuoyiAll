# Execution Log：排产工单主表与生成工单列表换行完整显示

- `BDD: 排产工单主表长编码列在列宽不足时换行完整显示 -> Given 排产编码、工单编码或产品编号很长 / When 主表渲染 / Then 三列都允许换行并显示全量内容。`
- `BDD: 生成工单列表长文本单元在列宽不足时换行完整显示 -> Given 待同步差异列表中的工单编码、产品编号、产品名称、规格型号或不可排原因很长 / When 列表渲染 / Then 这些单元都允许换行并显示全量文本。`
- `BDD: 其它列表列保持原有密度 -> Given 仅修复目标文本列 / When 页面渲染 / Then 未命中的列仍保持既有 tooltip/单行策略。`
- `GREEN: experience-preflight -> PASS，已按门禁读取 docs\experience-index.md、docs\powershell-memory.md、D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md 与 frontend-feature-delivery 技能。`
- `GREEN: bug-triage -> PASS，用户最新截图先后确认问题覆盖 排产工单主表 与 生成工单/待同步差异 列表；当前目标列为 主表的 排产编码 / 工单编码 / 产品编号，以及 待同步差异列表的 工单编码 / 产品编号 / 产品名称 / 规格型号 / 不可排原因。`
- `RED: git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 show HEAD:src/views/mes/pro/scheduleorder/index.vue -> FAIL，旧版源码不包含主表的 schedule-order-pool__main-table__cell--wrap 合同，也不包含待同步差异列表的 schedule-order-pool__admission-table__cell--wrap 合同。`
- `CHANGE: src/views/mes/pro/scheduleorder/index.vue，给主表新增 getMainTableCellClassName / schedule-order-pool__main-table-text，并给待同步差异列表新增 getAdmissionCellClassName / schedule-order-pool__admission-cell-text，仅对目标长文本列开放换行显示。`
- `CHANGE: tests/e2e/mes-schedule-order-main-table-wrap-static.spec.js，新增主表三列换行静态合同。`
- `CHANGE: tests/e2e/mes-schedule-order-admission-wrap-static.spec.js，新增待同步差异列表五列换行静态合同。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-main-table-wrap-static.spec.js -> PASS。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-admission-wrap-static.spec.js -> PASS。`

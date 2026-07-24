BDD: 设备详情展示最终版工序明细 -> Given 某设备在最终版 Excel 中对应多条工序记录 When 用户打开设备台账详情 Then 页面展示该设备的全部工序明细，而不是把多条记录错误压平到主表单值

BDD: 工序明细展示产线名称而不是 Excel 行号 -> Given 后端明细已提供 `lineName` When 用户查看工序明细页签 Then 表头显示 `产线名称`，并用真实产线值替代 `Excel源行号`

GREEN: pnpm exec eslint src/api/mes/dv/machinery/process.ts src/views/mes/dv/machinery/MachineryProcessList.vue src/views/mes/dv/machinery/MachineryForm.vue -> PASS

GREEN: Playwright 搜索并打开 A03190 详情 -> PASS，工序明细表头显示 `产线名称`，页面中不再出现 `Excel源行号`

GREEN: Playwright 工序明细抽样 -> PASS，`A03190` 明细中可见 `球囊扩张导管`、`棘突球囊扩张导管` 两条产线记录

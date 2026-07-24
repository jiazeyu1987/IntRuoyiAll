# Task: 设备台账最终版前端展示对齐

## Goal

在设备台账详情页展示最终版 Excel 对应的设备工序明细，并把明细表最后一列从 `Excel源行号` 调整为 `产线名称`。

## Milestones

- [x] M1: 确认最终版 Excel 规则与前端展示边界。
- [x] M2: 扩展工序明细 API 类型与详情组件。
- [x] M3: 完成设备详情工序明细联调。
- [x] M4: 将 `Excel源行号` 替换为 `产线名称` 并展示真实产线值。
- [x] M5: 完成页面验证并整理本次前端改动。

## Current Status

已完成。

## Expected Verification

- 工序明细表头显示 `产线名称`。
- 工序明细不再显示 `Excel源行号`。
- 多工序设备详情能展示真实产线数据，例如 `A03190` 同时显示 `球囊扩张导管` 和 `棘突球囊扩张导管`。

## Final Verification

- 定向 ESLint 通过：
  `pnpm exec eslint src/api/mes/dv/machinery/process.ts src/views/mes/dv/machinery/MachineryProcessList.vue src/views/mes/dv/machinery/MachineryForm.vue`
- Playwright 页面验证通过：
  打开 `http://localhost:8081/mes/dv/machinery`，搜索并打开 `A03190` 详情。
  工序明细表头显示 `产线名称`。
  页面中不再出现 `Excel源行号`。
  明细中可见 `球囊扩张导管` 和 `棘突球囊扩张导管` 两条产线记录。

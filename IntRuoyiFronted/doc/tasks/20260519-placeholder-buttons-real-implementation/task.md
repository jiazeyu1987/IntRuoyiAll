# 任务：补齐占位按钮真实实现

## 目标

将当前前端中已暴露给用户、但没有实际业务能力的占位按钮逐个补齐为真实可用功能，覆盖查询、模板选择、标签打印与发货执行出库入口。

## 范围

- 修复 DCC 受控文件岗位页“查询”按钮与回车查询入口。
- 修复 DCC 受控文件目录页“查询”按钮与回车查询入口。
- 为 MES 条码配置页“默认打印模板 -> 设置”补齐真实选择流程。
- 为 MES 通用“标签打印”按钮补齐真实打印/预览流程。
- 为 MES 发货通知单“执行出库”补齐真实业务提交流程。
- 为上述可观察行为补齐前端测试与执行证据。

## 非范围

- 不重做无关页面的视觉设计。
- 不引入 mock、fallback 或“提示已实现但实际无能力”的假流程。
- 不修改无关模块的业务规则。

## 前置任务检查

- 上一个前端任务：`yudao-ui-admin-vue3/doc/tasks/20260519-test-tenant-login-baseline/task.md`
- 启动前状态：已完成。
- 影响：可独立开展本次前端占位按钮补齐任务。

## 里程碑

- [x] M1：确认前置任务完成并创建前端任务包。
- [x] M2：记录 BDD 场景，逐项核实前端入口与后端/路由前置能力。
- [x] M3：补失败测试，覆盖查询、模板选择、标签打印和执行出库行为。
- [x] M4：完成最小实现并通过针对性回归。
- [x] M5：更新证据、任务文档并执行收尾预览。

## 预期验证

- 针对本任务新增或更新的 `node --test` 前端脚本。
- `pnpm exec eslint` / `pnpm exec vue-tsc` 针对受影响文件的静态校验。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-placeholder-buttons-real-implementation\frontend-feature-evidence.md`

## Current Status

Completed on 2026-05-19. 代码实现、RED/GREEN 回归、ESLint、证据校验与收尾预览均已完成。

## 阻塞与影响

- 阻塞：仓库级 `vue-tsc` 仍被既有无关类型错误阻断，当前未发现本任务改动文件的新增类型错误。
- 影响：“发货通知单执行出库”已按现有真实业务链路改为进入销售出库单流程；“默认打印模板”已具备真实配置能力，但模板路径暂未接入标签打印版式渲染。

## Final Verification Result

- PASS：`node --test scripts/placeholder-buttons-real-implementation.test.mjs`
- PASS：`pnpm exec eslint src/views/dcc/controlled-file/positions/index.vue src/views/dcc/controlled-file/directories/index.vue src/api/mes/wm/productsales/index.ts src/views/mes/wm/barcode/components/PrinterLabel.vue src/views/mes/wm/productsales/ProductSalesForm.vue src/views/mes/wm/salesnotice/SalesNoticeForm.vue src/views/mes/wm/barcode/config/BarcodeConfigForm.vue src/views/mes/wm/barcode/config/components/BarcodeTemplateSelectDialog.vue scripts/placeholder-buttons-real-implementation.test.mjs`
- PASS：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-placeholder-buttons-real-implementation\frontend-feature-evidence.md`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-placeholder-buttons-real-implementation --mode preview`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-placeholder-buttons-real-implementation --mode apply`
- INFO：`pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` 在增大 Node heap 后完成运行，但仍被仓库既有无关类型错误阻断，未发现本任务改动文件新增报错。

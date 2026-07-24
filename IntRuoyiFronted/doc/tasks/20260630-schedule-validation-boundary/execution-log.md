# 20260630 排产专用前端类型检查执行日志

## BDD

- `BDD: 排产专用类型检查只覆盖排产链路 -> Given 全仓 ts:check 会扫描所有 src 页面 / When 运行排产专用类型检查 / Then 只校验排产链路依赖的页面与 API。`
- `BDD: eDHR 页面不再阻塞排产专用检查 -> Given eDHR 页面当前存在独立类型错误 / When 运行排产专用类型检查 / Then 不会因为 eDHR 页面而失败。`
- `BDD: 全量前端类型检查仍保留真实问题暴露 -> Given 仓库仍需要完整类型校验入口 / When 继续运行全仓 ts:check / Then eDHR 当前问题仍会被真实暴露。`

## TDD Evidence

- `RED: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> FAIL, eDHR 页面 src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue:158,305 继续报 recordCategory 类型不兼容。`
- `GREEN: pnpm ts:check:schedule -> PASS`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-validation-boundary-static.spec.js -> PASS`

## Notes

- `schedule-tsc-explain.txt` 已保留在同任务目录，证明原链路曾通过 `src/config/axios/service.ts -> @/router -> router/modules/remaining.ts -> eDHR 页面` 被整套路由拖入。
- 本次收敛后，全量 `ts:check` 仍真实暴露 eDHR 问题；排产专用 `ts:check:schedule` 可独立通过。

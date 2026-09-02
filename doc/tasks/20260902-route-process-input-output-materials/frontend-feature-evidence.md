# Frontend Feature Evidence

## Feature Goal
工艺路线流程图工序属性面板将原“批记录物料”拆成“输入物料”和“输出物料”，保存时分别提交 `inputMaterialIds` 与 `outputMaterialIds`。

## Non-Goals
- 不新增表单槽位推断。
- 不做真实 E2E，用户本轮未明确要求 E2E。
- 不改变物料主数据选择接口。

## UI Entry Points
- `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`
- `IntRuoyiFronted/src/api/mes/pro/route/flowconfig.ts`

## API/Data States
- 前端接口类型暴露 `inputMaterialIds?: number[]` 和 `outputMaterialIds?: number[]`。
- 空输入/输出按空数组处理。

## BDD
BDD: 工艺路线编辑器显示双物料字段 -> Given 用户打开工艺路线工序属性面板 When 查看批记录配置 Then 页面显示“输入物料”和“输出物料”两个独立选择器。
BDD: 保存双物料字段 -> Given 用户分别选择输入物料和输出物料 When 保存工序批记录配置 Then 前端载荷提交 `inputMaterialIds` 和 `outputMaterialIds`，不提交旧字段。

## Acceptance
- 页面存在 `data-route-process-setting-field="input-material"` 和 `data-route-process-setting-field="output-material"`。
- 前端 API 类型存在 `inputMaterialIds?: number[]` 和 `outputMaterialIds?: number[]`。
- 前端路线编辑器不再读写 `frontlineReportMaterialIds`。

## TDD
RED: `node IntRuoyiFronted\tests\e2e\route-process-input-output-materials-static.spec.cjs` -> FAIL，前端主编辑器仍保留旧 `frontlineReportMaterialIds` 字段和单一“批记录物料”选择器。
GREEN: `node IntRuoyiFronted\tests\e2e\route-process-input-output-materials-static.spec.cjs` -> PASS。

## Verification
- `node IntRuoyiFronted\tests\e2e\route-process-input-output-materials-static.spec.cjs` -> PASS。
- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> BLOCKED，当前 worktree 缺少 `node_modules`，`vue-tsc` 不可解析。

## Blockers
- 暂无。

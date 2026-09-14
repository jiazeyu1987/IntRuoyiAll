# Verification Report

## Summary
- 后端批记录输入/输出物料保存、读取、发布快照和一线报工输出物料读取已通过定向测试。
- 前端工艺路线编辑器已通过静态契约测试，确认显示输入物料/输出物料并提交 `inputMaterialIds` / `outputMaterialIds`。
- 前端类型检查未运行成功，原因是当前 worktree 缺少 `node_modules`，`vue-tsc` 不可解析。

## Commands
- PASS: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteFlowConfigServiceImplTest,MesProRouteServiceImplTest,MesFrontlineProcessMaterialServiceTest" test` -> 71 tests, 0 failures, 0 errors。
- PASS: `node IntRuoyiFronted\tests\e2e\route-process-input-output-materials-static.spec.cjs`。
- PASS: `rg -n "frontlineReportMaterialIds|inputMaterialIds|outputMaterialIds" IntRuoyiBackend\yudao-module-mes\src IntRuoyiFronted\src IntRuoyiFronted\tests -S`。
- BLOCKED: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> `vue-tsc` not found because `node_modules` is absent。

## Acceptance
- 原批记录物料已改为输出物料。
- 新增输入物料字段，与输出物料独立保存、回显和发布快照。
- 一线报工只读取输出物料，不把输入物料作为填写批号/数量的要求。
- 未通过 `formBindings`、工序开始配置或相邻工序推断补齐批记录物料。

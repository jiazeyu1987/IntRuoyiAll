# Execution Log

BDD: 批记录物料拆成输入输出 -> Given 工艺路线工序配置批记录物料 When 用户保存并重新读取批记录配置 Then 输入物料从 `inputMaterialIds` 回显，输出物料从 `outputMaterialIds` 回显，旧 `frontlineReportMaterialIds` 不再作为正式字段。

BDD: 发布快照保留输入输出 -> Given 候选路线存在批记录输入物料和输出物料 When 发布路线版本 Then 当前 active 快照保留两个独立字段，并且不把输入物料作为一线报工物料。

BDD: 一线报工只读取输出物料 -> Given 冻结工序快照同时存在输入物料和输出物料 When 一线加载当前工序报工物料 Then 只返回输出物料，输入物料不要求填写完成数量、损耗数量和批号。

BDD: 空输入或空输出保持空列表 -> Given 某工序输入或输出为无 When 保存批记录配置 Then 对应字段为空列表，不写占位物料，也不从相邻工序推断。

## Rule Reads
- 2026-09-02: 读取 `docs/backend-development.md`、`docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/worktree-restrictions.md`、`docs/powershell-encoding.md`。
- 2026-09-02: 使用 `backend-api-delivery` 与 `frontend-feature-delivery` 技能，读取对应 evidence contract。

## TDD Evidence
- RED: 既有 worktree 已存在后端测试改动，当前继续任务时未能恢复原始 RED 输出；验证阶段将记录当前 GREEN/REGRESSION 结果。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteFlowConfigServiceImplTest,MesProRouteServiceImplTest,MesFrontlineProcessMaterialServiceTest" test` -> PASS，71 tests, 0 failures, 0 errors。
- GREEN: `node IntRuoyiFronted\tests\e2e\route-process-input-output-materials-static.spec.cjs` -> PASS。
- REGRESSION: `rg -n "frontlineReportMaterialIds|inputMaterialIds|outputMaterialIds" IntRuoyiBackend\yudao-module-mes\src IntRuoyiFronted\src IntRuoyiFronted\tests -S` -> PASS，生产代码仅保留 `inputMaterialIds` / `outputMaterialIds` 正式字段；旧字段仅在测试中作为不得存在断言。
- BLOCKER: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> BLOCKED，`node_modules` 不存在且 `vue-tsc` 不可解析；未按无授权环境变更自动安装依赖。

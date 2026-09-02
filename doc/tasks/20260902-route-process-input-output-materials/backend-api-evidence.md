# Backend API Evidence

## Scope
- 工艺路线批记录配置保存/读取 VO、候选配置解析、发布快照构建和一线报工物料读取。

## API Contract
- `MesProRouteFlowProcessConfigSaveReqVO` 使用 `inputMaterialIds` 和 `outputMaterialIds`。
- `MesProRouteFlowProcessConfigRespVO` 返回 `inputMaterialIds` 和 `outputMaterialIds`。
- `frontlineReportMaterialIds` 不再作为当前正式批记录物料字段。

## Data Contract
- 输入物料和输出物料均为当前工序显式配置的物料 ID 列表。
- 发布快照 `batchUseConfigs` 保留 `inputMaterialIds` 与 `outputMaterialIds`。
- 一线报工从冻结快照读取 `outputMaterialIds`。

## Auth, Permissions, Validation, Errors
- 本任务不新增权限点。
- 保存时合并校验输入/输出物料 ID 的正式物料存在性。
- 快照结构异常沿用现有 fail-fast 异常，不返回默认成功。

## BDD
BDD: 批记录物料拆成输入输出 -> Given 工艺路线工序配置批记录物料 When 用户保存并重新读取批记录配置 Then 输入物料从 `inputMaterialIds` 回显，输出物料从 `outputMaterialIds` 回显，旧 `frontlineReportMaterialIds` 不再作为正式字段。
BDD: 发布快照保留输入输出 -> Given 候选路线存在批记录输入物料和输出物料 When 发布路线版本 Then 当前 active 快照保留两个独立字段，并且不把输入物料作为一线报工物料。
BDD: 一线报工只读取输出物料 -> Given 冻结工序快照同时存在输入物料和输出物料 When 一线加载当前工序报工物料 Then 只返回输出物料，输入物料不要求填写完成数量、损耗数量和批号。

## TDD
RED: 当前为继续已有 worktree，后端测试改动已存在，原始失败输出不可恢复；此项记录为续作证据缺口。
GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteFlowConfigServiceImplTest,MesProRouteServiceImplTest,MesFrontlineProcessMaterialServiceTest" test` -> PASS，71 tests, 0 failures, 0 errors。

## Verification
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteFlowConfigServiceImplTest,MesProRouteServiceImplTest,MesFrontlineProcessMaterialServiceTest" test` -> PASS，71 tests, 0 failures, 0 errors。
- `rg -n "frontlineReportMaterialIds|inputMaterialIds|outputMaterialIds" IntRuoyiBackend\yudao-module-mes\src IntRuoyiFronted\src IntRuoyiFronted\tests -S` -> PASS，旧字段仅作为测试断言出现。

## Blockers
- 暂无。

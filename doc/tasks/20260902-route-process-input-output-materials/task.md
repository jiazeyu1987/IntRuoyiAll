# 工艺路线批记录物料输入输出改造

## Task Goal
将工艺路线工序里的批记录物料从单一“物料”改为“输出物料”，并新增独立“输入物料”。输入物料和输出物料需按用户提供的工序文档维护，编号相同代表同一批物料，但系统不得跨工序、跨链路自动推断补齐。

## Milestones
- [x] 建立任务记录，读取后端、前端、worktree、PowerShell 和收尾规则。
- [x] 梳理现有工艺路线批记录物料字段、工序保存/读取、发布快照和一线报工读取链路。
- [x] 设计字段合同：批记录配置使用 `inputMaterialIds` 与 `outputMaterialIds`，原批记录物料改为输出物料。
- [x] 按 BDD/TDD 补后端保存、读取、发布快照和一线报工测试。
- [x] 实现后端与前端最小改造。
- [x] 运行定向验证，记录 RED/GREEN/REGRESSION。
- [x] 完成验证报告；提交、推送和 worktree closeout 待用户明确授权。

## Expected Verification
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProRouteFlowConfigServiceImplTest,MesProRouteServiceImplTest,MesFrontlineProcessMaterialServiceTest test`
- `node IntRuoyiFronted\tests\e2e\route-process-input-output-materials-static.spec.cjs`
- `rg -n "frontlineReportMaterialIds|inputMaterialIds|outputMaterialIds" IntRuoyiBackend\yudao-module-mes\src IntRuoyiFronted\src IntRuoyiFronted\tests`

## Current Status
ready_for_closeout - 实现和定向验证已完成；前端类型检查因缺少 node_modules 阻塞，提交、推送和 worktree closeout 待用户明确授权。

## 设计约束检查
- 批记录只取逐工序正式绑定，表单槽位只取 `formBindings`，工序开始链路独立，不互相补齐或推断。
- 输入/输出物料只作为工艺路线批记录配置字段保存和回显；同编号跨工序一致由用户配置体现，不由系统隐式生成。
- 一线报工仍只读取输出物料，输入物料不参与完成数量、损耗数量和批号填写要求。
- 默认禁止 fallback、降级、吞异常、模拟成功和兼容补丁。
- 附加 worktree 位于 `D:\IntRuoyiWorktree\20260902-route-process-input-output-materials`。

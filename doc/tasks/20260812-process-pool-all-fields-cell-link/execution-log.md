# Execution Log

## User Intent

- 用户指出当前“报工数据”左侧只有数量字段，不包含一线生产每个工序里的设备、参数、checkbox 勾选状态等元素。
- 用户要求继续，使一线生产每个工序里所有可提交元素都能对应到该工序正式批记录表单。

## BDD

- BDD: 一线生产完整字段可映射 -> Given 当前路线版本工序存在正式设备和设备参数规则；When 用户在批记录单元格链接页面选择“报工数据”；Then 左侧来源字段包含基础数量、设备信息、参数实际值/参考值/单位/上下限、checkbox 勾选状态和人员/日期字段。
- BDD: 字段目录按当前工序生成 -> Given 不同工序设备和参数不同；When 用户进入某个工序的正式批记录表单链接配置；Then 只展示该工序可提交的一线生产字段，不用其它工序字段补齐。
- BDD: 多笔报工仍需聚合策略 -> Given 来源为一线生产报工字段；When 建立映射；Then 数值字段只能选择数值聚合，文本/勾选字段只能选择文本/状态聚合，不允许隐式猜测。

## Execution Evidence

- PRECHECK: 已读取 frontend-feature-delivery、backend-api-delivery 及前后端项目规则、PowerShell/closeout 规则。
- RED: `node -e "... if(!vo.includes('private String textValue')) throw ..."` -> FAIL, expected reason: `DeviceParameterReadingReqVO` 缺少 `textValue`，下拉/文本参数不能成为完整正式报工来源。
- GREEN: `node tests\e2e\mes\batch-record-cell-link-process-pool-report-static.spec.js` -> PASS。
- GREEN: `node -e "... if(!vo.includes('private String textValue')) throw ..."` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesP0BatchRecordBackfillClosedLoopTest,MesProFrontlineFeedbackSubmitDetailContractTest,MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 26 / Failures: 0 / Errors: 0.
- REGRESSION: `git diff --check -- <task-owned files>` -> PASS；仅 LF/CRLF 工作区提示。
- EXPERIENCE: 已按 project-experience-consolidation 合并长期经验到 `docs/backend-development.md#批记录单元格链接预填落库边界` 和 `docs/experience-index.md`。
- REVERIFY-2026-08-12 19:00: `node tests\\e2e\\mes\\batch-record-cell-link-process-pool-report-static.spec.js` -> PASS；`pnpm ts:check` -> PASS；`mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesP0BatchRecordBackfillClosedLoopTest,MesProFrontlineFeedbackSubmitDetailContractTest,MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 26 / Failures: 0 / Errors: 0.

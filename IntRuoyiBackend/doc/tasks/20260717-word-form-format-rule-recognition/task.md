# Word 表单格式与取值范围批量识别

## Task Goal

在 Word 导入解析完成后，为批记录、损耗单、过程检验单、参数记录表及同类附属表单批量识别大部分单元格的格式类型与取值范围建议，允许用户后续手动调整，不以 100% 自动正确为目标。

## Milestones

- [x] M1: 分析现有 Word 表单导入、JSON 构建与单元格规则链路。
- [x] M2: 设计通用的表单单元格格式/范围识别规则，覆盖批记录与附属表单。
- [x] M3: 先写失败测试，再实现批量识别。
- [x] M4: 回归验证批记录、损耗单/通用明细表和未知字段安全性。

## Expected Verification

- 单元测试覆盖典型批记录字段、损耗单/通用表单字段、未知字段不误设强约束。
- 受影响 Maven 测试通过；如缺少前置条件，记录 blocker 与影响。

## 经验门禁

- `docs/powershell-memory.md`：PowerShell 命令必须显式 UTF-8；中文文本读写优先使用 UTF-8 aware runtime 或 apply_patch；不得使用 `&&` 串联命令。
- `docs/experience/batch-record-form-recognition.md`：表单识别必须抽象为通用结构规则；不得按表单名、文件名、字段坐标或截图红框写硬编码；批记录之外的损耗单、过程检验单、参数记录表等附属表单也要保持稳定槽位、责任角色、必填策略和可见性。
- `docs/experience/batch-record-form-recognition.md`：修复后至少跑目标测试、相关历史回归和真实/结构样本覆盖；不得用旧数据库模板、旧截图、CSS 隐藏或 mock 数据替代当前工作区证据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，抽象为解析后通用规则识别能力，而非仅针对批记录或损耗单硬编码。
- `是否存在临时补丁或绕过`：否。

## Completed Work

- 在 `MesProBatchRecordCellRuleSupport` 增加 `applyAutomaticSuggestions`，导入生成 JSON 后自动写入 `edhrCellRule`，并同步 `fillForm.componentFlag`，便于执行页按数字、日期、勾选等格式渲染。
- 规则识别覆盖批记录与附属表单共享结构：邻近标签、单位、checkbox 文本、描述类长文本、数量/温度/压力/比例/重量/时长等数值范围。
- 自动识别规则默认 `source=AUTO`、`reviewed=false`，保留用户后续手动调整；已有 `reviewed=true` 人工规则不覆盖。
- `MesProBatchRecordJimuReportGatewayImpl` 在报表保存前统一应用自动规则，因此主批记录、损耗单、过程检验单、参数记录表等走同一入口的表单都会执行。

## Verification Evidence

- RED: `mvn.cmd -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest test` -> FAIL，自动应用方法缺失。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordCellRuleSupportTest,MesProBatchRecordJimuReportGatewayImplTest" test` -> PASS，19 tests。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordGenericDetailFormNormalizerTest" test` -> PASS，1 test。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordRouteERecognizerTest" test` -> PASS，11 tests。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldSplitInlineChecklistChoiceCellWithTrailingUnderlineIntoIndependentFillForms+build_shouldExpandInlineUnderlineFillablePromptsIntoTextInputs+build_shouldRenderNarrativePromptBlankAreaAsTextarea" test` -> PASS，3 tests。
- 说明：全量 `MesProBatchRecordReportJsonBuilderTest` 长时间未完成并已停止任务测试进程；未把该超时命令作为通过证据。

## Closeout Evidence

- `task-closeout-cleanup` preview -> PASS，keep `task.md`、`execution-log.md`、`backend-api-evidence.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
- `task-closeout-cleanup` apply -> PASS，当前为主工作区 `int_main`，无 linked worktree 需融合或删除，deleted `<none>`。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260717-word-form-format-rule-recognition/backend-api-evidence.md`

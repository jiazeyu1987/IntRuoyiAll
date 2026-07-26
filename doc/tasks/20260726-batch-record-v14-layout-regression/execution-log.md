# Execution Log

## User Intent

用户指出 V14 版本中仍存在此前声称已修复的截图/表格位置识别问题，要求不能针对某个表单做特例，要用适合所有表格的全局方式解决，并用相关表单截图验证。

## Milestone Evidence

BDD: V14 物料矩阵列归属不串列 -> Given 批记录 Word 中存在密集物料/自检表格且物料名称列包含压力表勾选项, When 使用新识别前后端导入并生成 V14, Then 只有物料名称列应出现压力表勾选项，批号、生产数量、自检合格数量等业务数量列不得重复承载该文本。

BDD: 说明区边界保持独立 -> Given 表格底部存在短标题加长说明的生产自检/合格标准/检验方法行, When 解析表格行结构, Then 说明区不得被上一条操作明细或物料矩阵吞并。

## Command And Verification Log

- 初始化任务：记录 V14 回归修复目标、全局约束和预期验证。
- GREEN: experience-preflight -> PASS，`docs/experience-index.md` 命中 `docs/backend-development.md#eDHR 批记录 Word 表格解析门禁`；本任务必须使用真实 DOC + 合成回归，且不得按表单名/工序名/文件名写特例。
- 复现：既有 V14.0 页面仍显示压力表 checkbox 串入业务列；审计 `runtime-v14-json-audit.json` 显示 `text` 坐标已正确，进一步审计 `runtime-v14-fillform-audit.json` 发现旧持久化 `fillForm.labelText=□30atm压力表`、`fillForm.componentFlag=checkbox`、`edhrCellRule.valueType=BOOLEAN` 仍残留在 `批号 / 生产数量 / 自检合格数量` 等列。
- 根因：此前全局导入/布局修复只覆盖新生成布局和 `text` 坐标，未在读取既有 V14.0 时刷新未确认的自动单元格规则；前端只读预览会按 `fillForm` / `edhrCellRule` 渲染 checkbox，因此旧 V14.0 看起来仍未修好。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest#refreshUnreviewedAutomaticSuggestions_repairsStaleCheckboxFillFormsUnderTypedHeaders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧 stale checkbox 的 `componentFlag` 会继续保留在 `批号` 等 STRING 列。
- 修复：在 `MesProBatchRecordCellRuleSupport.refreshUnreviewedAutomaticSuggestions(...)` 中全局刷新未确认 AUTO/可填候选规则，复用表格列头优先级；在 `getCellRules` 读取路径执行刷新并持久化；人工已确认规则保持不动。
- 修复约束：未按 `清洁工序`、`光固Ⅰ`、`压力泵`、`30atm`、文件名或报表名写特例；只依据共享行列邻域、列头强类型提示和未确认自动规则状态。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest#refreshUnreviewedAutomaticSuggestions_repairsStaleCheckboxFillFormsUnderTypedHeaders,MesProBatchRecordCellRuleSupportTest#refreshUnreviewedAutomaticSuggestions_keepsReviewedManualRules,MesProBatchRecordCellRuleSupportTest#applyAutomaticSuggestions_prefersColumnHeaderOverLeftCheckboxChoiceForBlankTableCells" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。
- 运行态加载：隔离 worktree 构建修复 Jar，加载到本机 `48081`；目标 Jar SHA256 为 `21653748FA95E8E8D250AC1860B083F63FBC5CFD37DB1DAD728BFA89654CD452`；`http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 真实前端验证：`V14_VERIFY_EXISTING_ONLY=1 node doc/tasks/20260726-batch-record-v14-layout-regression/v14-ui-import-verify.cjs` -> PASS；验证既有 `球囊扩张压力泵 / V14.0 / batchRecordVersionId=130` 的 `清洁工序生产记录` 和 `光固Ⅰ工序生产记录`。
- 真实前端审计：`v14-json-audit-2026-07-26T04-47-04-690Z.json` 中两张目标表 `offenders=[]`，`offenderCount=0`。
- 截图证据：`artifacts/v14-cleaning-fixed.png`、`artifacts/v14-lightcuring-fixed.png`。

## Blockers

- 最终提交/推送未执行：主工作区存在大量 unrelated dirty changes，按项目规则若提交需要先做全量脏工作区基线提交。为避免把其他任务改动混入本次修复，当前状态停在 `ready_for_closeout`，等待用户确认提交策略。

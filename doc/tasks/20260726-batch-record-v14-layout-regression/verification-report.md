# Verification Report

## Scope

- 验证对象：既有 `球囊扩张压力泵 / V14.0 / batchRecordVersionId=130` 批记录表单。
- 目标表单：
  - `清洁工序生产记录`，reportId=`ff909645721d4c7cb9220a74e4497254`
  - `光固Ⅰ工序生产记录`，reportId=`bd1ac748f2be41bd8e63b2c0a924059e`
- 源 Word：`C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`
- 验证目标：压力表 checkbox 只应保留在物料名称列，不得串入 `批号 / 生产数量 / 自检合格数量 / 不合格数量` 等业务列。

## Root Cause

V14.0 仍异常不是因为表格 `text` 坐标继续错位，而是旧版本 JSON 中已经持久化了过期的 `fillForm` 和 `edhrCellRule`。这些旧规则把相邻业务列标成 `checkbox / BOOLEAN / □30atm压力表`，前端只读预览按 `fillForm` 渲染，所以即使新导入布局逻辑已修正，既有 V14.0 仍会显示旧 checkbox。

本次修复采用全局方式：读取单元格规则时，对未确认的 AUTO/可填候选规则按当前共享列头推断逻辑重新生成并持久化；人工已确认规则保持不变。未按表单名、工序名、产品名、文件名或 `30atm` 文本做特例。

## Automated Verification

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest#refreshUnreviewedAutomaticSuggestions_repairsStaleCheckboxFillFormsUnderTypedHeaders,MesProBatchRecordCellRuleSupportTest#refreshUnreviewedAutomaticSuggestions_keepsReviewedManualRules,MesProBatchRecordCellRuleSupportTest#applyAutomaticSuggestions_prefersColumnHeaderOverLeftCheckboxChoiceForBlankTableCells" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS，Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。
- Coverage:
  - stale V14.0 checkbox fillForm 在 typed headers 下被刷新。
  - 已确认 MANUAL 规则不被读时刷新覆盖。
  - 密集表格中空白业务列优先使用上方列头，而不是左侧 checkbox 文本。

## Runtime Verification

- Runtime backend: `http://127.0.0.1:48081`
- Health: `{"status":"UP"}`
- Loaded Jar SHA256: `21653748FA95E8E8D250AC1860B083F63FBC5CFD37DB1DAD728BFA89654CD452`
- Real UI command: `V14_VERIFY_EXISTING_ONLY=1 node doc/tasks/20260726-batch-record-v14-layout-regression/v14-ui-import-verify.cjs`
- Result: PASS。
- Audit artifact: `artifacts/v14-json-audit-2026-07-26T04-47-04-690Z.json`
- Summary artifact: `artifacts/v14-verification-summary-2026-07-26T04-47-04-690Z.json`

## Results

- `清洁工序生产记录`: `offenderCount=0`，业务列未再出现压力表 checkbox 串列。
- `光固Ⅰ工序生产记录`: `offenderCount=0`，业务列未再出现压力表 checkbox 串列。
- 截图证据：
  - `artifacts/v14-cleaning-fixed.png`
  - `artifacts/v14-lightcuring-fixed.png`

## Closeout Status

- Implementation and verification are complete.
- Status remains `ready_for_closeout` because the main workspace contains unrelated dirty changes. Commit/push was intentionally not performed to avoid mixing other task changes into this fix without explicit user confirmation.

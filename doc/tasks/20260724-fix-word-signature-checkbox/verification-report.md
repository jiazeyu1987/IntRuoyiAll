# 验证报告：修复 Word 导入签名日期区域误识别 checkbox

## Scope

- Word 批记录表格导入后的 JSON 构建逻辑。
- 自动填写规则对签名日期区域 checkbox-like 文本的识别逻辑。

## Commands

- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordCellRuleSupportTest#buildSuggestions_doesNotPromoteSignatureDateColumnCheckboxFragments+buildSuggestions_doesNotPromoteMisalignedSignatureDateTailCheckboxFragments" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteCheckboxFragmentsUnderSignatureDateHeaders+build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Results

- 新增 Builder 回归用例：PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- 自动规则识别回归：PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。
- JSON 构建器回归：PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。
- 任务收尾清理：PASS，`task_closeout.py --mode preview/apply` 无 blocked、无 warnings、无删除路径。

## Residual Risk

- 现有已导入模板保存的错误 JSON 不会自动回写，发布后需重新导入或重新生成受影响模板。


## 2026-07-25 Real E2E

- 自动规则识别回归：PASS，5 tests，覆盖列偏移、空白签名日期格、中间表头遮挡、旧 checkbox fillForm 改写和 boolean 值清理。
- JSON 构建器回归：PASS，2 tests，覆盖签名日期表头与尾部错位 checkbox 碎片。
- 本地后端运行态：PASS，PID `39380`，Jar SHA256 `1090219624699F708D9440DB71E5FDC1303B71C7787EE7F99330E6E827C8B99F`，health `UP`。
- 真实页面 E2E：PASS，`edhr-word-template-import-real-flow.e2e.js` 使用 `pressure-pump-record.doc` 和 `数显球囊扩张压力泵（FDA)`，导入 15 份报表；`signatureDateCellsChecked=177`，未发现签名日期区域 checkbox。
- 真实页面 E2E 复跑：PASS，runId `signature-checkbox-20260725-e2e-rerun-codex`，导入 15 份报表；`signatureDateCellsChecked=177`，未发现签名日期区域 checkbox。

## Closeout Note

- 当前实现与验证已完成，任务状态进入 `ready_for_closeout`。工作区存在大量其它任务改动和已暂存内容，未执行本任务提交/推送，避免混入非任务文件。

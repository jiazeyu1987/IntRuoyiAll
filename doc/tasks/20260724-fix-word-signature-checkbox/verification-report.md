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

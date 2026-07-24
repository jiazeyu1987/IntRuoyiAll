# 执行日志：修复 Word 导入签名日期区域误识别 checkbox

## User Intent

用户反馈粗洗工序及其它 Word 导入表单中，签名位置会被识别成 checkbox，需要修复解析逻辑，避免 `操作人/日期`、`复核人/日期` 区域出现结果选项 checkbox。

## Milestone Log

- 2026-07-24：建立任务目录，记录本次缺陷目标、BDD 场景、验证方式和 `docs/experience-index.md` 缺失事实。
- 2026-07-24：新增列偏移场景回归测试，复现 `结果` 选项碎片落入签名日期尾部区域后未合并的问题。
- 2026-07-24：扩展 JSON 构建器和自动规则识别的签名日期尾部区域判断，从精确列覆盖改为识别 `结果 -> 签名/日期` 表头尾部区间。
- 2026-07-24：无关编译改动恢复可编译后，串行完成 JSON 构建器和自动规则识别两组目标回归。
- 2026-07-24：运行 `task_closeout.py --task-id 20260724-fix-word-signature-checkbox --mode preview/apply`，仅保留任务记录和缺陷回归证据，无需删除临时产物；主工作区非 linked worktree，未执行合并或工作区删除。

## BDD Scenarios

- BDD: 签名日期区域不识别为 checkbox -> Given Word 表格存在 `结果`、`操作人/日期`、`复核人/日期` 表头且列校准使结果选项碎片落入签名日期尾部区域 / When 生成积木报表 JSON 并应用自动填写规则 / Then 只有真实结果列生成 checkbox 选项，签名日期区域不得生成 checkbox 控件

## TDD Evidence

- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增断言失败：`signature/date checkbox fragments must be merged as result options ==> expected: not <null>`。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordCellRuleSupportTest#buildSuggestions_doesNotPromoteSignatureDateColumnCheckboxFragments+buildSuggestions_doesNotPromoteMisalignedSignatureDateTailCheckboxFragments" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteCheckboxFragmentsUnderSignatureDateHeaders+build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。

## Verification Evidence

- 已通过新增 Builder 目标用例，证明列偏移的签名日期尾部 checkbox 碎片会被折叠到真实结果列。
- 已通过自动规则识别的新增与既有签名日期列用例。
- 已通过 JSON 构建器的新增与既有签名日期列用例。

## Blockers

- 无。

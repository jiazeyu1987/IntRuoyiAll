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

- BDD: 空白签名日期格不继承结果 checkbox -> Given 粗洗工序表格表头包含 `结果`、`操作人/日期`、`复核人/日期`，正文行左侧结果列为 `□符合要求 □不符合要求`，签名日期列为空白可填写格 / When 自动规则建议并应用到导入后的报表 JSON / Then 结果列保持 checkbox，签名日期空白格必须保持 STRING/input-text，不得继承 checkbox。

- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增断言失败：`signature/date checkbox fragments must be merged as result options ==> expected: not <null>`。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordCellRuleSupportTest#buildSuggestions_doesNotPromoteSignatureDateColumnCheckboxFragments+buildSuggestions_doesNotPromoteMisalignedSignatureDateTailCheckboxFragments" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteCheckboxFragmentsUnderSignatureDateHeaders+build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。

## Verification Evidence

- 已通过新增 Builder 目标用例，证明列偏移的签名日期尾部 checkbox 碎片会被折叠到真实结果列。
- 已通过自动规则识别的新增与既有签名日期列用例。
- 已通过 JSON 构建器的新增与既有签名日期列用例。


- 2026-07-25：真实页面 E2E 使用测试租户 `aoteman`、本机 `8081/48081`、项目内 `pressure-pump-record.doc` 及真实 DCC 产品 `数显球囊扩张压力泵` 完成导入，随后在 API 核验中复现失败：`粗洗工序生产记录` 第 6 行第 16/18 列位于 `操作人/日期`、`复核人/日期` 表头下方，但仍持久化 `componentFlag=checkbox`。

## Blockers

- 无。


## 2026-07-25 E2E 复验与追加修复

- BDD: 中间器具/物料行不截断签名日期表头 -> Given 清洁工序表格上层存在 `操作人/日期`、`复核人/日期`，中间行存在非签名表头，正文左侧存在 `□30atm压力表` / When 自动规则建议空白签名日期格 / Then 签名日期格必须保持 STRING/input-text，不得继承左侧 checkbox。
- BDD: 签名日期区域旧 checkbox fillForm 必须改写 -> Given 签名日期单元格已有 `fillForm.componentFlag=checkbox` 且文本为 `□是 □否` / When 自动规则识别该区域属于 `操作人/日期` / Then 规则和 fillForm 都必须同步为 STRING/input-text，并清理 boolean value/defaultValue。
- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest#buildSuggestions_doesNotPromoteBlankSignatureDateCellsPastIntermediateCheckboxRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`expected: <STRING> but was: <BOOLEAN>`。
- GREEN: 同命令 -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest#buildSuggestions_rewritesExistingCheckboxFillFormUnderSignatureDateHeaders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，先复现 `expected: <input-text> but was: <checkbox>`，随后复现旧 boolean value 未清理 `expected: <> but was: <false>`。
- GREEN: 同命令 -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest#buildSuggestions_doesNotPromoteSignatureDateColumnCheckboxFragments+buildSuggestions_rewritesExistingCheckboxFillFormUnderSignatureDateHeaders+buildSuggestions_doesNotPromoteMisalignedSignatureDateTailCheckboxFragments+buildSuggestions_doesNotPromoteBlankSignatureDateCellsFromLeftCheckboxResult+buildSuggestions_doesNotPromoteBlankSignatureDateCellsPastIntermediateCheckboxRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 5, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteCheckboxFragmentsUnderSignatureDateHeaders+build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。
- BUILD: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，生成 `yudao-server-exec.jar`，SHA256 `1090219624699F708D9440DB71E5FDC1303B71C7787EE7F99330E6E827C8B99F`。
- RUNTIME: 停止已确认属于 `E:\IntRuoyi` int_main 后端的旧 PID `30940`，以同端口参数启动新 PID `39380`；`http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- GREEN: `EDHR_WORD_IMPORT_RUN_ID=signature-checkbox-20260725-e2e-final-2; EDHR_WORD_IMPORT_BATCH_RECORD_NAME=E2E-WORD-signature-checkbox-20260725-e2e-final-2; EDHR_WORD_IMPORT_SAMPLE_DOC=E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes\src\test\resources\fixtures\pressure-pump-record.doc; EDHR_WORD_IMPORT_PRODUCT_NAME=数显球囊扩张压力泵（FDA); node tests\e2e\edhr-word-template-import-real-flow.e2e.js` -> PASS，reports=15，autoSuggestions=1146，persistedAutoRules=1146，signatureDateSections=27，signatureDateRowsChecked=80，signatureDateCellsChecked=177。
- GREEN: experience-preflight -> PASS，E2E 复跑前已读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/experience-index.md` 和 Playwright 技能；本次只补充既有门禁证据，无需新增长期经验文档。
- GREEN: `EDHR_WORD_IMPORT_RUN_ID=signature-checkbox-20260725-e2e-rerun-codex; EDHR_WORD_IMPORT_BATCH_RECORD_NAME=E2E-WORD-signature-checkbox-20260725-e2e-rerun-codex; EDHR_WORD_IMPORT_SAMPLE_DOC=E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes\src\test\resources\fixtures\pressure-pump-record.doc; EDHR_WORD_IMPORT_PRODUCT_NAME=数显球囊扩张压力泵（FDA); node tests\e2e\edhr-word-template-import-real-flow.e2e.js` -> PASS，reports=15，autoSuggestions=1146，persistedAutoRules=1146，signatureDateSections=27，signatureDateRowsChecked=80，signatureDateCellsChecked=177。

# Verification Report

## Scope

单元格控件类型切换：文本、日期、日期时间、电子签名、下拉框、数字输入及数字上下限。

## Commands

- `node IntRuoyiFronted\tests\e2e\edhr-cell-control-type-switch-static.spec.js` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check` -> PASS；仅 LF/CRLF 工作区提示。
- Bug regression evidence validator -> PASS。
- Frontend feature evidence validator -> PASS。

## Result

- 前端规则弹窗支持 select/date/datetime/signature/input-number 控件类型切换。
- 下拉框保存前强制 `selectionMode=single` 和至少两个有效 `options`。
- 数字 min > max 在前端保存前和后端规则校验均 fail-fast。
- SIGNATURE 继续由后端要求 enabled `edhrSignature` marker，不降级为文本。
- 字段类型选择器使用显式 change 事件并替换当前规则行，选择数字等类型后显示值和对应配置区域立即刷新。

## Closeout Boundary

当前工作区仍有多个非本任务并行改动，且分支显示 `int_main...origin/int_main [ahead 10]`；本报告不声明已 push。

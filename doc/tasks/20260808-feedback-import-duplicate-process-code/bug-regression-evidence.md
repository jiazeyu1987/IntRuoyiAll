# Bug Regression Evidence

## Bug Summary

报工管理中的导入记录分页在 `source_payload_json.processCode` 对应多个工序主数据时返回系统异常。用户确认同编码工序是合法业务数据，因此分页读模型必须支持一对多匹配。

## Expected Behavior

导入记录分页应正常返回导入记录；当同一工序编码对应多个工序主数据时，可分配余量按所有匹配工序的余量合计展示，不能依赖唯一编码。

## Reproduction

- Path: `/mes/pro/feedback/import-record/page?pageNo=1&pageSize=10`
- Observed: `code=500, msg=系统异常`
- Runtime error: `TooManyResultsException: Expected one result ... but found: 2`

## Root Cause

`MesProFeedbackImportRecordServiceImpl.toImportRecordResp` 使用 `processMapper.selectByCode(payload.getProcessCode())`。该 mapper 底层是 `selectOne(MesProProcessDO::getCode, code)`，但 `mes_pro_process.code` 当前不是唯一字段，重复编码合法存在。

## Regression Test

已新增：`MesProFeedbackImportRecordServiceImplTest#getImportRecordPage_shouldAggregateSurplusWhenProcessCodeMatchesMultipleProcesses` 覆盖一个导入记录 payload 的 `processCode` 匹配两个工序主数据，分页返回不异常且 `surplusPoolQuantity` 为两个工序余量之和。

## RED / GREEN

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFeedbackImportRecordServiceImplTest#getImportRecordPage_shouldAggregateSurplusWhenProcessCodeMatchesMultipleProcesses" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`surplusPoolQuantity` 期望 `12.5`，实际 `0`。
- GREEN: API/UI PASS after runtime refresh；formal JUnit GREEN remains BLOCKED because current HEAD has unrelated MES test compile errors before the target test can execute.

## Verification

- Static Verification: `rg -n "processMapper\.selectByCode\(" IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/MesProFeedbackImportRecordServiceImpl.java` -> no matches。
- Runtime Class Verification: built Jar inner MES module calls `selectListByCodes(...)` from `loadImportProcesses(...)`.
- Runtime API Verification: `/mes/pro/feedback/page?pageNo=1&pageSize=10` -> `code=0`, `total=144`; `/mes/pro/feedback/import-record/page?pageNo=1&pageSize=10` -> `code=0`, `total=575`.
- Runtime UI Verification: Playwright read-only verification shows 20 visible rows on both feedback and import-record tabs, with no `暂无数据` and no `系统异常`.
- Evidence Validator: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-feedback-import-duplicate-process-code\bug-regression-evidence.md` -> PASS；文件明确 JUnit 未 GREEN，不伪造测试通过。

## Risk And Scope

影响范围限定在导入记录分页响应组装及相关导入分配候选的工序编码解析。重复编码不被视为非法数据，不删除业务数据、不新增唯一约束、不任取第一条工序。

## Blockers

目标 Maven GREEN 尚未完成：clean worktree 执行目标测试时，`yudao-module-mes:testCompile` 被当前 HEAD 中无关测试编译错误阻塞，包括 `MesFrontlineRuntimeConfig` 构造参数不匹配、`MesProFrontlineFeedbackSubmitServiceImpl` 构造参数不匹配和 Java 17 下 `List#getFirst()` 不存在。运行态 API/UI 已恢复，但该 blocker 仍需单独修复后复跑 JUnit。

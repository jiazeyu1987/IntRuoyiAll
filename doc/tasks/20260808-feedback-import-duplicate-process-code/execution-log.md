# Execution Log

## Intent

用户确认多个同编码工序是允许存在的，不同人员或同一人员不同时间的报工记录都可能使用相同工序编码。导入记录区域不能因为重复工序编码报系统异常，也不能通过删除重复数据或添加唯一约束解决。

## BDD

BDD: 导入记录分页支持重复工序编码 -> Given 导入记录 payload 中的 `processCode` 对应多个未删除工序主数据；When 打开报工管理的导入记录分页；Then 接口返回该导入记录，不抛 `TooManyResultsException`，并按所有同编码工序的可用余量合计展示可分配余量。

## Evidence

- 本地接口 `/mes/pro/feedback/page` 曾返回 `total=144`，说明报工管理正式数据未丢失。
- 本地接口 `/mes/pro/feedback/import-record/page?pageNo=1&pageSize=10` 曾返回 `code=500, msg=系统异常`。
- 运行日志指向 `MesProFeedbackImportRecordServiceImpl.toImportRecordResp(...)` 调用 `processMapper.selectByCode(payload.getProcessCode())`，重复工序编码触发 MyBatis Plus `selectOne` 多行异常。
- 当前 schema 未声明 `mes_pro_process.code` 唯一，重复编码是合法业务数据。

## TDD

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFeedbackImportRecordServiceImplTest#getImportRecordPage_shouldAggregateSurplusWhenProcessCodeMatchesMultipleProcesses" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现未读取 `selectListByCodes`，同编码两条工序余量合计期望 `12.5`，实际分页行余量为 `0`。
- Implementation: `MesProFeedbackImportRecordServiceImpl` 已移除导入记录链路中按编码 `selectByCode` 的唯一查询假设，改为 `selectListByCodes` 读取同编码工序集合，并按所有匹配工序 ID 合计余量池可用数量。
- Regression Test: `MesProFeedbackImportRecordServiceImplTest#getImportRecordPage_shouldAggregateSurplusWhenProcessCodeMatchesMultipleProcesses` 已覆盖一个导入 payload 的 `processCode` 匹配两个合法工序主数据时，分页响应 `surplusPoolQuantity = 12.5`。
- GREEN BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFeedbackImportRecordServiceImplTest#getImportRecordPage_shouldAggregateSurplusWhenProcessCodeMatchesMultipleProcesses" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before target execution at `yudao-module-mes:testCompile` because current HEAD contains unrelated MES test compile errors (`MesFrontlineRuntimeConfig` constructor arity, `MesProFrontlineFeedbackSubmitServiceImpl` constructor arity, and `List#getFirst()` under Java 17). This does not prove the duplicate-code regression failed, but blocks formal JUnit GREEN.
- Runtime Build: Created clean detached worktree `D:\IntRuoyiWorktree\feedback-import-runtime-refresh-20260808`, built `yudao-server-exec.jar` with `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> BUILD SUCCESS, SHA256 `81BE46653868397AB78CAA4A93C0F2EA37103771C9491681F76D1DC4374A3C98`. Tests were intentionally skipped only for runtime packaging after the JUnit compile blocker above, and this is not recorded as test GREEN.
- Runtime Class Check: Built Jar inner `yudao-module-mes-2026.04-SNAPSHOT.jar` contains `MesProFeedbackImportRecordServiceImpl.loadImportProcesses(...)` calling `MesProProcessMapper.selectListByCodes(...)`.
- Runtime Refresh: Replaced old 48081 process `25768` running `backend-latest-20260808-001225-report-allocation-form-log.jar` with process `63072` running `backend-latest-20260808-1026-feedback-import-duplicate-code.jar`; `/actuator/health` -> `UP`.
- API GREEN: Login-scoped read-only request `/mes/pro/feedback/page?pageNo=1&pageSize=10` -> `code=0`, `total=144`, `listCount=10`; `/mes/pro/feedback/import-record/page?pageNo=1&pageSize=10` -> `code=0`, `total=575`, `listCount=10`.
- UI GREEN: Playwright read-only page load `/mes/pro/feedback` -> feedback API `code=0`, `total=144`, visible rows `20`, no `暂无数据`, no `系统异常`; `/mes/pro/feedback?tab=import-record` -> import API `code=0`, `total=575`, visible rows `20`, no `暂无数据`, no `系统异常`.
- Verification Evidence: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-feedback-import-duplicate-process-code\bug-regression-evidence.md` -> PASS。

## Notes

- 不删除任何 `mes_pro_process`、`mes_pro_feedback` 或 `mes_pro_feedback_import_record` 数据。
- 不添加 `mes_pro_process.code` 唯一约束。
- 不任取第一条工序作为隐式降级。
- 重复工序编码本身允许存在；列表读模型应按一对多关系处理，不应把重复编码当作导入记录非法。
- 临时构建 worktree `D:\IntRuoyiWorktree\feedback-import-runtime-refresh-20260808` 已移除；Jar 检查临时目录清理被命令策略拦截，未影响运行态。

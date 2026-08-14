# Verification Report

## Result

runtime_fixed_with_test_blocker - 本机 48081 后端已刷新到包含重复工序编码修复的新 Jar，正式报工和导入记录页面均恢复数据；目标 JUnit GREEN 被当前 HEAD 的无关 MES 测试编译错误阻塞。

## Business Rule

多个相同工序编码是合法数据：不同人员可以提交相同工序编码，同一人员也可以在不同时间提交相同工序编码。导入记录区域不能把重复编码视为非法，也不能通过删除重复数据或添加唯一约束解决。

## Root Cause

导入记录分页旧链路把 `source_payload_json.processCode` 当作唯一键，调用 `processMapper.selectByCode(...)`。当前 `mes_pro_process.code` 不是唯一字段，重复编码合法存在，因此 `selectOne` 在多行命中时触发 `TooManyResultsException`，导致导入记录分页返回 `code=500`。

## Implementation Evidence

- `MesProFeedbackImportRecordServiceImpl` 当前导入记录服务链路未再调用 `processMapper.selectByCode(...)`。
- 新增 `loadImportProcesses(...)` 通过 `processMapper.selectListByCodes(...)` 读取同编码工序集合。
- 新增 `sumAvailableQuantityByProcessIds(...)` 按所有匹配工序 ID 合计余量池可用数量。
- `toImportRecordResp(...)` 已按同编码工序集合计算 `surplusPoolQuantity`，避免分页响应因重复编码异常。

## Test Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFeedbackImportRecordServiceImplTest#getImportRecordPage_shouldAggregateSurplusWhenProcessCodeMatchesMultipleProcesses" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现返回余量 `0`，期望 `12.5`。
- Regression Test: `MesProFeedbackImportRecordServiceImplTest#getImportRecordPage_shouldAggregateSurplusWhenProcessCodeMatchesMultipleProcesses` 已覆盖同一 `processCode` 对应两个工序 ID 时合计余量 `5.0 + 7.5 = 12.5`。
- JUnit GREEN BLOCKED: clean worktree 执行目标测试时，`yudao-module-mes:testCompile` 被无关测试编译错误阻塞，目标测试未执行，不能记录 JUnit PASS。
- Runtime Package: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> BUILD SUCCESS；该命令只用于生成运行 Jar，不作为测试通过证据。
- Runtime Jar: `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260808-1026-feedback-import-duplicate-code.jar`，SHA256 `81BE46653868397AB78CAA4A93C0F2EA37103771C9491681F76D1DC4374A3C98`。
- Runtime Restart: old PID `25768` replaced by PID `63072`; health `/actuator/health` -> `UP`.
- API Verification: `/mes/pro/feedback/page?pageNo=1&pageSize=10` -> `code=0`, `total=144`, `listCount=10`; `/mes/pro/feedback/import-record/page?pageNo=1&pageSize=10` -> `code=0`, `total=575`, `listCount=10`.
- UI Verification: Playwright read-only `/mes/pro/feedback` -> feedback API `total=144`, visible rows `20`; `/mes/pro/feedback?tab=import-record` -> import API `total=575`, visible rows `20`; no `暂无数据`, no `系统异常`.
- Evidence Validator: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-feedback-import-duplicate-process-code\bug-regression-evidence.md` -> PASS。

## Remaining Verification

并发 Maven 释放后，复跑：

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProFeedbackImportRecordServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

当前本机运行态已经验证通过。剩余工作是修复无关 MES 测试编译错误后复跑目标 JUnit，并清理因命令策略未删除的本任务临时 Jar 检查目录。

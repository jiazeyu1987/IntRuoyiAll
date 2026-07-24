# eDHR 主数据追溯分页 itemCount 合同修复

## Task Goal

修复 eDHR 主数据追溯分页接口 `/mes/pro/batch-record-execution/domain-trace/page` 缺少追溯项数量的问题。真实前端列表页需要展示 `items=<count>`，真实 E2E 已证明当前分页行既没有 `itemCount` 也没有 `items[]`，导致列表页只能落到默认 0，无法作为生产放行证据。

本任务要求后端分页 Response VO 明确返回 `itemCount`，服务层从本页 DomainTrace snapshot 的真实 item 记录批量计算数量；缺少 snapshot 时保持未产生追溯证据的语义，不用 mock、默认成功或 silent fallback 掩盖。

## Milestones

- [completed] M1: 创建任务文档与 BDD/TDD 计划。
- [completed] M2: RED 单元/合同测试证明分页行必须暴露真实 `itemCount`。
- [completed] M3: GREEN 后端 VO 与 service 最小实现，分页行返回真实追溯项数量。
- [completed] M4: 主 reviewer 运行后端测试与前端真实 E2E 复验。
- [completed] M5: 独立 reviewer 复审通过，执行收尾预览，当前任务改动进入收尾提交。

## BDD

BDD: 主数据追溯分页返回追溯项数量 -> Given eDHR 执行记录已有 DomainTrace snapshot 和真实 item 明细, When 前端查询 `/domain-trace/page`, Then 分页行返回 `itemCount` 等于该 snapshot 下真实 item 数量，并继续返回 `blockerCount`、`status`、`domainTraceHash`。

BDD: 主数据追溯分页未产生快照时不伪造数量 -> Given eDHR 执行记录尚无 DomainTrace snapshot, When 前端查询 `/domain-trace/page`, Then 分页行不得伪造 `itemCount=0` 作为已验证证据，而应保持缺少追溯快照的阻塞语义。

BDD: 主数据追溯列表 E2E 可放行 -> Given 测试租户存在已验证 DomainTrace 执行记录, When 前端真实 E2E 打开主数据追溯列表, Then 页面可展示来自后端分页的 `items=<itemCount>` 并从列表进入详情。

## Expected Verification

- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest test`
- `git diff --check`
- Frontend reviewer rerun: `pnpm e2e:edhr:domain-trace`

## Current Status

Completed. Reviewer verification is GREEN. Targeted backend tests pass, `yudao-server` package succeeds, and the frontend real E2E passes against the current fixed backend on `48098`, proving the list page now receives `itemCount=8` for execution `40 / BRE202605280518101280040`. Independent reviewer review passed. The final backend implementation batch-loads page snapshots and item rows to avoid adding per-row item count queries.

## Milestone Evidence

- M2 RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" test` -> FAIL at testCompile because `MesProBatchRecordDomainTracePageRespVO#getItemCount()` and the backend item-count source are missing.
- M3 GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" test` -> PASS, 11 tests run, 0 failures, 0 errors; page rows return item counts from batch-loaded persisted item rows.
- Worker diff check: `git diff --check` -> PASS, no whitespace errors.
- M4 GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" test` -> PASS, 11 tests run, 0 failures, 0 errors.
- M4 GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS after stopping the stale local Java process that held the old target jar open.
- M4 GREEN: frontend real E2E `pnpm e2e:edhr:domain-trace` -> PASS with list/final `status=VERIFIED`, `blockerCount=0`, `itemCount=8`.
- M5 REVIEW: independent reviewer -> PASS, no blocking findings.
- M5 GREEN: final frontend real E2E against the current rebuilt backend jar on `48098` -> PASS with list/final `status=VERIFIED`, `blockerCount=0`, `itemCount=8`.

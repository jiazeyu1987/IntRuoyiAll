# Execution Log

## Intent

用户要求“进行修复”，范围来自上一轮静态分析：AC-M12 至 AC-M15 及相关 QA/PQC/放行链路不符合项，包括 QA 规程保存发布未接入、PQC 任务正式生成器缺失、计划/实际数量不一致、多件样本被截断、上午/下午巡检身份隔离不足、末检不适用依据缺失和放行完整性只检查已存在任务。

## Preflight

- 已读取 `bug-regression-fix-loop`、`backend-api-delivery`、`frontend-feature-delivery` 技能及其 evidence contract。
- 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取 `docs/experience-index.md`，命中 PQC/QA 规程相关门禁并摘入 `task.md`。
- 当前 Git 状态：`int_main...origin/int_main [ahead 4]`，存在既有脏改动；本任务实现前需按规则独立基线提交既有脏改动，避免混入当前修复。

## BDD Scenarios

- BDD: PQC task planned quantity is authoritative -> Given a pending PQC task generated from a published QA regulation with planned quantity N / When the inspector submits actual quantity not equal to N or provides more/fewer sample values / Then the backend rejects the submission and does not write piece details or process-pool events.
- BDD: Formal PQC tasks are generated from published regulation -> Given a confirmed work order has enabled schedule-order processes and a published QA regulation for each product/route/process / When the leader adds the work order to the active pool / Then the backend creates FIRST, PATROL AM, PATROL PM and FINAL PQC tasks with business date, shift code, round number, planned quantity and published regulation version; missing regulation or duplicate identity fails fast.
- BDD: Patrol AM and PM task identities are isolated -> Given a published regulation requiring morning and afternoon patrol tasks for the same active order and process / When a task is generated or submitted / Then business date, shift code, round number and patrol period identity must match, and reusing the morning task for afternoon fails.
- BDD: Release completeness requires expected PQC task identities -> Given an active order has frozen process snapshots / When release inspection-result completeness is evaluated / Then every process snapshot must have confirmed FIRST, PATROL AM, PATROL PM and FINAL PQC task identities; already-confirmed but incomplete task sets still block release.
- BDD: Final inspection applicability is explicit -> Given a published regulation marks final inspection applicable or not applicable / When release completeness is evaluated / Then applicable final inspection requires a confirmed task, while not applicable must have persisted explicit evidence and must not be treated as a missing task.
- BDD: QA regulation persistence is formal -> Given QA updates inspection rules for a product/route/process / When saving draft or publishing / Then backend persists a draft/published immutable version or fail-fast reports the missing formal API, and the UI must not pretend a preview-only draft is a saved rule.

## RED / GREEN Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，未进入业务断言；JVM native memory/pagefile 不足，`hs_err_pid55128.log` 显示 `Native memory allocation (mmap) failed ... G1 virtual space`。
- RED: `node tests/e2e/mes-frontline-pqc-task-quantity-static.spec.js` -> FAIL，旧前端缺少 `isPqcInspectionQuantityLocked`，检验数量仍可编辑且提交 payload 使用 `.slice(0, pqcInspectionQuantity.value)` 截断样本。
- GREEN: `node tests/e2e/mes-frontline-pqc-task-quantity-static.spec.js` -> PASS，前端锁定任务计划数量并使用 exact sample values 提交。
- GREEN: `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS，PQC 提交链路仍调用正式后端提交接口。
- GREEN: `node tests/e2e/frontline-formal-submit-static.spec.cjs` -> PASS，一线正式提交合同未被破坏。
- GREEN: `node tests/e2e/edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> PASS，PQC 页面相邻全屏合同未被破坏。
- GREEN: `node yudao-module-mes\src\test\js\mes-pqc-task-generation-static.spec.cjs` -> PASS，静态合同证明活动订单生成器读取已发布 QA 规程和项目，生成 FIRST、PATROL AM、PATROL PM、FINAL 任务；`301×5%` 使用 `RoundingMode.CEILING` 得到 `16`；写入前按 `activeOrderId + routeProcessId + inspectionType + businessDate + shiftCode + roundNo` 查重，唯一键冲突转为 `PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT`；放行完整性按 activeOrder 工序快照要求 FIRST、PATROL AM、PATROL PM、FINAL 预期任务身份完整。
- GREEN: `git diff --check -- <task-owned files>` -> PASS，仅出现 CRLF 工作区提示，无 whitespace error。
- BLOCKED: `mvn -o -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> TIMEOUT，180 秒无 surefire 结果；任务自有 PID 53080 已用 `taskkill /PID 53080 /F` 清理，不能声明后端 GREEN。
- BLOCKED: `$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'; mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> TIMEOUT，240 秒无 surefire 结果；仅清理本次命令 PID 55352 进程树，保留其他并行 Maven 任务。

## Milestone Updates

- M1 completed：已建立后端 RED 测试与前端静态 RED 证据，覆盖计划数量不一致和样本多件截断。
- M2a completed：后端 `MesFrontlinePqcContextServiceImpl` 新增 `PRO_FRONTLINE_PQC_TASK_QUANTITY_MISMATCH`，提交前校验任务计划数量、实际提交数量和每个检验项目样本数量完全一致，不一致时不更新任务、不写 piece detail、不创建 process-pool event。
- M2b completed：后端 `MesTeamLeaderActiveOrderServiceImpl` 在新增活跃订单后，基于启用排产工序、产品、路线版本和已发布 QA 规程生成正式 `MesPqcInspectionTaskDO`；首检/末检使用固定数量，巡检按比例向上取整，同时生成 AM/PM 独立任务；缺产品、缺计划日期、缺已发布规程、缺项目、数量规则冲突或重复身份均 fail fast。
- M2c completed：后端 `MesOrderReleaseCompletenessServiceImpl` 在全部已有 PQC task 已确认后，继续按 `MesProcessPoolActiveOrderProcessSnapshotMapper.selectListByActiveOrderId` 读取活跃订单工序快照，逐工序要求 FIRST/FIRST、PATROL/AM、PATROL/PM、FINAL/FINAL 且 `roundNo=1` 的预期任务身份，缺任一身份时阻塞放行。
- M3a completed：前端 `FrontlineFixedTemplatePanel.vue` 将 PQC 任务快照下的检验数量输入和 +/- 按钮锁定；提交前逐项目校验样本数；`itemResults`、`pqcItemDetails`、`rawPayload.pqcPieceValues` 不再 `.slice()` 截断。
- M4 partial：后端 PQC 任务生成静态合同、前端静态合同和结构检查已通过；后端 JUnit 被当前机器 JVM native memory/pagefile、并行 Maven 占用与超时阻塞。

## Blockers

- 已完成既有脏改动基线隔离：`bf24cdc6e chore: baseline existing job matrix docs`、`4b0280901 chore: baseline concurrent task changes`。
- 当前工作区仍有并行任务改动，必须继续 selective staging，禁止 `git add -A`。
- 后端 Maven/JUnit 当前无法产出 GREEN：`hs_err_pid55128.log` 显示 JVM native memory/pagefile 不足；后续小堆离线单测 180 秒超时，包含新增生成器测试的目标命令 240 秒超时；当前机器仍存在多条并行 Maven 进程，不能强杀非本任务 PID。
- 末检“不适用”显式依据仍缺少正式字段、表、VO 和放行校验模型；当前发布规程仍强制 `FINAL` 规则存在，因此本次只能安全生成“适用时末检任务”，不能伪造“不适用”依据或宣称 AC-M15 全量 `ACCEPTED`。

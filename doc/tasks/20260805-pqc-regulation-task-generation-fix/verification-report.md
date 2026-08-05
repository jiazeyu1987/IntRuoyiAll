# Verification Report

## Scope Verified

- 后端代码修复范围：`MesFrontlinePqcContextServiceImpl` 提交侧数量校验、`ErrorCodeConstants` 新增数量不一致错误码、`MesFrontlinePqcContextServiceTest` 新增计划数量和样本数量 RED 测试。
- 后端生成器修复范围：`MesTeamLeaderActiveOrderServiceImpl` 接入已发布 QA 规程和规程项目，生成 FIRST、PATROL AM、PATROL PM、FINAL 正式 PQC 任务；`MesPqcInspectionTaskMapper` 增加完整身份查询；`ErrorCodeConstants` 增加任务生成阻塞和重复身份错误码；`MesTeamLeaderActiveOrderServiceTest` 增加生成、缺规程、重复身份测试。
- 后端放行完整性修复范围：`MesOrderReleaseCompletenessServiceImpl` 读取 active order 工序快照并要求 FIRST、PATROL AM、PATROL PM、FINAL 预期 PQC 任务身份完整；`MesProcessPoolActiveOrderProcessSnapshotMapper` 增加 activeOrder 查询；`MesOrderReleaseCompletenessServiceTest` 增加缺 PM 阻塞和四类身份齐全通过测试。
- 前端代码修复范围：`FrontlineFixedTemplatePanel.vue` 锁定 PQC 任务快照检验数量，提交 payload 改为 exact sample values；新增 `mes-frontline-pqc-task-quantity-static.spec.js` 静态合同。

## Passed

- `node yudao-module-mes\src\test\js\mes-pqc-task-generation-static.spec.cjs` -> PASS。
- `node tests/e2e/mes-frontline-pqc-task-quantity-static.spec.js` -> PASS。
- `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS。
- `node tests/e2e/frontline-formal-submit-static.spec.cjs` -> PASS。
- `node tests/e2e/edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> PASS。
- `git diff --check -- <task-owned files>` -> PASS；仅 CRLF 工作区提示，无 whitespace error。

## Blocked

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 未产出业务 RED/GREEN；JVM 报 `Native memory allocation (mmap) failed ... G1 virtual space`，见 `E:\IntRuoyi\IntRuoyiBackend\hs_err_pid55128.log`。
- `mvn -o -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` 180 秒超时无 surefire 结果；任务自有 Maven PID 53080 已清理。
- `$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'; mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` 240 秒超时无 surefire 结果；仅终止本次 PID 55352 进程树。
- AC-M15 全量验收仍需末检“不适用”显式依据的正式持久化设计；当前 QA 规程发布模型强制 `FINAL` 规则存在，本次不能伪造不适用依据或将 AC-M15 标记为全量 `ACCEPTED`。

## Result

当前结果为部分修复：数量一致性、前端不截断链路、正式 PQC 任务生成、上午/下午巡检身份分离、`301×5%` 向上取整、适用末检任务生成和放行完整性预期任务集合校验已完成并通过静态合同验证；后端 JUnit 运行验证和末检“不适用”依据模型仍阻塞。

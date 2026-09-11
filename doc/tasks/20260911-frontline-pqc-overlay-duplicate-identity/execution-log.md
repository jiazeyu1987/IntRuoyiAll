# Execution Log

BDD: 跨生产工序的同一 QA 项目任务分别展示 -> Given 同一活跃订单、QA 版本、QA 工序、检验项目和巡检规则在两道冻结生产工序各有一条待检任务，When 一线 PQC 加载工序任务选项，Then 两条任务按各自 routeProcessId 和 processId 精确匹配，不报重复身份。

BDD: 跨日期班次轮次的任务分别展示 -> Given 同一生产工序的同一 QA 项目存在不同业务日期、班次或轮次任务，When 构建覆盖结果，Then 每条任务按完整时序身份匹配。

BDD: 完整身份重复继续阻断 -> Given 两条待检任务的全部正式身份字段完全相同，When 构建覆盖结果，Then 后端明确抛出重复身份异常，不任取第一条。

## Evidence

- REPRO: `output/runtime/int_main/logs/yudao-server.log` -> `activeOrderId=1009200075, regulationVersionId=70, qaProcessId=166, qaItemCode=PQC-IDI-001-I001, inspectionRuleKey=PATROL_AM` 被 `MesFrontlinePqcTaskOverlay.fromExpectedTask` 判定重复。
- ROOT CAUSE: `ExpectedTaskIdentity` 与匹配过滤器遗漏 `routeProcessId/processId`，且已有 `businessDate/shiftCode/roundNo` 未进入匹配条件。
- RED: 新增跨生产工序、跨日期班次轮次和完整重复场景后，旧匹配身份无法区分合法任务。
- GREEN: `mvn --% -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dtest=MesFrontlinePqcTaskOverlayTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，7 tests。
- RED: `node yudao-module-mes/src/test/js/mes-frontline-pqc-overlay-complete-identity-static.spec.cjs --baseline` -> FAIL，基线缺少 `Long routeProcessId`。
- GREEN: `node yudao-module-mes/src/test/js/mes-frontline-pqc-overlay-complete-identity-static.spec.cjs` -> PASS。
- REGRESSION: `mvn --% -f pom.xml -pl yudao-module-mes -Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlinePqcContextServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，21 tests。
- GREEN: `mvn.cmd -q -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlinePqcContextServiceTest,MesFrontlinePqcSubmissionConcurrencyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，26 tests，0 failures，0 errors。
- GREEN: `node yudao-module-mes\src\test\js\mes-frontline-pqc-overlay-complete-identity-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff-files --check -- IntRuoyiBackend IntRuoyiFronted docs doc` -> PASS，仅换行提示。
- GREEN: backend API evidence validator -> PASS。
- GREEN: bug regression evidence validator -> PASS。

## Completed Work

- 覆盖身份增加 `routeProcessId/processId`。
- 匹配过滤增加生产工序、业务日期、班次和轮次精确条件。
- 重复异常补充完整任务身份。
- PQC 上下文构建覆盖身份时透传任务生产工序字段。

## Remaining Blockers

- 当前运行 Jar 早于本次源码修复；未经授权不重启 int_main，运行态页面待后续授权复验。
- 本轮未执行真实 E2E、Git 提交或推送。

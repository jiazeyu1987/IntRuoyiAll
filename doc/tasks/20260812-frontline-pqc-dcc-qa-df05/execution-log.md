# Execution Log

## 2026-08-12

- Intent: 执行 DF05，仅在允许范围内修改 QA 管理前端、DCC 项目列表 QA 状态列、QA regulation save/publish 片段、专属静态合同和后端服务测试。
- Rule reads: AGENTS.md、docs/backend-development.md、docs/frontend-development.md、docs/database-rules.md、docs/e2e-rules.md、docs/powershell-encoding.md、docs/task-closeout-rules.md、主管 dev-plan.md / test-plan.md、DF05 设计任务和共享接口/数据/BDD/TDD 合同。
- BDD: DCC直接管理QA -> Given 用户选择一个DCC项目代码, When 在QA管理页保存和发布规程, Then payload只包含dccProjectCodeId及完整rules/processes/items字段，不包含product/route/MES process身份，且同一DCC只形成一份QA规程。
- BDD: DCC列表批量组合QA状态 -> Given DCC项目代码列表当前页有多条项目, When 页面加载或翻页筛选, Then 前端仅按当前页dccProjectCodeId批量调用MES project-statuses并合并状态，过期响应不得覆盖新页。
- BDD: 后端拒绝旧推算身份 -> Given save/publish请求携带DCC ID和QA完整业务字段, When 服务端保存或发布, Then regulation.dccProjectCodeId是唯一关系来源，服务不读取productId/routeId/routeProcessId/processId来推算QA归属。

## Evidence

- RED: node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs -> FAIL, API item resultType 仍为 string，未收紧为 BOOLEAN/NUMERIC/TEXT。
- RED: node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs -> FAIL, DCC 项目代码 QA 状态加载缺少 request serial，过期响应可能覆盖新页。
- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest#saveDraft_rejectsLegacyResultTypes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, NUMBER/CHOICE 未抛 ServiceException。
- NOTE: 完整 Maven 指定命令在 RED 阶段首次以 120s 超时退出，未作为行为 RED；后续用单方法命令取得后端行为 RED，完整指定命令在 GREEN/回归阶段通过。
- GREEN: node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs -> PASS。
- GREEN: node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs -> PASS。
- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest#saveDraft_rejectsLegacyResultTypes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS。
- REGRESSION: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 8, Failures: 0, Errors: 0, Skipped: 0。
- SCAN: git diff --name-only -> 仅 DF05 允许范围内文件；禁止项扫描命中均为既有 DCC 产品建档/Mockito/测试旧枚举/QA processId 字段上下文，未新增 DCC-side binding 表、QA 状态产品/路线/MES 工序推算或 fallback。

## Blockers

- none

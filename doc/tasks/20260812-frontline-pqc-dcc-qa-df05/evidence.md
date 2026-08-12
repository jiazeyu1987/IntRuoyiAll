# DF05 Evidence

## BDD

- BDD: DCC直接管理QA -> Given 用户选择一个DCC项目代码, When 在QA管理页保存和发布规程, Then payload只包含dccProjectCodeId及完整rules/processes/items字段，不包含product/route/MES process身份，且同一DCC只形成一份QA规程。
- BDD: DCC列表批量组合QA状态 -> Given DCC项目代码列表当前页有多条项目, When 页面加载或翻页筛选, Then 前端仅按当前页dccProjectCodeId批量调用MES project-statuses并合并状态，过期响应不得覆盖新页。
- BDD: 后端拒绝旧结果类型 -> Given save/publish请求携带DCC ID和QA完整业务字段, When item resultType 为 NUMBER/CHOICE, Then 服务端明确拒绝旧结果类型且不保存。

## RED

- node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs -> FAIL，API item resultType 仍为 string。
- node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs -> FAIL，QA 状态加载缺少 request serial。
- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest#saveDraft_rejectsLegacyResultTypes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，NUMBER/CHOICE 未抛 ServiceException。

## GREEN / Regression

- node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs -> PASS。
- node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs -> PASS。
- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest#saveDraft_rejectsLegacyResultTypes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS。
- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 8, Failures: 0, Errors: 0, Skipped: 0。

## Scope / Safety

- 未提交、未合并、未删除 worktree、未 push、未部署、未启动服务、未修改共享业务数据。
- 未新增 DCC 侧 QA 绑定表；QA 状态读取按 dccProjectCodeId 批量调用 MES project-statuses。
- 未引入 fallback/降级/吞异常；无 blocker。

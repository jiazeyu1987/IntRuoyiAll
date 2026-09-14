# 活跃订单放行资料 TDD 计划 V2

## Purpose and Scope

本文定义 V2 实现的严格 TDD 顺序。测试目标从“按钮能申请”提升为“真实历史数据能形成双 100%，并能按映射生成正式资料后走到负责人放行”。

## Evidence Reviewed

- BDD 场景 V2。
- 后端设计 V2。
- 测试数据计划 V2。
- 项目 BDD/TDD 与 no-fallback 规则。

## TDD Sequence

1. 来源校验服务测试：覆盖生产历史、PQC 历史、历史表单、签名和双 100% 来源。
2. QA/批记录约束测试：覆盖 PQC 数据符合 QA 文件、生产数据符合批记录表单。
3. 映射服务测试：覆盖复用 `MesTeamLeaderBatchRecordBackfillServiceImpl`、PQC 到过程检验单、损耗到损耗单。
4. 完成性检查测试：覆盖必填字段、设备参数、检验项目、签名、审核、来源追溯和损耗完整性。
5. 申请编排测试：覆盖批次执行、A3/A4/A5 writer 调用、release precheck、负责人待办和幂等。
6. Controller/API 测试：覆盖权限、`idempotencyKey`、`dossierSummary.sourceSnapshotHash`、`blockerType/reason/suggestion` 和 blocker。
7. 前端静态合同测试：覆盖按钮、确认框、状态、blocker 展示。
8. 真实 E2E 测试：用任务自有历史数据走生产组长申请和生产负责人放行。

## RED Commands

```powershell
cd E:\IntRuoyi\IntRuoyiBackend
mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderReleaseReadinessServiceTest" test
```

```powershell
cd E:\IntRuoyi\IntRuoyiBackend
mvn -pl yudao-module-mes -am "-Dtest=MesReleaseDossierMappingServiceTest" test
```

```powershell
cd E:\IntRuoyi\IntRuoyiBackend
mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseApplicationServiceTest" test
```

```powershell
cd E:\IntRuoyi\IntRuoyiBackend
node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-static.spec.cjs
```

```powershell
cd E:\IntRuoyi\IntRuoyiFronted
node tests/e2e/team-leader-active-order-release-application-static.spec.js
```

```powershell
cd E:\IntRuoyi\IntRuoyiFronted
pnpm ts:check
```

## Expected Failures

- 来源校验 RED：缺 `ActiveOrderReleaseReadinessService` 或无法读取历史来源。
- 映射 RED：缺生产/PQC/损耗字段映射服务。
- 完成性 RED：缺 QA 文件约束或批记录表单约束检查。
- 申请编排 RED：缺 A3/A4/A5 writer 接入、批次执行关联、release precheck 或负责人待办。
- 前后端契约 RED：请求仍使用 `clientRequestId`、响应依赖必填 `generatedDocuments[]` 或 blocker 字段命名不一致。
- 前端 RED：缺按钮、API wrapper、状态列或 blocker 展示。
- E2E RED：缺任务自有历史数据或页面入口。

## GREEN Commands

```powershell
cd E:\IntRuoyi\IntRuoyiBackend
mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderReleaseReadinessServiceTest,MesReleaseDossierMappingServiceTest,MesTeamLeaderActiveOrderReleaseApplicationServiceTest" test
```

```powershell
cd E:\IntRuoyi\IntRuoyiBackend
node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-static.spec.cjs
```

```powershell
cd E:\IntRuoyi\IntRuoyiFronted
node tests/e2e/team-leader-active-order-release-application-static.spec.js
pnpm ts:check
```

```powershell
cd E:\IntRuoyi\IntRuoyiFronted
pnpm test:e2e -- tests/e2e/team-leader-active-order-release-application-real.e2e.js
```

## Refactor Checks

- 不直接修改活跃订单进度字段制造通过。
- 不让测试数据绕过生产/PQC 历史列表和历史表单。
- 不把 `formBindings`、默认 `MAIN` 或工序开始配置当正式批记录。
- 不把 raw payload 当过程检验单正式来源。
- 不用当前登录人/当前时间默认签名人员和时间。
- 不在资料不完整时创建负责人待办。
- 不把生产组长申请误写成已放行。
- 不用 release task 或资料数量摘要替代 A3/A4/A5 writer 执行证明。
- 成功路径 `signatureEvidenceCount` 必须大于 0，且签名主体/时间来自来源记录。

## Evidence Log Template

```markdown
BDD: 真实历史数据形成双100后申请放行 -> Given/When/Then ...
RED: mvn ... MesActiveOrderReleaseReadinessServiceTest -> FAIL, 来源校验服务不存在
GREEN: mvn ... MesActiveOrderReleaseReadinessServiceTest -> PASS
RED: mvn ... MesReleaseDossierMappingServiceTest -> FAIL, 映射服务不存在
GREEN: mvn ... MesReleaseDossierMappingServiceTest -> PASS
RED: Playwright real E2E -> FAIL, 缺任务自有历史数据 fixture
GREEN: Playwright real E2E -> PASS
```

## Test Blockers

- TB-01 无法创建任务自有历史生产/PQC 数据时，真实成功路径阻塞。
- TB-02 QA 文件或批记录表单缺失时，只能验收 blocker。
- TB-03 签名配置缺失时，人员和时间映射不可验收。

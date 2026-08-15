# 测试计划：批记录表单导入重建工艺路线候选版本

- Task ID: task-6586818a22-20260814T121328
- Created: 2026-08-14T12:13:28
- Workspace: E:\IntRuoyi
- Validation surface: real-runtime
- Required tools: Maven, Java 17, Node.js, pnpm, Playwright, local int_main frontend/backend when running real E2E

## Test Scope

验证批记录表单 Word 导入在“工艺流程”勾选后的路线重建行为：

- 未勾选“工艺流程”：不触发按 Word 顺序重建工序节点、流程边或 START/END 边界；如仅因批记录表单绑定升版生成绑定候选，候选 flowGraph 必须沿用原 ACTIVE，不得按 Word 重排。
- 无现有路线：新建路线、工序、流程关系、DCC 项目代码绑定和初始 ACTIVE 版本。
- 已有路线：只创建或更新 DRAFT 候选版本，ACTIVE 发布前不变。
- 升版候选：Word 新工序节点按顺序生成，旧路线中可唯一映射的逐工序正式批记录表单绑定、formBindings 和工序开始配置保留。
- 异常路径：DCC 缺失、候选锁定、版本漂移、配置无法映射、正式绑定来源缺失时 fail fast。

Out of scope:

- 不验证工序结束绑定，因为当前业务不存在结束绑定关系。
- 不验证非 Word 导入。
- 不验证生产环境发布。

## Environment

- Backend root: E:\IntRuoyi\IntRuoyiBackend
- Frontend root: E:\IntRuoyi\IntRuoyiFronted
- Local int_main backend: http://127.0.0.1:48081，仅在确认服务已运行后用于真实 E2E。
- Local int_main frontend: http://127.0.0.1:8081 或 http://localhost:8081，仅在确认服务已运行后用于真实 E2E。
- Java 17 和 Maven 可用。
- pnpm 依赖已安装。
- Playwright 可用，并使用项目既有真实登录和租户规则。

缺少任一真实 E2E 前置时，真实 E2E 必须标记 BLOCKED，不得用 mock、API-only 或直接 SQL 代替。

## Accounts and Fixtures

- 后端 DB 测试使用任务自有 fixture，不依赖生产租户数据。
- 真实 E2E 需要已确认的测试租户、admin 或具备批记录表单导入/工艺路线维护权限的账号。
- 测试数据必须可追踪，命名建议包含 CODX-WORD-ROUTE-PRESERVE-日期时间。
- 写入型 E2E 必须在完成后通过页面或正式接口清理任务自有数据；清理失败需记录残留 ID。

## Commands

- Backend targeted tests:
  - cd E:\IntRuoyi\IntRuoyiBackend
  - mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordRouteGovernanceContractTest,MesProBatchRecordRouteCandidateGovernanceTest,MesProRouteVersionPublishProjectionServiceImplTest" test
  - Expected: all selected tests pass, with RED/GREEN evidence recorded during implementation.

- Frontend static contracts:
  - cd E:\IntRuoyi\IntRuoyiFronted
  - pnpm ts:check
  - node tests/e2e/batch-record-word-import-route-candidate-static.spec.js
  - node tests/e2e/batch-record-word-import-production-upgrade-dedupe-static.spec.js
  - node tests/e2e/batch-record-word-import-dcc-identity-static.spec.cjs
  - Expected: static contracts pass and confirm checkbox/route-candidate/DCC identity boundaries.

- Real browser E2E, only when runtime and test data are confirmed:
  - cd E:\IntRuoyi\IntRuoyiFronted
  - node tests/e2e/edhr-word-import-upgrade-action-real-flow.e2e.js
  - node tests/e2e/edhr-word-import-upgrade-missing-source-negative.e2e.js
  - Expected: real page path proves upgrade action and missing-source negative path without API-only replacement.

## Test Cases

### T1: 未勾选工艺流程不重建工序节点和流程关系

- Covers: P1-AC1
- Level: frontend-static plus backend negative contract
- Command: node tests/e2e/batch-record-word-import-route-candidate-static.spec.js
- Expected: submit payload does not include process-node rebuild confirmation; backend不得按 Word 工序顺序重建 flowGraph.nodes、edges 或 boundaryEdges。若只勾选“批记录表单”并生成绑定候选，该候选 flowGraph.nodes、edges、boundaryEdges 必须与原 ACTIVE 一致。

### T2: 勾选工艺流程并已有路线时提示候选版本

- Covers: P1-AC2, P2-AC3
- Level: frontend-static
- Command: node tests/e2e/batch-record-word-import-route-candidate-static.spec.js
- Expected: user sees “生成或更新候选版本，发布后生效”语义；payload carries frozen expectedRouteId、expectedRouteVersionId、expectedRouteCandidateVersionId.

### T3: 候选锁定状态阻断

- Covers: P1-AC3, P2-AC4
- Level: backend-db and frontend-static
- Command: mvn targeted tests plus node tests/e2e/batch-record-word-import-route-candidate-static.spec.js
- Expected: PENDING_APPROVAL 或 READY_TO_PUBLISH 候选存在时，前端阻断或后端 fail fast，不创建新版本、不更新旧候选。

### T4: 无现有路线时创建完整路线

- Covers: P2-AC2
- Level: backend-db
- Command: mvn targeted tests
- Expected: creates route, route processes, flow edges, START/END boundary, DCC project binding, initial ACTIVE route version; no duplicate route target is chosen.

### T5: 已有路线生成或更新 DRAFT 候选且 ACTIVE 不变

- Covers: P2-AC3, P4-AC2
- Level: backend-db
- Command: mvn targeted tests
- Expected: same-source DRAFT is updated in place; no V3 duplicate; ACTIVE route process, flow and existing configs remain unchanged before publish.

### T6: 候选节点按 Word 顺序生成

- Covers: P3-AC1
- Level: backend-db
- Command: mvn targeted tests
- Expected: candidate flowGraph.nodes sort/order follows Word process order; every node has processId and publishable routeProcess identity.

### T7: 正式批记录表单绑定保留

- Covers: P3-AC2
- Level: backend-db
- Command: mvn targeted tests
- Expected: old per-process formal batch record report bindings from batchUseConfigs.batchRecordReports map to the candidate new routeProcessId; no value is inferred from batchUseConfigs.formBindings、工序开始或默认 MAIN.

### T8: formBindings 保留但不替代批记录表单

- Covers: P3-AC3
- Level: backend-db and static contract
- Command: mvn targeted tests
- Expected: batchUseConfigs.formBindings are preserved only as form slot configs; batchRecordFormNames and formal batch record binding still come from batchUseConfigs.batchRecordReports or the dedicated per-process binding source.

### T9: 工序开始配置保留，工序结束不生成绑定

- Covers: P3-AC4
- Level: backend-db
- Command: mvn targeted tests
- Expected: routeStartProductionLeaders、batchRecordAttachmentOwners or equivalent start-node configuration maps to candidate; END remains flow boundary only and has no invented binding row.

### T13: 只导入批记录表单绑定时不重排 flowGraph

- Covers: P1-AC1
- Level: backend-db and frontend-static
- Command: mvn targeted tests plus node tests/e2e/batch-record-word-import-route-candidate-static.spec.js
- Expected: 只勾选“批记录表单”或未勾选“工艺流程”时，即使生成绑定候选，candidate flowGraph.nodes、edges、boundaryEdges 也必须与来源 ACTIVE 一致，不得按 Word 工序顺序重排。

### T10: 配置工序无法唯一映射时失败

- Covers: P3-AC5, P4-AC3
- Level: backend-db negative
- Command: mvn targeted tests
- Expected: missing configured process, duplicate ambiguous process, or missing formal binding source causes explicit failure with route/process context.

### T11: 发布候选后正式路线保留三类配置

- Covers: P4-AC1
- Level: backend-db plus optional real-browser
- Command: mvn targeted tests; real E2E when runtime confirmed
- Expected: after publish projection, active route has Word-updated nodes and preserved formal batch record binding, formBindings and start config.

### T12: DCC 与版本并发保护

- Covers: P2-AC1, P2-AC4
- Level: backend-db
- Command: mvn targeted tests
- Expected: missing dccProjectCodeId、DCC 停用、expected route/version drift、candidate id drift all fail fast.

## Coverage Matrix

| Case ID | Area | Scenario | Level | Acceptance IDs | Evidence |
| --- | --- | --- | --- | --- | --- |
| T1 | 导入入口 | 未勾选不重建工序节点和流程关系 | frontend-static/backend | P1-AC1 | execution-log.md |
| T2 | 导入入口 | 已有路线提示候选 | frontend-static | P1-AC2, P2-AC3 | execution-log.md |
| T3 | 候选治理 | 锁定候选阻断 | backend/frontend | P1-AC3, P2-AC4 | execution-log.md |
| T4 | 新路线 | 创建路线和 DCC 绑定 | backend-db | P2-AC2 | execution-log.md |
| T5 | 已有路线 | DRAFT 候选且 ACTIVE 不变 | backend-db | P2-AC3, P4-AC2 | execution-log.md |
| T6 | 候选节点 | Word 顺序 | backend-db | P3-AC1 | execution-log.md |
| T7 | 批记录表单 | 正式绑定保留 | backend-db | P3-AC2 | execution-log.md |
| T8 | 表单槽位 | formBindings 独立保留 | backend-db | P3-AC3 | execution-log.md |
| T9 | 工序开始 | START 配置保留 | backend-db | P3-AC4 | execution-log.md |
| T10 | 失败分支 | 无法唯一映射失败 | backend-db | P3-AC5, P4-AC3 | execution-log.md |
| T11 | 发布投影 | 发布后正式路线完整 | backend-db/real-browser | P4-AC1 | test-report.md |
| T12 | 并发/DCC | 版本和 DCC fail fast | backend-db | P2-AC1, P2-AC4 | execution-log.md |
| T13 | 批记录绑定候选 | 只导入绑定不重排 flowGraph | backend-db/frontend-static | P1-AC1 | execution-log.md |

## Evaluator Independence

- Mode: blind-first-pass
- Validation surface: real-runtime
- Required tools: Maven, pnpm, Playwright
- First-pass readable artifacts: prd.md, test-plan.md
- Withheld artifacts: execution-log.md, task-state.json
- Real environment expectation: UI 路径必须通过真实浏览器和本地运行态验证；写入型 E2E 只能使用确认的测试租户和任务自有数据。
- Escalation rule: 测试员首轮 verdict 前不读取执行日志；若发现结果与计划不一致，再读取 withheld artifacts 做差异分析。

## Pass / Fail Criteria

Pass when:

- 所有 P1 到 P4 acceptance id 均有对应测试证据。
- 未勾选和勾选“工艺流程”的工序节点/流程关系重建边界均被覆盖。
- 旧绑定关系保留、候选发布、ACTIVE 发布前不变均通过。
- DCC、候选锁定、版本漂移、映射失败均 fail fast。

Fail when:

- 任何测试通过 formBindings、默认 MAIN、空值或工序开始配置证明正式批记录表单绑定。
- 导入直接覆盖 ACTIVE 路线。
- 候选发布前运行态读取候选配置。
- 旧配置无法映射却仍成功。
- 真实 E2E 前置缺失时被替换为 mock、API-only 或直接 SQL。

## Regression Scope

- 批记录表单 Word 导入普通升版和新建版本。
- 路线候选版本工作区、DRAFT 更新、候选取消和发布。
- DCC 项目代码到路线正式绑定。
- 批次执行中“批记录表单”、formBindings 和工序开始三条链路展示。
- 现有 batch-record-word-import、mes-route-candidate、route publish projection 测试。

## Reporting Notes

- RED/GREEN 证据写入 execution-log.md。
- 独立测试 verdict 写入 test-report.md。
- 真实 E2E 截图、trace、创建的数据 ID 和清理结果必须记录。
- 被阻塞的真实 E2E 要记录缺少的具体前置和业务影响，不得写成通过。

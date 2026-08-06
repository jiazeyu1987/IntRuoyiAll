# Test Report

- Task ID: `20260805-production-leader-process-config-unification`
- Created: `2026-08-06`
- Workspace: `D:\IntRuoyiWorktree\20260805-process-config-unification`
- User Request: `生产组长损耗管理、设备映射和设备参数设置合并为以路线工序为主线的统一配置表；参数维护目标值、上下限，实际平均值从生产提交统计并显示周期与样本数；2026-08-06 用户取消真实 E2E 合并前门禁，要求直接合并到主代码并手动验证。`

## Environment Used

- Evaluation mode: full-context
- Validation surface: code-only
- Tools: python, maven, node, pnpm, git
- Initial readable artifacts: prd.md, test-plan.md, execution-log.md, task-state.json
- Initial withheld artifacts:
- Initial verdict before withheld inspection: no

## Results

### T1: 历史空路线工序或目标值迁移阻塞

- Result: passed
- Covers: P1-AC1
- Command run: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_process_pool_device_parameter_route_process_migration.py -q`
- Environment proof: code-only migration contract; no runtime required
- Evidence refs: execution-log.md#P1-TDD-Evidence, database-schema-evidence.md#Migration-Verification
- Notes: 迁移测试覆盖历史 `route_process_id` 或 `default_value` 为空时 fail fast，且不按 `deleted` 状态排除历史行。

### T2: 参数规则非空和唯一约束

- Result: passed
- Covers: P1-AC2
- Command run: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Environment proof: code-only JUnit schema contract; no runtime required
- Evidence refs: execution-log.md#P1-TDD-Evidence, database-schema-evidence.md#Migration-Verification
- Notes: schema 合同锁定 `route_process_id NOT NULL`、`default_value NOT NULL` 和路线工序维度唯一索引。

### T3: 禁止猜测回填

- Result: passed
- Covers: P1-AC3
- Command run: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260805-production-leader-process-config-unification\migration-policy-gate.json`
- Environment proof: code-only release migration policy gate; no runtime required
- Evidence refs: execution-log.md#Authorized-Blocker-Fix, migration-policy-gate.json
- Notes: 全仓 migration policy gate 已通过；参数规则迁移不包含默认路线、首条路线或任意目标值回填。

### T4: 授权路线工序聚合列表

- Result: passed
- Covers: P2-AC1
- Command run: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Environment proof: code-only backend target test suite; no runtime required
- Evidence refs: execution-log.md#P2-TDD-Evidence, backend-api-evidence.md
- Notes: 统一列表只返回当前生产组长经“工序开始”授权的路线工序，并聚合损耗、设备和参数。

### T5: 路线工序设备绑定校验

- Result: passed
- Covers: P2-AC2
- Command run: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Environment proof: code-only backend target test suite; no runtime required
- Evidence refs: execution-log.md#P2-TDD-Evidence, backend-api-evidence.md
- Notes: 设备绑定以 `routeProcessId + deviceId` 为输入，由服务端解析正式 `processId` 并拒绝未授权、非当前组长或不可用设备。

### T6: 参数区间和映射校验

- Result: passed
- Covers: P2-AC3
- Command run: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Environment proof: code-only backend target test suite; no runtime required
- Evidence refs: execution-log.md#P2-TDD-Evidence, backend-api-evidence.md
- Notes: 参数保存要求非空 `routeProcessId`、已映射设备和合法 `lowerLimit <= targetValue <= upperLimit`。

### T7: 相同参数编码执行 upsert

- Result: passed
- Covers: P2-AC4
- Command run: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Environment proof: code-only backend target test suite; no runtime required
- Evidence refs: execution-log.md#P2-TDD-Evidence, backend-api-evidence.md
- Notes: 相同 `routeProcessId + deviceId + parameterCode` 再保存走更新路径，不新增重复有效规则。

### T8: 近 30 天正式数值平均值

- Result: passed
- Covers: P2-AC5
- Command run: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Environment proof: code-only backend target test suite; no runtime required
- Evidence refs: execution-log.md#P2-TDD-Evidence, backend-api-evidence.md
- Notes: 平均值只统计近 30 天正式 `PRODUCTION_SUBMIT` 的数值型 `raw_payload.equipmentParameters`，并按完整上下文精确过滤。

### T9: 无样本统计语义

- Result: passed
- Covers: P2-AC6
- Command run: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Environment proof: code-only backend target test suite; no runtime required
- Evidence refs: execution-log.md#P2-TDD-Evidence, backend-api-evidence.md
- Notes: 无样本返回 `actualAverage=null`、`sampleCount=0` 和统计周期，不用目标值或默认值回填。

### T10: 清除空 routeProcess fallback 和重复写路径

- Result: passed
- Covers: P2-AC7
- Command run: `rg -n "MesProcessDeviceParameterRuleService|MesProcessDeviceParameterRuleSaveReqBO|runtime-device-parameter-rule|device-parameter-rule/save|process-device-binding/save" IntRuoyiBackend\yudao-module-mes\src\main IntRuoyiBackend\yudao-module-mes\src\test`
- Environment proof: code-only source scan and backend regression; no runtime required
- Evidence refs: execution-log.md#P2-TDD-Evidence, backend-api-evidence.md
- Notes: 旧重复参数写服务已删除，空 `routeProcessId` 规则不再作为前线运行态兼容来源。

### T11: 单一工序配置入口

- Result: passed
- Covers: P3-AC1
- Command run: `node IntRuoyiFronted\tests\e2e\team-leader-process-config-unified-static.spec.cjs`
- Environment proof: code-only frontend static contract; no runtime required
- Evidence refs: execution-log.md#P3-TDD-Evidence, frontend-feature-evidence.md
- Notes: 生产组长页面保留单一“工序配置 / processConfig”入口，旧独立损耗入口和裸 ID 配置卡片不再作为可操作入口。

### T12: 统一表字段和设备参数展开

- Result: passed
- Covers: P3-AC2
- Command run: `node IntRuoyiFronted\tests\e2e\team-leader-process-config-unified-static.spec.cjs`
- Environment proof: code-only frontend static contract; no runtime required
- Evidence refs: execution-log.md#P3-TDD-Evidence, frontend-feature-evidence.md
- Notes: 统一表按 `routeProcessId` 行键展示路线工序、损耗、设备、参数标准、平均值、样本数和统计周期。

### T13: 行上下文弹窗

- Result: passed
- Covers: P3-AC3
- Command run: `pnpm ts:check`; `node IntRuoyiFronted\tests\e2e\team-leader-process-config-unified-static.spec.cjs`
- Environment proof: code-only frontend type check and static contract; no runtime required
- Evidence refs: execution-log.md#P3-TDD-Evidence, frontend-feature-evidence.md
- Notes: 三类维护均从当前路线工序行进入，弹窗冻结 `routeProcessId`，平均值、样本数和统计周期保持只读。

### T14: 前端校验、错误和正式刷新

- Result: passed
- Covers: P3-AC4
- Command run: `pnpm ts:check`; `node IntRuoyiFronted\tests\e2e\team-leader-process-config-unified-static.spec.cjs`
- Environment proof: code-only frontend type check and static contract; no runtime required
- Evidence refs: execution-log.md#P3-TDD-Evidence, frontend-feature-evidence.md
- Notes: 前端校验非法区间并在保存成功后重新读取正式统一行数据，不使用本地数组假回显。

### T15: 无样本页面展示

- Result: passed
- Covers: P3-AC5
- Command run: `node IntRuoyiFronted\tests\e2e\team-leader-process-config-unified-static.spec.cjs`
- Environment proof: code-only frontend static contract; no runtime required
- Evidence refs: execution-log.md#P3-TDD-Evidence, frontend-feature-evidence.md
- Notes: `actualAverage=null` 时显示“暂无样本”或 `--`，样本数显示 `0`，不以目标值冒充平均值。

### T16: 响应式和可测试性

- Result: passed
- Covers: P3-AC6
- Command run: `node IntRuoyiFronted\tests\e2e\team-leader-process-config-unified-static.spec.cjs`; `node IntRuoyiFronted\tests\e2e\team-leader-workbench-static.spec.cjs`; `node IntRuoyiFronted\tests\e2e\mes-process-pool-team-leader-static.spec.js`; `node IntRuoyiFronted\tests\e2e\frontline-team-config-static.spec.cjs`
- Environment proof: code-only frontend static contracts; no runtime required
- Evidence refs: execution-log.md#P3-TDD-Evidence, frontend-feature-evidence.md
- Notes: 关键控件具备稳定选择器，相邻生产组长、MES 工序池和前线运行态静态合同均通过。

### T17: 自动回归门禁

- Result: passed
- Covers: P4-AC1
- Command run: Database、Backend、Frontend、Final Checks 的保留命令；不包含用户已取消的真实写入型 E2E
- Environment proof: code-only retained gate set; no runtime required
- Evidence refs: verification-report.md#Commands, execution-log.md#P4-Manual-Verification-Scope-Change
- Notes: 目标迁移、全仓 migration policy、目标 Maven `-am`、前端类型检查、静态合同、`node --check`、`git diff --check` 和端口守卫构成合并前自动门禁。

### T18: 真实 E2E 脚本语法资产

- Result: passed
- Covers: P4-AC2
- Command run: `node --check tests\e2e\team-leader-process-config-unified-real.e2e.js`
- Environment proof: code-only script syntax check; no runtime required
- Evidence refs: verification-report.md#Commands, execution-log.md#P4-Manual-Verification-Scope-Change
- Notes: 真实 E2E 脚本保留为可选后续验证资产；本轮不运行真实写入型 Playwright，不生成截图或 trace，也不声明真实页面已通过。

### T19: 用户手动验收交接

- Result: passed
- Covers: P4-AC3
- Command run: 审阅 `verification-report.md`、`test-report.md` 和 `docs\changes\20260806-production-leader-process-config-manual-verification.md`
- Environment proof: documentation handoff; no runtime required before merge
- Evidence refs: verification-report.md#Manual-Verification-Handoff, docs/changes/20260806-production-leader-process-config-manual-verification.md
- Notes: 手动验收范围覆盖统一表配置闭环、一线正式提交平均值和无样本 null/0 语义，用户将在主代码验证。

### T20: 范围变更与风险记录

- Result: passed
- Covers: P4-AC4
- Command run: `python -X utf8 C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260806-production-leader-process-config-manual-verification.md`; `python -X utf8 C:\Users\BJB110\.codex\skills\spec-driven-delivery\scripts\validate_test_report.py --cwd D:\IntRuoyiWorktree\20260805-process-config-unification --task-id 20260805-production-leader-process-config-unification --expected-outcome passed`
- Environment proof: governance artifact validation; no runtime required
- Evidence refs: docs/changes/20260806-production-leader-process-config-manual-verification.md, verification-report.md#P4-Final-Verdict
- Notes: 取消 E2E 的用户原话、保留门禁、未运行真实 E2E 边界、非任务历史回归失败和合并风险均已记录；未把真实 E2E 写成已通过。

## Final Verdict

- Outcome: passed
- Verified acceptance ids: P1-AC1, P1-AC2, P1-AC3, P2-AC1, P2-AC2, P2-AC3, P2-AC4, P2-AC5, P2-AC6, P2-AC7, P3-AC1, P3-AC2, P3-AC3, P3-AC4, P3-AC5, P3-AC6, P4-AC1, P4-AC2, P4-AC3, P4-AC4
- Blocking prerequisites:
- Summary: 更新后的 code-only/full-context 合并前门禁全部通过；真实 Playwright 写入型验证由用户明确移出完成门禁并改为合并后手动验收，报告不声明真实 E2E 已通过。

## Open Issues

- No completion blocker remains under the user-approved merge scope.
- Broader Maven regression仍存在非任务历史失败；本轮不将其写成本任务 GREEN，也不在未授权情况下扩展修复。

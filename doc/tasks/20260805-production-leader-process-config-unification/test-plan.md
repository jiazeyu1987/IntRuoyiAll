# Test Plan

- Task ID: `20260805-production-leader-process-config-unification`
- Created: `2026-08-05`
- Workspace: `D:\IntRuoyiWorktree\20260805-process-config-unification`
- User Request: `生产组长损耗管理、设备映射和设备参数设置合并为以路线工序为主线的统一配置表；参数维护目标值、上下限，实际平均值从生产提交统计并显示周期与样本数；2026-08-06 用户取消真实 E2E 合并前门禁，要求直接合并到主代码并手动验证。`

## Test Scope

验证参数规则数据库迁移、路线工序统一读写、30 天实际平均值、前端统一表交互、相邻生产组长/前线运行态静态合同、真实 E2E 脚本语法和人工验收交接记录。

测试必须证明：

- 参数规则 `routeProcessId` 和目标值非空，迁移遇到历史 `route_process_id` 或 `default_value` 空值时 fail fast。
- 参数保存满足 `lowerLimit <= targetValue <= upperLimit`。
- 相同 `routeProcessId + deviceId + parameterCode` 更新原规则，不产生重复有效规则。
- 实际平均值只统计近 30 天正式 `PRODUCTION_SUBMIT` 的数值型 `equipmentParameters`。
- 无样本时返回并展示 `actualAverage=null/sampleCount=0`，不得以目标值冒充平均值。
- 生产组长页面只有一个“工序配置”统一入口，设备、参数和损耗维护均从路线工序行进入。
- 设备档案和其它班组配置保持独立，不进入统一表。
- 真实 Playwright 写入型页面验证不再作为合并前门禁；用户会在主代码合并后手动验证。

## Environment

- OS: Windows，PowerShell 命令不使用 `&&`。
- Workspace: `D:\IntRuoyiWorktree\20260805-process-config-unification`。
- Runtime profile: `int_main slot 4`，前端 `8085`、后端 `48085`；本轮合并前不启动运行态。
- Backend: Java 17、Maven、Spring Boot。
- Frontend: Vue 3、TypeScript、pnpm、Vite。
- Evidence root: `doc\tasks\20260805-production-leader-process-config-unification`。

## Accounts and Fixtures

- 合并前自动化验证不需要真实账号、租户、路线工序、设备或一线填写页 fixture。
- 用户手动验收时需自行使用具备生产组长权限、工序开始授权、可选设备和一线生产填写入口的测试数据。
- 手动验收应覆盖设备映射、参数新增/更新、非法区间拒绝、损耗维护、正式提交平均值和无样本语义。
- 不得把 API-only、mock、静态合同、旧截图或旧 trace 写成真实页面已通过。

## Commands

### Artifact Validation

```powershell
python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260806-production-leader-process-config-manual-verification.md
python C:\Users\BJB110\.codex\skills\spec-driven-delivery\scripts\validate_artifacts.py --cwd D:\IntRuoyiWorktree\20260805-process-config-unification --task-id 20260805-production-leader-process-config-unification
python C:\Users\BJB110\.codex\skills\spec-driven-delivery\scripts\validate_test_report.py --cwd D:\IntRuoyiWorktree\20260805-process-config-unification --task-id 20260805-production-leader-process-config-unification --expected-outcome passed
```

成功信号：变更记录、PRD、测试计划和测试报告结构有效。

### Database

```powershell
python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_process_pool_device_parameter_route_process_migration.py -q
python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260805-production-leader-process-config-unification\migration-policy-gate.json
```

成功信号：迁移合同 `4 passed`，全仓 migration policy gate `status=passed`。

### Backend

```powershell
mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderProcessConfigServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

成功信号：目标 schema、聚合、授权、upsert、统计、Controller 和前线回归全部通过，Maven `BUILD SUCCESS`。

### Frontend

```powershell
pnpm ts:check
node tests\e2e\team-leader-process-config-unified-static.spec.cjs
node tests\e2e\team-leader-workbench-static.spec.cjs
node tests\e2e\mes-process-pool-team-leader-static.spec.js
node tests\e2e\frontline-team-config-static.spec.cjs
node --check tests\e2e\team-leader-process-config-unified-real.e2e.js
```

工作目录：`D:\IntRuoyiWorktree\20260805-process-config-unification\IntRuoyiFronted`。

成功信号：类型检查、统一表静态合同、相邻静态合同和真实 E2E 脚本语法检查通过。

### Final Checks

```powershell
git diff --check
scripts\preflight\branch-runtime-port-guard.ps1
python C:\Users\BJB110\.codex\skills\spec-driven-delivery\scripts\check_completion.py --cwd D:\IntRuoyiWorktree\20260805-process-config-unification --task-id 20260805-production-leader-process-config-unification --apply
```

成功信号：无 whitespace error，端口守卫通过，completion gate 通过。

## Test Cases

### T1: 历史空路线工序或目标值迁移阻塞

- Covers: P1-AC1
- Level: migration contract
- Command: `pytest` 目标迁移测试。
- Expected: 任一历史参数规则行存在 `route_process_id IS NULL` 或 `default_value IS NULL` 时迁移触发 `SQLSTATE 45000`，包括 `deleted` 历史行；不猜测回填。

### T2: 参数规则非空和唯一约束

- Covers: P1-AC2
- Level: schema
- Command: `MesProcessPoolTeamLeaderSchemaTest`。
- Expected: `route_process_id NOT NULL`、`default_value NOT NULL`，新唯一索引包含路线工序，旧唯一索引不存在。

### T3: 禁止猜测回填

- Covers: P1-AC3
- Level: migration policy
- Command: 迁移 pytest 与 migration policy gate。
- Expected: SQL 不含默认路线、首条路线、按 `process_id` 自动回填或任意默认目标值，policy gate PASS。

### T4: 授权路线工序聚合列表

- Covers: P2-AC1
- Level: backend unit/integration
- Command: `MesTeamLeaderProcessConfigServiceTest`。
- Expected: 只返回授权路线工序，顺序稳定，每行聚合损耗、设备和参数；未授权行不存在。

### T5: 路线工序设备绑定校验

- Covers: P2-AC2
- Level: backend unit/controller
- Command: Service 与 Controller 目标测试。
- Expected: 服务端从 `routeProcessId` 解析 `processId`；未授权、非所属、禁用或报修设备均拒绝且 mapper insert/update 未调用。

### T6: 参数区间和映射校验

- Covers: P2-AC3
- Level: backend unit/controller
- Command: `MesTeamLeaderRuntimeConfigServiceTest` 与 Controller 测试。
- Expected: 合法边界可保存；`lower>target`、`target>upper`、空 `routeProcessId`、未映射设备均返回业务错误且无写入。

### T7: 相同参数编码执行 upsert

- Covers: P2-AC4
- Level: backend integration
- Command: `MesTeamLeaderProcessConfigServiceTest`。
- Expected: 第二次保存返回同一规则 ID，执行 update 而非 insert，有效规则数保持 1，审计包含 before/after。

### T8: 近 30 天正式数值平均值

- Covers: P2-AC5
- Level: backend unit/integration
- Command: `MesTeamLeaderProcessConfigServiceTest`。
- Expected: 只纳入窗口内、按 `routeProcessId + deviceId + parameterCode` 匹配、事件类型为 `PRODUCTION_SUBMIT` 的 JSON 数值；排除窗口外、其它事件、其它上下文和非数值。

### T9: 无样本统计语义

- Covers: P2-AC6
- Level: backend unit/controller
- Command: Service 与 Controller 目标测试。
- Expected: `actualAverage=null`、`sampleCount=0`、统计起止时间和窗口天数完整，不读取目标值作为平均值。

### T10: 清除空 routeProcess fallback 和重复写路径

- Covers: P2-AC7
- Level: backend static/unit regression
- Command: `MesFrontlineRuntimeConfigServiceTest` 及任务专用源码合同。
- Expected: 空 `routeProcessId` 规则不匹配任何运行工序；只有一个正式参数写 Service/Controller 链路，无双写、接口别名或默认成功。

### T11: 单一工序配置入口

- Covers: P3-AC1
- Level: frontend static
- Command: `team-leader-process-config-unified-static.spec.cjs`。
- Expected: 页面存在一个“工序配置”入口和一张以 `routeProcessId` 为行键的统一表；旧独立损耗表、裸 ID 设备映射和参数卡片不可操作。

### T12: 统一表字段和设备参数展开

- Covers: P3-AC2
- Level: frontend static
- Command: 任务专用静态合同。
- Expected: 路线工序、损耗、设备、参数标准、平均值、样本数和统计周期均有正式绑定及稳定选择器。

### T13: 行上下文弹窗

- Covers: P3-AC3
- Level: frontend static/component
- Command: 任务专用静态合同与 `pnpm ts:check`。
- Expected: 三类维护从当前行进入；`routeProcessId` 冻结；设备通过列表选择；平均值、样本数、周期没有输入控件。

### T14: 前端校验、错误和正式刷新

- Covers: P3-AC4
- Level: frontend static/component
- Command: 任务专用静态合同与 `pnpm ts:check`。
- Expected: 非法区间前端阻止或后端明确拒绝；错误可见；成功后重新 GET 正式行数据，不用本地数组假回显。

### T15: 无样本页面展示

- Covers: P3-AC5
- Level: frontend static
- Command: 任务专用静态合同。
- Expected: 平均值显示“暂无样本”或 `--`，样本数为 `0`，DOM 中不以目标值代替平均值。

### T16: 响应式和可测试性

- Covers: P3-AC6
- Level: frontend static
- Command: 任务专用静态合同与源码审查。
- Expected: 关键控件选择器稳定，表格可横向访问或合理展开，无重复写入口和不可操作遮挡。

### T17: 自动回归门禁

- Covers: P4-AC1
- Level: regression
- Command: 本计划 Database、Backend、Frontend、Final Checks 的保留命令。
- Expected: 目标 RED/GREEN 有记录，最终全部 PASS；非任务历史全量 Maven 失败不得写成本任务 GREEN，也不作为本次合并前门禁。

### T18: 真实 E2E 脚本语法资产

- Covers: P4-AC2
- Level: static
- Command: `node --check tests\e2e\team-leader-process-config-unified-real.e2e.js`。
- Expected: 脚本语法有效；真实写入型 Playwright 不运行，不生成截图/trace，不被记录为已通过。

### T19: 用户手动验收交接

- Covers: P4-AC3
- Level: documentation
- Command: 审阅 `verification-report.md`、`test-report.md` 和变更记录。
- Expected: 手动验收范围明确覆盖统一表配置闭环、一线正式提交平均值和无样本 null/0 语义。

### T20: 范围变更与风险记录

- Covers: P4-AC4
- Level: governance
- Command: `validate_change_request.py`、`validate_test_report.py` 和 `check_completion.py`。
- Expected: 取消 E2E 的用户原话、原因、保留门禁、未运行真实 E2E 边界和风险记录完整；不得把未运行真实 E2E 写成 PASS。

## Coverage Matrix

| Case ID | Area | Scenario | Level | Acceptance IDs | Evidence |
| --- | --- | --- | --- | --- | --- |
| T1 | Database | 空历史阻断 | migration | P1-AC1 | pytest |
| T2 | Database | 非空和唯一约束 | schema | P1-AC2 | JUnit |
| T3 | Database | 禁止回填 | policy | P1-AC3 | policy gate |
| T4 | Backend | 授权聚合列表 | unit | P2-AC1 | Maven |
| T5 | Backend | 设备绑定 | unit/controller | P2-AC2 | Maven |
| T6 | Backend | 参数区间 | unit/controller | P2-AC3 | Maven |
| T7 | Backend | upsert | integration | P2-AC4 | Maven |
| T8 | Backend | 平均值 | integration | P2-AC5 | Maven |
| T9 | Backend | 无样本 | unit/controller | P2-AC6 | Maven |
| T10 | Backend | 清除 fallback | static/unit | P2-AC7 | Maven/source scan |
| T11 | Frontend | 单一入口 | static | P3-AC1 | Node |
| T12 | Frontend | 统一字段 | static | P3-AC2 | Node |
| T13 | Frontend | 行上下文 | static/type | P3-AC3 | Node/ts:check |
| T14 | Frontend | 校验刷新 | static/type | P3-AC4 | Node/ts:check |
| T15 | Frontend | 无样本展示 | static | P3-AC5 | Node |
| T16 | Frontend | 响应式与选择器 | static | P3-AC6 | Node/source review |
| T17 | Regression | 自动门禁 | regression | P4-AC1 | verification-report.md |
| T18 | E2E asset | 脚本语法 | static | P4-AC2 | node --check |
| T19 | Manual handoff | 手动验收范围 | documentation | P4-AC3 | verification-report.md |
| T20 | Governance | 范围变更风险 | governance | P4-AC4 | docs/changes |

## Evaluator Independence

- Mode: full-context
- Validation surface: code-only
- Required tools: python, maven, node, pnpm
- First-pass readable artifacts: prd.md, test-plan.md, execution-log.md, task-state.json
- Withheld artifacts:
- Real environment expectation: 合并前不要求真实运行态；用户在主代码手动验证真实生产组长和一线路径。
- Evidence requirement: 通过的自动化用例必须引用命令、日志或任务报告证据；真实 E2E 被用户移出合并前门禁，不要求截图或 trace。
- First verdict: tester 可在 full-context 下审阅全部任务证据。
- Role boundary: tester 或主 Agent 不得把未运行的真实 E2E 写成通过，不得用 API-only 或 mock 替代用户手动验收。
- Escalation rule: 保留自动化门禁失败时必须停止；已取消的真实 E2E 前置缺失只记录为用户手动验收前置，不阻塞合并。

## Pass / Fail Criteria

Pass when:

- T1-T20 按本轮更新后的门禁全部通过，P1-AC1 至 P4-AC4 每项至少有一个测试或治理证据。
- 数据库迁移在历史 `route_process_id` 或 `default_value` 为空时明确失败，在合法场景完成两列非空约束和新唯一索引。
- 后端统计严格满足 30 天、正式提交、数值、完整上下文和无样本 null/0 口径。
- 前端只有一个统一入口，参数维护与错误展示由静态合同和类型检查覆盖。
- 真实 E2E 取消原因、用户手动验收责任和未运行边界写入变更记录、测试报告、验证报告和执行日志。
- 无 fallback、默认平均值、猜测回填、双写、吞异常或未解释的保留门禁失败。

Fail when:

- 任一保留 AC 未覆盖、测试失败、跳过或证据缺失。
- 历史空 `route_process_id` 或空目标值被自动回填，或迁移继续执行。
- 参数区间非法仍写入、同上下文新增重复规则或空 routeProcess 仍被运行态接受。
- 平均值包含窗口外、非正式、非数值或其它上下文数据，或无样本返回非 null 平均值。
- 未运行真实 E2E 却被记录成已通过，或用 API-only、mock、静态合同、旧截图或本地数组假回显替代用户手动验收。
- 用户范围变更未写入任务文档和变更记录。

## Regression Scope

- 现有损耗原因新增、修改、删除和授权范围。
- 生产组长工作台相邻 Tab 静态合同。
- 前线运行态参数下发和空 `routeProcessId` 拒绝。
- MES 工序池生产组长静态合同。
- release migration policy gate。

## Manual Verification Handoff

用户在合并后手动验证时建议覆盖：

- 打开生产组长工作台，确认只有一个“工序配置”入口。
- 在某个授权路线工序行维护损耗原因并刷新回显。
- 为该路线工序选择当前组长可维护设备并刷新回显。
- 新增参数编码、名称、单位、值类型、下限、目标值、上限并刷新回显。
- 用相同参数编码更新目标值，确认不是新增第二条有效规则。
- 输入非法区间，确认页面或后端给出可见错误且不写入。
- 通过一线正式生产提交产生设备参数数值，返回统一表确认 30 天平均值和样本数。
- 确认另一个无样本参数显示 `actualAverage=null` / 样本数 `0` 的语义。

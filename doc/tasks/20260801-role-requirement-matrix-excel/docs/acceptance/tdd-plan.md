# 岗位需求矩阵严格 TDD 计划

## Purpose and Scope

本文件规定后续实现必须以一个 `AC-*` 或可独立验收的最小行为切片为交付单位，严格执行 BDD 确认、RED、最小 GREEN、REFACTOR、REGRESSION 和真实 E2E。禁止先完成生产代码后补测试。

## Evidence Reviewed

- `prd.md` 的业务规则和 16 个 BDD 场景。
- `development-plan.md` 的 M0-M6、实施区域和验证命令。
- `test-plan.md` 的 62 项验收测试矩阵。
- 当前后端 JUnit、前端静态合同、类型检查和 Playwright 真实路径基线。

## TDD Sequence

1. 选择一个 `AC-*`，确认对应 Excel 行、BDD、正向行为和失败/边界行为。
2. 明确测试层级：schema/contract、service、API、frontend、E2E、concurrency、security、migration、performance。
3. 先创建或扩展可被测试运行器发现的测试类/脚本，禁止修改生产代码。
4. 执行 RED，确认失败来自目标业务断言，而不是编译失败、No tests、脚本缺失或环境缺失。
5. 只实现令当前 RED 通过的最小正式方案，不加入兼容双读、默认值或占位成功。
6. 执行目标 GREEN，并确认 tests run 大于 `0`、failures/errors 为 `0`。
7. 在测试保护下执行 REFACTOR，清除重复、死分支、隐式 fallback 和跨层泄漏。
8. 执行相邻 REGRESSION、权限、租户、并发和数据快照检查。
9. 对用户可见行为执行真实 Playwright E2E，并通过 API 做最终只读核验。
10. 将 BDD、RED、GREEN、REFACTOR、REGRESSION、E2E 和证据 ID 写入实施任务日志，再允许该 AC 完成。

状态只能按以下顺序推进：

`PLANNED -> BDD_APPROVED -> TEST_ADDED -> RED_VALID -> IMPLEMENTING -> GREEN -> REFACTORED -> REGRESSION_PASS -> E2E_PASS -> ACCEPTED`

任何阶段失败都回到当前 AC，不得跳到后续 AC 或把 milestone 标记完成。

## RED Commands

- M1：权威活跃订单、schema、迁移和跨角色查询测试。
- M2：工序事实、修订、系数分配、完成和批记录聚合测试。
- M3：QA 规程、PQC 任务、逐件提交、签名和复核测试。
- M4：调拨覆盖、开工检查、过程检验、异常、完整性和放行测试。
- M5：班组配置、范围、日结、只读、审计和历史快照测试。
- M6：迁移、并发、性能、真实 E2E 和全量回归。

精确 PowerShell 命令和计划新增测试类见 `test-plan.md` 的 RED/GREEN 章节。每条命令必须单独执行或逐条检查 `$LASTEXITCODE`。

## Expected Failures

- M1 RED：PQC 仍读取旧活跃来源、重复身份或迁移缺路线版本未阻塞。
- M2 RED：报工仍强制订单上下文、生产系数未应用、代表事件丢数或并发重复回填。
- M3 RED：固定项目/类型/数量仍存在、PQC 依赖生产事件、规程可原地修改或自我确认未阻塞。
- M4 RED：多调拨覆盖不完整、未确认数据被汇集、放行仍只有占位来源或缺项可放行。
- M5 RED：独立创建设备、范围不完整、日结漏项、历史快照被新配置改写。
- M6 RED：迁移冲突未阻塞、并发重复终态、权限越界、N+1 或真实路径缺入口。

## GREEN Commands

- 重跑当前 RED 的同一命令，不得换成更弱或更宽的测试掩盖失败。
- 后端 GREEN 后运行对应模块相邻 JUnit。
- 前端静态合同 GREEN 后运行 `pnpm --dir IntRuoyiFronted ts:check`。
- milestone 完成前运行 `test-plan.md` 中该 milestone 的完整回归集合。
- M6 必须运行真实 `e2e:role-requirement-matrix:real:check` 和 `e2e:role-requirement-matrix:real`。

## Refactor Checks

- 活跃订单只能有一个权威查询/命令入口，不保留双读。
- 数量、规程、签名、批记录和异常来源不得在 Controller、页面或 SQL 中重复实现业务规则。
- 删除固定 PQC 项目、默认 `PATROL`、数量 `30`、损耗 `1` 和默认合格。
- 删除代表事件、默认系数 `1`、默认 `MAIN` 和 `formBindings` 替代正式批记录的分支。
- 一对多读模型先聚合再分页，避免重复行和总数漂移。
- 每次 refactor 后必须重跑当前 GREEN 和相邻 REGRESSION。

## Evidence Log Template

```text
BDD: <AC-ID> <scenario> -> Given ... / When ... / Then ...
TEST_ADDED: <test class or script> -> discovered, tests run > 0
RED: <command> -> FAIL, <expected business reason>
GREEN: <same command> -> PASS
REFACTOR: <changed design> -> no fallback/duplicate rule introduced
REGRESSION: <command> -> PASS
E2E: <script/path> -> PASS, <visible assertion and read-only API evidence>
ACCEPTED: <AC-ID> -> evidence paths/IDs
```

## Test Blockers

- 测试运行器找不到测试、依赖未安装或环境未启动时停止，不允许把前置错误当 RED。
- 正式 ERP、QA、异常、库存或批记录来源未确认时停止对应 AC，不允许 mock success。
- 真实账号、签名、浏览器或测试数据缺失时 E2E 标记 BLOCKED，不允许 API-only 替代。
- 相邻回归失败、并发失败、权限越界或清理失败时 AC 保持未完成。

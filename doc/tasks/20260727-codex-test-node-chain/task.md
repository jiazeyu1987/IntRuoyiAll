# 测试管理串行节点串能力

## Task Goal

为 `系统管理 > 测试管理` 增加正式的串行节点串能力，使测试项可以归属到不同节点串、按节点串单独筛选查看，并在执行时按串内序号严格串行；前置节点失败、阻塞或超时后，后续节点不得继续执行。

## Milestones

- [x] 建立任务记录并确认现有顺序执行边界。
- [x] 增加节点串数据库字段和迁移契约。
- [x] 增加后端节点串校验、筛选、排序和失败停止控制。
- [x] 增加前端节点串筛选、展示和编辑能力。
- [ ] 在本机测试租户为现有 14 个节点分配 3 条可见节点串，并保留精确回滚方式。
- [ ] 补齐 RED/GREEN/REGRESSION 验证与收尾证据（后端单元与迁移契约已通过；真实运行态和 Playwright 验证待 supervisor 完成）。

## Expected Verification

- 测试项支持 `节点串名称` 和 `串内序号`。
- 测试管理页可以选择一个节点串单独查看，并能看到不同节点串选项。
- 同一节点串内序号不得重复，节点串测试项只能使用顺序执行且不允许并行。
- 选择同一节点串执行时，后端按串内序号创建执行项，不依赖前端勾选顺序。
- 串行执行只允许领取当前最前面的未完成节点。
- 任一节点失败、阻塞或超时后，其余待执行节点标记为阻塞并说明前置节点未通过。
- 非节点串测试项保留现有顺序执行和并行执行能力。
- 本机测试租户可见 `工艺路线节点闭环`、`批记录节点闭环`、`智能排产节点闭环` 三条节点串，节点数分别为 4、6、4。

## Current Status

in_progress

## 经验门禁

### 测试管理 schema 迁移门禁

- Trigger: 修改 `system_codex_test_case`、测试项分页或测试管理页面字段。
- Preflight check: 同步新增正式迁移、H2 测试表结构和迁移契约测试，不允许代码先引用数据库中不存在的字段。
- Blocker: 迁移文件、迁移依赖、H2 字段或契约测试任一缺失时停止进入运行态验证。
- Verification: `script/tests/test_codex_test_node_chain_migration.py` 与目标 JUnit 均通过。
- Forbidden action: 禁止用前端隐藏字段、后端默认值、吞 SQL 异常或 mock 成功绕过 schema 缺失。
- Evidence: `docs/database-rules.md#测试管理-schema-迁移门禁`。

### 前端静态契约隔离门禁

- Trigger: 为节点串筛选、列展示和表单约束增加前端 RED/GREEN 契约。
- Preflight check: 使用任务专用最小静态契约，不修改无关大契约来迁就历史失败。
- Blocker: 专用契约不能稳定先 RED 后 GREEN，或失败点无法证明属于当前需求时停止。
- Verification: `tests/e2e/system-codex-test-node-chain-static.spec.js` RED/GREEN 结果写入执行日志。
- Forbidden action: 禁止把无关 `ts:check` 或大契约失败当作本任务通过证据。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

### Codex Runner 严格节点串门禁

- Trigger: 修改测试项领取、串行执行或前置失败停止逻辑。
- Preflight check: 单元测试必须覆盖容量大于 1 时同一串只领取首节点，以及前置失败、阻塞或超时后后续节点和目标项均被阻塞。
- Blocker: Runner 仍可领取前置未通过节点、失败后遗留 `PENDING` 后续节点，或依赖 Runner 并发数固定为 1 时停止。
- Verification: `CodexTestRunnerServiceImplTest` 和 `CodexTestExecutionServiceImplTest` 目标用例通过。
- Forbidden action: 禁止把 Runner 单并发、前端勾选顺序或人工停止当作正式串行能力。
- Evidence: `docs/e2e-rules.md#codex-runner-自动测试门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，新增正式数据字段和后端执行约束，不依赖 Runner 并发数为 1 的偶然行为。
- `是否存在临时补丁或绕过`：否。

## Local Data Scope

- 只修改本机 Docker MySQL `ruoyi-vue-pro`、`tenant_id=1`、`deleted=0` 的 14 个既有测试项。
- 仅写入 `node_chain_name` 和 `node_chain_sort`，不修改测试项名称、测试方法、测试数据、检查点、启停状态或业务模块数据。
- 执行前必须确认 14 个目标项的节点串字段均为空；执行后必须精确得到 3 条节点串，数量为 4、6、4。
- 回滚方式：按同一批精确测试项名称将 `node_chain_name` 和 `node_chain_sort` 恢复为 `NULL`，并复核影响行数为 14。

## Independent Follow-up Verification Scope

- 租户：`tenant_id=1`。
- 路径：真实浏览器进入 `系统管理 > 测试管理`。
- 断言：`测试管理`、`测试项`、`Runner 状态` 同时可见。
- 数据边界：仅查看，不创建、修改、执行或删除测试项，不修改任何业务数据。

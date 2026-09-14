# 批记录测试“订单分配”Tab 测试计划

- Task ID: `20260809-batch-record-test-order-allocation-tab`
- Created: `2026-08-09T15:00:22`
- Workspace: `E:\IntRuoyi`
- User Request: `在批记录测试页面新增订单分配 Tab，将确认的工序报工池、FIFO、手动调整、放行锁定、列表历史和并发审计规则整理成可执行验证的测试段落`

## Purpose and Scope

验证“批记录测试”页面新增的“订单分配”内部 Tab、八段结构化测试定义、标准列表能力和既有 CODE_READONLY 测试入口。验证对象是测试定义 UI 是否准确、完整、可执行；不验证或模拟订单分配生产功能已经实现。

## Evidence Reviewed

- `prd.md` 中的 `P1-AC1` 至 `P1-AC6`。
- 当前 `BatchRecordTestPage.vue` 的三个内部 Tab、`BatchRecordTestListKey`、列表元数据、固定测试行、描述持久化和 CODE_READONLY 执行实现。
- 现有 `edhr-batch-record-test-tab-static.spec.cjs` 与 `edhr-batch-record-test-description-wrap-static.spec.cjs`。
- 任务级 E2E、真实菜单和 fail-fast 要求。

## BDD Scenarios

### BDD-1：通过真实页面阅读订单分配测试合同

- Given：测试人员已登录有“批记录测试”菜单权限的测试租户，真实前后端运行正常。
- When：从菜单进入“批记录测试”，切换到“订单分配”内部 Tab。
- Then：页面显示独立标准列表及八行测试定义，每行完整展示任务和可换行描述，并有“测试”操作入口。

### BDD-2：测试段落完整表达已确认业务规则

- Given：订单分配生产功能尚未在本任务实现，但后续开发需要稳定验收合同。
- When：测试人员逐项阅读八行任务及描述。
- Then：文字可分别验证工序共享报工池且无当前订单、固定列表顺序 FIFO、超额保留、不足部分满足、两种手工调整、放行锁定和绿色状态、列表/历史、质量数量、审计、并发和工单状态变化，且没有含混的“数量不足整次失败”或“所有数量必须一次分完”口径。

### BDD-3：订单分配测试项沿用正式执行链路

- Given：测试人员已选择测试租户且拥有 `system:codex-test:execute` 权限。
- When：点击任一订单分配测试行的“测试”按钮。
- Then：系统按该行唯一 `caseName` 和 `testScope` 构造含完整描述检查点的 CODE_READONLY 定义，并通过既有原子执行入口启动，不从浏览器裸调 CLI、不返回 mock 成功。

### BDD-4：缺少真实前置时失败

- Given：真实菜单、账号权限、测试租户、服务运行态或结构化持久化描述缺失。
- When：尝试加载或执行订单分配测试定义。
- Then：页面或验证报告明确失败原因，不能静默回退到默认成功、mock 页面或 API-only 路径。

## Test Scope

- In scope：新内部 Tab、独立列表状态和锚点、八行固定定义、标准列表交互、描述换行、测试 payload 接线、静态合同、类型检查及真实浏览器可见性。
- Out of scope：FIFO 和手工分配生产执行、放行/冲销、生产数据库、业务 API、菜单迁移、订单分配写数据 E2E。

## Environment

- OS：Windows，PowerShell；命令不得使用 `&&`。
- Frontend root：`E:\IntRuoyi\IntRuoyiFronted`。
- Real browser URL：确认 `int_main` 运行后使用 `http://127.0.0.1:8081`。
- Validation surface：`real-browser`。
- Required tools：`playwright`。
- Playwright CLI prerequisite：`npx` 已确认位于 `D:\Programs\npx.ps1`；使用 `npx --yes --package @playwright/cli playwright-cli` 和独立 session。
- 浏览器证据目录：`E:\IntRuoyi\output\playwright\batch-record-test-order-allocation-tab\`，不把截图、trace 或 session 产物放进任务文档目录。

## Accounts and Fixtures

- 使用项目规则允许的真实测试租户和账号，账号必须可见“批记录测试”菜单。
- 页面所需租户列表接口、批记录 Codex 测试项分页接口正常；浏览器检查不创建订单、不执行订单分配、不写订单分配业务数据。
- 若要实际点击“测试”，还需 `system:codex-test:execute` 权限及正式 Codex 执行运行态；本阶段真实浏览器验收只要求按钮存在和可用状态，不创建执行批次。
- 任一前置缺失时 fail fast，并在 `test-report.md` 记录影响范围。

## Strict TDD Sequence

1. RED：先新增并运行订单分配静态合同；在生产页面尚无 `orderAllocation` Tab、列表元数据和八行定义时，测试必须因这些精确缺口失败。
2. GREEN：仅在 `BatchRecordTestPage.vue` 补齐第四个内部 Tab、八行定义和现有列表能力接线，使新静态合同通过。
3. REFACTOR CHECK：运行相邻批记录测试页面合同和描述换行合同，确认三个旧 Tab 及共享持久化/执行契约没有回归。
4. TYPE CHECK：运行 `pnpm ts:check`，确认新增联合类型、ref、computed 和模板绑定类型成立。
5. REAL BROWSER：通过 Playwright 真实菜单进入并检查 UI、文本、操作入口和浏览器错误。

执行者必须在 `execution-log.md` 使用以下精确标记记录证据：

```text
BDD: 订单分配测试定义可阅读且可执行 -> Given/When/Then
RED: node .\tests\e2e\edhr-batch-record-test-order-allocation-static.spec.cjs -> FAIL, 页面尚无 orderAllocation Tab、稳定列表合同和八段定义
GREEN: node .\tests\e2e\edhr-batch-record-test-order-allocation-static.spec.cjs -> PASS
```

## Commands

从前端根目录分别运行，禁止把命令串成 `&&`：

```powershell
Set-Location 'E:\IntRuoyi\IntRuoyiFronted'
node .\tests\e2e\edhr-batch-record-test-order-allocation-static.spec.cjs
node .\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs
node .\tests\e2e\edhr-batch-record-test-description-wrap-static.spec.cjs
pnpm ts:check
```

成功信号依次为：新合同输出 `PASS`；两个相邻合同输出各自 `PASS`；`pnpm ts:check` 退出码为 0 且无类型错误。

真实浏览器检查必须先依据项目规则确认 `int_main` 运行态、登录账号和菜单权限，再在独立证据目录运行：

```powershell
Set-Location 'E:\IntRuoyi\output\playwright\batch-record-test-order-allocation-tab'
npx --yes --package @playwright/cli playwright-cli --session br-order-allocation open http://127.0.0.1:8081 --headed
npx --yes --package @playwright/cli playwright-cli --session br-order-allocation snapshot
```

随后必须使用最新 snapshot 中的真实元素引用依次进入“批记录测试”菜单、点击“订单分配”Tab；每次显著页面变化后重新 `snapshot`，最终执行 `screenshot`、`console`，需要时使用 `tracing-start`/`tracing-stop`。不得预写或复用失效的 `e*` 引用。

## Test Cases

### T1: 订单分配静态合同 RED/GREEN

- Covers: P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC5
- Level: static
- Command: `node .\tests\e2e\edhr-batch-record-test-order-allocation-static.spec.cjs`
- Expected: 订单分配分类、八段规则和正式执行接线的全部静态断言通过，并输出订单分配静态合同 `PASS`。
- Expected details:
  - 精确验证 `BatchRecordTestListKey`、`batchRecordTestListMetas` 和 `activeInnerTab` 支持 `orderAllocation`。
  - 验证标签“订单分配”、稳定 `table-key`、稳定 DOM 锚点、独立查询/筛选/分页和标准工具栏操作。
  - 验证八个稳定标题、`批记录测试-订单分配-` 唯一 case 前缀、固定八行数量和 `订单分配：` testScope。
  - 逐项断言八段描述包含 PRD 规定的关键可验证语义，不能只检查模糊关键词。
  - 验证每行仍通过共享 `buildCodeReadonlyCasePayload` 把 `description` 写入 `checkpoint.remark` 并可进入 CODE_READONLY 原子接口。
  - 验证页面未引入后端请求替代品、浏览器裸调 CLI、空 catch 或 mock 成功。
- RED expected reason：实施前缺少第四个 Tab、orderAllocation 类型/元数据、八行定义和列表接线。
- GREEN success：进程退出码 0 并输出订单分配静态合同 `PASS`。

### T2: 相邻三个 Tab 与描述换行回归

- Covers: P1-AC3, P1-AC4, P1-AC5
- Level: static regression
- Command: `node .\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs`; `node .\tests\e2e\edhr-batch-record-test-description-wrap-static.spec.cjs`
- Commands detail:
  - `node .\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs`
  - `node .\tests\e2e\edhr-batch-record-test-description-wrap-static.spec.cjs`
- Expected: 原三个分类、菜单/路由边界、测试租户、CRUD、结构化描述恢复、CODE_READONLY 原子执行和描述换行均保持通过；合同中原先固定“三张列表”的数量断言须按新增第四张列表更新为四张，但原三张语义不得放宽或删除。

### T3: Vue/TypeScript 类型检查

- Covers: P1-AC3, P1-AC4, P1-AC6
- Level: typecheck
- Command: `pnpm ts:check`
- Expected: 退出码 0；`BatchRecordTestListKey`、列表元数据 Record、查询状态、列控制、筛选、分页和 CRUD 分派均无类型缺口。

### T4: 真实浏览器菜单与订单分配 Tab

- Covers: P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC6
- Level: e2e
- Command: 按 `## Commands` 中的 Playwright CLI 命令启动 `br-order-allocation` session，并通过最新 snapshot 元素引用完成真实菜单和 Tab 检查。
- Tool: Playwright CLI
- Steps:
  1. 通过真实登录态从侧栏进入“批记录测试”，不得直接挂载组件或使用 mock 路由。
  2. 确认原三个内部 Tab 仍可见，点击其后的“订单分配”。
  3. 确认列表锚点出现，固定八行任务及完整描述可见；逐行核对标题和关键语义。
  4. 确认测试租户选择、新增按钮和每行“测试/修改/删除”操作存在，当前任务不点击会创建执行批次的“测试”。
  5. 在 1440x900 桌面视口确认长描述自然换行，文本、分页和操作按钮无重叠。
  6. 记录 final snapshot、截图、console 输出和 page error；若有 error 则失败。
- Expected: 所有 UI 断言成立，console 无新增 error，page error 为 0；证据文件位于 `E:\IntRuoyi\output\playwright\batch-record-test-order-allocation-tab\`。

### T5: 范围与测试可执行性审查

- Covers: P1-AC2, P1-AC3, P1-AC5
- Level: review
- Command: 审查本任务变更文件和 T1 合同断言。
- Expected: 产品改动仅限 `BatchRecordTestPage.vue`；测试仅新增/更新批记录测试 UI 静态合同；无订单分配生产服务、API、数据库、菜单或路由改动。八行 `caseName` 唯一，点击路径使用正式 CODE_READONLY payload，不能把固定文字当作生产功能通过证据。

## Coverage Matrix

| Case ID | Area | Scenario | Level | Acceptance IDs | Evidence |
| --- | --- | --- | --- | --- | --- |
| T1 | 页面静态合同 | Tab、列表、八段语义和执行接线 | static | P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC5 | RED/GREEN 输出 |
| T2 | 相邻回归 | 原三个 Tab、描述、持久化和执行合同 | static regression | P1-AC3, P1-AC4, P1-AC5 | 两条 PASS 输出 |
| T3 | Vue/TypeScript | 新分类全链类型契约 | typecheck | P1-AC3, P1-AC4, P1-AC6 | `pnpm ts:check` 退出码 0 |
| T4 | 真实用户路径 | 菜单 -> 批记录测试 -> 订单分配 | real-browser | P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC6 | snapshot、截图、console/page error 记录 |
| T5 | 范围控制 | 仅测试定义 UI，不实现生产功能 | review | P1-AC2, P1-AC3, P1-AC5 | 变更文件清单及合同审查 |

## Evaluator Independence

- Mode: blind-first-pass
- Validation surface: real-browser
- Required tools: playwright
- First-pass readable artifacts: prd.md, test-plan.md
- Withheld artifacts: execution-log.md, task-state.json
- Real environment expectation: 使用真实仓库、真实 `int_main` 运行态、真实登录会话、真实菜单路径、产品改动和静态测试；API 只能作为最终只读支持检查，本任务不得使用 mock 或 API-only 替代浏览器。
- Escalation rule: 独立测试者写出首次结论前不读取 withheld artifacts；需要分析证据差异时再读取。

## Pass / Fail Criteria

- Pass when：T1 至 T5 全部通过，P1-AC1 至 P1-AC6 均有证据，浏览器证据可解析，且没有超出“测试定义 UI”的产品改动。
- Fail when：任一八段规则缺失或含义被弱化；缺少稳定 Tab/列表/执行入口；旧 Tab 回归；类型错误；真实浏览器不可达、出现新增 console/page error 或布局重叠；发现 mock、fallback、API-only 降级或生产订单分配改动。
- Blocked when：真实服务、测试账号/租户、菜单权限、Playwright 或正式 CODE_READONLY 前置缺失。阻塞必须写明缺失项及影响，不得转为静态页面成功。

## Regression Scope

- “生产组长”“一线PQC”“一线生产”三个既有内部 Tab 的标签、顺序和固定行。
- 四个分类共享的 `BatchRecordTestListKey` 分派、查询分页、列控制、筛选、CRUD、持久化恢复和错误处理。
- 测试租户选择、执行权限、CODE_READONLY payload、原子启动和结果轮询。
- 描述列换行、桌面布局、分页及操作列稳定性。

## Reporting Notes

- 所有结果写入 `test-report.md`，必须使用 T1-T5 编号和对应验收 ID。
- T4 至少引用一个任务目录外实际存在的截图或 trace，并记录 URL、浏览器 session、视口、登录角色、console 和 page error 结论。
- 生产功能尚未开发导致某条 CODE_READONLY 业务检查不通过，不属于本 UI 定义任务失败；但测试定义缺失、无法点击或错误返回默认成功属于失败。
- 不得把“八段文字显示成功”表述为“订单分配生产功能测试通过”。

## Test Blockers

- `edhr-batch-record-test-order-allocation-static.spec.cjs` 未创建或不能稳定表达八段语义时，停止实现评审。
- `int_main` 前后端、真实菜单、测试租户/账号或 `npx`/Playwright 缺失时，T4 标记 blocked，P1 不得完成。
- 发现必须修改生产订单分配模块才能让本页面成立时，按范围冲突阻塞并另建功能任务，不在本任务扩展。

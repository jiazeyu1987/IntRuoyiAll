# Codex 测试管理前端设计

## Purpose and Scope

本设计定义 `系统管理 > 测试管理` 的前端页面、路由、组件和状态流。页面用于维护自然语言测试项、任意数量检查点、租户级执行上下文，以及查看 Codex Runner 通过 Playwright 真实页面路径产生的执行结果。

范围包含测试项增删查改、顶层测试租户选择、单项执行、批量顺序执行、批量并行执行、执行记录列表、检查点结果、红叉失败截图和差异说明展示。范围不包含用前端直接运行 Playwright；真实执行由后端任务和外部 Runner 完成，前端只发起任务并轮询或订阅结果。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/system/user/index.vue`：系统管理列表页使用 `UnifiedListTemplate`、`v-hasPermi` 和密集表格。
- `IntRuoyiFronted/src/views/system/role/index.vue`：权限角色管理页面的工具栏、表格、分类和操作按钮模式。
- `IntRuoyiFronted/src/views/system/tenant/index.vue`：租户列表和租户精简接口使用方式。
- `IntRuoyiFronted/src/api/system/role/index.ts`、`permission/index.ts`、`tenant/index.ts`：系统模块 API wrapper 约定。
- `IntRuoyiFronted/package.json`：项目使用 Vue 3、Vite、TypeScript、Element Plus、Pinia、Playwright。
- `D:/ProjectPackage/Int/IntPP/FRONTEND_STYLE.md`：新增管理页应遵循蓝/中性、密集运营控制台风格。
- `docs/e2e-rules.md` 与 `docs/login-access.md`：E2E 必须通过真实前端路径，涉及租户和账号时不得静默切换。

## Pages and Routes

- 路由路径：`/system/codex-test-management`。
- 组件路径：`system/codex-test-management/index`。
- 组件名：`SystemCodexTestManagement`。
- 菜单名称：`测试管理`，挂在系统管理根菜单下。
- 菜单权限：`system:codex-test:query`。
- 按钮权限：`system:codex-test:create`、`system:codex-test:update`、`system:codex-test:delete`、`system:codex-test:execute`、`system:codex-test:cancel`、`system:codex-test:artifact`。
- 页面进入条件：登录用户拥有 `测试管理员` 角色且动态菜单响应包含 `测试管理` 菜单。
- 非授权行为：未授权用户不显示菜单；直接访问路由时页面 API 返回权限错误，前端显示权限失败状态而不是空表格。

## Components

- `CodexTestManagementPage`：页面容器，负责顶层租户选择、视图切换、批量执行和全局状态提示。
- `CodexTestCaseToolbar`：搜索、状态筛选、执行模式筛选、新增、批量删除、批量顺序执行、批量并行执行。
- `CodexTestCaseTable`：展示测试项名称、状态、默认执行方式、并行安全、检查点数量、最近执行结果、最近执行时间和操作。
- `CodexTestCaseForm`：新增/编辑测试项，包含自然语言测试方法、多行测试数据说明、默认执行方式、并行安全开关和检查点编辑器。
- `CheckpointEditor`：支持自由增删排序检查点；每个检查点包含检查点名称、期待结果文本、严重级别和可选备注。
- `RunConfirmDialog`：执行前确认目标租户、执行模式、选中测试项、并行安全约束和 Runner 在线状态。
- `ExecutionRecordDrawer`：展示执行批次、单个测试项执行状态、开始结束时间、Runner 信息、错误摘要。
- `CheckpointResultList`：逐条展示期待结果、实际结果、绿色勾或红色叉、失败原因、截图入口。
- `FailureArtifactPreview`：打开后端临时截图读取接口返回的截图；截图不可用时显示明确过期或缺失原因。

## State and Data Flow

1. 页面加载时请求租户精简列表、测试项分页和最近执行摘要。
2. 用户选择顶层测试租户后，页面将 `targetTenantId` 存入当前页面状态；测试项本身不自动改写租户。
3. 新增或编辑测试项时，前端提交自然语言 `methodText` 和检查点数组；检查点顺序由 `sort` 控制。
4. 用户点击执行时，前端提交 `targetTenantId`、`executionMode` 和 `caseIds`。
5. 后端创建执行批次后返回 `executionId`；前端轮询执行详情或使用后续可扩展的事件通道刷新状态。
6. Runner 回写结果后，前端以检查点粒度渲染：`PASS` 为绿色勾，`FAIL` 为红色叉并显示失败原因和截图入口，`BLOCKED` 显示阻塞标签。
7. 并行执行提交前，前端根据后端返回的测试项属性显示预检摘要；最终约束以后端为准，前端不得自行把并行降级成顺序。

## Error States

- 未选择测试租户：执行按钮禁用，并提示“请选择测试租户”。
- 未选择测试项：批量执行按钮禁用。
- Runner 未在线或执行能力不足：后端返回前置条件失败，前端展示阻塞原因，不创建绿色结果。
- 并行模式包含未标记并行安全的测试项：后端拒绝执行，前端展示具体测试项名称。
- 测试项自然语言为空或检查点为空：表单校验失败，不允许保存。
- 失败截图过期：红叉仍保留，截图预览展示“临时截图已过期”，不改写历史结果。
- 权限不足：按钮通过 `v-hasPermi` 隐藏；接口权限失败时显示错误消息和请求动作名称。
- 后端返回非 0 业务码：显示后端 message，不转成默认成功。

## Accessibility and Responsive Behavior

- 检查点结果不能只依赖颜色；绿色勾配合 `通过` 文本，红色叉配合 `失败` 文本。
- 表格列使用固定宽度与省略号，长自然语言内容放入抽屉或详情弹窗。
- 失败截图预览支持键盘关闭和替代文本，替代文本包含测试项名称、检查点名称和失败时间。
- 小屏幕下工具栏折叠为两行，表格保持横向滚动，不隐藏关键结果列。
- 所有执行按钮在 loading 时显示明确状态，避免重复点击造成重复执行批次。

## Open Questions

- 是否需要在第一版提供 WebSocket/SSE 实时推送，还是使用轮询即可。
- 是否允许测试管理员查看所有租户名称，或只允许查看标记为测试租户的租户。
- 是否需要在测试项表单中提供变量占位符提示，例如 `${workOrderCode}`，以减少长自然语言重复输入。

## Design Blockers

- 实现前必须确认测试租户筛选规则；如果系统没有“测试租户”标识字段，则只能展示当前系统已有租户列表，并在文档中记录数据风险。
- 实现前必须确认 Runner 在线状态接口；没有 Runner 状态时，执行入口必须返回前置条件失败，不能显示已开始。
- 实现前必须确认截图临时目录可由后端读取并通过受权限保护的接口输出；不能直接暴露服务器绝对路径。


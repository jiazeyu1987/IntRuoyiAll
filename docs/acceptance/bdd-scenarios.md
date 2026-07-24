# Codex 测试管理 BDD 场景

## Purpose and Scope

本文档定义系统管理测试管理能力的可观察行为场景。场景覆盖权限可见性、测试项 CRUD、任意检查点、顶层租户选择、Codex Runner 领取任务、Playwright 真实执行、顺序与并行执行、失败截图和失败原因展示。

## Evidence Reviewed

- 用户已确认：系统中点击按钮后让 Codex 调用 Playwright 真实执行。
- 用户已确认：测试方法是自然语言描述，Codex 根据描述执行。
- 用户已确认：检查点数量不固定，用户可以自由新增。
- 用户已确认：失败截图存临时目录。
- 用户已确认：测试租户在页面顶层选择，所有操作在同一租户里进行。
- `docs/e2e-rules.md`：必须走真实前端用户路径。
- `docs/system/backend-api-design.md`、`docs/system/frontend-design.md`、`docs/system/data-model.md`：当前系统设计契约。

## Feature Scenarios

### Scenario: 测试管理员看到测试管理菜单

Given 用户拥有启用的 `测试管理员` 角色  
When 用户登录后台并展开 `系统管理`  
Then 菜单中显示 `测试管理`  
And 页面路由为 `/system/codex-test-management`  
And 页面可以加载测试项列表

### Scenario: admin 被赋予测试管理员角色

Given tenant 1 存在启用的 `admin` 用户  
And 系统存在启用的 `测试管理员` 角色  
When 权限初始化迁移执行完成  
Then `admin` 用户拥有 `测试管理员` 角色  
And `admin` 的登录权限响应包含 `system:codex-test:query`  
And `admin` 可以看到测试管理菜单

### Scenario: 非测试管理员看不到测试管理

Given 用户没有 `测试管理员` 角色  
When 用户登录后台并展开 `系统管理`  
Then 菜单中不显示 `测试管理`  
And 用户直接访问测试管理接口时收到权限失败

### Scenario: 新增自然语言测试项和任意检查点

Given 测试管理员打开测试管理页面  
When 用户填写测试项名称、自然语言测试方法、手写测试数据  
And 用户新增 4 个检查点并填写期待结果  
And 用户点击保存  
Then 系统保存测试项  
And 测试项详情按用户排序展示 4 个检查点  
And 自然语言和工单号文本保持用户原文

### Scenario: 编辑测试项后不改写历史执行快照

Given 测试项已经执行过一次  
When 用户把工单号从 `881MO093613,881MO093615` 改为其他值并保存  
Then 新的测试项详情展示新工单号  
And 历史执行记录仍展示执行时的旧工单号快照

### Scenario: 顶层租户选择控制本次执行上下文

Given 测试管理员选择测试租户 A  
And 勾选多个测试项  
When 用户点击顺序执行  
Then 后端创建一个执行批次  
And 执行批次的 `targetTenantId` 是租户 A  
And Runner 在租户 A 的登录上下文中执行所有测试项

### Scenario: 顺序执行多个测试项

Given 测试管理员选择测试租户  
And 勾选测试项 1 和测试项 2  
When 用户选择顺序执行  
Then Runner 先执行测试项 1  
And 测试项 1 完成后再执行测试项 2  
And 执行详情按顺序展示每个测试项结果

### Scenario: 并行执行只允许并行安全测试项

Given 测试管理员勾选多个测试项  
And 所有测试项都标记为并行安全  
When 用户选择并行执行  
Then 后端创建并行执行批次  
And Runner 可以按能力并行领取多个测试项  
And 每个测试项仍独立回写检查点结果

### Scenario: 检查点通过显示绿色勾

Given Runner 真实执行 Playwright 页面路径  
And 检查点实际结果与期待结果一致  
When Runner 回写检查点结果  
Then 前端在该检查点显示绿色勾  
And 显示 `通过` 文本  
And 不要求失败截图

### Scenario: 检查点失败显示红色叉、截图和原因

Given Runner 真实执行 Playwright 页面路径  
And 检查点实际结果与期待结果不同  
When Runner 回写检查点失败结果、失败描述和截图 artifact  
Then 前端在该检查点显示红色叉  
And 显示 `失败` 文本  
And 用户可以打开临时截图  
And 用户可以看到为什么不同的文字说明

### Scenario: 排产工单手动重排样例测试项

Given 测试项方法描述为在排产工单页选择来源生产工单号 `881MO093613,881MO093615` 的两个排产工单并手动重排  
And 检查点包含重排成功、仅两个工单产品编号变橙色、最近一次成功排产时间更新、甘特图有且仅有这两个工单  
When 用户在测试租户中执行该测试项  
Then Codex Runner 使用 Playwright 打开真实页面并完成手动重排流程  
And 每个检查点按实际页面结果分别显示通过或失败  
And 任一失败检查点包含截图和差异说明

## Failure Scenarios

### Scenario: Runner 离线时拒绝开始执行

Given 没有在线 Runner  
When 测试管理员点击执行  
Then 后端返回 Runner 离线错误  
And 前端显示阻塞原因  
And 系统不生成绿色通过结果

### Scenario: 目标租户不可用时拒绝执行

Given 测试管理员选择的租户不存在、禁用或过期  
When 用户点击执行  
Then 后端返回目标租户不可用错误  
And Runner 不领取任务

### Scenario: 并行执行包含不安全测试项时拒绝执行

Given 测试管理员选择并行执行  
And 至少一个选中测试项未标记为并行安全  
When 用户确认执行  
Then 后端拒绝创建并行执行批次  
And 返回不安全测试项名称  
And 系统不自动改为顺序执行

### Scenario: Runner 回写结果结构非法

Given Runner 已领取测试项  
When Runner 回写缺少检查点状态或失败原因的结果  
Then 后端拒绝该回写  
And 执行项进入阻塞或失败状态  
And 记录结构非法原因

### Scenario: 失败截图过期

Given 某检查点历史结果为失败  
And 关联临时截图已超过保留期并被清理  
When 用户打开失败截图  
Then 前端显示截图已过期  
And 检查点仍保持失败状态和文字原因

## Boundary Scenarios

### Scenario: 检查点数量为一

Given 用户只配置一个检查点  
When 用户保存测试项并执行  
Then 系统允许保存和执行  
And 结果按单个检查点展示

### Scenario: 检查点数量很多

Given 用户配置多个检查点  
When 用户保存测试项  
Then 系统按 sort 保持顺序  
And 执行详情可滚动查看全部检查点结果

### Scenario: 禁用测试项不能执行

Given 测试项状态为禁用  
When 用户勾选并点击执行  
Then 后端拒绝执行该测试项  
And 前端展示禁用原因

### Scenario: 删除运行中的测试项被阻止

Given 测试项存在运行中的执行记录  
When 用户点击删除  
Then 后端拒绝删除  
And 前端提示该测试项正在执行

## Open Questions

- 是否只允许选择带测试标识的租户，还是允许测试管理员选择全部启用租户。
- 是否需要在第一版支持测试项配置包导入导出。
- 是否需要将 Runner 在线状态做成页面上的持续状态条。

## Test Blockers

- Runner 未安装 Codex CLI、Playwright 或浏览器时，真实执行验收阻塞。
- 本机或授权测试环境前端入口不可访问时，真实执行验收阻塞。
- 测试租户账号凭据映射缺失时，真实执行验收阻塞。
- 目标业务数据由用户手写；若工单号在目标租户不存在，对应测试项应失败或阻塞并记录原因。


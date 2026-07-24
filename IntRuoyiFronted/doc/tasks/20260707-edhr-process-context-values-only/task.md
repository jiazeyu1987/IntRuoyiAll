# eDHR 工序栏上下文仅显示值

## 任务目标

按截图要求，工序栏顶部上下文不再显示 `生产工单：`、`批记录号：` 标签，只直接展示当前生产工单号和批记录号，并完整显示不截断。

## 经验门禁

- PowerShell / Windows shell：已读取根仓 `docs/powershell-memory.md`，命令输出显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，保持蓝灰运维台风格和紧凑信息密度。
- 前端复刻：已读取 `replicate-frontend-ui`，只改当前前端页面展示，不改接口、DTO、后端、mock 或数据源。
- 前端特性：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，保留现有 API、权限、路由和状态边界。
- BDD/TDD：先记录 Given/When/Then 和 RED/GREEN 证据；静态测试锁定只显示值与完整展示。
- 禁止 fallback：不新增降级、兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整上下文展示结构和样式，避免标签占位和 ellipsis 截断。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 工序栏上下文只显示值 -> Given 用户打开批次详情页 / When 查看工序栏顶部上下文 / Then 只显示当前生产工单号和批记录号，不显示 `生产工单：` 或 `批记录号：` 标签。

BDD: 工序栏上下文完整显示 -> Given 当前生产工单号较长 / When 页面渲染工序栏顶部上下文 / Then 工单号和批记录号允许换行完整显示，不使用 ellipsis 截断。

## 里程碑

- [x] M1：创建任务文档并记录 BDD、门禁和设计约束。
- [x] M2：新增 RED 静态测试，证明当前仍显示标签并截断。
- [x] M3：改为仅显示值并完整换行展示，不改变业务逻辑。
- [x] M4：运行静态测试和必要语法检查，记录 GREEN 证据。
- [x] M5：收尾清理预览并按范围提交或报告提交阻塞。

## 预期验证

- `node tests/e2e/edhr-process-header-context-values-only-static.spec.js` 先 RED 后 GREEN。
- `node tests/e2e/edhr-process-header-context-static.spec.js` 保持通过。
- `node --check tests/e2e/edhr-process-header-context-values-only-static.spec.js` 通过。

## 完成记录

- 实现：工序栏顶部上下文改为直接显示生产工单号和批记录号，移除 `生产工单：`、`批记录号：` 标签。
- 样式：上下文值取消 ellipsis 截断，改为允许换行完整显示。
- 回归：同步确认待处理工序详情仍位于右侧详情栏，左侧工序列表不承载详情或操作控件。

## 当前状态

completed

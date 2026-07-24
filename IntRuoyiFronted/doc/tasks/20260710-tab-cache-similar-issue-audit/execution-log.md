# Execution Log

## Audit Criteria

- CONFIRMED: 标签页可保留，但组件监听全局 `route.query`，离开和返回同一路由会重新加载相同数据。
- HIGH RISK: 详情路由设置 `noCache: true`，组件首次挂载即加载接口，标签返回必然重新创建并加载。
- EXPECTED: 页面明确设计为不缓存，或参数实际变化时重新加载。

## Evidence

- 扫描 `src/router/modules/*.ts` 共解析 84 个页面路由，其中详情/编辑/表单类 31 个。
- HIGH RISK：22 个详情/编辑类路由设置 `noCache: true`，且组件挂载或路由参数变化时加载数据；这些页面切走再返回会重新创建并重新请求。
- CACHED WATCHER RISK：除已修复的批次详情外，未发现 `noCache: false` 且存在未受限 `route.query` watcher 的详情页。
- eDHR 已确认同类页面 8 个：
  - `MesProFeedbackEdhrExecutionForm`：`noCache: true`，同时存在 `onMounted(loadExecution)` 与未受限的 `route.query.id` watcher。
  - `MesProFeedbackEdhrDomainTraceDetail`：`noCache: true`，同时存在 `onMounted(loadDetail)` 与未受限的 `route.query.executionId` watcher。
  - `MesProFeedbackEdhrFieldAuditDetail`：`noCache: true`，同时存在 `onMounted(loadDetail)` 与未受限的多参数 watcher。
  - `MesProFeedbackEdhrOperationAudit`：`noCache: true`，挂载时按上下文加载列表。
  - `MesProFeedbackEdhrApprovalDetail`：`noCache: true`，挂载时加载详情。
  - `MesProEdhrBatchExecutionReview`：复用批次详情组件，但路由仍为 `noCache: true`。
  - `MesProEdhrBatchHistory`：`noCache: true`，挂载时加载批次历史。
  - `MesProFeedbackEdhrDhrTemplate`：`noCache: true`，挂载时加载目录与模板列表。
- 其他模块另有 14 个同类高风险详情/编辑页，主要分布在 BPM、DCC、商城、IoT、代码生成和排产编辑；其中编辑器页面可能是有意禁用缓存，需要逐页确认业务预期后再改。
- 本次仅审计，不修改其他页面。

## Blockers

- 无。

## Conclusion

- 用户描述的现象不只存在于批次详情。
- eDHR 范围内优先级最高的是执行表单、主数据追溯详情、字段审计详情；即使只打开缓存，它们的未受限 watcher 仍会在标签切换时重复加载。
- 批次复盘、审批详情、操作审计、批次历史、DHR 模板主要由 `noCache: true` 导致返回时重载。

## Closeout

- GREEN: task-closeout-preview -> PASS，仅计划删除一次性 `audit-tab-cache.cjs`。
- GREEN: task-closeout-apply -> PASS，核心任务记录和验证报告保留。
- GREEN: final-status -> completed。

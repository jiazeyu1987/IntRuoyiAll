# Frontend Feature Evidence

## Feature Goal

将 DCC 受控文件详情页中的审批路线快照、版本历史、分发状态三块列表改为标准列表模板，统一显示字段、列宽持久化、分页和工具栏位置。

## Non-goals

- 不改签核追溯和签名留痕列表。
- 不改详情接口、后端字段、分发签收/回收操作语义。
- 不新增 mock、fallback、降级或错误吞没逻辑。

## Requirements and Acceptance IDs

- REQ-1: 三块目标列表必须使用 `UnifiedListTemplate`。
- REQ-2: 三块目标列表必须使用稳定 table key 和 `useUserTableColumns`。
- REQ-3: 三块目标列表必须保留原列内容、行级操作和现有可读摘要。

## UI Entry Points

- 路由页面：DCC 受控文件详情页。
- 组件文件：`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`。

## API Contracts and Data States

- 审批路线快照数据仍来自 `fileDetail.routeSnapshots`。
- 版本历史数据仍来自 `fileDetail.versionHistory`。
- 分发状态数据仍来自 `fileDetail.distributionStatuses`。
- 本次仅新增前端显示摘要字段和本地分页切片，不改变接口请求或响应结构。

## BDD Scenarios

- BDD: 审批路线快照使用标准列表模板 -> Given 用户打开 DCC 受控文件详情；When 查看审批路线快照；Then 列表由 `UnifiedListTemplate` 承载，显示字段按钮位于标准列表工具栏，列配置按稳定 table key 保存。
- BDD: 版本历史使用标准列表模板 -> Given 用户打开 DCC 受控文件详情；When 查看版本历史；Then 版本历史表格保留原有列、查看详情操作和后继版本摘要，同时接入标准列表模板。
- BDD: 分发状态使用标准列表模板 -> Given 用户打开 DCC 受控文件详情；When 查看分发状态；Then 分发状态表格保留导出/打印回执与行级签收/回收操作，同时接入标准列表模板。

## RED: Command

- `node tests/e2e/dcc-detail-trace-lists-standard-template-static.spec.js` -> FAIL, expected reason: missing `UnifiedListTemplate` import.

## GREEN: Commands

- `node tests/e2e/dcc-detail-trace-lists-standard-template-static.spec.js` -> PASS.
- `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS.
- `node tests/e2e/dcc-detail-version-successor-summary-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.

## Verification: Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- 标准列表空态文案沿用原三块列表。
- 分发状态导出/打印按钮禁用条件沿用 `distributionReceiptRows.length`。
- 行级签收、加签、纸质发放、回收按钮沿用原 `v-if` 条件和 loading 状态。
- 未变更权限指令、后端错误处理或接口调用路径。

## Blockers and Follow-up Skills

- 当前功能实现无 blocker。
- 共享分支存在非本任务脏文件，提交前需要显式路径或补丁级暂存。

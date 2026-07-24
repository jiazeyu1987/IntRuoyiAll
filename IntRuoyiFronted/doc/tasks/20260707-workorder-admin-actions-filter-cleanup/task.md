# 生产工单列表管理员操作与筛选项清理

## 任务目标

按截图要求调整 MES 生产工单列表：红框内操作仅管理员可见；蓝框内筛选项从页面删除；查询、重置及管理员操作按钮移动到筛选首行右侧空白区域；生产工单每页 10 条时表格底部不保留大块空白区域。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，中文读写和命令输出显式 UTF-8。
- 前端特性交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，采用静态契约测试先行。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整生产工单页面可见性与筛选项结构。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 红框操作仅管理员可见 -> Given 用户打开生产工单列表 / When 用户不是管理员 / Then 导出、增量同步、全部展开、全部折叠、创建 ERP 测试单不可见；管理员仍可见这些操作。

BDD: 蓝框筛选项删除 -> Given 用户打开生产工单列表 / When 查看查询区 / Then 工单名称、客户、产品、工单类型筛选项不再显示。

BDD: 查询按钮移动到首行右侧 -> Given 用户打开生产工单列表 / When 查看查询区 / Then 查询、重置及管理员操作按钮显示在筛选首行右侧空白区域，不再单独占用下一行左侧。

BDD: 表格少量行不保留底部空白 -> Given 用户打开生产工单列表且分页为 10 条 / When 当前页只显示 10 条数据 / Then 表格高度随内容自然收起，分页直接跟随表格内容，不出现大块空白。

## 里程碑

- [x] M1：创建任务记录并确认页面当前实现。
- [x] M2：新增静态契约测试复现当前不符合要求。
- [x] M3：实现管理员可见性与筛选项删除。
- [x] M4：运行验证、更新证据并提交。
- [x] M5：调整查询按钮组到筛选首行右侧并验证。
- [x] M6：移除固定表格高度导致的底部空白并验证。

## 预期验证

- `node tests/e2e/workorder-admin-actions-filter-cleanup-static.spec.js`
- `node tests/e2e/workorder-key-columns-static.spec.js`
- `node tests/e2e/workorder-product-candidate-filters-static.spec.js`
- `pnpm.cmd ts:check`

## 当前状态

blocked：生产工单查询按钮右移与表格底部空白修复已完成，工单相关静态回归已通过；提交前全量类型检查仍阻塞于无关 eDHR 脏改 `src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue` 的 `closedAt` 类型错误。

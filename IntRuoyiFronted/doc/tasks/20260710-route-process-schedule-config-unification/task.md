# 排产员工作台按路线工序统一展示

## 任务目标

- 工作台按 `routeVersionId + routeProcessId` 分行。
- 增加工艺路线编码和名称列。
- 同一路线工序汇总所有产品订单，不同路线独立修改。
- 前端提交新的路线工序设置契约，移除夜班混合状态。

## 当前状态

completed

## 上一任务检查

- 当前 worktree 基于前端 `int_main@fb4d755eb` 创建。
- 主工作区存在其他 Agent 未提交文件，本任务在隔离 worktree 内实施。

## 经验门禁

- 遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 和现有 `UnifiedListTemplate`。
- 不改变现有快速筛选、显示字段、列宽持久化、分页和订单跳转。
- 不隐藏后端错误，不增加 mock 或默认成功。
- 真实 E2E 使用测试租户真实页面路径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- 同一路线工序多个产品只显示一行。
- 相同基础工序属于不同路线时显示多行。
- 夜班或开排日期只更新选中路线工序。
- 班次产能不随订单数成倍增加。

## 里程碑

1. `COMPLETED` 建立任务台账和前端契约测试。
2. `COMPLETED` 完成 API 类型和工作台展示修改。
3. `COMPLETED` 静态契约和构建通过；测试租户真实 E2E 已验证 44 条路线工序、多产品合并、夜班切换恢复、当前工序编码名称和“球囊扩张导管”不含“全检导丝”；“芋道源码/admin”重试已验证 49 条路线工序，“RX口检测”在“棘突球囊扩张导管”和“球囊扩张导管”中分别显示，且零 MES 写请求。
4. `COMPLETED` 实现提交完成，最新 `int_main` 已合入任务分支；融合后静态契约发现并移除重新引入的夜班混合状态语义，生产构建通过。
5. `COMPLETED` “芋道源码/admin”融合分支只读复验加载 49 条路线工序，跨路线分行、多订单聚合和路线列展示通过，零 MES 写请求。
6. `COMPLETED` 已快进融合到 `int_main`，主工作区静态契约复验通过；收尾预览选中的临时证据和构建产物已清理，任务 worktree 已移除。

## 当前阻塞

- 无。

## 预期验证

- API 类型检查通过。
- 工作台目标测试通过。
- 前端构建通过。
- 测试租户真实 Playwright E2E 通过。
- “芋道源码/admin”最终只读 Playwright E2E 通过且没有 MES 写请求。

## Cleanup Keep

- `doc/tasks/20260710-route-process-schedule-config-unification/task.md`
- `doc/tasks/20260710-route-process-schedule-config-unification/execution-log.md`

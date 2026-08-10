# 排产工单筛选与排序缺陷修复

## Task Goal

修复排产工单真实页面已复现的 3 个交互缺陷：组合筛选删除单个条件清空全部条件并保留旧结果、反向承诺交期被静默清空并恢复全量数据、优先级排序状态和可访问性缺陷；保留现有页面入口、API 契约和只读/写入边界。

## Scope

- 前端范围：`IntRuoyiFronted/src/views/mes/pro/scheduleorder/` 及直接复用的排产工单列表/筛选/表格组件。
- 后端范围：排产工单分页请求 VO 与 Mapper 白名单排序，确保优先级排序请求不是前端假参数。
- 测试范围：任务专用静态合同、目标 TypeScript/前端校验、真实 Playwright 页面回归。
- 非目标：不新增降级或兼容 shim、不改排产写操作、不提交 Git。

## Milestones

- [x] M1：读取 bug/frontend/playwright 技能和项目规则。
- [x] M2：创建任务记录、BDD 场景和证据骨架。
- [x] M3：定位 3 个缺陷根因和目标文件。
- [x] M4：补充失败优先的回归测试并记录 RED。
- [x] M5：实施最小前端/后端修复并记录 GREEN。
- [x] M6：运行目标真实页面回归与收尾验证。

## Expected Verification

- 组合筛选：删除一个已执行条件后，仅移除该条件，保留其他已执行条件，立即重新查询，结果不保留陈旧数据。
- 反向承诺交期：已输入日期范围不得被查询流程静默清空；请求必须携带正式日期筛选参数或页面明确暴露校验失败。
- 优先级排序：点击优先级表头必须同步正式排序请求参数；表头 `aria-sort` 必须与排序方向一致；优先级编辑输入具备可访问名称和最小值校验。
- 验证命令至少包含任务专用 RED/GREEN 静态合同、`node --check` 或等效脚本语法检查、相关前端检查、真实 Playwright 回归。

## BDD Scenarios

- BDD: 组合筛选删除单个条件 -> Given 排产工单已应用多个筛选条件 When 用户删除其中一个条件 Then 页面只移除该条件、保留其他条件并发起新查询。
- BDD: 反向承诺交期筛选 -> Given 用户在反向承诺交期筛选中输入日期范围 When 用户点击查询 Then 日期范围保留并进入排产工单列表请求，不能静默恢复全量列表。
- BDD: 优先级排序与可访问性 -> Given 用户点击排产工单优先级表头 When 排序方向改变 Then 请求携带正式排序参数、表头暴露 `aria-sort`，且优先级输入有可访问名称。

## Applicable Experience Gates

- `docs/frontend-development.md#统一列表复合工具栏布局门禁`：多维筛选条件 Tab、删除条件、正式 query 透传和真实回归。
- `docs/frontend-development.md#前端服务端分页排序链路门禁`：Element Plus sort-change、受控 sortState、sortField/sortOrder 和后端白名单排序。
- `docs/task-closeout-rules.md#任务验证脚本保留门禁`：保留任务复验 wrapper 和真实页面产物。
- `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：先运行 evidence validator 并归档 PASS，再清理临时 evidence 文件。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；先复现/测试锁定正式行为，再修复状态和请求链路。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

- 当前阶段：任务已完成，cleanup preview/apply 已通过。
- 已完成：任务目录、BDD 场景、RED/GREEN 静态合同、前端 TS 检查、后端 MES 编译、真实页面只读回归、evidence validator 和 cleanup。
- 阻塞项：无；`unified-list-template-all-headers-sortable-static.spec.js` 仍存在大量非本任务历史失败，未作为本任务放行门禁。

## Cleanup Keep

- doc/tasks/20260808-schedule-order-filter-sort-fix/verify-schedule-order-filter-sort-fix.cjs
- output/playwright/20260808-schedule-order-filter-sort-fix/result.json
- output/playwright/20260808-schedule-order-filter-sort-fix/schedule-order-final.png

## Cleanup Notes

- 技能 evidence 文件的 validator PASS 和核心结论已归档到 `execution-log.md` 与 `verification-report.md`，cleanup 可按默认规则清理临时 evidence 文件。

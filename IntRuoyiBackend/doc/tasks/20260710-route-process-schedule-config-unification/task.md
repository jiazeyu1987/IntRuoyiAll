# 工艺路线工序排产配置全局统一

## 任务目标

- 将路线排产配置唯一口径统一为 `tenantId + routeVersionId + routeProcessId`。
- 产品 `itemId` 不再参与有效排产配置识别。
- 排产员工作台按路线工序分行，同一路线工序聚合全部产品订单，不同路线互不影响。
- 保留每组 `item_id IS NULL` 的通用配置，产品级配置转为历史软删除记录。

## 当前状态

completed

## 最终验证授权

- 用户明确要求使用“芋道源码/admin”进行最终验证，并在验证通过后融合。
- 本阶段仅允许只读页面验证，不修改“芋道源码”租户的 MES 数据；浏览器网络请求必须确认没有 MES `POST`、`PUT`、`PATCH` 或 `DELETE` 写请求。
- 用户提供的登录口令仅用于本次临时验证，不写入源码、脚本、任务文档或长期记录。

## 上一任务检查

- 当前 worktree 基于后端 `int_main@4b187d7b4e` 创建。
- 已完成的相关上一任务为 `20260710-scheduler-workbench-night-shift-toggle-error`。
- 主工作区存在其他 Agent 的 DCC 未提交改动，本任务不读取、不修改、不提交这些改动。

## 经验门禁

- PowerShell 命令显式使用 UTF-8，禁止 Bash heredoc 和 `&&`。
- 使用前后端同名隔离 worktree，开发、测试、提交、合并和清理均按 `docs/worktree-memory.md` 执行。
- 智能排产统计必须区分路线工序与基础工序，不得按 `processId` 跨路线合并。
- 新增 SQL 必须包含 `release-migration` 元数据、失败前置检查、回滚说明和发布迁移契约测试。
- 数据迁移不得自动选择冲突配置；每组必须恰好存在一条有效通用配置，否则 fail fast。
- 真实 E2E 前必须读取 `docs/login-access.md` 并通过官方登录 preflight。
- 前端表格保持现有统一列表模板和 IntPP 操作台风格，不做无关视觉重构。
- 未获授权不访问或修改正式服、备份服。

## Worktree 配置

- 分支：`codex/20260710-route-process-schedule-config-unification`
- 后端：`D:\ProjectPackage\Int\IntRuoyiWorktrees\20260710-route-process-schedule-config-unification\ruoyi-vue-pro`
- 前端：`D:\ProjectPackage\Int\IntRuoyiWorktrees\20260710-route-process-schedule-config-unification\yudao-ui-admin-vue3`
- 计划后端端口：`48121`
- 计划前端端口：`8091`
- 计划验证数据库：`ruoyi-vue-pro-route-config-unification`
- Redis：本机 Redis，使用隔离 database index
- 文件服务：沿用本机受保护配置，不修改 `infra_file_config.id=28`

## BDD 场景

- `BDD: 同一路线工序跨产品合并 -> Given 多个产品订单使用同一路线版本和路线工序 / When 打开排产员工作台 / Then 只显示一行并汇总订单数、需求和报工。`
- `BDD: 相同基础工序跨路线分行 -> Given 两条路线包含相同 processId / When 打开排产员工作台 / Then 按 routeVersionId + routeProcessId 显示两行。`
- `BDD: 路线工序设置隔离保存 -> Given 两条路线使用相同基础工序 / When 修改其中一条路线工序夜班或开排日期 / Then 只更新目标路线工序的配置和在制订单。`
- `BDD: 班次产能不按订单累加 -> Given 同一路线工序有多个在制订单 / When 查询工作台 / Then 班次产能显示唯一通用配置产能，未完成需求仍按订单汇总。`
- `BDD: 产品级配置全局迁移 -> Given 每组恰好一条通用配置和若干产品配置 / When 执行迁移 / Then 通用配置保留、产品配置软删除、引用重定向且唯一约束生效。`
- `BDD: 配置冲突迁移失败 -> Given 某组缺少通用配置或存在多条通用配置 / When 执行迁移 / Then 在任何数据更新前明确失败。`
- `BDD: 当前路线工序定义优先 -> Given 在制快照仍保存历史基础工序，但当前路线工序已调整为另一基础工序 / When 打开排产员工作台 / Then 展示和筛选使用当前路线工序的编码和名称。`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；同时修正数据模型、唯一约束、后端分组键和前端请求契约。
- `是否存在临时补丁或绕过`：否。
- 有效配置只允许 `item_id IS NULL`；非空 `item_id` 仅保留于软删除历史记录。
- 路线版本继续作为配置边界，不跨版本共享设置。
- 已完成、冻结订单的历史快照不重算。

## 里程碑

1. `COMPLETED` 建立隔离 worktree、任务台账、BDD 与经验门禁。
2. `COMPLETED` 编写后端、迁移和前端失败测试，取得 RED。
3. `COMPLETED` 实现数据库迁移、唯一约束和基线 schema。
4. `COMPLETED` 实现后端路线工序聚合、保存隔离和配置同步。
5. `COMPLETED` 实现前端路线列、稳定行键和新请求契约。
6. `COMPLETED` 完成目标 GREEN、迁移隔离库验证、前后端构建和当前路线工序显示修复。
7. `COMPLETED` 真实 Playwright 已验证测试租户 44 条路线工序、多产品合并、夜班切换恢复、当前工序编码名称和“球囊扩张导管”不含“全检导丝”；“芋道源码/admin”重试加载 49 条路线工序，“RX口检测”同时出现在“棘突球囊扩张导管”和“球囊扩张导管”两条路线中，跨路线分行验证通过，且全程零 MES 写请求。
8. `COMPLETED` 用户在获知完整 MES 基线既存失败后明确要求完成管理员验证并融合；实现提交完成，最新 `int_main` 已合入任务分支并完成冲突归并。
9. `COMPLETED` 融合分支后端目标回归 66 个测试、迁移契约、完整跳过测试打包、前端静态契约和生产构建通过；“芋道源码/admin”再次只读验证 49 条路线工序、跨路线分行和多订单聚合通过，零 MES 写请求。
10. `COMPLETED` 前后端已快进融合到 `int_main`，主工作区目标回归和迁移契约复验通过；收尾预览清单已执行，隔离 worktree 和残留构建目录均已清理。

## 当前阻塞

- 无任务阻塞。

## 残余基线风险

- 完整 MES 测试在本任务基线提交上已存在大量失败，代表性失败包括“排产员工作台写接口权限契约”“工艺路线启用校验”“自动排产夜班窗口契约”“报工超量进度显示”等。本任务分支完整执行结果为 1616 个测试中 23 个失败、40 个错误；选取的代表性失败已在干净基线提交上复现。
- “球囊扩张导管”没有“全检导丝”已确认；该记录属于已删除的历史路线工序残留，迁移后不再进入有效配置和工作台，真实页面断言已通过。

## 预期验证

- 后端 MES 模块目标单测和完整相关回归通过。
- 前端类型检查、目标单测和构建通过。
- 迁移在隔离数据库上执行成功，重复配置计数和悬空引用均为零。
- 迁移在缺少或重复通用配置的测试库中明确失败。
- Playwright 使用 `测试租户/aoteman` 完成真实页面验证。
- Playwright 使用 `芋道源码/admin` 完成最终只读页面验证，确认跨路线分行数据前置和页面展示，并断言没有 MES 写请求。
- 融合后的 `int_main` 再次通过相关验证。

## Cleanup Keep

- `doc/tasks/20260710-route-process-schedule-config-unification/task.md`
- `doc/tasks/20260710-route-process-schedule-config-unification/execution-log.md`

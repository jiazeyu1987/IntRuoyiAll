# 排产员工作台动态重排说明页签

## 任务目标

- 将现有静态“排产逻辑”页签升级为最近一次成功重排的动态说明。
- 用短句、数字卡片和可展开明细展示订单顺序、工序、班次产能、保护任务、物料计算、问题和最终结果。
- 在进入页签或窗口重新获得焦点时刷新；加载失败必须明确提示且不伪装为已刷新。

## 上一任务检查

- 前端上一任务 `doc/tasks/20260710-edhr-process-companion-forms/task.md` 状态为 `completed`，不阻塞本任务。
- 前序排产逻辑页签任务 `doc/tasks/20260710-scheduler-workbench-algorithm-guide-tab/task.md` 状态为 `completed`，本任务在其静态说明基础上扩展。

## 经验门禁

- PowerShell / UTF-8：已读取 `docs/powershell-memory.md`；中文文件显式按 UTF-8 处理，PowerShell 不使用 `&&`。
- 前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；保持紧凑排产操作台风格，不引入营销式卡片或无关重设计。
- 智能排产：已读取 `docs/agent-memory/project-error-prevention.md`；数量必须来自权威接口，不在前端重新计算或提供默认值。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；覆盖加载、空数据、错误、权限和真实用户路径。
- BDD + 严格 TDD：先新增失败的静态/组件契约测试，再实现页面和 API 类型。
- 无 fallback：接口失败时明确展示失败，不把旧数据或模拟数据当成本次结果。
- Worktree：前端工作目录为 `D:\ProjectPackage\Int\IntRuoyiWorktrees\20260710-scheduler-workbench-dynamic-replan-explanation\yudao-ui-admin-vue3`，分支为 `codex/20260710-scheduler-workbench-dynamic-replan-explanation`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；页面只消费后端成功重排快照并按固定七步结构展示。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 用户进入排产逻辑查看最近重排 -> Given 当前租户已有成功重排 / When 用户进入排产逻辑页签 / Then 页面显示重排时间、来源、操作人、原因和七步实际数值。
- BDD: 用户查看完整物料计算 -> Given 本次重排包含充足和短缺物料 / When 用户查看问题步骤 / Then 页面展示每种物料需求、库存、短缺并可展开订单贡献。
- BDD: 用户查看工序和产能 -> Given 本次重排包含多道工序 / When 用户查看拆分工序和计算产能步骤 / Then 页面展示班次、工作站、设备、人员、产能、时长和瓶颈。
- BDD: 页签激活和窗口聚焦刷新 -> Given 页面已打开 / When 用户切入排产逻辑或窗口重新获得焦点 / Then 页面重新获取最新成功重排且不启动定时轮询。
- BDD: 没有成功重排 -> Given 当前租户没有成功重排快照 / When 用户进入页签 / Then 页面显示“暂无已应用的重排记录”且不生成模拟数据。
- BDD: 查询失败明确可见 -> Given 查询接口失败 / When 用户进入页签 / Then 页面显示加载失败且不把旧数据标记为最新。

## 里程碑

1. [完成] 建立任务文档、BDD 和前端证据骨架。
2. [完成] 新增动态说明契约 RED 测试和 API 类型。
3. [完成] 实现七步动态数据、明细表和刷新状态。
4. [完成] 运行静态测试、类型检查和相关回归。
5. [进行中] 完成真实页面验证、提交、融合和收尾。

## 预期验证

- 动态说明静态/组件测试覆盖七步字段、物料展开、产能、空数据、错误和刷新触发。
- 不存在轮询定时器，页签激活和窗口 focus 会请求最新说明。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 通过。
- 测试租户真实重排后，页面显示与权威接口一致的数据；第二次重排后重新聚焦页面可见更新。

## 已完成工作

- 将静态“排产逻辑”升级为最近一次成功重排的动态说明，并展示重排来源、时间、操作人、原因、开始时间和产能模式。
- 七步展示订单检查、最终顺序、工序拆分、实际产能、保护任务、完整物料和问题、最终任务结果。
- 物料按物料汇总并支持展开查看订单贡献；工单产能支持展开查看每道工序的班次、工作站、设备、人员、产能、时长和瓶颈。
- 页签激活和窗口 focus 会重新查询；无数据和加载错误有独立明确状态，不使用轮询。

## 验证结果

- 两个动态说明静态契约测试和重排成功提示回归测试通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 通过。
- 真实 E2E 连续应用两次测试租户重排；第一次页面显示 `2026-07-10 12:54`，第二次切换到页签后更新为 `2026-07-10 12:59`。
- 页面实际显示 1 个订单、24 道工序、27 种物料、51 个新增任务、51 个删除任务和 0 个保留任务。
- 已展开验证物料订单贡献 `TESTERP62AF41D87EFA = 685`，并展开验证瓶颈工序“外管拉伸2”的班次、工作站、产能、时长和排产时间。

## Cleanup Candidates

- `.playwright-cli/`
- `output/e2e-runtime/`
- `output/playwright/`

## Closeout Result

- 用户明确要求融合后，仅暂存主工作区中与本任务重叠的 `docs/request-command-log.md`，其他任务的已跟踪和未跟踪改动均保持原状。
- 任务分支再次变基到最新 `int_main` 后完成 `--ff-only` 快进融合；恢复请求日志时保留了本任务记录和原有 eDHR 任务记录。
- 融合后三个目标静态回归测试通过；主工作区完整 TypeScript 检查被无关未提交文件 `BatchExecutionDetailPage.vue` 的既有缺失方法阻塞，干净任务分支上的 TypeScript 检查已通过。
- 临时 worktree 已从 Git 注册中移除；残留 `node_modules` 长路径目录经目标路径校验后删除，任务 worktree 根目录已不存在。

## Current Status

completed

# 批次执行工艺流程跳转

## 任务目标

在批次执行详情的批记录预览顶部黄框区域显示当前批次执行关联的工艺流程，并支持点击后直接打开该工艺流程的流转关系图。

## 经验门禁

- PowerShell / Windows shell：显式使用 UTF-8，PowerShell 5.1 不使用 `&&`。
- 前端行为变更：先记录 BDD 场景，再按严格 TDD 完成 RED、GREEN、REGRESSION。
- 数据与路由：复用批次执行详情正式返回的 `routeId`、`routeName`、`routeCode` 和现有 `MesProRouteEdit` 路由，不新增 mock、fallback 或静默降级。
- 页面样式：遵循 IntPP 前端统一样式，保持批记录顶部信息条的紧凑蓝灰操作台风格。
- 混合工作区：目标组件已有其他任务未提交改动，只追加本任务最小增量，不覆盖、不回退既有修改；提交时只暂存本任务 hunk。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；缺少有效 `routeId` 时显示明确未关联状态并禁用跳转。
- 是否从根因和长期维护角度解决：是；展示数据直接来自批次执行正式关联，跳转复用工艺路线编辑页的 `flow` 页签。
- 是否存在临时补丁或绕过：否。

## 里程碑

1. [已完成] 定位批记录预览顶部、关联字段、工艺流程详情路由和现有测试。
2. [已完成] 编写顶部工艺流程展示与跳转的失败测试。
3. [已完成] 实现工艺流程名称显示和点击打开流转关系图。
4. [已完成] 运行目标测试、静态检查和真实浏览器验证。
5. [已完成] 完成任务收尾、提交任务所属变更。

## 预期验证

- 打开已有批次执行详情并选择工序后，批记录预览顶部中间显示关联工艺流程名称，名称缺失时显示正式路线编码。
- 点击工艺流程后打开 `MesProRouteEdit` 对应路线的 `flow` 页签。
- 缺少有效路线 ID 时显示“未关联工艺流程”并禁用点击，不跳转到默认路线。
- 新增回归测试先 RED，最小实现后 GREEN，相关回归检查通过。
- 使用真实浏览器从 `http://localhost:8081` 登录并验证只读实际用户路径。

## Current Status

completed

## 验证结果

- 静态契约验证工艺流程链接位于批记录上下文与填写载体切换之间，缺少路线时禁用跳转。
- 目标静态回归、既有顶部上下文回归、主区域滚动回归、ESLint 和 TypeScript 检查通过。
- 本机 `芋道源码/admin` 真实只读 E2E：批次 `900000000480` 显示工艺流程“球囊扩张压力泵”，点击后进入 `/mes/pro/route/edit/922099?tab=flow`。
- E2E 全程 MES 写请求为 0，浏览器控制台错误和页面未处理异常均为 0。

## Cleanup Candidates

- `doc/tasks/20260710-edhr-batch-process-route-link/e2e-verify.cjs`
- `doc/tasks/20260710-edhr-batch-process-route-link/e2e-output/`
- `doc/tasks/20260710-edhr-batch-process-route-link/frontend-feature-evidence.md`
- `output-edhr-batch-process-route-link-8081.out.log`
- `output-edhr-batch-process-route-link-8081.err.log`

## 收尾结果

- 实现提交：`b35fbcce4 任务: 增加批次工艺流程跳转`。
- `task-closeout-cleanup` preview/apply 均通过；任务 E2E 脚本、截图、结果 JSON、前端 evidence 和任务运行日志已清理。
- 第一次 apply 因任务启动的 Vite 正占用日志而失败；确认 PID 与命令行属于当前仓后停止该进程，第二次 apply 成功。
- 当前为主工作区，无额外 worktree 合并或删除动作。

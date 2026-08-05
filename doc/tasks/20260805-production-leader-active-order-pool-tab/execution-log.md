# Execution Log

## User Intent

- 生产组长的活跃订单池作为独立 Tab。
- Tab 下使用标准列表模板展示全部活跃订单池。
- 提供新增按钮，点击后可以新增活跃订单。

## BDD

- BDD: 生产组长查看活跃订单池 -> Given 用户进入生产组长页面，When 点击“活跃订单池”Tab，Then 页面使用统一标准列表模板展示正式接口返回的全部活跃订单。
- BDD: 生产组长新增活跃订单 -> Given 用户位于“活跃订单池”Tab，When 点击“新增活跃订单”并提交合法数据，Then 页面调用正式加入接口、关闭对话框并刷新列表。
- BDD: 生产组长移出活跃订单 -> Given 列表存在活跃订单，When 用户点击该行“移出”并确认，Then 页面调用正式移出接口并刷新列表。
- BDD: 其它生产组长模块保持不变 -> Given 用户切换人员管理、报工管理、看板、异常、损耗管理或班组配置，When 页面渲染对应模块，Then 原有模块内容和 PQC 组长行为不受影响。

## Initial Inspection

- 页面入口：`IntRuoyiFronted/src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue`。
- 共享实现：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 正式接口：`getTeamLeaderActiveOrderList`、`addTeamLeaderActiveOrder`、`removeTeamLeaderActiveOrder`。
- 当前活跃订单维护位于“班组配置”模块中的内嵌卡片，尚未作为独立 Tab，也未使用统一标准列表模板。
- `docs/experience-index.md` 已存在；命中统一列表、角色内容页签拆分和前端静态契约隔离门禁。

## Dirty Worktree Baseline

- 初始分支：`int_main`，跟踪 `origin/int_main`。
- 初始工作区存在当前任务开始前的并行改动，涉及后端 Team Leader 配置、系统用户 API、`UnifiedListTemplate`、PQC 规程页、`TeamLeaderWorkbenchPage.vue`、若干测试和既有任务文档。
- 按项目规则将在当前任务实现前提交这些既有改动作为独立脏工作区基线；当前任务文件不纳入该基线提交。

## Verification Evidence

- RED: pending
- GREEN: pending
- REGRESSION: pending

## Blockers

- 暂无。若并行任务继续修改本任务目标文件并产生同一区域冲突，将停止并报告。


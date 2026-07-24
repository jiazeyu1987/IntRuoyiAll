# 任务：版本信息显示到菜单栏底部

## 目标

按用户截图和要求，将运行版本与查看变更入口从页面悬浮条改为显示在左侧菜单栏最下方，避免遮挡页面内容，并让它成为菜单栏的一部分。

## 上一任务检查

- 上一相关任务：`doc/tasks/20260624-release-info-visible-on-business-frontend/task.md`
- 状态：COMPLETED。
- 处理：沿用上一任务的 release-info 数据读取与弹窗展示，只调整挂载位置和样式。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - UI 使用紧凑操作台风格，固定尺寸，避免遮挡和布局跳动。
  - 不使用浮动装饰元素占用页面内容层。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；仍然直接显示“版本信息未生成”。
- `是否从根因和长期维护角度解决`：是；组件挂载到菜单栏结构内，不再依赖页面 fixed 定位。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 菜单底部显示版本 -> Given 用户打开业务前端 When 左侧菜单渲染 Then 版本信息区域显示在菜单栏最下方。`
- `BDD: 版本信息不悬浮 -> Given 页面内容滚动或切换路由 When 版本信息显示 Then 它不使用 fixed 悬浮定位。`

## 里程碑

1. 写入任务文档与 RED 静态契约测试。`COMPLETED`
2. 调整挂载位置和样式。`COMPLETED`
3. 运行契约测试和类型检查。`COMPLETED`
4. 提交本任务改动。`COMPLETED`

## 预期验证

- `node scripts/release-info-dock-contract.test.mjs` 通过。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` 通过。

## 当前状态

COMPLETED：`ReleaseInfoDock` 已从 `App.vue` 移入 `Menu.vue` 的 `release-info-menu-footer`，组件不再使用 fixed 悬浮定位；菜单滚动区占用剩余高度，版本信息固定在左侧菜单栏底部。

## Current Status

COMPLETED

## 验证证据

- `node scripts/release-info-dock-contract.test.mjs` -> RED：`Menu.vue` 未挂载 `ReleaseInfoDock`，组件仍包含 `position: fixed`。
- `node scripts/release-info-dock-contract.test.mjs` -> GREEN：2 tests passed。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> BLOCKED：仓库现有 `src/views/mes/pro/task/calendar/index.vue(1863,37)` 报 `Property 'id' does not exist on type 'ProScheduleCalendarIssueItemVO'`，不属于本次改动文件。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm build:test` -> GREEN，构建成功。
- `task-closeout-cleanup --mode preview/apply` -> GREEN，删除一次性 `frontend-feature-evidence.md`，保留 `task.md` 与 `execution-log.md`。

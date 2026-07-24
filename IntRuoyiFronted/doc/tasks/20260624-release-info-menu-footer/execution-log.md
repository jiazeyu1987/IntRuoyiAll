# Execution Log

## BDD

BDD: 菜单底部显示版本 -> Given 用户打开业务前端 When 左侧菜单渲染 Then 版本信息区域显示在菜单栏最下方。

BDD: 版本信息不悬浮 -> Given 页面内容滚动或切换路由 When 版本信息显示 Then 它不使用 fixed 悬浮定位。

## RED

RED: node scripts/release-info-dock-contract.test.mjs -> FAIL，`Menu.vue` 未挂载 `ReleaseInfoDock`，`ReleaseInfoDock.vue` 仍包含 `position: fixed`。

## GREEN

GREEN: node scripts/release-info-dock-contract.test.mjs -> PASS，2 tests passed。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm build:test -> PASS，输出 `Build successful. Please see dist-test directory`。

BLOCKER: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> `src/views/mes/pro/task/calendar/index.vue(1863,37): error TS2339: Property 'id' does not exist on type 'ProScheduleCalendarIssueItemVO'`，该文件不属于本次改动范围。

GREEN: task-closeout-cleanup --mode preview/apply -> PASS，删除一次性 `frontend-feature-evidence.md`，保留 `task.md` 与 `execution-log.md`。

## BLOCKER

- 暂无。

# 20260523 Showroom 任务卡片仅执行中显示

## Goal

修复 `showroom/product` 产品管理页顶部“一键讲解任务”“一键封面任务”卡片的显示条件：只有对应后台任务处于执行中或续跑中的真实运行态时才显示；任务未开始、已停止、已完成或仅允许执行时不显示。

## Milestones

- [x] 核对前置任务状态并确认可开始当前任务。
- [x] 记录本次 BDD 场景与严格 TDD 验证基线。
- [x] 为任务卡片显示条件补充 RED 回归测试。
- [x] 以最小前端改动收紧卡片可见条件并同步静态断言。
- [x] 运行目标验证并回写任务证据。
- [x] 运行 closeout preview 并完成任务级提交准备。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `node --test scripts/showroom-admin-frontend.test.mjs`
- `node tests/e2e/showroom-product-toolbar-layout.spec.js`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-task-banners-running-only open http://127.0.0.1:8081/login --headed`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-task-banners-running-only run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-showroom-task-banners-visible-only-while-running\scripts\verify-showroom-task-banners-hidden-when-idle.mjs`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260523-showroom-task-banners-visible-only-while-running/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260523-showroom-task-banners-visible-only-while-running/frontend-feature-evidence.md`

## Current Status

Completed on 2026-05-23. 组件回归测试、静态前端断言、真实 `showroom/product` 登录路径验证与 task closeout 清理均已完成，当前任务已准备好做任务级提交。

## Verification Evidence

- 已核对最近前端任务文档，确认阻塞项为本地菜单未入库与既有 `ts:check` 问题，和本次修改范围无关。
- `node --test scripts/showroom-admin-product-list.test.mjs` 先 RED 后 GREEN：
  - RED：新增 `task banners stay hidden when backend tasks are not running` 后首次执行失败，断言 `true !== false`，证明旧逻辑会在任务未运行时仍显示卡片。
  - GREEN：最小修复后同命令 17/17 通过。
- `node --test scripts/showroom-admin-frontend.test.mjs` 21/21 通过。
- `node tests/e2e/showroom-product-toolbar-layout.spec.js` 通过，固定任务区与工具栏布局未回退。
- Playwright 真实验证通过：
  - 使用 `测试租户(122) / aoteman / admin123` 从 `http://127.0.0.1:8081/login` 登录后进入 `http://127.0.0.1:8081/showroom/product`
  - 页面在当前空闲状态下未显示 `一键讲解任务` 与 `一键封面任务`
  - 截图：`output/playwright/showroom-task-banners-hidden-when-idle-green.png`
- 真实状态接口复核：
  - 讲解任务：`active=false`、`running=false`、`matchedCount=180`、`remainingCount=0`
  - 封面任务：`active=false`、`running=false`、`taskStatus=COMPLETED`、`startAllowed=true`、`remainingPendingCount=0`
- `task-closeout-cleanup` preview 通过：
  - keep：仅 `task.md`、`execution-log.md`
  - delete：本任务 evidence 文档、一次性 Playwright 校验脚本、green 截图
  - blocked：`<none>`
- `task-closeout-cleanup` apply 通过：已按 preview 删除 evidence 文档、一次性 Playwright 校验脚本与 green 截图，仅保留任务主记录与正式回归测试。

## Remaining Blockers

- 无。

## Cleanup Candidates

- 已按 `task-closeout-cleanup` apply 处理完成；本节保留为本次 closeout 范围记录。

# Task: DCC 空白页 E2E 截图验证

## Goal

使用 Playwright 走真实前端登录和菜单点击路径，验证 `DCC 文控中心` 下至少 `DCC目录管理`、`DCC访问规则`、`DCC文件类别` 三个页签不再显示空白，并产出可交付截图。

## Scope

- 仅做前端真实路径 E2E 验证与截图取证。
- 使用当前本机运行中的前端入口与真实登录账号。
- 不引入 mock、fallback 或测试专用前端控件。
- 将截图、脚本与验证记录保存到当前前端仓库任务目录中。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-dcc-controlled-file-frontend-surface/task.md`
- Status before this task: completed
- Impact: DCC 前端表面源码已验证完成，可以继续做运行态 E2E 截图取证。

## Milestones

- [x] M1: 检查上一条前端任务状态并创建本任务文档。
- [x] M2: 记录 BDD 场景与截图验证预期。
- [x] M3: 编写并运行 Playwright 截图脚本，走真实登录和认证后页面访问路径。
- [x] M4: 保存截图证据，更新执行日志与最终结果。

## Expected Verification

- Playwright 通过真实登录访问当前运行前端。
- 依次打开：
  - `DCC目录管理`
  - `DCC访问规则`
  - `DCC文件类别`
- 每个页面都产出一张截图，且截图中主内容区域不是空白。

## Current Status

已完成。Playwright 已对 `8081` 上的瑛泰管理系统执行真实登录，并保存三张 DCC 页面截图；页面壳体已渲染，不再是纯白空页。

## Blocker And Impact

- Blocker: none currently discovered.
- Impact: 需要截图来确认用户看到的运行态页面已经恢复正常。

## Final Verification Result

- GREEN: `npx --package @playwright/cli playwright-cli -s=dcc-empty-tabs-81-proof run-code --filename doc\\tasks\\20260514-dcc-empty-tabs-e2e-screenshots\\scripts\\capture-dcc-tabs.mjs`
- GREEN: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-empty-tabs\dcc-directories-8081-proof.png`
- GREEN: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-empty-tabs\dcc-access-rules-8081-proof.png`
- GREEN: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-empty-tabs\dcc-categories-8081-proof.png`

## Residual Risk

- 三个页面已经能渲染出筛选区、表格区和操作区，不再是纯白空页。
- 但 Playwright 控制台仍捕获到 DCC 目录树和审批岗位等接口返回“系统未知错误”，所以页面当前展示为空数据壳体，后续若要恢复真实数据仍需继续排查后端接口。

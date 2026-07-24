# 任务：电子批记录报表视觉保真优化 Round 6 前端验证

## 任务目标

- 在新的前端 worktree 中配合 Automation `automation-2` Round 6。
- 使用真实前端路径点击 `清除电子批记录报表` 对应按钮，再点击 `A 直接 doc` 对应按钮，支持后端 Jimu 报表视觉保真对比。
- 除非真实入口或代理接线损坏，否则前端侧不修改生产代码。

## Worktree

- 前端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-automation-2-ebr-visual-fidelity-round6\yudao-ui-admin-vue3`
- 后端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-automation-2-ebr-visual-fidelity-round6\ruoyi-vue-pro`
- 分支：`codex/20260527-automation-2-ebr-visual-fidelity-round6`
- 本地端口：前端 `8109`，后端 `48109`

## BDD 场景

- BDD: 真实前端清除后生成 -> Given 测试租户可登录且本 worktree 前端代理到本 worktree 后端 / When 用户点击清除按钮并确认后再点击 `A 直接 .doc` / Then 前端必须触发真实 DELETE `/delete-all` 和 POST `/recognize-fixed?routeKey=A`，不能用 API-only 路径代替。
- BDD: 前端入口不掩盖失败 -> Given 后端或代理不可用 / When 用户执行清除或生成 / Then 前端应暴露失败信息，任务记录 blocker，不能改造前端隐藏错误。

## 里程碑

- [x] M1：创建 paired frontend worktree。
- [x] M2：安装前端依赖并记录本轮预览端口。
- [x] M3：启动前端 `batch-record-preview` 模式并验证代理指向 `48109`。
- [x] M4：使用 Playwright 完成真实清除和 `A 直接 .doc`。
- [x] M5：若修改前端生产代码，补 RED/GREEN 并回归；否则记录无前端生产变更。

## 预期验证

- `pnpm install --frozen-lockfile` 成功。
- 前端 `http://127.0.0.1:8109` 可访问，并通过本地代理访问后端 `http://127.0.0.1:48109`。
- Playwright 使用测试租户真实点击清除和生成。

## 当前状态

- 状态：completed。
- 已完成：worktree 创建、依赖安装、前端预览模式启动、真实 Playwright 清除和 `A 直接 .doc` 生成验证。
- 当前阻塞：无。
- 前端生产代码变更：无。
- 最终验证：`deletedReportCount=15`，`importedCount=15`，`finalPageTotal=15`，console/page errors empty。

## Current Status

completed

## Cleanup Candidates

- `.env.batch-record-preview.local`

## Cleanup Keep

- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/task.md`
- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/execution-log.md`

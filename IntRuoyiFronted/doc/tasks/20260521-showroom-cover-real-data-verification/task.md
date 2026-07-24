# Task: 展厅封面真实数据验证

## Goal

使用真实登录、真实前端页面、真实产品数据和真实后端接口，对“点击生成封面”做一次完整验证，确认：

- 前端能从 `http://localhost:8081` 真实进入展厅产品管理页；
- 点击 `AI生成` 时不再出现 `SHOWROOM_COVER_GENERATION_FAILED: codex cli command is required`；
- 后端实际走当前已部署代码；
- 最终真实接口返回成功或明确的上游图片生成失败原因。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-cover-real-data-verification\**`
- Playwright 真实浏览器路径
- 必要时的本机前后端运行状态检查

## Non-Scope

- 不新增测试专用前端控件。
- 不用 mock 数据替代真实页面路径。
- 不修改后端业务逻辑，除非验证过程中发现本次刚修复的代码未被当前运行实例加载。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-import-form-missing\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一同仓前端任务已闭环，不阻塞本次真实数据验证。

## Milestones

1. 建立验证任务记录并确认运行前提。
2. 确认当前前后端运行实例与端口。
3. 用 Playwright 走真实登录与真实页面路径点击生成封面。
4. 记录结果、生成验证报告，并在需要时说明阻塞。

## Expected Verification

- Playwright 真实浏览器回放
- 必要的运行时端口/进程检查
- `doc/tasks/20260521-showroom-cover-real-data-verification/verification-report.md`

## Current Status

- Status: Completed
- Completed work:
  - 已确认前端 `http://localhost:8081` 可访问。
  - 已确认后端 Java 进程正在监听 `48081`。
  - 已用真实租户 `测试租户 / aoteman / admin123` 进入展厅产品页，真实点击已发布产品的 `AI生成`。
  - 已确认旧错误 `codex cli command is required` 未复现。
  - 已先复现“对象存储直链 403、页面图片不显示”的真实阻塞。
  - 已在后端修复部署后复跑真实点击，确认回填 URL 改为 `/admin-api/infra/file/...`，页面图片已真实加载成功。
- Remaining blockers:
  - None.

## Cleanup Keep

- doc/tasks/20260521-showroom-cover-real-data-verification/verification-report.md

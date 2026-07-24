# Task: 展厅封面真实数据 E2E 复验

## Goal

使用真实登录、真实产品数据和真实前后端运行实例，对展厅产品封面生成做一次完整 E2E 复验，确认：

- 能从 `http://localhost:8081` 真实登录并进入 `产品管理`；
- 对真实已发布产品点击 `AI生成` 后，接口成功；
- 关闭弹窗后，列表中的封面列真实渲染图片，而不是“未上传”；
- 最后通过真实后端接口核对 `displayRevision.fields.cover_image` 已写入。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-cover-e2e-rerun\**`
- Playwright 真实浏览器回放
- 必要的运行时接口核对

## Non-Scope

- 不新增测试专用前端控件。
- 不修改业务逻辑，除非复验再次暴露当前运行实例未加载最新代码。
- 不处理与封面 E2E 无关的在途改动。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-single-cover-refresh\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 单个封面刷新修复已完成，本次只做独立真实 E2E 复验。

## Milestones

1. 创建复验任务记录并确认运行实例可访问。
2. 用 Playwright 走真实登录和真实封面生成路径。
3. 用真实接口核对 `cover_image`。
4. 写入验证报告并按任务收尾规范预览。

## Expected Verification

- Playwright 真实浏览器回放
- `doc/tasks/20260521-showroom-cover-e2e-rerun/verification-report.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-cover-e2e-rerun --mode preview`

## Current Status

- Status: Completed
- Completed work:
  - 已确认前端 `8081` 与后端 `48081` 当前都返回 HTTP `200`。
  - 已用真实租户 `测试租户 / aoteman / admin123` 进入 `产品管理`。
  - 已在真实列表中确认 `product_001` 当前显示 `V15 / 已发布`，且封面列有真实图片。
- Remaining blockers:
  - None.

## Cleanup Keep

- doc/tasks/20260521-showroom-cover-e2e-rerun/verification-report.md

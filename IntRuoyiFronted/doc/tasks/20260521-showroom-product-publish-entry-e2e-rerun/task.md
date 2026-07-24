# 任务：产品管理发布入口真实数据 E2E 复验

## Goal

使用真实测试租户、真实前端页面 `http://localhost:8081/showroom/product`、真实后端接口与真实产品数据，对“产品管理发布入口拆分”做一次完整 E2E 复验，确认：

- 企宣账号列表行存在唯一 `发布` 按钮，且位于 `删除` 左边；
- 企宣账号基础信息与详细信息弹窗都只保留 `保存`；
- 企宣账号保存讲解稿后可从列表直接发布，发布后的真实产品状态变为 `PUBLISHED`；
- 发布后的中文讲解稿 `sourceRevisionId` 与新发布 revision 一致；
- 编辑账号没有列表 `发布` 按钮，但基础信息与详细信息弹窗仍保留 `保存草稿 + 提交审批`。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-publish-entry-e2e-rerun\**`
- Playwright 真实浏览器回放
- 必要的真实接口核对

## Non-Scope

- 不修改业务逻辑，除非复验再次暴露当前运行实例未加载最新代码。
- 不新增测试专用前端控件或 mock 数据。
- 不处理与本次产品发布入口 E2E 无关的在途改动。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-publish-button-split\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 发布入口拆分已完成并有源码/真实验证记录；本次只做独立真实数据 E2E 复验，不改动原交付结论。

## Milestones

1. 创建复验任务记录并确认 `8081` 前端入口、`48081` 后端接口和测试租户可访问。
2. 用 Playwright 走企宣真实链路，验证列表发布入口、弹窗保存入口和发布结果。
3. 用 Playwright 走编辑真实链路，验证无发布按钮且审批入口保留。
4. 用真实接口核对发布后的 revision 与讲解稿绑定关系。
5. 写入验证结果并按任务收尾规范预览 cleanup。

## Expected Verification

- Playwright 真实浏览器回放
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-publish-entry-e2e-rerun\verification-report.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-entry-e2e-rerun --mode preview`

## Current Status

Completed on 2026-05-21.

## Blockers And Impact

- Blocker: none.
- Impact:
  - 企宣真实链路已确认列表独立 `发布` 按钮、基础信息/详细信息页脚单一 `保存`、发布后 `PUBLISHED` 状态和讲解稿 revision 绑定均正常。
  - 编辑真实链路已确认无列表 `发布`，仍保留 `保存草稿 + 提交审批`。

## Final Verification Result

- PASS: 企宣真实浏览器回放，结果见 `verification-report.md`
- PASS: 编辑真实浏览器回放，结果见 `verification-report.md`
- PASS: 真实接口复核，结果见 `verification-report.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-entry-e2e-rerun --mode preview`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-entry-e2e-rerun --mode apply`

## Cleanup Result

- 已按 preview 结果删除本次 E2E 复验的 Playwright 脚本与两张截图。
- 当前任务目录保留 `task.md`、`execution-log.md` 与 `verification-report.md` 作为长期记录。

## Cleanup Keep

- `doc/tasks/20260521-showroom-product-publish-entry-e2e-rerun/verification-report.md`

## Cleanup Candidates

- `output/playwright/showroom-product-publicity-publish-entry.png`
- `output/playwright/showroom-product-editor-submit-entry.png`

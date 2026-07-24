# 任务：展厅公司英文介绍可编辑并使用音频组件播放

## Goal

按当前展厅公司编辑弹框的真实流程，允许用户在 `AI生成介绍` 后手动修改英文介绍，再基于中英文介绍重新生成语音，并且无论是英文介绍文本变更还是重新生成后的语音版本，都可以单独保存成功。公司页只读区与编辑弹框中的中英文播放入口都不再使用独立按钮，而改为使用与产品列表音频列一致的原生音频播放组件。

## Scope

- 调整 `showroom/company` 编辑弹框中的英文介绍为可编辑态，而不是只读回显。
- 调整前端生成语音与保存语音请求，带上当前英文介绍草稿并保持刷新后可见。
- 将公司页只读区与弹框中的播放交互改为音频播放器组件，不再保留 `播放中文 / 播放英文` 按钮。
- 补齐定向前端源码回归、真实路径验证、任务文档与证据文件。

## Non-Scope

- 不改动产品列表音频列的既有视觉风格与业务逻辑。
- 不改动前台 `showroom/company-intro` 路由结构。
- 不新增 fallback、mock 数据、兼容分支或隐藏错误。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-hide-manufacturing-honors\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一任务已完成公司工作台字段可见范围调整；本次在同一页面继续修改语音介绍区交互，不回退上一任务结果。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在其他未提交改动与多组任务文档。
- Impact: 本任务仅允许修改公司工作台、相关 API 类型、定向测试与本任务文档，不覆盖无关改动。

## Milestones

- [x] M1: 确认上一同仓任务状态并创建本任务文档。
- [x] M2: 先补前端 RED 测试，锁定“英文可编辑、播放改为音频组件、保存链路使用当前英文草稿”的可观察行为。
- [x] M3: 最小改动公司工作台与 API 类型，完成英文编辑、播放器展示与请求参数调整。
- [x] M4: 运行定向前端测试、lint、真实路径验证并记录 GREEN。
- [x] M5: 更新前端证据、执行 closeout preview，并准备同仓提交。

## Expected Verification

- `node --test scripts/showroom-admin-company-dashboard-history.test.mjs`
- `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-dashboard-history.test.mjs --format stylish`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-editable-english run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-editable-english-narration\scripts\verify-showroom-company-editable-english.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-showroom-company-editable-english-narration/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-editable-english-narration --mode preview`

## Current Status

Completed on 2026-05-21.

已完成公司语音介绍区交互调整。当前 `showroom/company` 页面中，英文介绍可在 AI 自动翻译后继续手改；`生成语音` 会同时提交当前中英文草稿；只读区与弹框内均改为音频播放器组件；用户手改文案后旧草稿音频会失效，重新生成后可正常保存。

## Blockers And Impact

- Blocker: none.
- Impact: none.

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-company-dashboard-history.test.mjs`
- PASS: `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-dashboard-history.test.mjs --format stylish`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-editable-english run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-editable-english-narration\scripts\verify-showroom-company-editable-english.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-showroom-company-editable-english-narration/frontend-feature-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-editable-english-narration --mode preview`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-editable-english-narration --mode apply`

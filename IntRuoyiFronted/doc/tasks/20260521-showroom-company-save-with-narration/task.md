# 任务：展厅公司底部保存同时保存语音

## Goal

移除 `showroom/company` 编辑弹框里的单独 `保存语音` 按钮，改为由右下角主按钮 `保存` 统一承担公司内容保存与已生成双语语音发布。用户在当前弹框里完成 `AI生成介绍 -> 手改介绍 -> 生成语音` 后，无需再点击第二个保存入口。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\company\CompanyWorkbench.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-company-dashboard-history.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-save-with-narration\**`

## Non-Scope

- 不改动后端公司保存/语音发布接口契约。
- 不改动产品讲解稿与产品音频页面。
- 不新增 fallback、mock、兼容分支或隐藏错误。
- 不额外增加仅用于测试的前端控件。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-editable-english-real-e2e\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一任务已验证公司页真实链路可完成 `AI生成介绍 -> 手改英文 -> 生成语音 -> 保存语音`；本次继续在同一真实路径上收敛交互入口，不回退英文可编辑与音频播放器结果。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在其他未提交改动与多组任务文档。
- Impact: 本任务仅修改公司工作台、定向源码测试与本任务文档，不覆盖无关改动。

## Milestones

- [x] M1: 创建任务文档并确认上一同仓任务状态。
- [x] M2: 先补 RED 测试，锁定“无单独保存语音按钮，底部保存承担语音保存”的可观察行为。
- [x] M3: 最小改动公司工作台保存流程与按钮展示。
- [x] M4: 运行定向源码测试、lint、真实路径验证并记录 GREEN。
- [x] M5: 更新证据、执行 closeout preview，并记录最终状态。

## Expected Verification

- `node --test scripts/showroom-admin-company-dashboard-history.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-dashboard-history.test.mjs --format stylish`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-save-with-narration run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-save-with-narration\scripts\verify-showroom-company-save-with-narration.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-showroom-company-save-with-narration/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-save-with-narration --mode preview`

## Current Status

Completed on 2026-05-21.

已完成公司页语音保存入口收敛。当前 `showroom/company` 编辑弹框内不再显示单独 `保存语音` 按钮；右下角 `保存` 在存在可发布双语语音草稿时会直接承担语音发布，并且在“只有语音草稿待保存、公司文字未改动”的情况下也可点击保存。

## Blockers And Impact

- Blocker: none.
- Impact: none.

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-company-dashboard-history.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-dashboard-history.test.mjs --format stylish`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-save-with-narration open http://127.0.0.1:8081/showroom/company`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-save-with-narration run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-save-with-narration\scripts\verify-showroom-company-save-with-narration.mjs`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-save-with-narration\frontend-feature-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-save-with-narration --mode preview`
- PASS: 真实测试租户 `测试租户 / aoteman / admin123` 登录后，编辑弹框中不再显示 `保存语音`，语音草稿生成后右下角 `保存` 由禁用变为可用，点击后命中 `/showroom/company/publish-narration` 并成功提示 `公司双语语音已保存`。
- Screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-save-with-narration\green-showroom-company-save-with-narration.png`

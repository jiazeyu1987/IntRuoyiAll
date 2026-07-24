# 任务：修复展厅公司 AI生成介绍 报错

## Goal

修复 `展厅 -> 展厅公司` 编辑弹框中点击 `AI生成介绍` 直接报错的问题，确保用户在真实页面路径下点击该按钮时，前端与后端按当前公司真实数据生成中英文介绍，缺少前置条件时显式暴露准确原因，不引入 fallback、mock 或静默降级。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\company\**`
- 如定位到接口或数据契约问题，再补充对应后端同任务记录并在 owning repo 内最小修改
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\**`

## Non-Scope

- 不顺带改动产品讲解、封面生成、审批流或其他菜单。
- 不为测试新增专用前端控件。
- 不使用 mock 数据替代真实页面链路。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-lazy-directory-frontend\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一同仓前端任务已完成，不阻塞本次缺陷修复。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的进行中改动与历史任务文档。
- Impact: 本任务只允许修改公司讲解相关代码与本任务文档，避免混入其他变更。

## Milestones

- [x] M1: 创建任务文档并确认上一同仓任务已完成。
- [x] M2: 记录 BDD 与复现路径，补 RED 测试锁定当前报错行为。
- [x] M3: 定位根因并完成最小修复；如需改后端，先在对应仓库补任务记录。
- [x] M4: 运行定向测试与真实链路验证，记录 GREEN。
- [x] M5: 更新任务文档、执行 closeout preview，并按结果准备提交。

## Expected Verification

- `node --test scripts/showroom-admin-company-*.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-*.test.mjs --format stylish`
- Playwright 真实页面路径验证 `http://localhost:8081/showroom/company`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-ai-script-error --mode preview`

## Current Status

Completed on 2026-05-21.

已完成真实页面复现、根因定位与联动修复验证。本次前端链路本身无需业务代码改动；真实阻塞点位于 companion backend 任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-ai-script-timeout\` 中的 `CodexCliChatModel` 超时失效。重打后端 jar 并重启本地运行时后，`/showroom/company` 点击 `AI生成介绍` 已恢复正常回填中英文介绍。

## Blockers And Impact

- Blocker: none.
- Impact: 当前本地 `http://localhost:8081/showroom/company` 真实路径已恢复 `AI生成介绍`。

## Final Verification Result

- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-ai-script-error-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\scripts\reproduce-showroom-company-ai-script-error.mjs`，在修复前稳定复现请求长时间无响应，定位到 backend AI 调用链路。
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-ai-script-error-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\scripts\reproduce-showroom-company-ai-script-error.mjs`，在修复并重启运行时后真实页面返回 `code=0`，中文介绍已回填，toast 正常显示成功提示。
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-ai-script-error --mode preview`，已按 preview 清理临时脚本与截图，仅保留 `task.md / execution-log.md`。

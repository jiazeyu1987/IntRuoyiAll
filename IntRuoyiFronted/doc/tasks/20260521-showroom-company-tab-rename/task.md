# 任务：展柜公司页签改名为公司信息

## Goal

将展厅后台 `/showroom/company` 的页签标题从 `展柜公司` 调整为 `公司信息`，确保路由元信息、相关静态测试与依赖页签标题的验证脚本保持一致，不引入其他菜单结构、权限或业务行为变更。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\router\modules\showroom.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-copy-rename.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\permission-hidden-shell-route-merge.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-phase1-admin-content-approval.e2e.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-tab-rename\**`

## Non-Scope

- 不改动公司工作台表单、保存逻辑、审批逻辑或 AI 生成逻辑
- 不顺带调整 `展柜` 顶级菜单名、`展柜管理` 页签名或其他展厅页签文案
- 不新增 fallback、兼容分支、mock 或测试专用前端控件

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一同仓且同公司模块任务已完成，不阻塞本次页签文案调整

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的进行中改动与未跟踪任务目录
- Impact: 本任务只允许修改公司页签标题与直接相关测试/任务文档，提交时必须单独暂存本任务文件

## Milestones

- [x] M1: 确认最近同仓相关任务状态并创建当前任务文档
- [x] M2: 先补 RED 测试，锁定公司页签标题应为 `公司信息`
- [x] M3: 最小修改路由元信息与相关断言
- [x] M4: 运行定向测试并记录 GREEN 结果
- [x] M5: 更新任务文档、证据文档并执行 closeout preview

## Expected Verification

- `node --test --test-name-pattern "showroom admin route titles keep 展柜 menu copy and rename company tab to 公司信息|showroom merge should" scripts/showroom-admin-copy-rename.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs`
- `pnpm exec eslint src/router/modules/showroom.ts scripts/showroom-admin-copy-rename.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-tab-rename open http://127.0.0.1:8081/showroom/company --headed`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-tab-rename run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-tab-rename\scripts\verify-showroom-company-tab-title.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-tab-rename\frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-tab-rename --mode preview`

## Current Status

Completed on 2026-05-21.

## Final Verification Result

- PASS: `node --test --test-name-pattern "showroom admin route titles keep 展柜 menu copy and rename company tab to 公司信息|showroom merge should" scripts/showroom-admin-copy-rename.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs`
- PASS: `pnpm exec eslint src/router/modules/showroom.ts scripts/showroom-admin-copy-rename.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs scripts/showroom-phase1-admin-content-approval.e2e.mjs`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-tab-rename open http://127.0.0.1:8081/showroom/company --headed` + `run-code --filename ...\\verify-showroom-company-tab-title.mjs`，真实登录 `测试租户 / aoteman / admin123` 后确认左侧菜单、顶部标签按钮和页面标题均显示 `公司信息`，旧文案 `展柜公司` 不再可见
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-tab-rename\frontend-feature-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-tab-rename --mode preview`，preview 结果为 `ready`

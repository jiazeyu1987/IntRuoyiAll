# Execution Log - 20260521-showroom-company-tab-rename

BDD: showroom company tab title rename -> Given 用户进入展厅后台公司页签 When 前端根据 `showroom.ts` 注册 `/showroom/company` 菜单与页签 Then 可见页签标题显示 `公司信息` 且不再显示 `展柜公司`

BDD: showroom route merge keeps renamed company tab -> Given 权限路由合并逻辑处理 `/showroom` 静态与动态子路由 When 子路由包含 `ShowroomAdminCompany` Then 合并结果中的公司子路由标题保持为 `公司信息`

RED: `node --test --test-name-pattern "showroom admin route titles keep 展柜 menu copy and rename company tab to 公司信息|showroom merge should" scripts/showroom-admin-copy-rename.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs` -> FAIL, `scripts/showroom-admin-copy-rename.test.mjs` 断言 `meta: { title: '公司信息' }` 未命中，说明运行前源码仍为 `展柜公司`

GREEN: `node --test --test-name-pattern "showroom admin route titles keep 展柜 menu copy and rename company tab to 公司信息|showroom merge should" scripts/showroom-admin-copy-rename.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs` -> PASS

GREEN: `pnpm exec eslint src/router/modules/showroom.ts scripts/showroom-admin-copy-rename.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs scripts/showroom-phase1-admin-content-approval.e2e.mjs` -> PASS

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-tab-rename open http://127.0.0.1:8081/showroom/company --headed` + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-tab-rename\scripts\verify-showroom-company-tab-title.mjs` -> PASS，真实登录 `测试租户 / aoteman / admin123` 后，左侧菜单项、顶部标签按钮和页面标题均显示 `公司信息`，`展柜公司` 不再可见

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-tab-rename\frontend-feature-evidence.md` -> PASS

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-tab-rename --mode preview` -> PASS，preview 结果为 `ready`，保留 `task.md / execution-log.md`，其余任务附属文件可清理

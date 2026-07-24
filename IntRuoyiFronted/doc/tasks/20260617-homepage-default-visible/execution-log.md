# 执行日志：20260617-homepage-default-visible

BDD: 首页默认隐藏但可直达 -> Given 用户已通过本机后台登录 / When 系统渲染左侧菜单和标签栏 / Then 首页不作为默认菜单项或固定标签显示；When 用户直接访问 `/index` / Then 首页内容仍可正常展示。

INFO: 经验门禁 -> 已读取 `docs/experience-index.md`、`docs/login-access.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。

INFO: 前置任务检查 -> 最近完整前端任务 `20260616-showroom-company-honor-hall` 已完成；`20260615-main-branch-build-publish-test` 仅有截图产物，无任务文档，不作为当前阻塞项。

GREEN: experience-preflight -> PASS，本次仅在本机 `http://localhost:8081` 使用真实登录页进行只读首页复现；已确认 `npx` 可用且本机前端入口返回 200；不访问测试服/正式服，不切换账号、租户或环境。

RED: `node tests\e2e\homepage-default-hidden-static.spec.js` -> FAIL，预期原因：当前 `Home` 父路由缺少 `meta.hidden=true`，首页仍参与默认菜单渲染。

GREEN: `node tests\e2e\homepage-default-hidden-static.spec.js` -> PASS。

GREEN: `node scripts\home-showroom-entry.test.mjs` -> PASS。

GREEN: `node tests\e2e\layout-logo-use-home-icon-static.spec.js` -> PASS。

GREEN: `node scripts\permission-hidden-shell-route-merge.test.mjs` -> PASS。

INFO: `npx eslint src\router\modules\remaining.ts tests\e2e\homepage-default-hidden-static.spec.js` 首次 30 秒超时，无结果，不作为通过；延长超时后重跑。

GREEN: `npx eslint src\router\modules\remaining.ts tests\e2e\homepage-default-hidden-static.spec.js` -> PASS。

INFO: `pnpm ts:check` 首次在默认 heap 下 OOM，退出码 134；按本项目大型前端检查需要显式 Node heap 的实际前置重跑。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

INFO: Playwright 只读验证首次脚本使用 `require + top-level await`，Node 24 报模块格式错误，未进入页面逻辑；改为 async IIFE 后重跑。

GREEN: Playwright 本机真实登录只读验证 -> PASS，`http://localhost:8081/index` 首页内容可见，菜单和标签栏均未显示首页，业务写请求数为 0。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence yudao-ui-admin-vue3\doc\tasks\20260617-homepage-default-visible\bug-regression-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260617-homepage-default-visible --mode preview` -> PASS；`delete=<none>`、`blocked=<none>`、`warnings=<none>`。

# 执行日志：展厅页签路由子页签化

BDD: 后台管理页签可按菜单授权 -> Given 用户拥有某个展厅后台子菜单权限 When 用户进入对应 `/showroom-admin/*` 子路由 Then 页面高亮对应子页签并展示该子页签内容，而不是固定展示默认页签。

BDD: 数字展厅页签可按菜单授权 -> Given 用户拥有某个数字展厅子菜单权限 When 用户进入对应 `/showroom/display/*` 子路由 Then 页面高亮对应子页签并按该路由加载展示内容。

RED: 待执行 -> FAIL，尚未补充路由子页签化测试。

RED: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs` -> FAIL，缺少 `ShowroomAdminHistory` / `ShowroomAdminAssignment` / `ShowroomAdminDiscussion` 子路由，后台页签未使用 `showroomAdminTabs` + `route.name` 驱动，前台页签未使用 `showroomDisplayTabs` 驱动。

GREEN: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs` -> PASS，14 个展厅前端路由/组件约束测试通过。

BLOCKED: `pnpm exec eslint ...` -> FAIL，独立 worktree 未安装 `node_modules`，无法解析 `eslint` 命令。

GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js ...` -> PASS，复用主工作区已安装依赖完成本 worktree 文件 lint。

GREEN: `python -X utf8 <inline Playwright route subtabs smoke>` -> PASS，真实登录后从首页入口进入展厅后台和数字展厅，点击后台“产品管理/版本历史”、前台“公司/设置”均切换到对应路由并激活对应页签。

BLOCKER: 前台展示接口 -> FAIL，浏览器回归仍捕获 `No static resource admin-api/showroom/display/home.` 与 `No static resource admin-api/showroom/display/company.`，属于既有后端接口缺口。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260519-showroom-route-subtabs/frontend-feature-evidence.md` -> PASS，证据文档校验通过；随后按 closeout 预览清理该临时证据文件。

BLOCKED: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-route-subtabs --mode preview` -> FAIL for automatic linked-worktree closeout，清理项为空，但脚本检测到默认主分支 `master` 没有已检出 worktree，无法自动快进合并并删除 worktree。

# 执行日志：展厅页签权限门禁

BDD: 无展厅权限不显示展厅页签 -> Given 后端权限菜单没有返回 `/showroom` 或 `Showroom` 菜单 / When 前端生成可见菜单和顶层页签 / Then 菜单源不包含展厅，初始路由表也不注册展厅入口。

BDD: 有展厅权限时按后端菜单显示展厅 -> Given 后端权限菜单返回展厅父菜单及授权子菜单 / When 前端合并静态展厅组件壳 / Then 只显示授权子菜单，并动态注册对应展厅路由。

- PRECHECK: `doc/tasks/20260610-scheduler-e2e-closure/task.md` -> PASS，最近一个存在 `task.md` 的前端任务已完成。
- PRECHECK: `src/router/modules/showroom.ts`、`src/router/modules/remaining.ts`、`src/store/modules/permission.ts` -> FAILING BEHAVIOR FOUND，`remainingRouter` 初始展开 `showroomRoutes`，`permissionStore.generateRoutes()` 又把静态路由整体放入菜单源，导致无后端展厅菜单时仍出现展厅顶层页签。
- RED: `node scripts\showroom-tab-permission-gate.test.mjs` -> FAIL，`remaining.ts` 仍导入并展开 `showroomRoutes`，`permission.ts` 尚未声明权限受控静态展厅壳。
- GREEN: `node scripts\showroom-tab-permission-gate.test.mjs` -> PASS，展厅已从初始路由移除，并改为后端动态菜单授权后合并。
- GREEN: `node --test scripts\showroom-admin-frontend.test.mjs` -> PASS，22 tests。
- GREEN: `pnpm exec eslint src\router\modules\remaining.ts src\store\modules\permission.ts scripts\showroom-tab-permission-gate.test.mjs scripts\showroom-admin-frontend.test.mjs` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- INFO: `node --test scripts\showroom-admin-frontend.test.mjs scripts\showroom-route-integration.test.mjs` -> FAIL，`showroom-route-integration` 中产品编辑加载策略断言与当前代码不一致，和本次展厅权限路由无关，未在本任务中修改。
- GREEN: `node -e "...playwright..."` 打开 `http://localhost:8081/login?redirect=%2Findex` -> PASS，页面标题为 `瑛泰管理系统`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260611-showroom-tab-permission-gate\bug-regression-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260611-showroom-tab-permission-gate --mode preview` -> READY，keep `task.md` / `execution-log.md`，delete candidate `bug-regression-evidence.md`，blocked `<none>`，warnings `<none>`；本任务保留 evidence 文件用于缺陷回归校验追溯，未执行 apply。

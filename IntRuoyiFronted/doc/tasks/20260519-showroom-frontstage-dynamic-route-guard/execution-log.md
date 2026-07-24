# 执行日志：20260519-showroom-frontstage-dynamic-route-guard

## BDD

BDD: 动态展厅详情页不应作为直达菜单入口 -> Given 数字展厅的展厅详情页和产品详情页依赖 `hallId` 或 `productId` 动态参数 / When 用户从左侧菜单进入展厅前台 / Then 菜单只暴露静态前台入口，动态详情页只能通过带真实参数的业务路径访问。

## TDD 证据

PRECHECK: `doc/tasks/20260519-showroom-single-parent-tabs/task.md` -> PASS，上一条展厅菜单合并任务已标记“已完成”。

PRECHECK: `src/router/modules/showroom.ts` + `src/views/showroom-frontstage/index.vue` -> PASS，当前报错链路已定位到 `ShowroomDisplayHall` 依赖 `route.params.hallId`，而路由配置仍将动态详情页暴露为菜单可达项。

PRECHECK: `doc/tasks/20260519-showroom-frontstage-shared-foundation/task.md` -> BLOCKED，前序共享基础任务的 staged 文件复核时发现 follow-up 回归，本任务负责继续收口。

RED: `node --test scripts/showroom-frontstage.test.mjs` -> FAIL，`ShowroomDisplayHall` / `ShowroomDisplayProduct` 仍保留开放参数路由与无效直达入口元数据，前台卡片仍执行 `playProductNarration(product)`，没有进入真实详情页。

GREEN: `node --test scripts/showroom-frontstage.test.mjs` -> PASS，10 tests, 0 failures。动态详情路由仅接受数值参数，且 hall/product 卡片已切换为真实详情导航。

GREEN: `pnpm exec eslint src/router/modules/showroom.ts src/views/showroom-frontstage/index.vue scripts/showroom-frontstage.test.mjs` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-frontstage-dynamic-route-guard\bug-regression-evidence.md` -> PASS。

BLOCKER: real Playwright E2E on `http://localhost:8081` -> FAIL，缺少 `SHOWROOM_E2E_TENANT_NAME`、`SHOWROOM_E2E_FRONTSTAGE_USERNAME`、`SHOWROOM_E2E_FRONTSTAGE_PASSWORD` 环境变量；影响是当前无法完成带认证的真实前台用户路径复核。

CLOSEOUT-PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-frontstage-dynamic-route-guard --mode preview` -> READY，默认保留 `task.md` 与 `execution-log.md`，默认清理 `bug-regression-evidence.md`。

BLOCKER: task-scoped Git commit -> FAIL，`showroom-frontstage` 相关文件已被前序任务 `20260519-showroom-frontstage-shared-foundation` staged，占用相同文件，当前无法在不混入前序 staged 改动的前提下单独提交本任务。

# Execution Log: Showroom Frontstage Shared Foundation

BDD: Shared payload and narration foundation -> Given the showroom frontstage must later support screen, Pad, and mobile shells / When shared payload parsing, narration state handling, and route helpers are extracted / Then the current frontstage page can reuse them without duplicating business logic.

BDD: Shared foundation preserves public frontstage rules -> Given the current public frontstage already enforces incomplete markers, narration-only language switching, and advanced-field exclusion / When the shared foundation is extracted / Then those rules remain enforced and testable outside the monolithic page.

RED: Shared-foundation tests pending -> FAIL, shared frontstage foundation files and tests do not exist yet.

RED: `node --test scripts/showroom-frontstage-shared.test.mjs` -> FAIL, shared frontstage foundation files and route-directory index module do not exist yet.

BDD: 参数化前台详情路由不出现在菜单 -> Given 展厅前台的 hall、product、narration 路由都依赖上下文参数或查询参数 / When 用户从侧边菜单进入数字展厅 / Then 菜单只能暴露无参入口页，不能暴露会生成无效参数路径的详情页。

RED: `node --test scripts/showroom-frontstage-shared.test.mjs scripts/showroom-frontstage.test.mjs` -> FAIL, showroom detail routes are still visible in `src/router/modules/showroom.ts`, so the sidebar can navigate to `/showroom/display-product/:productId` and trigger `route.params.productId` number parsing failure.

GREEN: `node --test scripts/showroom-frontstage-shared.test.mjs scripts/showroom-frontstage.test.mjs` -> PASS

GREEN: `pnpm exec eslint src/router/modules/showroom.ts scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-shared.test.mjs` -> PASS

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260519-showroom-frontstage-shared-foundation/bug-regression-evidence.md` -> PASS

BDD: Direct narration route reports route prerequisites accurately -> Given the `ShowroomDisplayNarration` route requires `targetType` and `targetId` query context / When the route is opened without those values / Then the shared route helper must fail fast with a route-field error, not an API payload error.

RED: `node --test scripts/showroom-frontstage-shared.test.mjs` -> FAIL, `src/views/showroom-frontstage/shared/route.ts` still reused payload validators, so missing `route.query.targetType` and `route.query.targetId` were reported as API-field failures.

GREEN: `node --test scripts/showroom-frontstage-shared.test.mjs` -> PASS

GREEN: `node --test scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-shared.test.mjs` -> PASS

BLOCKER: staged-file review before commit found the follow-up `route.params.hallId` direct-entry regression and missing hall/product card navigation path; task-scoped commit deferred to `20260519-showroom-frontstage-dynamic-route-guard`.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-frontstage-shared-foundation\bug-regression-evidence.md` -> PASS, bug regression evidence is valid.

NOTE: During this verification turn, the current `int_main` branch already matched the expected F1 shared-foundation production code; the remaining output in this task was verification evidence and closeout documentation.

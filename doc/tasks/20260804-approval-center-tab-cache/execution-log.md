# Execution Log

## User Intent

- 2026-08-04：用户要求审批中心从其它顶部页签切换回来后不要每次重新加载。
- 验收解释：首次进入正常加载；切走再切回保留页面实例及已有状态；主动查询、刷新和有效筛选变化仍正常重新加载。

## Baseline

- 分支：`int_main`。
- 初始状态：分支领先 `origin/int_main` 1 个提交，工作区存在其它任务改动。
- 分支端口门禁：`scripts/preflight/branch-runtime-port-guard.ps1` -> PASS，`int_main` 使用前端 `8081`、后端 `48081`。
- 既有脏改动基线提交：`0dcee54f8 Baseline: preserve existing worktree changes before approval center cache fix`。
- 基线提交包含既有 13 个文件，本任务尚未修改审批中心源码或测试。

## BDD / TDD

- BDD: 审批中心页签切回保留页面 -> Given 用户已进入审批中心并完成列表加载、筛选或分页操作，When 用户切换到其它顶部页签后再切回审批中心，Then 页面沿用原实例和已有状态且不重复执行初始化请求。
- BDD: 审批中心主动操作仍可刷新 -> Given 审批中心页面处于缓存状态，When 用户主动查询、刷新或改变有效路由筛选，Then 页面按正式请求链路重新加载并明确暴露失败。

## Milestone Updates

### M1 根因与 RED

- 状态：已完成。
- 根因 1：`ApprovalCenterTodo/Done/MyInitiated/Cc` 均设置 `noCache: true`，切走时页面实例被卸载。
- 根因 2：四个路由名与共享 SFC 组件名 `ApprovalCenterWorkbench` 不一致，直接改为 `noCache: false` 仍无法命中 `keep-alive include`。
- 根因 3：共享页面 route watcher 在缓存实例失活期间仍会观察全局路由；若不限定实例路由并比较成功加载状态，切回或切换审批子路由仍会重复请求。
- RED: `pnpm e2e:approval-center:tab-return-no-reload:static` -> FAIL，预期原因：`ApprovalCenterTodo must enable keep-alive caching`。

### M2 最小修复

- 状态：已完成。
- 已为审批中心四个路由启用缓存并声明共享 `keepAliveName: 'ApprovalCenterWorkbench'`。
- 已让 `AppView` 和 `TagsView` 按显式组件缓存身份加入、保留和主动刷新删除缓存。
- 已让审批中心缓存实例只响应自己的 route name，并仅在模块与列表按同一 route state 成功加载后跳过同状态切回加载。

### M3 验证与收尾

- 状态：已完成，进入 `ready_for_closeout`。
- GREEN: `pnpm e2e:approval-center:tab-return-no-reload:static` -> PASS。
- REGRESSION: `node tests/e2e/approval-center-pagination-preserve-page-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/approval-center-route-filter-visible-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/approval-center-pagination-event-payload-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS，退出码 0。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260804-approval-center-tab-cache\bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260804-approval-center-tab-cache\frontend-feature-evidence.md` -> PASS。
- REGRESSION: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main` 前端 `8081`、后端 `48081`。
- 经验沉淀：复用现有 `docs/frontend-development.md` 的“顶部菜单页签切回缓存”门禁，补充共享 SFC 路由显式 `keepAliveName`、`AppView`/`TagsView` 同一缓存身份和审批中心 route state guard；同步更新 `docs/experience-index.md` 关键词索引。
- 真实 E2E 前置探针：`http://127.0.0.1:8081/` -> HTTP 200；`http://127.0.0.1:48081/actuator/health` -> `UP`；`node --check doc\tasks\20260804-approval-center-tab-cache\approval-center-tab-return-no-reload-real.e2e.cjs` -> PASS。
- 真实 E2E: `node doc\tasks\20260804-approval-center-tab-cache\approval-center-tab-return-no-reload-real.e2e.cjs` -> PASS。
- 真实用户路径：审批中心初始加载后点击侧边菜单“个人中心”，再点击顶部“审批中心”页签返回；`initialResponseCount=2`、`returnResponseCount=0`、目标响应 HTTP 200、`pageErrors=[]`。
- 真实 E2E 结果：`doc/tasks/20260804-approval-center-tab-cache/approval-center-tab-return-no-reload-result.json`；成功截图：`doc/tasks/20260804-approval-center-tab-cache/approval-center-tab-return-no-reload.png`。
- 早期真实脚本失败原因已修正：避免登录重定向后的重复整页导航，并使用真实侧边菜单点击保持 TagsView 实例；失败截图列入 cleanup candidate，不作为最终通过证据。
- 相邻大合同 `pnpm e2e:approval-center:standard-list:static` 曾失败于 `approval-center-reviewer-column-static.spec.js` 的审核人/状态标签断言，归属并发“申请人/审核人列”任务，不作为本任务失败证据。
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-approval-center-tab-cache --mode preview` -> PASS；keep 为 task/execution/verification、真实 E2E 脚本、结果 JSON 和成功截图；delete 为失败截图和临时 evidence 文件。
- CLEANUP APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-approval-center-tab-cache --mode apply` -> PASS；已删除失败截图、`bug-regression-evidence.md` 和 `frontend-feature-evidence.md`。
- 保留证据跟踪：真实 E2E 脚本和成功截图被 `.gitignore` 的 `doc/tasks/**/*.cjs`、`doc/tasks/**/*.png` 命中，提交时需要对这两个保留文件使用 `git add -f`。

## Blockers

- 产品实现和验证无 blocker。
- 仍需完成任务级 cleanup、选择性提交和推送；当前工作区有其它并发任务脏改动，必须避免混入本任务提交。

# 20260617 首页默认隐藏修复

## 任务目标

按“首页默认不显示”的要求收敛首页路由展示行为：保留 `/index` 和根路径重定向可访问，但默认菜单和标签栏不展示首页，不通过删除页面或错误跳转实现。

## 前置任务检查

- 前端最近完整任务 `20260616-showroom-company-honor-hall` 状态为 `COMPLETED`。
- 前端任务目录 `20260615-main-branch-build-publish-test` 缺少 `task.md` / `execution-log.md`，仅包含截图产物；本任务不把该目录作为未完成任务接续。

## 经验门禁

- 命中 `docs/login-access.md`：本机后台默认入口为 `http://localhost:8081/login?redirect=/index`；涉及登录或真实 E2E 时使用本机测试租户 `测试租户/aoteman/111111`，登录失败必须阻塞记录，不得静默切换账号、租户或环境。
- 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：如需修改首页或布局表现，保持现有蓝/中性运营台风格，不新增营销化视觉或无关装饰。

## BDD 场景

- BDD: 首页默认隐藏但可直达 -> Given 用户已通过本机后台登录 / When 系统渲染左侧菜单和标签栏 / Then 首页不作为默认菜单项或固定标签显示；When 用户直接访问 `/index` / Then 首页内容仍可正常展示。

## 里程碑

1. M1：记录任务文档、经验门禁和 BDD 场景。`DONE`
2. M2：RED：用新增回归测试复现首页仍默认显示。`DONE`
3. M3：GREEN：最小修改首页路由元信息，使其默认隐藏且仍可直达。`DONE`
4. M4：REGRESSION：运行目标测试、相关静态检查和必要的 Playwright 只读验证。`DONE`
5. M5：Closeout：更新任务证据，运行 task-closeout-cleanup 预览并按验证结果提交。`DONE`

## 预期验证

- 新增首页默认隐藏回归测试先失败后通过。
- 相关前端静态检查通过。
- 如本机前端与登录链路可用，Playwright 使用 `测试租户/aoteman/111111` 确认默认菜单/标签不显示首页，并直接打开 `/index` 确认首页内容仍可见。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是修复默认首页路由/渲染链路，不通过隐藏错误或改默认成功值绕过。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：任务文档、经验门禁、BDD 场景、RED 静态回归测试、路由元信息修复、静态检查、类型检查、Playwright 只读验证、bug 回归证据校验和 task-closeout-cleanup 预览。
- 待完成：无。

## 验证结果

- RED：`node tests\e2e\homepage-default-hidden-static.spec.js` -> FAIL，预期原因：`Home` 父路由缺少 `meta.hidden=true`。
- GREEN：`node tests\e2e\homepage-default-hidden-static.spec.js` -> PASS。
- GREEN：`node scripts\home-showroom-entry.test.mjs` -> PASS。
- GREEN：`node tests\e2e\layout-logo-use-home-icon-static.spec.js` -> PASS。
- GREEN：`node scripts\permission-hidden-shell-route-merge.test.mjs` -> PASS。
- GREEN：`npx eslint src\router\modules\remaining.ts tests\e2e\homepage-default-hidden-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：Playwright 本机真实登录只读验证 -> PASS，`http://localhost:8081/index` 首页内容可见，菜单和标签栏均未显示首页，业务写请求数为 0。
- GREEN：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence yudao-ui-admin-vue3\doc\tasks\20260617-homepage-default-visible\bug-regression-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260617-homepage-default-visible --mode preview` -> PASS，`delete=<none>`、`blocked=<none>`、`warnings=<none>`。

## Cleanup Keep

- `doc/tasks/20260617-homepage-default-visible/bug-regression-evidence.md`

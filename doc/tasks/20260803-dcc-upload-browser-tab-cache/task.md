# DCC 文件上传 / 受控浏览页签切回不重复加载

## Task Goal

修复 DCC 顶部页签在“文件上传”和“受控浏览”之间切换时，受控浏览页签被重新挂载并重复加载的问题。正式行为应是两个菜单页签进入后保留在 `keep-alive` 缓存中，切回已打开页签不重新执行首屏 `onMounted` 加载。

## Milestones

- [x] 建立任务文档、BDD 场景和适用门禁。
- [x] 增加 RED 静态合同，证明动态菜单路由必须强制缓存文件上传与受控浏览。
- [x] 修复动态路由元数据覆盖，确保两个正式页签不受菜单 `keepAlive` 异常值影响。
- [x] 运行定向静态合同和相邻回归验证。
- [ ] 收尾状态、验证报告、清理和提交推送。

## Expected Verification

- `pnpm e2e:dcc:upload-browser-tab-cache:static`
- `pnpm e2e:dcc:browser-single-tab:static`
- `pnpm e2e:dcc:redbox-first-open-performance:static`
- `pnpm ts:check`

## Applicable Gates

- 前端页签首屏按需挂载门禁：未激活页签不应重复挂载；已访问页签应通过明确缓存边界保留，禁止用延时器、空数据、mock、关闭错误提示或隐藏页签冒充优化。
- DCC 受控浏览当前有效版与权限隔离门禁：本任务仅调整前端页签缓存，不改变受控浏览数据源、权限过滤、当前有效版口径、预览或下载链路。
- E2E 脚本入口存在性门禁：静态合同必须在 `package.json` 中有明确脚本；静态 PASS 不冒充真实 Playwright 页面验收。

## Current Status

ready_for_closeout

## Completed Work

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md` 和 `bug-regression-fix-loop` 技能规则。
- 已定位现有根因候选：`src/utils/routerHelper.ts` 由后端动态菜单 `route.keepAlive` 推导 `meta.noCache`，当前只给受控浏览设置 `tagsViewKeyMode='path'`，未强制两个红框菜单页签保留缓存。
- 已新增 `e2e:dcc:upload-browser-tab-cache:static` 静态合同，覆盖菜单 componentName、`AppView` keep-alive、TagsView 缓存集合和动态路由覆盖。
- 已在 `src/utils/routerHelper.ts` 增加文件上传/受控浏览正式缓存路径与组件集合，并在动态路由覆盖中强制 `tagsViewKeyMode='path'` 与 `noCache=false`。
- 已补齐既有 `dcc-browser-single-tab-static.spec.js` 的 package 脚本入口，避免相邻回归命令缺失。
- 已运行 task-closeout-cleanup preview/apply；已将 bug evidence 的 RED/GREEN/验证摘要归档到 `execution-log.md` 和 `verification-report.md`，并删除临时 `bug-regression-evidence.md`。

## Verification Evidence

- RED: `pnpm e2e:dcc:upload-browser-tab-cache:static` -> FAIL，预期失败原因为旧代码缺少 `DCC_UPLOAD_ROUTE_COMPONENT` 与两个页签的强制缓存覆盖。
- GREEN: `pnpm e2e:dcc:upload-browser-tab-cache:static` -> PASS。
- REGRESSION: `pnpm e2e:dcc:browser-single-tab:static` -> 首次 FAIL 因 package script 缺失，补齐脚本后 PASS。
- REGRESSION: `pnpm e2e:dcc:redbox-first-open-performance:static` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- CHECK: `git diff --check -- IntRuoyiFronted/src/utils/routerHelper.ts IntRuoyiFronted/tests/e2e/dcc-upload-browser-tab-cache-static.spec.js IntRuoyiFronted/package.json doc/tasks/20260803-dcc-upload-browser-tab-cache` -> PASS；仅输出 CRLF 工作区提示。
- CLEANUP: `task_closeout.py --task-id 20260803-dcc-upload-browser-tab-cache --mode preview` -> PASS；keep `task.md`、`execution-log.md`、`verification-report.md`，delete 临时 `bug-regression-evidence.md`。
- CLEANUP: `task_closeout.py --task-id 20260803-dcc-upload-browser-tab-cache --mode apply` -> PASS；已删除临时 `bug-regression-evidence.md`。

## Blockers

- 工作区在任务开始前已有大量未提交改动，且 `int_main` 已领先 `origin/int_main` 1 个提交；本任务仅修改本缺陷相关文件。若提交/推送前仍存在无关脏改动，需要按项目 Git 策略处理，否则不能标记 completed。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在动态路由元数据层固定正式页签缓存契约。
- `是否存在临时补丁或绕过`：否。

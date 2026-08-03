# DCC 文件上传 / 受控浏览页签切回不重复加载

## Task Goal

修复 DCC 顶部页签在“文件上传”和“受控浏览”之间切换时，受控浏览页签被重新挂载并重复加载的问题。正式行为应是两个菜单页签进入后保留在 `keep-alive` 缓存中，切回已打开页签不重新执行首屏 `onMounted` 加载。

## Milestones

- [x] 建立任务文档、BDD 场景和适用门禁。
- [ ] 增加 RED 静态合同，证明动态菜单路由必须强制缓存文件上传与受控浏览。
- [ ] 修复动态路由元数据覆盖，确保两个正式页签不受菜单 `keepAlive` 异常值影响。
- [ ] 运行定向静态合同和相邻回归验证。
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

in_progress

## Completed Work

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md` 和 `bug-regression-fix-loop` 技能规则。
- 已定位现有根因候选：`src/utils/routerHelper.ts` 由后端动态菜单 `route.keepAlive` 推导 `meta.noCache`，当前只给受控浏览设置 `tagsViewKeyMode='path'`，未强制两个红框菜单页签保留缓存。

## Verification Evidence

- 待记录 RED/GREEN/REGRESSION 命令结果。

## Blockers

- 工作区在任务开始前已有大量未提交改动，且 `int_main` 已领先 `origin/int_main` 1 个提交；本任务将只修改本缺陷相关文件。若提交/推送前仍存在无关脏改动，需要按项目 Git 策略处理，否则不能标记 completed。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在动态路由元数据层固定正式页签缓存契约。
- `是否存在临时补丁或绕过`：否。

# 执行日志：20260524-showroom-frontstage-removal

BDD: 左侧菜单不再暴露前台大屏 -> Given 用户登录 IntRuoyi 后台并加载展柜菜单 / When 权限路由合并静态与动态菜单 / Then 左侧菜单不得再出现可见的 `前台大屏` 子项。
BDD: 首页只保留展厅后台入口 -> Given 用户打开后台首页 `http://localhost:8081` / When 首页渲染数字展厅入口卡片 / Then 页面只显示进入展厅后台的入口，不再显示 `进入展厅前台` 按钮或前后台双入口文案。
BDD: IntRuoyi 不再注册前台展示路由 -> Given 应用加载展柜路由模块 / When 读取 `showroom.ts` 路由定义 / Then 不得再注册 `display/screen/*`、`display/pad/*`、`display/mobile/*` 与旧 alias `home/company-intro/display-hall/display-product/settings/narration`。

PRECHECK: previous same-repo task `20260524-showroom-product-cover-prompt-management` -> BLOCKED，已在其 `task.md` 记录当前任务与 `src/router/modules/showroom.ts` 的重叠提交边界。
RED: `node --test scripts/home-showroom-entry.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs scripts/showroom-frontstage-runtime.test.mjs` -> FAIL，首页源码仍包含 `进入展厅前台` 按钮与 `openShowroomFrontstage`，`src/router/modules/showroom.ts` 仍注册 `display/screen/home` 等前台路由；同时本地默认 `127.0.0.1:48081` 未监听，运行时脚本无法连接本地后端。
GREEN: `node --test scripts/home-showroom-entry.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs` -> PASS，首页与菜单合并断言均确认 IntRuoyi 不再暴露前台入口。
GREEN: `node --test scripts/showroom-admin-frontend.test.mjs` -> PASS，展厅后台路由与工作台契约仍然完整，未被前台删除改动带坏。
GREEN: `pnpm exec eslint src/router/modules/showroom.ts src/views/Home/Index.vue scripts/home-showroom-entry.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs scripts/showroom-frontstage-runtime.test.mjs` -> PASS。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
RED: `$env:INT_RUOYI_ADMIN_API_BASE='http://127.0.0.1:48082/admin-api'; node --test scripts/home-showroom-entry.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs scripts/showroom-frontstage-runtime.test.mjs` -> FAIL，仅 `scripts/showroom-frontstage-runtime.test.mjs` 失败；本地后端公开接口 `/showroom/display/website-config` 返回 `SHOWROOM_TARGET_NOT_FOUND: live product ZH narration source revision mismatch`。
RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-frontstage-removal open http://localhost:8081/login?redirect=%2Findex` + 初版 `run-code --filename ...verify-showroom-frontstage-removal.mjs` -> FAIL，`playwright-cli` 的 `run-code` 会话未触发 `/admin-api/system/auth/login` 请求。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-frontstage-removal run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-frontstage-removal\scripts\verify-showroom-frontstage-removal.mjs` -> PASS，改为 API 注入登录态后，真实浏览器确认首页无前台按钮、后台入口可进入 `/showroom/company`，旧前台地址 `/showroom/display/screen/home`、`/showroom/home`、`/showroom/company-intro`、`/showroom/display-hall/1`、`/showroom/display-product/1`、`/showroom/settings`、`/showroom/narration?...` 均不再命中 IntRuoyi 前台页面，最终页面标题为 `404`。
GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-frontstage-removal\frontend-feature-evidence.md` -> PASS。
GREEN: follow-up backend local live-data repair `c5b7eeb172` -> PASS，本地 `website-config` 数据重新对齐后，`$env:INT_RUOYI_ADMIN_API_BASE='http://127.0.0.1:48082/admin-api'; node --test scripts/showroom-frontstage-runtime.test.mjs` 已通过。
CLOSEOUT: task-scoped commit `be9aa73a` `任务: 移除展柜前台入口` -> PASS。
CLOSEOUT-PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260524-showroom-frontstage-removal --mode preview` -> READY，默认保留 `task.md` 与 `execution-log.md`，默认清理 `frontend-feature-evidence.md` 与 `scripts/verify-showroom-frontstage-removal.mjs`。

# Execution Log: 工艺路线基础信息 Tab 调整

BDD: 基础信息作为独立页签 -> Given 用户打开工艺路线编辑页 / When 查看页签 / Then 基础信息作为独立 Tab 显示在组成工序和流转关系图之间。
BDD: 顶部保存保留基础信息保存能力 -> Given 用户修改基础信息 / When 点击页面顶部保存 / Then 仍调用原路线保存接口并保留原校验行为。

RED: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> FAIL，缺少 `type RouteFormInitialTab = 'process' | 'basic' | 'flow' | 'product'`，证明现有页面尚无基础信息 Tab。
GREEN: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> PASS，基础信息 Tab 存在且位于组成工序与流转关系图之间，基础字段不再位于 Tab 外层。
GREEN: `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js` -> PASS，编辑页仍默认打开流转关系图。
GREEN: `node tests/e2e/mes-route-edit-page-static.spec.js` -> PASS，编辑页共享表单与隐藏路由契约保持有效。
BLOCKER: `pnpm ts:check` -> FAIL，Node 默认堆内存 OOM，非代码类型错误。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
GREEN: frontend-feature-evidence -> PASS，`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-basic-info-tab/frontend-feature-evidence.md`。
GREEN: closeout-preview -> PASS，`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-basic-info-tab --mode preview`，delete/blocked/warnings 均为 `<none>`。
BLOCKER: experience-preflight -> 默认 Playwright headless shell 启动失败，`Invalid file descriptor to ICU data received`；改用系统 Chrome 后官方 `login-preflight.mjs` 等待 `/system/auth/login` 响应 60000ms 超时。按门禁停止真实页面验证，不使用旁路登录、接口直调或 mock 替代。
BLOCKER: commit -> 当前前端 `RouteEditPage.vue`、`RouteFormContent.vue` 在本轮开始前已有未提交重叠改动，后端仓也存在大量非本任务改动；按混合工作区提交规则，本轮不整文件提交，避免夹带其它任务或用户改动。
GREEN: experience-preflight-retry -> PASS，官方登录预检使用系统 Chrome 已进入本机测试租户 `/mes/pro/route`。
GREEN: `node tests/e2e/mes-route-basic-info-tab-real.e2e.js` -> PASS，路线 `RT000017` 的页签顺序、基础字段和顶部保存按钮通过真实只读页面验证，未产生 MES 写请求。
GREEN: commit-boundary -> PASS，本次协调提交统一核对重叠文件及其完整回归后纳入独立前后端提交。

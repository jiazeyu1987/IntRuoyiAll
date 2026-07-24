# 任务：删除顶部栏红框控件

## 任务目标

按用户截图要求，删除顶部栏红框中的控件：租户访问下拉、菜单搜索入口、字号下拉、语言下拉。删除范围限定为前端顶部栏显示与相关导入，不修改后端接口、租户数据、登录逻辑、消息通知和用户信息入口。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-runtime-control-remove-health-guide-cards/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改顶部栏控件删除、前端静态测试和任务证据。

## BDD 场景

- BDD: 顶部栏不再显示红框控件 -> Given 用户登录后台进入任意页面 / When 顶部栏渲染完成 / Then 不显示租户访问下拉、菜单搜索、字号下拉、语言下拉。
- BDD: 删除红框控件不影响保留入口 -> Given 用户登录后台进入任意页面 / When 顶部栏渲染完成 / Then 消息通知和用户信息入口仍保留。
- BDD: 删除红框控件不改变租户数据隔离 -> Given 用户使用测试租户登录 / When 顶部栏渲染完成 / Then 本次变更不新增后端请求、不修改租户数据、不引入 fallback 或静默降级。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：新增 RED 静态测试，锁定顶部栏红框控件删除契约。
- [x] M3：删除 `ToolHeader.vue` 中红框控件的导入、状态计算和渲染。
- [x] M4：运行目标测试、类型检查和 frontend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/layout-header-remove-redbox-controls-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅删除顶部栏控件显示，不增加 fallback 或异常吞噬逻辑。
- `是否从根因和长期维护角度解决`：是。直接移除顶部栏对这些控件的导入、状态读取和渲染，避免隐藏式保留入口。
- `是否存在临时补丁或绕过`：否。不新增测试专用内容或临时 CSS 隐藏。

## Current Status

completed

## 验证结果

- VERIFY：上一前端任务 `doc/tasks/20260604-runtime-control-remove-health-guide-cards/task.md` 状态为 `completed`。
- RED：`node tests/e2e/layout-header-remove-redbox-controls-static.spec.js` -> FAIL，原因：`ToolHeader.vue` 仍存在 `TenantVisit`。
- GREEN：`node tests/e2e/layout-header-remove-redbox-controls-static.spec.js` -> PASS。
- GREEN：`pnpm ts:check` -> PASS。
- GREEN：Playwright 真实页面检查 `http://127.0.0.1:8081/index` -> PASS，`#v-tool-header` 中租户访问下拉、菜单搜索、字号下拉、语言下拉计数均为 0，消息入口和用户信息入口均保留。
- GREEN：`frontend-feature-evidence.md` 已补齐。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-layout-header-remove-redbox-controls/frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-header-remove-redbox-controls --mode preview` -> PASS，未发现待删除文件、阻塞或警告。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-header-remove-redbox-controls --mode apply` -> PASS，未删除文件。

## Cleanup Keep

- `doc/tasks/20260604-layout-header-remove-redbox-controls/frontend-feature-evidence.md`

## 剩余阻塞

- 暂无。

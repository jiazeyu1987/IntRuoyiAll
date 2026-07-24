# 任务：顶部栏保留模块搜索框

## 任务目标

按用户补充要求，顶部栏需要保留模块搜索框。范围限定为恢复 `ToolHeader.vue` 中的 `RouterSearch` 导入、搜索开关状态读取和渲染；继续移除租户访问下拉、字号下拉、语言下拉。不修改后端接口、租户数据、登录逻辑、消息通知和用户信息入口。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-layout-header-remove-redbox-controls/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务按用户新要求调整顶部栏删除范围。

## BDD 场景

- BDD: 顶部栏保留模块搜索框 -> Given 用户登录后台进入任意页面 / When 顶部栏渲染完成 / Then 显示模块搜索入口和“请输入菜单内容”搜索框。
- BDD: 其它红框控件仍删除 -> Given 用户登录后台进入任意页面 / When 顶部栏渲染完成 / Then 不显示租户访问下拉、字号下拉、语言下拉。
- BDD: 保留搜索框不改变租户数据隔离 -> Given 用户使用测试租户登录 / When 顶部栏渲染完成 / Then 本次变更不修改租户数据、不引入 fallback 或静默降级。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：更新 RED 静态测试，锁定模块搜索框必须保留。
- [x] M3：恢复 `ToolHeader.vue` 中 `RouterSearch` 的导入、状态读取和渲染。
- [x] M4：运行目标测试、类型检查、真实页面检查和 frontend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/layout-header-remove-redbox-controls-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：Playwright 真实页面检查 `http://127.0.0.1:8081/index`
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅恢复顶部栏模块搜索框，不增加 fallback 或异常吞噬逻辑。
- `是否从根因和长期维护角度解决`：是。通过恢复组件导入、状态读取和 TSX 渲染实现，不使用 CSS 临时显示或隐藏。
- `是否存在临时补丁或绕过`：否。不新增测试专用内容或临时绕过。

## 当前状态

completed

## 验证结果

- VERIFY：上一前端任务 `doc/tasks/20260604-layout-header-remove-redbox-controls/task.md` 状态为 `completed`。
- RED：`node tests/e2e/layout-header-remove-redbox-controls-static.spec.js` -> FAIL，原因：`ToolHeader.vue` 缺少 `RouterSearch` 导入。
- GREEN：`node tests/e2e/layout-header-remove-redbox-controls-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：Playwright 真实页面检查 `http://127.0.0.1:8081/index` -> PASS，顶部栏中模块搜索框计数为 1，租户访问下拉、字号下拉、语言下拉计数均为 0，消息入口和用户信息入口均保留。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-layout-header-keep-module-search/frontend-feature-evidence.md` -> PASS。
- CLOSEOUT PREVIEW：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-header-keep-module-search --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Cleanup Keep

- `doc/tasks/20260604-layout-header-keep-module-search/frontend-feature-evidence.md`

## 剩余阻塞

- 暂无。

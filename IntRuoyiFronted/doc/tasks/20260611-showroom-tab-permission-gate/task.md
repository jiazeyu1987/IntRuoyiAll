# 任务：展厅页签权限门禁

## 任务目标

修复后台展厅顶层页签无权限也可见的问题，使展厅入口只在后端权限菜单返回对应菜单时才显示并注册路由；无展厅菜单权限的用户不能通过顶部页签、侧边菜单或直接访问 `/showroom` 绕过权限。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。不通过本地兜底菜单或静默隐藏错误实现，展厅入口必须由后端权限菜单驱动。
- 是否从根因和长期维护角度解决：是。调整静态展厅壳与动态权限菜单的合并边界，避免权限外静态路由进入初始路由表。
- 是否存在临时补丁或绕过：否。不增加测试专用控件，不在 UI 层临时过滤文本。

## BDD 场景

- BDD: 无展厅权限不显示展厅页签 -> Given 后端权限菜单没有返回 `/showroom` 或 `Showroom` 菜单 / When 前端生成可见菜单和顶层页签 / Then 菜单源不包含展厅，初始路由表也不注册展厅入口。
- BDD: 有展厅权限时按后端菜单显示展厅 -> Given 后端权限菜单返回展厅父菜单及授权子菜单 / When 前端合并静态展厅组件壳 / Then 只显示授权子菜单，并动态注册对应展厅路由。

## 里程碑

1. 确认上一个前端任务状态并建立本任务文档。
2. 补充展厅权限门禁回归测试，先捕获当前无权限仍可见的失败。
3. 调整路由初始化与权限合并逻辑，让展厅成为权限驱动的静态壳。
4. 运行定向静态测试、类型/语法检查，并记录 RED/GREEN 证据。
5. 执行收尾清理预览，完成任务文档与提交。

## 预期验证

- `node scripts/showroom-tab-permission-gate.test.mjs`
- `node --test scripts/showroom-admin-frontend.test.mjs`
- `pnpm exec eslint src\router\modules\remaining.ts src\store\modules\permission.ts scripts\showroom-tab-permission-gate.test.mjs scripts\showroom-admin-frontend.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- 本地登录页 Playwright 烟测；如更宽的历史展厅测试被无关断言阻塞，记录精确阻塞。

## 当前状态

completed

## 最终验证结果

- RED: `node scripts\showroom-tab-permission-gate.test.mjs` -> FAIL，旧代码仍把展厅放在初始路由并绕过动态菜单授权。
- GREEN: `node scripts\showroom-tab-permission-gate.test.mjs` -> PASS。
- GREEN: `node --test scripts\showroom-admin-frontend.test.mjs` -> PASS，22 tests。
- GREEN: `pnpm exec eslint src\router\modules\remaining.ts src\store\modules\permission.ts scripts\showroom-tab-permission-gate.test.mjs scripts\showroom-admin-frontend.test.mjs` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260611-showroom-tab-permission-gate\bug-regression-evidence.md` -> PASS。
- GREEN: Playwright 登录页烟测 -> PASS，`http://localhost:8081/login?redirect=%2Findex` 标题为 `瑛泰管理系统`。
- CLOSEOUT PREVIEW: task-closeout-cleanup -> READY，blocked `<none>`，warnings `<none>`；未执行 apply，保留 `bug-regression-evidence.md` 作为本次缺陷回归证据。

## 剩余阻塞

- 无本任务阻塞。
- 已记录一个非本任务阻塞：`scripts/showroom-route-integration.test.mjs` 的产品编辑加载策略旧断言与当前代码不一致，未在本次权限门禁任务中修改。

## 前置任务检查

- 最近一个存在 `task.md` 的前端任务：`doc/tasks/20260610-scheduler-e2e-closure/task.md`。
- 检查结果：已完成，可开始本任务。

## 任务边界

- 本任务只修改展厅路由权限合并、对应静态测试和任务文档。
- 工作区已有其他未提交改动，不纳入本任务提交。

# QA 规程显示字段按钮标题栏布局

## Task Goal

将 QA 规程“检验项目”页签中位于表格工具栏右侧的“显示字段”按钮移动到“工序检验方法与抽样方案”标题栏右侧，并放在“新增检验方法”按钮左侧。

## Milestones

- [x] M1：冻结当前仓库脏工作区基线并确认目标组件边界。
- [x] M2：先补充按钮位置静态契约并取得 RED。
- [x] M3：完成最小前端布局修改并取得 GREEN。
- [x] M4：完成相邻回归、类型检查和可执行的页面验证。
- [ ] M5：完成任务清理、提交与推送。

## Expected Verification

- `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check`
- 若本机 `int_main` 前后端运行态和登录前置可用，使用 Playwright 打开真实 QA 规程页面核对按钮位置。

## Applicable Gates

- 截图局部布局修改必须由任务专用静态契约先 RED 后 GREEN，断言目标标题栏动作容器和内置工具栏入口关闭。
- 当前目标文件存在并行改动；必须保留已有改动，并在提交前检查目标文件差异与暂存内容。
- 只允许修改前端页面、任务专用静态契约和任务证据；API、路由、后端、DTO、数据库和业务数据受保护。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；复用现有列配置组件和保存事件，只调整正式渲染位置并关闭重复入口。
- 是否存在临时补丁或绕过：否。

## Current Status

ready_for_closeout

实现和必需验证已完成；真实浏览器验证因本机会话登录超时而未完成，当前进入清理、提交和推送阶段。

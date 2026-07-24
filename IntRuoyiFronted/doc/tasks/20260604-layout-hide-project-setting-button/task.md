# 任务：隐藏右侧浮动项目配置按钮

## 任务目标

按用户截图要求，不再显示后台页面右侧浮动的“项目配置”齿轮按钮。变更范围限定为前端全局布局入口：移除 `Layout.vue` 对 `Setting` 项目配置组件的导入和渲染，不删除业务页面内其他设置按钮，不修改后端接口、租户数据、主题配置存储或登录逻辑。

## Previous Task Check

- 上一个已提交前端任务：`doc/tasks/20260604-layout-header-remove-redbox-controls/task.md`
- 状态：`completed`
- 当前发现的未完成前端任务：`doc/tasks/20260604-runtime-control-recovery-set-contract/task.md`
- 状态：`blocked`
- 处理：已在该任务文档记录阻塞原因与影响；本任务只处理右侧浮动项目配置按钮。

## BDD 场景

- BDD: 右侧浮动项目配置按钮不再显示 -> Given 用户登录后台进入任意页面 / When 全局布局渲染完成 / Then 页面右侧不显示固定齿轮按钮，也不能打开“项目配置”抽屉。
- BDD: 隐藏项目配置不影响主体布局 -> Given 用户登录后台进入任意页面 / When 全局布局渲染完成 / Then 菜单、页面内容、返回顶部入口和移动端遮罩逻辑仍保留。
- BDD: 删除全局入口不引入 fallback -> Given 用户使用测试租户登录 / When 页面渲染 / Then 本次变更不新增后端请求、不修改租户数据、不通过 CSS 隐藏或异常吞噬绕过。

## Milestones

- [x] M1：建立任务文档并确认前置任务状态。
- [x] M2：新增 RED 静态契约测试，锁定右侧项目配置按钮不再由全局布局渲染。
- [x] M3：删除 `Layout.vue` 中 `Setting` 的导入和渲染。
- [x] M4：运行目标测试、类型检查、必要的页面验证和 frontend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/layout-hide-project-setting-button-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。直接删除全局布局中的项目配置入口，不增加 fallback、降级或异常吞噬。
- `是否从根因和长期维护角度解决`：是。按钮由 `Layout.vue` 全局挂载，直接移除挂载点，避免用 CSS 隐藏造成入口仍存在。
- `是否存在临时补丁或绕过`：否。不新增测试专用内容，不修改租户数据，不绕过真实页面路径。

## Current Status

completed

## 验证结果

- RED：`node tests/e2e/layout-hide-project-setting-button-static.spec.js` -> FAIL，原因：`Layout.vue` 仍存在 `import { Setting } from '@/layout/components/Setting'`。
- GREEN：`node tests/e2e/layout-hide-project-setting-button-static.spec.js` -> PASS。
- GREEN：`pnpm ts:check` -> PASS。
- GREEN：`git diff --check` -> PASS，仅提示 `src/layout/Layout.vue` 后续 Git 触碰时 LF 会替换为 CRLF。
- GREEN：Playwright 本机只读登录 DOM 验证 -> PASS，登录后 `http://127.0.0.1:8081/index` 未渲染 `.v-setting`，页面可见文本不包含“项目配置”。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-layout-hide-project-setting-button/frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-hide-project-setting-button --mode preview` -> PASS，未发现待删除文件、阻塞或警告。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-hide-project-setting-button --mode apply` -> PASS，未删除文件。

## Cleanup Keep

- `doc/tasks/20260604-layout-hide-project-setting-button/frontend-feature-evidence.md`

## Blockers

- 暂无。

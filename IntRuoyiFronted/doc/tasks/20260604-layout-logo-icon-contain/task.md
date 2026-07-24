# 任务：左侧品牌 ICON 缩放完整显示

## 任务目标

按用户截图要求，修复后台左上角品牌 ICON 显示不完整/显示区域不协调的问题。变更范围限定为前端全局 Logo 组件和静态契约测试：让品牌图片按固定正方形容器缩放完整显示，并保持系统标题、菜单布局和登录逻辑不变。

## Previous Task Check

- 上一个已提交前端任务：`doc/tasks/20260604-layout-hide-project-setting-button/task.md`
- 状态：`completed`
- 当前发现的未完成无关任务：`doc/tasks/20260604-runtime-control-recovery-set-contract/task.md`
- 状态：`blocked`
- 处理：无关任务已记录阻塞原因；本任务只处理左上角品牌 ICON 缩放显示。

## BDD 场景

- BDD: 左侧品牌 ICON 完整缩放显示 -> Given 用户进入后台页面 / When 侧边栏 Logo 渲染完成 / Then 品牌图片使用固定正方形显示区域、`object-contain` 和居中对齐，图片不被裁切。
- BDD: LOGO 修复不影响系统标题 -> Given 用户进入后台页面 / When Logo 区域渲染完成 / Then “瑛泰管理系统”标题仍按当前布局显示。
- BDD: LOGO 修复不引入 fallback -> Given 品牌图片资源存在 / When 页面渲染 / Then 不新增备用图片、不吞掉资源加载失败、不修改后端或租户数据。

## Milestones

- [x] M1：建立任务文档并确认前置任务状态。
- [x] M2：新增 RED 静态契约测试，锁定品牌 ICON 缩放完整显示规则。
- [x] M3：更新 `Logo.vue` 的图片容器与缩放显示方式。
- [x] M4：运行目标测试、类型检查、页面验证和 frontend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/layout-logo-icon-contain-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：Playwright 本机页面验证
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只调整已有品牌图片的显示尺寸与缩放规则，不增加备用资源或静默降级。
- `是否从根因和长期维护角度解决`：是。通过稳定容器尺寸和 `object-contain` 规则解决显示不完整/比例不协调问题。
- `是否存在临时补丁或绕过`：否。不使用测试专用 CSS，不隐藏错误。

## Current Status

completed

## 验证结果

- RED：`node tests/e2e/layout-logo-icon-contain-static.spec.js` -> FAIL，原因：`Logo.vue` 缺少 `logo-icon-frame` 固定图标承载区。
- GREEN：`node tests/e2e/layout-logo-icon-contain-static.spec.js` -> PASS。
- GREEN：`pnpm ts:check` -> PASS。
- GREEN：`git diff --check` -> PASS，仅提示 `src/layout/components/Logo/src/Logo.vue` 后续 Git 触碰时 LF 会替换为 CRLF。
- GREEN：Playwright 本机真实登录页面验证 -> PASS，`logo-icon-frame=40x40`、`img=40x40`、`objectFit=contain`、`objectPosition=50% 50%`、系统标题存在。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-layout-logo-icon-contain/frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-logo-icon-contain --mode preview` -> PASS，未发现待删除文件、阻塞或警告。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-logo-icon-contain --mode apply` -> PASS，未删除文件。

## Cleanup Keep

- `doc/tasks/20260604-layout-logo-icon-contain/frontend-feature-evidence.md`

## Blockers

- 暂无。

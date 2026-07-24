# 任务：左侧品牌 ICON 换成首页图标

## 任务目标

按用户截图要求，把左上角品牌 ICON 替换为左侧菜单“首页”前的同款 `ep:home-filled` 图标。变更范围限定为前端品牌 SVG 资源、静态契约测试和任务记录；保留既有 `Logo.vue` 40px 正方形缩放容器、系统标题、菜单、登录逻辑、后端接口和租户数据。

## Previous Task Check

- 上一个已提交前端任务：`doc/tasks/20260604-layout-logo-icon-contain/task.md`
- 状态：`completed`
- 当前发现的未完成无关任务：`doc/tasks/20260604-runtime-control-recovery-set-contract/task.md`
- 状态：`blocked`
- 处理：无关任务已记录阻塞原因；本任务只替换左上角品牌 ICON 图形。

## BDD 场景

- BDD: 品牌 ICON 使用首页图标 -> Given 用户进入后台页面 / When 左上角品牌区渲染 / Then 品牌图片使用与“首页”菜单相同的 `ep:home-filled` 图形。
- BDD: 更换图形不影响缩放容器 -> Given 品牌图片资源已替换 / When Logo 组件渲染 / Then 仍使用 40px 正方形承载区和 `object-contain` 居中缩放。
- BDD: 更换图形不引入 fallback -> Given 首页图标源自本地 Iconify 元数据 / When 资源被构建加载 / Then 不新增备用图片、不吞掉资源加载失败、不修改后端或租户数据。

## Milestones

- [x] M1：建立任务文档并确认前置任务状态。
- [x] M2：新增 RED 静态契约测试，锁定品牌 SVG 必须使用 `ep:home-filled` 图形。
- [x] M3：替换 `sidebar-brand-logo.svg` 为首页图标路径。
- [x] M4：运行目标测试、类型检查、页面验证和 frontend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/layout-logo-use-home-icon-static.spec.js`
- GREEN：`node tests/e2e/layout-logo-icon-contain-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：Playwright 本机页面验证
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。直接替换已有品牌 SVG 内容，不增加备用图或异常吞噬。
- `是否从根因和长期维护角度解决`：是。品牌资源直接采用菜单首页图标 `ep:home-filled` 的本地 SVG path，避免截图图形与品牌图不一致。
- `是否存在临时补丁或绕过`：否。不使用测试专用 CSS，不修改租户数据。

## Current Status

completed

## 验证结果

- RED：`node tests/e2e/layout-logo-use-home-icon-static.spec.js` -> FAIL，原因：`sidebar-brand-logo.svg` 仍是旧 `64x64` 品牌图，不含 `ep:home-filled` 的 `1024x1024` viewBox。
- GREEN：`node tests/e2e/layout-logo-use-home-icon-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/layout-logo-icon-contain-static.spec.js` -> PASS。
- GREEN：`pnpm ts:check` -> PASS。
- GREEN：`git diff --check` -> PASS，仅提示 `src/assets/imgs/sidebar-brand-logo.svg` 后续 Git 触碰时 LF 会替换为 CRLF。
- GREEN：Playwright 本机真实登录页面验证 -> PASS，页面 `logo-icon-frame=40x40`、`img=40x40`、`objectFit=contain`，服务端返回的 SVG 含 `ep:home-filled` path 和 `fill="#009688"`。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-layout-logo-use-home-icon/frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-logo-use-home-icon --mode preview` -> PASS，未发现待删除文件、阻塞或警告。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-logo-use-home-icon --mode apply` -> PASS，未删除文件。

## Cleanup Keep

- `doc/tasks/20260604-layout-logo-use-home-icon/frontend-feature-evidence.md`

## Blockers

- 暂无。

# 执行日志：左侧品牌 ICON 换成首页图标

BDD: 品牌 ICON 使用首页图标 -> Given 用户进入后台页面 / When 左上角品牌区渲染 / Then 品牌图片使用与“首页”菜单相同的 `ep:home-filled` 图形。

BDD: 更换图形不影响缩放容器 -> Given 品牌图片资源已替换 / When Logo 组件渲染 / Then 仍使用 40px 正方形承载区和 `object-contain` 居中缩放。

BDD: 更换图形不引入 fallback -> Given 首页图标源自本地 Iconify 元数据 / When 资源被构建加载 / Then 不新增备用图片、不吞掉资源加载失败、不修改后端或租户数据。

RED: `node tests/e2e/layout-logo-use-home-icon-static.spec.js` -> FAIL，原因：`sidebar-brand-logo.svg` 仍是旧 `64x64` 品牌图，不含 `ep:home-filled` 的 `1024x1024` viewBox。

GREEN: `node tests/e2e/layout-logo-use-home-icon-static.spec.js` -> PASS。

GREEN: `node tests/e2e/layout-logo-icon-contain-static.spec.js` -> PASS。

GREEN: `pnpm ts:check` -> PASS。

GREEN: `git diff --check` -> PASS，仅提示 `src/assets/imgs/sidebar-brand-logo.svg` 后续 Git 触碰时 LF 会替换为 CRLF。

GREEN: Playwright 本机真实登录页面验证 -> PASS，页面 `logo-icon-frame=40x40`、`img=40x40`、`objectFit=contain`，服务端返回的 SVG 含 `ep:home-filled` path 和 `fill="#009688"`。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-layout-logo-use-home-icon/frontend-feature-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-logo-use-home-icon --mode preview` -> PASS，未发现待删除文件、阻塞或警告。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-logo-use-home-icon --mode apply` -> PASS，未删除文件。

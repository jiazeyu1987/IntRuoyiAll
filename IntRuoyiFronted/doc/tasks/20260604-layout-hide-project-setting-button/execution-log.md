# 执行日志：隐藏右侧浮动项目配置按钮

BDD: 右侧浮动项目配置按钮不再显示 -> Given 用户登录后台进入任意页面 / When 全局布局渲染完成 / Then 页面右侧不显示固定齿轮按钮，也不能打开“项目配置”抽屉。

BDD: 隐藏项目配置不影响主体布局 -> Given 用户登录后台进入任意页面 / When 全局布局渲染完成 / Then 菜单、页面内容、返回顶部入口和移动端遮罩逻辑仍保留。

BDD: 删除全局入口不引入 fallback -> Given 用户使用测试租户登录 / When 页面渲染 / Then 本次变更不新增后端请求、不修改租户数据、不通过 CSS 隐藏或异常吞噬绕过。

RED: `node tests/e2e/layout-hide-project-setting-button-static.spec.js` -> FAIL，原因：`Layout.vue` 仍存在 `import { Setting } from '@/layout/components/Setting'`。

GREEN: `node tests/e2e/layout-hide-project-setting-button-static.spec.js` -> PASS。

GREEN: `pnpm ts:check` -> PASS。

GREEN: `git diff --check` -> PASS，仅提示 `src/layout/Layout.vue` 后续 Git 触碰时 LF 会替换为 CRLF。

GREEN: Playwright 本机只读登录 DOM 验证 -> PASS，登录后 `http://127.0.0.1:8081/index` 未渲染 `.v-setting`，页面可见文本不包含“项目配置”。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-layout-hide-project-setting-button/frontend-feature-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-hide-project-setting-button --mode preview` -> PASS，未发现待删除文件、阻塞或警告。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-layout-hide-project-setting-button --mode apply` -> PASS，未删除文件。

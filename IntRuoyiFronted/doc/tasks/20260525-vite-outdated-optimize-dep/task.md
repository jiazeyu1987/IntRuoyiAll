# 任务：修复 Vite Outdated Optimize Dep 导航失败

## 任务目标

- 修复本地前端 `http://localhost:8081` 动态进入运行控制台相关路由时，Element Plus 懒加载样式依赖触发 `504 (Outdated Optimize Dep)`，导致 Vue Router 动态导入页面失败的问题。
- 不通过 mock、静默降级或隐藏异常处理问题；应从 Vite 依赖优化配置或实际导入链路上消除冷启动失配。

## BDD 场景

- BDD: 冷启动后进入运行控制台页面不因 Element Plus 样式依赖优化失效中断 -> Given 前端开发服务使用空的 Vite optimized deps 缓存启动, When 用户从管理端路由进入运行控制台页面并加载包含 `DatePicker`、`Tree` 的懒加载页面, Then 动态页面模块加载成功，浏览器控制台没有 `Outdated Optimize Dep` 或 `Failed to fetch dynamically imported module`。

## 里程碑

- [x] M1: 复现并定位触发 `element-plus` 样式依赖重新优化的导入链路。
- [x] M2: 增加最小可维护修复和回归测试。
- [x] M3: 运行目标验证并记录 RED/GREEN 证据。
- [x] M4: 完成任务文档、清理预览和独立提交。

## 预期验证

- RED：新增回归测试前，目标 Element Plus 懒加载样式依赖未被 Vite 显式预优化覆盖。
- GREEN：目标回归测试通过。
- GREEN：前端开发服务冷启动后加载目标页面不再出现 `504 (Outdated Optimize Dep)`。

## 当前状态

- 状态：completed
- 已完成：
  - 已定位到 `system/nas/index.vue` 变换后注入 `base`、`loading`、`divider`、`message-box` 等 Element Plus 样式入口，其中部分入口不在 Vite `optimizeDeps.include` 内，冷启动懒加载时会触发 optimizer 重新生成 browser hash。
  - 已新增 `tests/e2e/vite-element-plus-optimize-deps.spec.js` 回归测试。
  - 已将缺失的 Element Plus 样式入口加入 `build/vite/optimize.ts`。
  - 已用临时 `http://127.0.0.1:19081` 冷启动开发服务验证 `/system/nas` 页面无 `Outdated Optimize Dep` 或动态导入失败。
  - 已完成 task-closeout-cleanup 预览与清理。
- 阻塞与影响：暂无。

## 最终验证结果

- `node tests\e2e\vite-element-plus-optimize-deps.spec.js` -> PASS。
- `curl` 检查 `base`、`loading`、`date-picker`、`tree`、`divider`、`message-box` 六个 optimized dep URL -> 全部 HTTP 200，且使用同一 browser hash `ecae0dbb`。
- Browser 验证 `http://127.0.0.1:19081/system/nas` -> PASS，`NAS 管理` 可见，相关错误日志数量为 0。
- `task_closeout.py --mode preview/apply` -> PASS，仅清理本任务临时 Vite 日志。
- 收尾后重新拉起 `http://127.0.0.1:19081` -> PASS，六个目标 optimized dep URL 全部 HTTP 200，当前 browser hash 为 `c41c38ab`。

## Current Status

completed

# Bug Regression Evidence

## Bug Summary

文控中心 > 文控日志页面加载后展示重复“系统异常”。截图显示列表区域错误条与顶部 toast 同时出现；当前页面 catch 已有 `loadError` 和 `message.error`，但 API wrapper 未关闭 Axios 全局错误提示。

## Expected Behavior

文控日志分页接口应稳定返回正式日志分页数据；如果主查询确实失败，错误应由文控日志页面统一归属展示，不得吞异常、默认成功、返回 mock/placeholder 数据，也不得由 Axios 与页面重复弹出同一“系统异常”。

## Reproduction

- `node tests/e2e/dcc-controlled-file-logs-static.spec.js`：新增重复错误提示静态断言后，旧实现 RED。
- 本机 API smoke：真实租户查找、登录和 `/dcc/controlled-file-logs/page?pageNo=1&pageSize=10` 返回业务码 `0`，说明当前本机数据链路可正常返回日志分页。

## Root Cause

`getControlledFileLogPage` 调用未设置 `ignoreErrorMessage: true`。当后端返回业务错误时，Axios response interceptor 会先弹出全局错误提示；随后文控日志页面 `catch` 又设置 `loadError` 并调用 `message.error(errorMessage)`，造成截图中的重复“系统异常”。修复不吞异常，仍由页面展示真实错误文本。

## Regression Test

更新 `IntRuoyiFronted/tests/e2e/dcc-controlled-file-logs-static.spec.js`，断言文控日志 API 请求必须包含 `ignoreErrorMessage: true`，避免全局 Axios toast 与页面错误提示重复。

## RED:

`node tests/e2e/dcc-controlled-file-logs-static.spec.js` -> FAIL，缺少 `ignoreErrorMessage: true`。

## GREEN:

`node tests/e2e/dcc-controlled-file-logs-static.spec.js` -> PASS。
`pnpm ts:check` -> PASS。
本机 API smoke -> PASS，日志分页返回 `total=6776`、`list=10`。

## Risk And Regression Scope

- DCC 文控日志统一分页接口。
- 文控日志前端错误展示。
- Axios 全局错误提示归属。
- 关联日志来源：访问审计、生命周期、分发、项目代码修正、培训执行。

## Verification

- 静态合同、前端类型检查和本机 API smoke 已通过。
- 后端目标 JUnit 与真实页面 E2E 的阻塞原因已单独记录。

## Blockers

- 后端目标 JUnit 目前被非本任务 DCC 编译前置阻塞。
- 真实页面 E2E 目前被本机 Playwright Chromium 缓存缺失阻塞。

## Follow-Up

- 后端目标 JUnit 目前被非本任务 DCC 编译前置阻塞。
- 真实页面 E2E 目前被本机 Playwright Chromium 缓存缺失阻塞。

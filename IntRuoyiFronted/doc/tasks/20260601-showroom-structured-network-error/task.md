# 任务：展厅结构化网络错误提示

## 任务目标

- 当展厅发布等操作遇到浏览器未收到后端响应的 `Network Error` 时，前端错误提示必须显示稳定错误码、请求目标和排查方向。
- 保持已有后端结构化错误格式不变。

## BDD 场景

- BDD: 网络无响应错误提示 -> Given 浏览器请求展厅接口且没有收到后端响应 / When 前端格式化错误 / Then 提示包含 `NETWORK_RESPONSE_UNAVAILABLE`、请求方法与 URL、后端服务/网络/CORS/反向代理排查方向和原始错误。
- BDD: 后端结构化错误保持原格式 -> Given 后端返回已有结构化错误信息 / When 前端格式化错误 / Then 仍显示后端错误码、目标、资源和接口信息。

## 里程碑

- [x] M1：建立任务文档和 BDD 场景。
- [x] M2：补充结构化网络错误回归测试。
- [x] M3：实现网络无响应分支并保持后端结构化错误格式。
- [x] M4：运行目标验证。
- [x] M5：提交任务改动。

## 预期验证

- `node --check scripts\showroom-structured-network-error.test.mjs`
- `node scripts\showroom-structured-network-error.test.mjs`

## 当前状态

status: completed

## Current Status

completed

## 最终验证

- `node --check scripts\showroom-structured-network-error.test.mjs` -> PASS。
- `node scripts\showroom-structured-network-error.test.mjs` -> PASS，2 passed。

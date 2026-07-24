# 任务：MES 应用重排确认链路超时修复

## 任务目标
- 修复点击“确认应用重排”后弹出 `接口请求超时，请刷新页面重试！ timeout of 30000ms exceeded` 的问题。
- 手动重排确认链路中的排产前检查、重排预览、最终应用都使用同一个手动重排专用长超时。
- 不修改全局 axios 默认超时，不影响普通接口。

## 经验门禁
- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文文档与命令输出使用显式 UTF-8。
- 登录 / 租户 / Playwright E2E：已读取 `docs/login-access.md`；真实 E2E 已先跑官方登录前置并通过。
- 前端页面 / 表格 / 样式：已按经验索引命中前端任务；本次只改接口超时契约与测试，不做视觉重设计。
- Bug 回归：按 `bug-regression-fix-loop` 要求先记录复现证据、补 RED 回归断言，再做最小修复。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；根因是确认应用链路中的 preflight / replan preview 未使用长耗时接口超时配置。
- `是否存在临时补丁或绕过`：否。

## BDD 场景
- `BDD: 应用重排确认链路长耗时不被 30 秒前端超时截断 -> Given 用户在手动重排弹框选择今天或明天 / When 点击确认应用重排 / Then 前端对 preflight、replan preview、replan apply 均使用手动重排专用长超时。`
- `BDD: 全局接口超时不被放大 -> Given 其他普通接口请求 / When 发起请求 / Then 仍使用全局 30000ms 默认超时。`

## 里程碑
1. M1：已补充 RED 静态契约，证明 preflight 和 replan preview 必须带手动重排长超时。
2. M2：已实现最小前端修复，不修改全局 axios 默认超时。
3. M3：已运行静态契约、排产页面类型检查、官方登录前置和真实手动重排 E2E。
4. M4：已更新执行证据；本任务相关改动进入单独提交。

## 当前状态
- completed：截图中的 30000ms 超时根因已修复，手动重排确认链路 preflight、preview、apply 均使用 `REPLAN_REQUEST_TIMEOUT = 180000`。
- 最终验证：静态契约、脚本语法检查、排产页面类型检查、官方登录前置、真实 Playwright E2E、数据库只读核对均通过。

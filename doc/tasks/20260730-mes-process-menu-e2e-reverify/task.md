# MES 工序菜单 E2E 独立复验

## Task Goal

- 独立复验 `MES工序` 菜单改名在当前 `int_main` 本机运行态是否真实生效。
- 不复用上一任务的 PASS 结论，重新执行登录、菜单、搜索、页面、接口和只读性断言。

## Milestones

1. 核对 E2E 规则、登录来源、端口与浏览器前置。
2. 运行官方登录前置。
3. 使用 Playwright 走真实页面路径并采集证据。
4. 独立判断 PASS / FAIL / BLOCKED。
5. 清理本次任务自有临时脚本和截图，完成任务记录。

## Expected Verification

- 前端 `http://127.0.0.1:8081` HTTP 200。
- 后端 `http://127.0.0.1:48081/actuator/health` 为 `UP`。
- 使用 `芋道源码/admin` 通过官方登录前置。
- 真实展开 `MES 系统 > 生产管理`，可见 `MES工序`，不可见 `标准模板列表`。
- 顶部搜索 `mes工序` 命中 `/mes/pro/mes-process`。
- 页面资源接口 HTTP 200、业务码 0。
- 页面无 `系统异常`、无浏览器 page error、无 MES 写请求。

## Applicable Gates

- 动态菜单页签重命名门禁：必须验证真实登录态的可见菜单、顶部搜索结果和页面可见标题，不以隐藏重复 DOM 或可能隐藏的 `doc-alert` 代替。
- Playwright 浏览器可执行文件门禁：使用本机 Google Chrome，并通过 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` 显式指定。
- 官方登录前置与 admin-only 全量验证门禁：先运行 `scripts/preflight/login-preflight.mjs`，身份为 `芋道源码/admin`，本任务只读且不写入 MES 数据。
- Element Plus 下拉选择门禁：顶部搜索框按可见 `input.el-select__input[role="combobox"]` 定位。
- 只读资源池引用完整性门禁：资源接口必须 HTTP 200、业务码 0，页面不得出现 `系统异常`。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务只做真实运行态独立验收。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- output/playwright/20260730-mes-process-menu-e2e-reverify/

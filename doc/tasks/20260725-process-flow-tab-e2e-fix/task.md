# 20260725-process-flow-tab-e2e-fix

## Task Goal

使用真实数据 E2E，以本机授权身份 `芋道源码/admin` 访问 `工艺流程` 页签，复现并修复访问过程中遇到的问题。

## Milestones

1. 建立任务记录并完成规则门禁读取。✅
2. 使用真实前端路径登录并定位 `工艺流程` 页签访问问题。✅
3. 补充失败回归用例并实施最小正式修复。✅
4. 运行目标验证与真实路径 E2E 复验。✅
5. 更新验证报告并完成收尾。✅

## Expected Verification

- Playwright 真实用户路径：登录 `芋道源码/admin`，访问 `工艺流程` 页签并确认页面可正常打开。
- 针对根因的真实 E2E 回归测试：先 RED 后 GREEN。
- 相关静态检查或构建验证通过。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，新增任务自有真实 E2E 覆盖本机 `int_batch` 的 `芋道源码/admin -> 工艺流程` 访问路径，并将断言收敛到本机/API 访问失败；第三方统计与 Iconify 外部 abort 仅记录为外部证据，不作为本机业务访问失败。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 命中真实 E2E、登录租户、分支运行端口与 PowerShell UTF-8 门禁。
- `int_batch` 本机入口使用 `http://127.0.0.1:8041`，后端使用 `http://127.0.0.1:48041`。
- E2E 使用真实前端页面，不用 API-only 代替页面路径；API 响应仅作为页面路径中的辅助证据。
- 命令和任务日志不记录密码明文。

## Cleanup Keep

- `output/playwright/20260725-process-flow-tab-e2e-fix/process-flow-admin-tab-result.json`
- `output/playwright/20260725-process-flow-tab-e2e-fix/process-flow-admin-tab.png`
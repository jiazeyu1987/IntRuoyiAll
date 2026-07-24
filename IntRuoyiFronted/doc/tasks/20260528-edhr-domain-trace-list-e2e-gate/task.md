# eDHR 主数据追溯列表真实 E2E 门禁

## Task Goal

把 eDHR 主数据追溯列表页纳入真实 Playwright E2E 放行证据。现有脚本已经覆盖详情页、前端触发校验和 UI/API 一致性，本任务补齐列表页真实用户路径：测试租户用户必须先打开 `/mes/pro/feedback/edhr-domain-trace`，通过真实分页接口看到目标执行记录，再从列表进入详情并继续执行现有详情校验。

该任务不得使用 mock、API 替代前端路径、静默跳过或 fallback。缺少前端入口、测试租户、真实账号、目标执行记录、Playwright runtime 或后端真实数据时必须 fail fast，并记录影响。

## Milestones

- [completed] M1: 创建任务文档与 BDD/TDD 计划。
- [completed] M2: RED 合同测试证明当前真实 E2E 脚本缺少列表页路径、分页接口等待和列表证据。
- [completed] M3: 子 agent 实现最小 GREEN：列表页真实打开、分页响应解析、目标行断言、列表截图和从列表进入详情。
- [completed] M4: 主 reviewer 运行静态合同、语法检查、真实 E2E，并审查无副作用。
- [completed] M5: 独立 reviewer 复审通过，执行收尾预览，当前任务改动进入收尾提交。

## BDD

BDD: 主数据追溯列表可查询 -> Given 测试租户存在真实 eDHR 执行记录和主数据追溯结果, When 执行人通过前端打开 `/mes/pro/feedback/edhr-domain-trace` 并按执行编号查询, Then 页面展示目标执行编号、追溯状态、domainTraceHash、blockerCount 和 itemCount。

BDD: 主数据追溯列表进入详情 -> Given 目标执行记录已经出现在主数据追溯列表, When 用户点击列表中的执行编号或详情入口, Then 前端进入 `/mes/pro/feedback/edhr-domain-trace/detail` 并继续展示该执行记录的 canonical 详情证据。

BDD: 主数据追溯详情校验保持有效 -> Given 用户已经从列表进入详情页, When 用户触发主数据追溯校验, Then 前端仍然发起真实 `/domain-trace/verify` 请求，最终 `status`、`domainTraceHash`、`blockers[]` 和 `items[]` 与后端详情 API 一致。

BDD: 主数据追溯列表 E2E 缺前置即阻塞 -> Given 缺少真实前端入口、测试租户、账号、执行记录、分页接口响应或 Playwright runtime, When E2E 启动或打开列表页, Then 脚本必须 fail fast 写入 evidence markdown，不得使用 mock、API 替代列表路径或 silent downgrade。

## Expected Verification

- `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs`
- `pnpm e2e:edhr:domain-trace:check`
- `$env:EDHR_E2E_BASE_URL='http://localhost:8081'; $env:EDHR_E2E_TENANT='测试租户'; $env:EDHR_E2E_EXECUTOR_USERNAME='aoteman'; $env:EDHR_E2E_EXECUTOR_PASSWORD='admin123'; $env:EDHR_E2E_DOMAIN_TRACE_EXECUTION_ID='40'; $env:EDHR_E2E_DOMAIN_TRACE_EXECUTION_CODE='BRE202605280518101280040'; $env:EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS='VERIFIED'; $env:EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT='0'; $env:EDHR_E2E_TASK_ID='20260528-edhr-domain-trace-list-e2e-gate'; pnpm e2e:edhr:domain-trace`
- `git diff --check`

## Current Status

Completed. Reviewer real E2E completed after the backend `/domain-trace/page` contract repair and again after restarting the current rebuilt backend jar on `48098`. The list path opens `/mes/pro/feedback/edhr-domain-trace`, waits for the real page API, finds execution `40 / BRE202605280518101280040`, records the list screenshot, enters detail from the list, triggers real verification, and finishes with `status=VERIFIED`, `blockerCount=0`, `itemCount=8`, `domainTraceHash=2c7c5aa13178e7c452697672e86ca1efa2c22ca00ada1e2ff22da0e19dd72a79`. Independent reviewer review passed.

## Reviewer Notes

- 放行条件不是“脚本能跑”，而是列表页、列表到详情、详情校验三段真实用户路径都有证据。
- 列表页断言必须来自真实前端页面和 `/domain-trace/page` 响应；接口只能作为已登录页面上下文的佐证，不得绕过列表 UI。
- 当前切片只允许修改主数据追溯 E2E/合同测试和本任务文档，不做无关 UI 或业务逻辑重构。

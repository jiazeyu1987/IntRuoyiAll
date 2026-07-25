# 20260725 jiluben 融合后真实 E2E 验证

## Task Goal

在 `jiluben_20260722_clean` 残留融合已推送到 `int_main` 后，使用本机 `int_main` 前端 `8081` 和后端 `48081` 运行真实用户路径 E2E 验证，复现并修复验证过程中发现的问题，确保记录本/eDHR/批记录相关路径可用。

## Milestones

- [x] M1: 读取 E2E、登录、本地运行态、worktree、端口、PowerShell、任务收尾规则和 Playwright/bug fix 技能。
- [ ] M2: 确认本机前后端运行态、登录入口和可执行 E2E 脚本前置条件。
- [ ] M3: 运行融合后真实路径 E2E，记录 RED 失败证据。
- [ ] M4: 对 E2E 失败做最小正式修复，补充或更新回归测试。
- [ ] M5: 复跑 E2E/静态/类型/端口守卫，完成 cleanup、提交和推送。

## Expected Verification

- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`，且 `48081` PID 属于 `E:\IntRuoyi\IntRuoyiBackend`。
- `http://127.0.0.1:8081/login?redirect=/index` 可访问，且前端配置指向 `48081`。
- 至少运行一条覆盖融合范围的真实 Playwright E2E；若写入型 E2E 前置环境变量缺失，按 fail-fast 记录 blocker，不以 API-only 替代。
- 如果出现产品缺陷，先记录 RED，再做最小修复并用目标 E2E 或回归测试 GREEN 证明。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过，`git diff --check` 无空白错误。

## BDD Scenarios

- BDD: 融合后 eDHR 详情真实页面可访问 -> Given `int_main` 前后端在 8081/48081 运行, When 用户以本机默认测试身份进入 eDHR/记录本相关页面, Then 页面应加载真实接口数据且不出现融合后的前端运行时错误。
- BDD: 记录本写入型 E2E 前置条件 fail-fast -> Given 写入型 E2E 需要任务专用环境变量和测试数据, When 任一必要变量缺失, Then 脚本必须阻塞并记录缺失前置条件，不覆盖历史证据或改用 API-only。
- BDD: 验证失败最小正式修复 -> Given 真实 E2E 暴露融合后缺陷, When 修复代码, Then 必须有 RED/GREEN 证据证明缺陷闭合且无 fallback/降级。

## Experience Gates

- 本地后端数据库凭据门禁：若 `48081` 未监听或日志出现 `dynamic-datasource create datasource named [master] error` / `Access denied for user 'root'@'localhost'`，停止后端成功结论，不改端口、不切换数据源，修复前置后再验证 health。
- 任务专用 E2E 环境变量门禁：运行 `edhr-batch-execution-real-flow.e2e.js` 或写入型 E2E 前，显式设置 `EDHR_BATCH_E2E_TASK_ID` 或 `EDHR_BATCH_E2E_EVIDENCE_FILE`，缺少账号、工单、批次、路线、签名密码等变量时 fail-fast。
- Element Plus 表格/下拉门禁：真实路径选择必须按可见业务唯一文本定位，不用数组下标、坐标点击、API-only 或表头全选绕过。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按真实 E2E 暴露的问题定位修复，不用默认成功或 mock 数据替代。
- `是否存在临时补丁或绕过`：否。
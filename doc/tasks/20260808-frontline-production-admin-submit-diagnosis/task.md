# 一线生产 admin 模拟提交失败诊断

## Task Goal

使用本机 `芋道源码/admin` 真实一线生产页面路径模拟一次正式提交，分析 `提交设备/工作站上下文与授权工序不一致` 的具体失败原因，重点核对提交载荷中的设备/工作站上下文与后端授权候选来源。

## Milestones

- [x] M1: 读取任务、登录、本机运行态、E2E、编码和经验门禁。
- [x] M2: 确认本机前端 `8081`、后端 `48081` 和登录态可用。
- [x] M3: 通过真实页面使用 `芋道源码/admin` 进入一线生产并尝试模拟提交。
- [x] M4: 采集失败接口、页面选择、运行态配置和授权候选证据。
- [x] M5: 输出具体根因、影响范围和验证结论。

## Expected Verification

- 使用 Playwright 操作真实前端页面，不用 API-only 代替模拟提交。
- 记录目标写接口 `/admin-api/mes/pro/feedback/frontline/submit` 的请求/响应、提交载荷关键字段和页面当前选择。
- 只读辅助核对 `/device-account/processes` 与 `/device-account/runtime-config` 返回的设备/工作站上下文。
- 如运行态、账号、签名密码或任务数据缺失，明确记录 blocker 和影响。

## Current Status

completed

已使用 `芋道源码/admin` 通过真实前端页面路径完成一次一线生产正式提交模拟，后端返回业务错误 `1040760111`，错误文案为“提交设备/工作站上下文与授权工序不一致，submittedDeviceId=980009, submittedWorkstationId=980010, expectedDeviceId=41, expectedWorkstationId=980010”。根因是 route-start 生产组长授权候选使用工作站正式设备 `mes_dv_machinery.id=41`，而运行态设备卡片与提交载荷取班组设备 `mes_process_pool_team_device.id=980009`，正式提交授权比较时直接比较了两个不同 ID 域。

最终验证：Playwright 真实页面模拟完成；cleanup preview/apply 均通过，无删除项、无阻塞；核心证据保留在 `verification-report.md`、`frontline-admin-submit-diagnosis-result.json` 和 `execution-log.md`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务先诊断正式链路根因，不通过默认设备、API-only 或临时绕过掩盖失败。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 命中 `docs/backend-development.md#一线运行态 route-start 生产组长来源必须独立于班组设备绑定`：必须区分 `ROUTE_START_PRODUCTION_LEADER` 候选来源与班组设备映射来源，工作站正式设备 ID 不等于班组维护设备 ID。
- 命中 `docs/backend-development.md#一线生产正式提交必须单事务落链并按唯一组长归属可见`：正式提交必须在后端写入前校验运行态候选、设备上下文、选择员工、签名和参数，且前端失败后保留输入不显示成功。
- 命中 `docs/e2e-rules.md#Playwright 目标链路与外部资源异常归因门禁`：本次只把 `/frontline/submit`、`/device-account/processes`、`/runtime-config`、`/switch-employee` 作为目标链路；被浏览器中止的工作台背景请求记录为非目标异常。
- 已将本次“正式提交比较不同设备 ID 域”的可复用经验合并到 `docs/backend-development.md` 既有门禁。

## Cleanup Keep

- `doc/tasks/20260808-frontline-production-admin-submit-diagnosis/frontline-admin-submit-diagnosis.mjs`
- `doc/tasks/20260808-frontline-production-admin-submit-diagnosis/frontline-admin-submit-diagnosis-result.json`

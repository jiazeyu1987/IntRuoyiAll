# eDHR 追踪与签名真实路径 E2E Evidence

- Task ID: `20260529-edhr-tracking-signature-real-e2e-gate`
- 生成时间：2026-06-08T05:51:48.223Z
- 前端 worktree：D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3
- 固定前端入口：`http://localhost:8081`
- 固定测试租户：`测试租户`
- 默认账号名：`aoteman`；密码由 `EDHR_TRACKING_SIGNATURE_PASSWORD` 注入，不写入仓库证据。
- 真实 E2E 复跑命令：`pnpm e2e:edhr:tracking-signature`
- 静态语法检查命令：`pnpm e2e:edhr:tracking-signature:check`
- 证据文件：默认写入本任务目录 `doc/tasks/20260529-edhr-tracking-signature-real-e2e-gate/real-e2e-evidence.md`。
- 临时产物目录：`test-results/edhr-tracking-signature/`（截图、trace、result.json 不提交）
- 当前状态：PASS
- executionId：`40`
- executionCode：`BRE202605280518101280040`
- batchCode：`EDHR-BATCH-122-E2E-APPROVE-GATE05280525`

## BDD

- BDD: 追踪页按真实执行编号筛选 -> Given 测试租户存在真实 eDHR 执行记录、追踪事件和动态菜单 `eDHR追踪` / When 用户登录并打开 `/mes/pro/feedback/edhr-tracking?executionCode=<real-code>` / Then 前端请求真实 `/mes/pro/batch-record-execution/tracking-page`，页面展示执行编号、工单号、批次号、当前状态、最后事件、意见/原因、最后处理时间和归档状态。
- BDD: 追踪页进入真实执行详情 -> Given 追踪页列表展示目标执行记录 / When 用户点击该行“查看” / Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>`，详情页请求真实 `/tracking-timeline`，展示同一执行编号与提交、审批或归档时间线证据。
- BDD: 签名页按真实执行筛选 -> Given 测试租户存在真实 eDHR 电子签名记录和动态菜单 `eDHR签名记录` / When 用户打开 `/mes/pro/feedback/edhr-signatures?executionId=<real-id>` / Then 前端请求真实 `/mes/pro/batch-record-execution/signature-page`，页面展示签名编号、执行编号、动作、签名含义、签名人、签名方式、密码校验、流程任务、签名时间和意见/原因。
- BDD: 签名页动作筛选真实有效 -> Given 目标执行记录存在 SUBMIT、APPROVE 或 ARCHIVE_SEAL 等真实签名动作 / When 用户在动作筛选中选择真实动作并查询 / Then API 查询参数包含对应 `actionType`，且目标响应 rows 全部匹配该动作。
- BDD: 缺少真实前置即阻塞 -> Given 缺少测试租户密码、真实执行记录、追踪事件、签名记录、菜单权限或前端入口 / When 执行 E2E / Then 脚本写入 `BLOCKED/FAIL` 证据并退出非零，不使用模拟数据、API-only 或降级路径。

## GREEN

- GREEN: `pnpm e2e:edhr:tracking-signature` -> PASS, 真实追踪筛选、详情时间线、签名查询和动作筛选已完成。
- 追踪页目标执行记录可见 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-tracking-signature\01-tracking-page.png`
  - tracking: executionCode=BRE202605280518101280040, workOrderCode=EDHR-MO-122-E2E-APPROVE-GATE05280525, batchCode=EDHR-BATCH-122-E2E-APPROVE-GATE05280525, status=3, lastEventType=ARCHIVE_SEAL, lastEventAt=1779916719000, archiveStatus=SEALED
- 追踪页查看进入详情时间线 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-tracking-signature\02-tracking-detail-timeline.png`
  - timeline: eventType=SUBMIT, actionType=SUBMIT, actorName=芋道1, occurredAt=1779916697000
- 签名页目标签名记录可见 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-tracking-signature\03-signature-page.png`
  - signature: actionTypes=APPROVE,ARCHIVE_SEAL,FIELD_CHANGE,SUBMIT, actors=芋道1, selectedAction=--, rowCount=4
- 签名页 actionType 筛选真实有效 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-tracking-signature\04-signature-action-filter.png`
  - signature: actionTypes=ARCHIVE_SEAL, actors=芋道1, selectedAction=ARCHIVE_SEAL, rowCount=1
- Trace: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-tracking-signature\trace.zip`


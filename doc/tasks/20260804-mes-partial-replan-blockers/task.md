# 自动重排逐工单阻断可视化

## Task Goal

自动重排遇到可归因到单个工单的阻断时，不再中止整批重排；没有阻断的工单继续完成重排，有阻断的工单保留任务并标红，同时可查看阻断原因。全局前置错误、无法归因错误和日历 token 等应用门禁仍必须 fail fast。

## Milestones

- [x] 建立 BDD/TDD 证据，复现“任一阻断导致整批不应用”的当前缺陷。
- [x] 后端按工单区分可应用与阻断范围，持久化阻断 issue 并返回汇总字段。
- [x] 排产工单列表接口返回阻断数量和最新原因，供前端标红展示。
- [x] 前端允许部分应用、展示阻断工单红色状态与原因。
- [ ] 运行目标后端、前端静态合同和类型检查验证，并记录证据。
- [ ] 收尾前执行经验沉淀、清理预览/应用、提交并推送。

## Expected Verification

- 后端 RED/GREEN：目标 `MesProAutoScheduleAlgorithmContractTest` 覆盖混合可排/阻断工单的部分应用行为。
- 后端回归：重排应用、夜间重排和 issue 持久化相关目标测试通过。
- 前端 RED/GREEN：新增静态合同覆盖不因局部阻断禁用整批应用、阻断行标红和原因可见。
- 前端回归：相邻重排静态合同通过，`pnpm ts:check` 通过或记录与本任务无关的既有阻塞。
- 真实页面 E2E：`mes-pro-schedule-order-partial-replan-blockers-real-fixture.e2e.js` 使用用户明确授权的 `芋道源码/admin`，通过真实页面创建任务自有阻断 issue，验证阻断行红色状态和原因，再通过真实页面关闭并核对清理；缺少样本时只允许任务自有 fixture，不写入非任务自有工单。
- 真实页面全选应用 E2E：`mes-pro-schedule-order-full-select-replan-admin-real.e2e.js` 使用 `芋道源码/admin`，在排产工单页逐行勾选当前页可选排产工单，点击“手动重排 -> 开始重排 -> 确认应用重排”，断言发出 `preflight / preview / apply` 三段真实请求，`apply` 业务码为 `0`，不存在“未参与排产”二次阻塞确认框，进度不再停在 90%，成功后日期弹窗关闭。
- Evidence validators：bug、backend、frontend evidence 通过。

## Applicable Gates

- `docs/backend-development.md`：MES 排产算法与应用落库必须保留预览/应用重算、日历 token、issue 暴露和无静默降级。
- `IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md`：应用重排必须重新计算并显式处理阻断 issue；缺路线、缺日历、缺产能等仍不能默认成功。
- `docs/frontend-development.md`：前端请求失败、后端阻断和页面状态必须可见，不得吞异常或静默成功。
- `docs/e2e-rules.md`：真实用户路径不可被 API-only 替代；本任务先以静态合同锁定 UI 行为。
- `docs/e2e-rules.md#MES 手动重排全选应用完成门禁`：用户确认开始日期后必须观察真实 `preflight / preview / apply` 三段请求并等待 UI 完成收敛；不得把夹具红行、预览或中间进度当作全选应用 E2E 通过。
- `docs/powershell-memory.md`：脏工作区先独立基线提交，Maven `-D` 参数整体加引号，逐条记录测试退出码。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务把“逐工单阻断不中止整批”建模为正式业务行为；全局/不可归因阻断仍 fail fast。
- `是否从根因和长期维护角度解决`：是。后端计算、应用、issue 持久化、列表响应和前端展示统一按正式阻断 issue 链路处理。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

实现、前端验证和用户授权的 `芋道源码/admin` 真实页面 E2E 已完成。2026-08-05 针对用户截图中“确认应用重排后卡在 90%”重新补充全选应用 E2E：逐行勾选当前页 12 条可选排产工单，开始日期 `2026-08-06`，真实发出 `POST /admin-api/mes/pro/schedule-order/preflight`、`POST /admin-api/mes/pro/auto-schedule/replan/preview`、`POST /admin-api/mes/pro/auto-schedule/replan/apply`，`apply` 返回业务码 `0`，汇总为应用工单 11 个、标记阻断 1 个、跳过 0 个、新增任务 490 个、删除任务 490 个、保留任务 6 个；最终快照显示日期确认弹窗关闭、未出现“存在未参与排产的工单”阻塞确认框、进度未停留 90%。同时，旧的二次确认阻塞逻辑已由非阻塞通知替代。此前 Fixture E2E 创建 task-owned issue `19256`，验证排产工单 `SCH-881MO098538-20260707-0001` / 来源工单 `881MO098538` 标红、原因可见、红色背景 `rgb(255, 241, 240)`，并通过页面关闭异常；最终只读核验确认 task marker issue `19252, 19253, 19254, 19255, 19256` 未关闭数量为 `0`。后端最终 JUnit 已于 2026-08-05 复跑，但在目标 Surefire 启动前被无关 `MesQaInspectionRegulationServiceTest` / `MesQaInspectionRegulationProjectStatusRespVO` getter 不匹配的 `testCompile` 错误阻塞。代码与部分测试已被共享分支并发基线提交吸收，收尾提交/推送暂未完成。
2026-08-05 追加用户体验收口：非阻塞通知已简化为只显示 `工单：<code>；原因：<blocked reason>`，不再显示产品编号、产品名称或其它细节；聚焦静态合同、相邻重排合同、真实 E2E 脚本语法检查和 `pnpm.cmd ts:check` 均通过。

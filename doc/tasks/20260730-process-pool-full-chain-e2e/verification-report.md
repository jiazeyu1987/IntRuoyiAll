# 工序池完整一线闭环 E2E 验证报告

## Result

BLOCKED

## Scope

本报告验证是否可以立即针对以下完整路径运行真实 Playwright E2E：

报工入口 -> 设备账号工艺路线/工序切换 -> 员工切换 -> UI 模板切换 -> 生产/PQC 填写 -> 电子签名 -> 一体提交 -> 报工/记录本/工序池落库 -> FIFO 分配 -> 审核副本 -> 原始记录修改限制 -> 工序池时间线查看。

## Preflight Evidence

- `npx --version` -> PASS, `11.6.2`。
- `IntRuoyiFronted/package.json` -> `test:e2e` 存在，命令为 `playwright test`。
- `IntRuoyiFronted/tests/e2e` 当前只有 `process-pool-review-copy-and-revision.spec.ts` 等 F5/F6 局部 E2E 和静态合同，未发现完整链路 spec。
- `rg "frontlineSubmit|switchFrontlineActualEmployee|loadFrontlineDeviceProcesses" IntRuoyiFronted/src/views/mes/pro/feedback IntRuoyiFronted/src/api/mes/pro/feedback` -> 仅 API 和未接入的 helper 中存在；报工页面组件没有调用。
- `python -X utf8 -` 静态探针 -> `FrontlineFixedTemplatePanel.vue` 中 `frontlineSubmit=False`、`switchFrontlineActualEmployee=False`、`loadFrontlineDeviceProcesses=False`；`feedback/index.vue` 中同样为 `False`。
- `rg "process-pool/timeline|TimelinePage" IntRuoyiFronted/src/router/modules/remaining.ts` -> 未命中；路由只登记 `review-copy` 和 `event-revision`。

## Blockers

- B1: 报工页面没有完整的一线提交动作。当前固定模板面板只支持“解析模板”和“校验 payload”，不能通过真实页面触发 `/mes/pro/feedback/frontline/submit`。
- B2: 报工页面没有接入设备账号可切换工序、员工候选和实际员工切换 UI，无法验证“设备账号内切换用户，不切换账号”。
- B3: 工序池时间线组件存在，但没有正式路由/菜单入口，无法通过真实页面验证每天谁提交了什么内容。
- B4: 在 B1-B3 解除前，测试租户、设备账号绑定、员工绑定、生产工单、电子签名身份和 FIFO 样本数据即使存在，也无法构成合规真实 E2E。

## Not Run

- 未运行完整路径 Playwright E2E。原因是正式页面入口前置条件缺失，项目规则禁止用 API-only、静态合同、mock 或直接 URL 绕过冒充真实 E2E。

## Release Impact

当前系统代码能力已经覆盖较多后端和局部页面能力，但还不能宣称“完整一线闭环 E2E 已通过”。进入试点前必须先补齐一线报工页面完整提交入口和工序池时间线页面入口，再准备任务自有测试数据并运行 full-chain Playwright。


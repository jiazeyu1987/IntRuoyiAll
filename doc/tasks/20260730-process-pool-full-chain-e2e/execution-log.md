# 工序池完整一线闭环 E2E 验证执行日志

## Context

- Task id: `20260730-process-pool-full-chain-e2e`
- User intent: 确认是否可以针对完整路径进行 E2E 验证，并执行真实一线闭环验收。
- Scope: 验证报工入口到工序池、FIFO、审核副本、原始记录修订限制和时间线的真实浏览器闭环。

## Rules And Skills Read

- Skill: `quality-assurance-test-suite`
- Skill: `playwright`
- `docs/task-closeout-rules.md`
- `docs/e2e-rules.md`
- `docs/login-access.md`
- `docs/local-runtime.md`
- `docs/worktree-restrictions.md`
- `docs/powershell-encoding.md`
- `docs/experience-index.md`
- Skill: `project-experience-consolidation`

## BDD Scenarios

- BDD: 一线报工写入工序池完整闭环 -> Given 设备账号绑定至少一条工艺路线且路线包含生产工序和 PQC 工序，并存在两个按计划开始时间排序的生产工单；When 操作者在真实报工入口切换路线、工序和员工后提交生产模板与 PQC 模板；Then 系统生成报工信息、记录本/工序池事件、唯一电子签名、服务端提交时间、PQC 记录，并按 FIFO 分配给先计划开始的生产工单。
- BDD: 员工和 UI 模板切换 -> Given 同一设备账号可切换当前工序绑定员工；When 在设备账号内切换到不同员工；Then 页面不重新登录账号，实际员工身份变化，模板 UI 按员工/工序配置切换，提交电子签名代表实际员工。
- BDD: FIFO 锁定与审核修订闭环 -> Given 工序池事件已经按 FIFO 分配给生产工单；When 审核人生成上下限审核副本并尝试修改已分配原始记录；Then 审核副本保留原始值和修正值，已分配字段修改被拒绝，未分配字段修改必须重新电子签名并留下日志。
- BDD: 时间线只读追溯 -> Given 多个模板提交事件已经进入工序池；When 用户打开工序池时间线；Then 页面按服务端提交时间展示每天谁提交了什么，包含生产/PQC、FIFO、审核副本和修订摘要，且不提供写入动作。

## Evidence

- Preflight: 当前 `git status --short --branch` 显示 `int_main...origin/int_main`，并存在未跟踪的 unrelated 目录 `doc/tasks/20260730-route-tenant-export-import-consistency/artifacts/`；本任务不混入该目录。
- Preflight: `npx --version` -> PASS, `11.6.2`。
- Preflight: `IntRuoyiFronted/package.json` 存在 `test:e2e` 脚本，命令为 `playwright test`。
- Preflight: `Get-ChildItem IntRuoyiFronted/tests/e2e -Filter '*process-pool*'` -> 仅发现 F5/F6 相关 `process-pool-review-copy-and-revision.spec.ts`、`process-pool-review-copy-and-revision-static.spec.js`、`process-pool-event-revision-api-static.spec.js`，未发现 full-chain spec。
- Preflight: `rg "frontlineSubmit|switchFrontlineActualEmployee|loadFrontlineDeviceProcesses" IntRuoyiFronted/src/views/mes/pro/feedback IntRuoyiFronted/src/api/mes/pro/feedback` -> `frontlineSubmit` 只存在于 API wrapper；员工切换 helper 存在于 `frontlineDeviceEmployeeContext.ts`，但未被报工页面调用。
- Preflight: `python -X utf8 -` 静态探针 -> `FrontlineFixedTemplatePanel.vue` 中 `frontlineSubmit=False`、`switchFrontlineActualEmployee=False`、`loadFrontlineDeviceProcesses=False`；`feedback/index.vue` 中同样为 `False`。
- Preflight: `rg "process-pool/timeline|TimelinePage" IntRuoyiFronted/src/router/modules/remaining.ts` -> 未命中；`remaining.ts` 当前只登记 `/mes/pro/process-pool/review-copy` 和 `/mes/pro/process-pool/event-revision`。

## Current Findings

- BLOCKED: 完整路径 spec 不存在，且当前真实页面入口不足以运行完整路径 E2E。
- BLOCKED: 报工入口没有调用 `/mes/pro/feedback/frontline/submit`，只能做模板解析和 payload 校验。
- BLOCKED: 报工入口没有接入设备账号工序切换和实际员工切换 UI。
- BLOCKED: 工序池时间线页面组件存在，但缺少路由/菜单入口。
- Not run: 未启动完整 Playwright E2E；按 `docs/e2e-rules.md`，前端入口缺失时不能用 API-only、静态合同或 mock 冒充真实 E2E。
- Experience consolidation: 已检查 `project-experience-consolidation`。本次经验已由 `docs/e2e-rules.md` 的 E2E 脚本入口存在性门禁覆盖，不新增长期经验文档。

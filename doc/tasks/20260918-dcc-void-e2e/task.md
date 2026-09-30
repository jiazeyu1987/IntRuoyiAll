# DCC 受控文件作废完整 E2E

## Current Status
completed

## Task Goal

使用 Playwright、真实前端页面、真实测试租户“芋道源码”和 `admin` 账号，完整验证 DCC 受控文件作废流程。

## Milestones

1. 核对 E2E、登录、运行端口和收尾规则。
2. 确认 `int_qms` 的前后端运行态和真实页面入口。
3. 通过真实页面找到可作废的受控文件，并记录作废前状态。
4. 通过真实页面填写作废原因、确认影响并提交作废申请。
5. 通过真实页面完成作废审批。
6. 通过真实页面确认文件进入作废终态、从有效列表退出、历史仍可查看且作废动作不再可用。
7. 形成脱敏验证报告，区分 PASS、FAIL、BLOCKED 和未执行项。

## Expected Verification

- 登录真实租户“芋道源码”，账号标签为 `admin`。
- 作废前文件为当前受控状态，页面可见作废入口。
- 作废申请表单真实打开并提交，影响确认和作废原因真实填写。
- 审批待处理状态下文件仍保持受控。
- 审批完成后文件页面显示已作废终态，作废入口消失或被终态锁定。
- 默认有效列表不再出现该文件，历史详情仍可打开。
- Playwright 保存 trace、关键步骤截图、控制台/pageerror、自然写请求和响应状态证据。

最终执行结果：真实页面闭环 PASS。任务自有样本 `CODEX-E2E-OBSOLETE-2026-09-19T18-52-52-266Z` 经真实外来文件评审、真实生效申请、真实作废申请和真实审批中心作废审批后，已在受控浏览 `OBSOLETE` 状态下可见，并在 `ACTIVE` 状态筛选下无有效数据行。作废审批写动作由 Playwright 操作真实页面完成；后续终态校验也来自真实前端页面。

本轮必要修复：外部评审 APPROVE 后进入 `READY_TO_PUBLISH`，生效申请前置校验允许正式生效动作生成发布/盖章件，审批中心 DCC 适配器支持外部评审与表单中心作废 BPM 投影，作废审批 review 分流到 BPM 表单中心任务，前端表单动作面板允许 `requiresBpm=true` 且 `requiresForm=false` 的作废申请创建实例。

## BDD

BDD: 受控文件作废完整闭环 -> Given 当前租户存在可作废的受控文件且当前账号具备作废及审批权限；When 用户从真实详情页发起作废、填写原因并确认影响，随后在真实审批中心完成审批；Then 文件在审批前仍为受控，审批后进入作废终态、退出默认有效列表、保留历史查看能力且不再提供作废动作。

BDD: 文控管理员配置包可往返文件分类 -> Given 当前页面导出的文控管理员全量配置包包含任务专属类别及其默认文件分类；When 用户通过真实“文控管理员”页面导入该配置包以补齐任务类别的作废权限；Then 后端应保留并导入 `fileTypeTaxonomyId`，配置包导入成功，任务类别权限规则生效，不因缺默认文件分类阻断。

## Design Constraints

- 所有被验收业务动作只能由 Playwright 操作真实前端页面完成。
- 不使用 `fetch`、`apiGet`、API client、数据库写入或 mock 替代创建、作废申请、审批和最终业务断言。
- 允许监听页面自然请求并记录 HTTP 状态；最终业务断言来自页面 DOM、输入值、弹窗、按钮状态和页面导航。
- 不创建、修改或清理共享业务基线数据；优先使用任务自有或当前页面明确可作废的样本。
- 当前分支为 `int_qms`，使用前端 `8061`、后端 `48061`；已获用户明确授权，仅允许重启本任务所属的 `48061` 后端，不影响 `int_main` 或其他运行时。
- 不触碰本任务开始前已有的工作区改动。
- 证据不得记录密码、令牌、Cookie、Authorization 或签名凭据。
- 外部评审输出文件为任务自有文件；文档渲染检查因环境缺少 LibreOffice/`soffice.exe` 未完成，不将其写成渲染通过。

## Cleanup Keep

- doc/tasks/20260918-dcc-void-e2e/task.md
- doc/tasks/20260918-dcc-void-e2e/execution-log.md
- doc/tasks/20260918-dcc-void-e2e/verification-report.md
- doc/tasks/20260918-dcc-void-e2e/dcc-obsolete-real-sample-probe.json
- doc/tasks/20260918-dcc-void-e2e/migration-policy-gate.json
- doc/tasks/20260918-dcc-void-e2e/dcc-admin-config-original-20260919.json
- doc/tasks/20260918-dcc-void-e2e/dcc-admin-config-e2e-20260919.json
- doc/tasks/20260918-dcc-void-e2e/dcc-admin-config-original-fixed-20260919.json
- doc/tasks/20260918-dcc-void-e2e/dcc-admin-config-e2e-fixed-20260919.json
- doc/tasks/20260918-dcc-void-e2e/obsolete-e2e-source.pdf
- doc/tasks/20260918-dcc-void-e2e/restart-int-qms-temp-edhr.ps1
- doc/tasks/20260918-dcc-void-e2e/dcc-void-real-ui.e2e.cjs
- doc/tasks/20260918-dcc-void-e2e/continue-obsolete-approval-real-ui.cjs
- doc/tasks/20260918-dcc-void-e2e/obsolete-approval-2026-09-19T22-22-32-104Z/

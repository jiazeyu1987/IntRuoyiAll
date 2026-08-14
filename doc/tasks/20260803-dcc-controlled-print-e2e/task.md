# DCC 受控打印真实 E2E 验证

## Task Goal

在 `E:\IntRuoyi` 本机 `int_main` 运行态重新执行一次 DCC 文控“受控打印”真实 Playwright E2E，验证当前有效受控文件可从受控浏览/详情页发起受控打印，生成含受控信息的打印件并形成可追溯打印记录；同时验证无打印权限账号被按钮隐藏或权限拒绝。

## Scope

- 仅验证 DCC 受控打印场景，不修复或扩展其它 DCC 上传、发布、分发、培训、MES/eDHR 或非受控打印场景。
- 使用非 admin 账号，密码仅通过 `DCC_E2E_PASSWORD` 环境变量注入，不写入日志或报告。
- 通过真实页面操作创建打印记录；API/DB 仅用于最终只读核验。
- 复用既有任务自有 ACTIVE 受控文件作为测试对象，不使用真实业务文件。
- 当前系统受控打印设计为直接打印；若本轮发现打印审批入口，则必须走真实审批，否则按直接打印验证权限、水印和记录。

## Milestones

1. 读取规则、建立任务文档、记录 BDD 场景。
2. 核验本机前后端运行态、Playwright/Node 前置和目标测试数据。
3. 使用有打印权限非 admin 账号通过真实页面完成受控打印。
4. 验证打印件受控信息、水印、打印记录/只读 API/DB 追溯。
5. 使用无打印权限非 admin 账号验证同一入口按钮隐藏或权限阻断。
6. 输出 `verification-report.md`，记录 PASS 或 BLOCKED 证据。

## BDD Scenarios

BDD: 有权限用户打印当前有效受控文件 -> Given 任务自有受控文件为当前 ACTIVE 版本 When 有打印权限的非 admin 用户从受控浏览或详情页点击受控打印并填写必填信息 Then 页面生成带打印编号、文件编号、版本、打印人、打印时间的受控打印件 And 打印记录中出现本次记录。

BDD: 无打印权限用户被阻断 -> Given 非 admin 用户没有同一文件类别的 PRINT 权限 When 用户进入同一 ACTIVE 文件的受控浏览或详情页 Then 受控打印入口不可用、隐藏或点击后明确权限拒绝 And 不生成该用户打印记录。

BDD: 打印动作可追溯 -> Given 用户已完成一次真实页面受控打印 When 使用只读 API/DB 核验打印记录 Then 可看到打印记录 ID、文件编号、版本、份数、打印人、打印时间和直接打印或审批状态。

## Expected Verification

- `node --check doc/tasks/20260803-dcc-controlled-print-e2e/dcc-controlled-print-real.e2e.cjs`
- 使用 `DCC_E2E_PASSWORD` 环境变量运行真实 Playwright E2E。
- 输出结果 JSON、打印件截图、打印记录截图、无权限阻断截图。
- 只读 API/DB 核验打印记录、打印份数、打印人、状态、文件版本和当前有效 master 指针。

## Applicable Gates

- `docs/e2e-rules.md#DCC 受控打印门禁`
- `docs/e2e-rules.md#E2E 脚本入口存在性门禁`
- `docs/e2e-rules.md#Playwright 目标链路与外部资源异常归因门禁`
- `docs/e2e-rules.md#真实 E2E 主链路与扩展诊断产物隔离门禁`
- `docs/login-access.md#E2E 与数据约定`
- `docs/frontend-development.md#前端静态契约隔离门禁`

## Current Status

ready_for_closeout

## Verification Summary

- 2026-08-03 00:21 本机 `int_main` 真实 Playwright E2E PASS：前端 `8081`、后端 `48081` 运行态可用。
- 最终打印记录 ID `6`，打印编号 `DCCP-20260803002113-F7E73FEB`，文件 `CODX-DCC-ORIG-20260802101521`，版本 `V1.0`，状态 `DIRECT_PRINTED`，打印人 `王思雨 (wangsiyu)`，份数 `2`。
- 当前有效性已核验：`dcc_controlled_file.id=2054545668044070287` 为 `ACTIVE`，master `current_active_controlled_file_id=2054545668044070287`，`publishedFileId=9198354916366`，`stampedFileId=9198354916366`。
- 无权限账号 `zhangkeying` 可见同一 ACTIVE 文件但 `visiblePrintButtonCount=0`，覆盖无权限阻断。
- `targetNetworkFailures=[]`、`targetBadResponses=[]`、`consoleErrors=[]`、`pageErrors=[]`。

## Cleanup Keep

- doc/tasks/20260803-dcc-controlled-print-e2e/dcc-controlled-print-real.e2e.cjs
- doc/tasks/20260803-dcc-controlled-print-e2e/dcc-controlled-print-real-e2e-result.json
- doc/tasks/20260803-dcc-controlled-print-e2e/controlled-print-window-20260802162105.png
- doc/tasks/20260803-dcc-controlled-print-e2e/controlled-print-records-20260802162105.png
- doc/tasks/20260803-dcc-controlled-print-e2e/controlled-print-negative-20260802162105.png
- doc/tasks/20260803-dcc-controlled-print-e2e/verification-report.md

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：本任务为验证任务，不修改业务代码；若验证失败将记录 BLOCKED。
- `是否存在临时补丁或绕过`：否。

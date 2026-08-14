# Verification Report

## Status

PASS

## Summary

DCC 文控“受控打印”已完成本轮完整真实 Playwright E2E 验证：有打印权限的非 admin 用户从受控浏览进入当前有效受控文件详情页，通过真实页面生成受控打印件，并在页面记录、只读 API 和只读 DB 中形成可追溯打印记录；无打印权限账号在同一文件入口看不到“受控打印”按钮。

## Scope Compliance

- 非 admin 正向账号：`wangsiyu`，密码通过 `DCC_E2E_PASSWORD` 环境变量注入，未写入报告。
- 非 admin 负向账号：`zhangkeying`，同一 ACTIVE 文件可见但 `visiblePrintButtonCount=0`。
- 真实页面路径：登录 -> 文控中心 / 受控浏览 -> 任务自有 ACTIVE 文件 -> 受控打印 -> 填写用途、份数、接收部门、使用位置 -> 生成打印件。
- 禁止项遵守：未使用 admin 完成业务 E2E，未 API-only/SQL 创建打印记录，未 SQL 修改文件状态，未 mock 上传或打印成功。
- 审批口径：当前系统受控打印为直接打印设计，本轮未发现独立打印审批入口；状态按 `DIRECT_PRINTED` 验收，审批人不适用。

## Print Evidence

- 打印记录 ID：`6`
- 打印编号：`DCCP-20260803002113-F7E73FEB`
- 文件 ID：`2054545668044070287`
- 文件编号：`CODX-DCC-ORIG-20260802101521`
- 文件版本：`V1.0`
- 当前有效性：`ACTIVE`，master `current_active_controlled_file_id=2054545668044070287`
- 发布/盖章文件：`publishedFileId=9198354916366`，`stampedFileId=9198354916366`
- 打印人：`王思雨 (wangsiyu)` / `printUserId=910250`
- 份数：`2`
- 打印用途：`DCC受控打印E2E验证-20260802162105`
- 接收部门：`质量保证部-20260802162105`
- 使用位置：`DCC E2E验证工位-20260802162105`
- 审批人：不适用
- 审批/打印状态：`DIRECT_PRINTED`
- 打印时间：`2026-08-03 00:21:13`

## Verification Evidence

- 真实 E2E 命令：`node E:\IntRuoyi\doc\tasks\20260803-dcc-controlled-print-e2e\dcc-controlled-print-real.e2e.cjs` -> PASS，退出码 `0`。
- E2E 结果 JSON：`E:\IntRuoyi\doc\tasks\20260803-dcc-controlled-print-e2e\dcc-controlled-print-real-e2e-result.json`，`status=PASS`，`targetNetworkFailures=[]`，`targetBadResponses=[]`，`consoleErrors=[]`，`pageErrors=[]`。
- 打印件截图：`E:\IntRuoyi\doc\tasks\20260803-dcc-controlled-print-e2e\controlled-print-window-20260802162105.png`，可见水印、打印编号、文件编号、版本、打印人、打印时间、份数、用途、接收部门、使用位置、`DIRECT_PRINTED`。
- 打印记录截图：`E:\IntRuoyi\doc\tasks\20260803-dcc-controlled-print-e2e\controlled-print-records-20260802162105.png`，可见最新受控打印记录列表。
- 无权限截图：`E:\IntRuoyi\doc\tasks\20260803-dcc-controlled-print-e2e\controlled-print-negative-20260802162105.png`，同一 ACTIVE 文件可见但无“受控打印”按钮。
- 只读 API 核验：`/admin-api/dcc/controlled-files/2054545668044070287/controlled-print/records` 返回 `code=0` 且包含记录 ID `6`。
- 只读 DB 核验：`dcc_controlled_file_print_record.id=6`，文件编号/版本/份数/打印人/状态与页面一致；`dcc_controlled_file` 与 master 指向证明打印的是当前有效版本。

## Permission Evidence

- 正向账号页面数据：`canPrint=true`，`allowedActions` 包含 `PRINT`。
- 负向账号页面断言：同一 ACTIVE 文件可见，`visibleRowsForFile=1`，`visiblePrintButtonCount=0`。

## Remaining Notes

- 本轮调试期间通过真实页面产生了中间打印记录 ID `4`、`5`；最终验收以 ID `6` 为准，全部打印记录均由真实页面创建，未通过 API-only 或 SQL 创建。
- `task-closeout-cleanup` preview/apply 已完成，最终 E2E 脚本、结果 JSON、3 张验收截图和核心任务文档均保留，仅删除本任务失败尝试旧截图。
- 任务验证和清理已完成，当前文档状态进入 `ready_for_closeout`；本轮未提交/推送，因为当前共享 `int_main` 工作区已有其它任务未提交改动和 ahead 状态。

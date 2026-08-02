# DCC 签核追溯 UX 修复执行日志

## User Intent

- 修复签核追溯真实页面中 5 个 UX 问题。
- 修复后进行 E2E 验证。
- 不使用 admin，不用 API-only/SQL 改状态，不顺手修其它场景。
- 密码仅通过环境变量注入，文档和日志不记录明文。

## BDD

- BDD: 签名留痕权限提示业务化 -> Given 查看账号没有 DCC 电子签名管理权限 When 打开受控文件详情 Then 页面提示当前仍可查看签核追溯摘要，高级签名留痕需要额外权限。
- BDD: 操作日志空态闭环 -> Given 目标文件操作日志接口返回成功但无行 When 用户从目标文件进入操作日志 Then 页面显示暂无操作日志，并指引签核证据见签核追溯/生命周期。
- BDD: 追溯节点合并审批意见和签名证据 -> Given 已发布受控文件存在四级审批签名 When 用户查看签核追溯 Then 每个审批节点同时显示审批意见、签名时间、签名方式、证据状态、文件 hash 和文件证据。
- BDD: 发布盖章文件可点击验证 -> Given 受控文件已生成 publishedFileId/stampedFileId When 用户查看签核追溯 Then 页面提供查看盖章/发布文件入口，而不是只显示 ID。
- BDD: 签名失败诊断可操作 -> Given 用户输入错误签名密码或缺少签名授权 When 提交签名 Then 弹窗明确显示原因、处理建议和责任入口。

## RED/GREEN

- RED: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> FAIL，期望失败原因：当前实现仍显示“签名留痕无法加载；审批任务加载不受影响”。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node tests/e2e/dcc-controlled-file-logs-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-controlled-file-detail-retired-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> PASS。

## Implementation Evidence

- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`
  - 签核追溯新增审批意见列和文件证据列。
  - 文件证据列展示 `publishedFileId`、`stampedFileId`，并提供“查看盖章/发布文件”按钮。
  - 签核追溯导出 CSV 和打印 HTML 同步包含审批意见、文件证据。
  - 高级签名留痕权限提示改为“当前可查看签核追溯摘要；高级签名留痕需 DCC 电子签名管理权限。”
- `IntRuoyiFronted/src/views/dcc/controlled-file/logs/index.vue`
  - 当 URL 带 `controlledFileId` 且无日志行时，空态显示“暂无操作日志，签核证据请见签核追溯/生命周期。”
- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/approval-actions.ts`
  - 错误密码、缺授权、签名图片、证据快照失败均包含原因、处理建议、责任入口。

## Real E2E

- Command: `node doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` with `DCC_E2E_PASSWORD` injected by PowerShell expression.
- Final result: `traceability-ux-real-e2e-result-20260802112712.json` -> PASS。
- Reused controlled file: `CODX-DCC-ORIG-20260802101521` / `2054545668044070287` / `V1.0`。
- Viewer account: `wangsiyu`，非 admin。
- Page evidence:
  - `traceability-ux-detail-20260802112712.png`
  - `traceability-ux-file-evidence-viewer-20260802112712.png`
  - `traceability-ux-operation-logs-20260802112712.png`
  - `signature-trace-ux-export-20260802112712.csv`
- Read-only API verification:
  - `responseCode=0`
  - 文件 ID、文件编号、版本、状态、`publishedFileId`、`stampedFileId`、签名数量、审批意见均与只读详情响应一致。
  - `dccWriteRequests=[]`，未产生 DCC 写请求。

## Resumed E2E Blocker Resolution

- BDD: 低权限签名留痕提示真实触发 -> Given 非 admin `zhaojie` 可以在受控浏览看到任务自有 ACTIVE 文件且没有 `dcc:controlled-file:signature:manage` When 打开文件编号追溯详情 Then 页面展示“当前可查看签核追溯摘要；高级签名留痕需 DCC 电子签名管理权限。”
- BDD: 错误密码签名失败诊断真实触发 -> Given 任务自有原版文件处于首个待审批签名节点 When `zhaohaichen` 输入错误签名密码 Then 页面展示签名失败原因、处理建议、责任入口，且流程不推进；随后输入正确密码继续四级审批到 ACTIVE。
- GREEN: `node --check doc/tasks/20260802-dcc-traceability-ux-fixes/dcc-original-release-with-wrong-password-e2e.cjs` -> PASS。
- GREEN: `node --check doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` -> PASS。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node doc/tasks/20260802-dcc-traceability-ux-fixes/dcc-original-release-with-wrong-password-e2e.cjs` -> PASS；真实页面创建 `CODX-DCC-TRACE-DIAG-20260802115503` / `2054545668044070299`，先错误密码诊断，再四级审批签名至 `ACTIVE`。
- GREEN: `node doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` -> `traceability-ux-real-e2e-result-20260802120622.json` PASS；`dccWriteRequests=[]`，低权限提示 PASS，错误密码诊断来源 PASS。
- Runtime fix: `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` 原有 `})const openControlledBrowserLocation` 语法错误导致 Vite 动态模块 500，已做最小换行修复；复验 `http://127.0.0.1:8081/src/views/dcc/controlled-file/detail/index.vue` -> 200。

## Former E2E Gaps Resolved

- 签名失败诊断真实错误密码：PASS。`zhaohaichen` 首节点错误密码响应 `1080000022`，页面显示“签名失败原因 / 当前密码错误 / 处理建议 / 责任入口”，随后正确密码继续主链路。
- 低权限权限提示真实触发：PASS。`zhaojie` 无高级签名管理权限，能看到目标文件并进入追溯详情，页面显示业务化权限提示且旧误导文案不可见。

## Full Re-Verification 2026-08-02 22:11

- BDD: 五项签核追溯 UX 复验 -> Given 任务自有文件 `CODX-DCC-TRACE-DIAG-20260802115503` 已通过真实页面完成原版发布、四级审批签名和错误密码诊断 When 非 admin 查看账号从受控浏览进入详情、操作日志和文件证据预览 Then 页面同时证明业务化权限提示、操作日志空态闭环、审批意见+签名证据合并、盖章/发布文件可点击、签名失败诊断可见。
- RED: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> FAIL，期望失败原因：静态合同仍断言旧列名 `文件证据`，当前页面正式列名为 `盖章文件 / 发布文件证据`。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS，静态合同同步当前页面列名。
- GREEN: `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-dcc -Dtest=DccControlledFileLogQueryServiceTest test` -> PASS，3 tests / 0 failures / 0 errors；确认当前源码能处理缺 assignment/project-code 的项目代码变更日志，不触发文控日志 NPE。
- BLOCKED: `node doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` -> `traceability-ux-real-e2e-result-20260802132801.json` FAIL，影响：操作日志空态闭环未通过；页面显示空态指引但日志接口返回 `code=500` 并出现系统异常。
- Blocker root cause: 48081 当时运行旧 runtime jar，命中文控日志聚合旧 NPE；没有使用 API/SQL 造数据或改状态。
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests clean package` -> PASS，重新生成 `yudao-server-exec.jar`，清理旧 class/jar 污染。
- GREEN: clean build runtime -> 独立 jar `output/runtime/int_main/backend/yudao-server-exec-20260802-220742.jar` 启动，SHA256 `81D5AF927797043FAAA68D065865B3738A8785B668A4E17C4675275AA8319E4F`，health `UP` on 48081。
- GREEN: `node doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` -> `traceability-ux-real-e2e-result-20260802141029.json` PASS；`targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`、`dccWriteRequests=[]`。
- Evidence: `traceability-ux-detail-20260802141029.png`、`traceability-ux-file-evidence-viewer-20260802141029.png`、`traceability-ux-operation-logs-20260802141029.png`、`traceability-ux-permission-prompt-20260802141029.png`、`signature-trace-ux-export-20260802141029.csv`。

## Full Re-Verification 2026-08-02 22:20

- BDD: 五项签核追溯 UX 完整复验 -> Given 任务自有文件 `CODX-DCC-TRACE-DIAG-20260802115503` 已通过真实页面完成原版发布、四级审批签名和错误密码诊断 When 显式使用该错误密码诊断结果作为追溯复验源 Then 页面同时证明业务化权限提示、操作日志空态闭环、审批意见+签名证据合并、盖章/发布文件可点击、签名失败诊断可见。
- GREEN: frontend `http://127.0.0.1:8081/login?redirect=/index` -> HTTP 200；backend `http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> PASS。
- NOTE: `traceability-ux-real-e2e-result-20260802141849.json` used the default original-release source and correctly marked `signatureFailureDiagnosticStatus=BLOCKED` because that ACTIVE file no longer had a pending signature button; it was not used as final evidence.
- GREEN: `node doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` with `DCC_E2E_SOURCE_RESULT_PATH=doc/tasks/20260802-dcc-traceability-ux-fixes/dcc-original-release-wrong-password-20260802115503.json` -> `traceability-ux-real-e2e-result-20260802142044.json` PASS；`targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`、`dccWriteRequests=[]`、`signatureFailureDiagnosticStatus=PASS`。
- Evidence: `traceability-ux-detail-20260802142044.png`、`traceability-ux-file-evidence-viewer-20260802142044.png`、`traceability-ux-operation-logs-20260802142044.png`、`traceability-ux-permission-prompt-20260802142044.png`、`signature-trace-ux-export-20260802142044.csv`。

## Secret Handling

- Password injection command used environment variable only.
- Secret scan result: `NO_PASSWORD_LITERAL_FOUND`。

## Closeout Cleanup

- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260802-dcc-traceability-ux-fixes --mode preview` -> PASS，`status=ready`，`blocked=<none>`，`warnings=<none>`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260802-dcc-traceability-ux-fixes --mode apply` -> PASS，删除旧轮次重复截图/CSV/JSON 和已归档 `frontend-feature-evidence.md`，保留最终 E2E 脚本、最终 JSON、截图、CSV、`task.md`、`execution-log.md`、`verification-report.md`。
- GREEN: post-cleanup password literal scan -> `NO_PASSWORD_LITERAL_FOUND`。
- GREEN: scoped `git diff --check` for task implementation, tests, and task records -> PASS。
- GREEN: staged pre-commit checks -> `git diff --cached --check` PASS, staged password literal scan `NO_PASSWORD_LITERAL_FOUND_IN_STAGED`, staged large-file scan `NO_STAGED_FILE_OVER_50MB`。
- GREEN: `powershell -ExecutionPolicy Bypass -File E:\IntRuoyi\scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main/int_main: frontend 8081, backend 48081`。
- GREEN: `git commit -m "任务: 提交当前前后端代码续跑"` -> commit `a88d00bda`，59 files changed。
- GREEN: initial `git push origin int_main` failed because global GitHub-specific proxy `http.https://github.com.proxy=http://127.0.0.1:7890` pointed to an inactive local proxy；read-only diagnostics showed `127.0.0.1:7890` failed and `github.com:443` direct TCP succeeded。
- GREEN: `git -c http.https://github.com.proxy= push origin int_main` -> PASS，`7d847126e..a88d00bda int_main -> int_main`。未修改全局 Git 配置。
- Note: 工作区仍保留其它任务未提交产物和 PID 文件；本任务证据、脚本和报告已随 `a88d00bda` 推送。

## Experience Consolidation

- GREEN: 使用 `project-experience-consolidation` 技能复核本任务经验归宿；已有 `docs/e2e-rules.md` 适合作为长期规则归宿，无需新建经验文档。
- GREEN: `docs/e2e-rules.md` 已在“真实 E2E 主链路与扩展诊断产物隔离门禁”中补充：最终追溯复验需要证明错误密码/缺授权等扩展诊断时，必须显式绑定包含真实诊断阶段的任务自有结果文件；默认 `ACTIVE` 源无待签名按钮时只能记录诊断源不适用，不能作为最终诊断 PASS 证据。
- Verification: 本次沉淀只记录通用 E2E 诊断源绑定规则，不记录一次性文件 ID、临时账号状态或业务完成事实。

## Current Status

completed

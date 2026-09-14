# 20260802 DCC 原版发布当前验证

## Task Goal

对 DCC 文控“原版发布 / 签核追溯”执行真实 Playwright E2E 验证：验证任务自有原版文件从上传、四级审批/签名、发布生效，到受控浏览进入详情页后，页面可追溯“谁上传、谁审核、谁签名”、签名时间、签名方式、证据状态、文件 hash 与盖章/发布文件证据。

## Milestones

- [x] 读取必需项目规则、登录规则、E2E 规则、前端规则、本地运行态规则与收尾规则
- [x] 恢复并确认本机后台运行态：前端、后端、MinIO 均可用
- [x] 使用非 admin 上传人账号通过真实页面完成原版上传
- [x] 使用四个非 admin 审批/签名账号通过真实页面完成四级审批
- [x] 验证 V1.0 原版文件发布生效且 master 指向当前有效版本
- [x] 使用受控浏览真实页面验证当前有效版可见并可打开 viewer
- [x] 使用受控浏览文件编号入口进入非 viewer 签核追溯详情页
- [x] 验证签核追溯页面和导出 CSV 展示上传人、审批/签名人、签名时间、签名方式、证据状态、审批意见、hash 与盖章/发布文件 ID
- [x] 使用只读 DB/API 核验最终状态、签名证据和版本信息
- [x] 输出 `verification-report.md`
- [x] 修复受控浏览 viewer 缺少发布/盖章文件链路信息的 blocker

## Expected Verification

- `DCC_E2E_PASSWORD` 仅通过环境变量注入，不在日志或报告记录明文。
- 不使用 admin 账号执行业务路径。
- 不使用 API-only、SQL 改状态或 mock 上传成功。
- 只读 DB/API 仅用于最终核验：文件唯一、V1.0 `ACTIVE`、master 指向 V1.0、审批任务完成数、签名证据有效、受控浏览当前有效版可打开，且页面追溯证据与后端只读数据一致。
- viewer 页面必须展示最终目录路径、`publishedFileId`、`stampedFileId` 与 master 当前生效版本。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，viewer 模式复用详情页正式字段和目录计算结果展示发布链路信息，不通过权限、API-only 或状态改写绕过。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Final Result

- PASS: `CODX-DCC-ORIG-20260802101521` 已通过真实 Playwright 原版上传、四级审批/签名、发布生效、受控浏览 viewer、viewer 发布链路信息和签核追溯详情验证。
- Runtime PASS: 前端 HTTP `200`、后端 health `UP`、MinIO ready HTTP `200`。
- 文件 ID：`2054545668044070287`；Master ID：`2054545668044062896`；Process instance：`16e8802b-8e5b-11f1-93ff-00155d2984a0`。
- 当前有效版本：`V1.0` / `NEW` / `ACTIVE`；master 当前生效版本指向 `2054545668044070287`。
- 审批/签名：完成任务数 `4`；有效签名数 `4`；签名人为 `zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu`。
- 受控浏览：授权账号 `wangsiyu` 搜索该文件 `browserTotal=1`，并从文件名预览入口打开 `/dcc/controlled-file/detail/2054545668044070287?viewer=1&from=browser...`。
- viewer 链路：页面显示最终目录 `4.Ohter`、`publishedFileId=9198354916366`、`stampedFileId=9198354916366` 与 master 当前生效版本 `V1.0`。
- 权限负向：低权限账号 `pengyunfeng` 搜索同一文件编号 `browserTotal=0`，目标文件不可见。
- 签核追溯：从文件编号入口打开 `/dcc/controlled-file/detail/2054545668044070287?traceability=1&from=browser...`，页面展示上传人、四级审批/签名人、签名时间、签名方式、`已校验`、证据 hash、审批意见与文件 ID `9198354916366`。
- 导出/打印：导出 CSV `signature-trace-export-20260802102108-trace-fields-final4.csv` 字段完整，打印按钮可用。
- 本轮边界：受控打印、培训/分发等扩展场景不属于本轮签核追溯验收。

## Git Closeout Blocker

- E2E 验证已完成；本次未执行 cleanup apply、提交或推送。
- 当前 `int_main` 已存在非本任务 ahead 提交 `53d31d7f9`，且工作区存在大量非本任务脏改动；直接提交/推送会混入其它任务。

## Cleanup Keep

- doc/tasks/20260802-dcc-original-release-e2e-current/task.md
- doc/tasks/20260802-dcc-original-release-e2e-current/execution-log.md
- doc/tasks/20260802-dcc-original-release-e2e-current/verification-report.md
- doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-final-resume.cjs
- doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-e2e-current.cjs
- doc/tasks/20260802-dcc-original-release-e2e-current/e2e-result.json
- doc/tasks/20260802-dcc-original-release-e2e-current/e2e-result-final-main-chain-20260802101521.json
- doc/tasks/20260802-dcc-original-release-e2e-current/e2e-result-after-linkage-fix.json
- doc/tasks/20260802-dcc-original-release-e2e-current/e2e-result-controlled-browser-final-rerun.json
- doc/tasks/20260802-dcc-original-release-e2e-current/final-readonly-db-verification.json
- doc/tasks/20260802-dcc-original-release-e2e-current/traceability-entry-real-check-result.json
- doc/tasks/20260802-dcc-original-release-e2e-current/signature-trace-export-20260802102108-trace-fields-final4.csv

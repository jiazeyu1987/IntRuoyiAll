# 20260802 DCC 原版发布当前验证 Verification Report

## Status

E2E BLOCKED

## Scope

- 场景：DCC 文控“受控浏览”当前有效文件权限验证，覆盖原版上传、四级审批/签名、发布生效、授权账号可见/可预览、低权限账号不可见。
- 环境：本机 `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`，MinIO `http://127.0.0.1:9000`。
- 约束：未使用 admin；未用 API-only/SQL 改状态；API/DB 仅用于最终只读核验；未修其它场景。
- 结论：主链路与权限隔离已通过真实 Playwright 验证；但受控浏览 viewer 页面未展示验收要求的最终目录路径与 `publishedFileId/stampedFileId` 或等价发布文件信息，因此整体 BLOCKED。

## Target File

| Item | Value |
|---|---|
| Run ID | `20260802101521` |
| File number | `CODX-DCC-ORIG-20260802101521` |
| File name | `Codex DCC 原版上传链路 20260802101521` |
| Controlled file ID | `2054545668044070287` |
| Master ID | `2054545668044062896` |
| Process instance ID | `16e8802b-8e5b-11f1-93ff-00155d2984a0` |
| Version / change / status | `V1.0` / `NEW` / `ACTIVE` |
| Category | `907233` / `过程检验规程` |
| Final directory | `906515` / `4.Ohter` |
| File type path | `技术文档 / 设计和开发输出阶段 / 来料/过程/成品检验规范` |
| Published / stamped file ID | `9198354916366` / `9198354916366` |

## PASS Evidence

| Requirement | Result | Evidence |
|---|---|---|
| Runtime precheck | PASS | 前端 HTTP `200`；后端 health HTTP `200`；MinIO ready HTTP `200` |
| 原版上传 | PASS | 非 admin 上传人 `pengyunfeng` 通过真实上传页创建 `CODX-DCC-ORIG-20260802101521`，上传文件 `批记录节点-解析样本.docx` |
| 四级审批/签名 | PASS | `zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` 通过真实审批/签名页面完成四级处理 |
| 发布生效 | PASS | 只读 DB 核验 `V1.0` / `NEW` / `ACTIVE`，master 当前有效版本指向 `2054545668044070287` |
| 签名证据 | PASS | 4 条签名均为 `VALID`，`passwordVerified=1`，签名方式 `PASSWORD` |
| 授权账号受控浏览 | PASS | `wangsiyu` 进入 `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-ORIG-20260802101521&pageNo=1&pageSize=20`，`browserTotal=1`，行版本 `V1.0`、状态 `ACTIVE` |
| 授权账号打开预览 | PASS | 从受控浏览行打开 `/dcc/controlled-file/detail/2054545668044070287?viewer=1&from=browser...`，viewer 与受控预览壳加载成功，预览文件为 `stamped-approval-sample.pdf` |
| 低权限账号不可见 | PASS | `pengyunfeng` 进入同一路径搜索同一文件编号，`browserTotal=0`，页面显示 `暂无匹配受控文件`，目标编号未渲染 |
| 历史/草稿误看 | PASS | 该目标链路仅产生 1 条 V1.0 `NEW` 当前有效行，`revisionFileCount=0`；不存在可被默认打开的草稿/历史失效版 |
| 目标链路错误 | PASS | `targetNetworkFailures=0`，`consoleErrors=0`，`pageErrors=0`；诊断 `dccWriteRequests=[]` |

## BLOCKED Evidence

| Requirement | Result | Evidence |
|---|---|---|
| viewer 页面展示最终目录与发布文件信息 | BLOCKED | 诊断结果 `linkageVisible=false`、`linkageRequiredLabelsVisible=false` |
| 页面可见 `publishedFileId/stampedFileId` 或等价发布文件信息 | BLOCKED | 只读 DB 已确认 `publishedFileId=9198354916366`、`stampedFileId=9198354916366`，但从受控浏览打开的 viewer 页面未渲染该 linkage 信息 |

## Source Observation

- 当前前端模板中 viewer 模式只渲染 `dcc-controlled-preview-layout` 与 `dcc-controlled-preview-detail-pane`；`dcc-detail-controlled-browser-linkage` 位于非 viewer 详情路径，因此从受控浏览打开的 viewer 页面不可见该卡片。
- 按用户要求，本轮只记录 BLOCKED 与影响，未修改页面入口、权限、测试数据状态或产品代码。

## Artifacts

- `doc/tasks/20260802-dcc-original-release-e2e-current/e2e-result-final-main-chain-20260802101521.json`：当前有效版、四级签名、发布文件 ID 和授权账号预览复核 PASS。
- `doc/tasks/20260802-dcc-original-release-e2e-current/e2e-result.json`：同一目标的最新主结果副本。
- `doc/tasks/20260802-dcc-original-release-e2e-current/controlled-browser-current-permission-linkage.json`：同一最新文件的授权/低权限账号真实页面诊断与 linkage 缺口。
- `doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-e2e-current.cjs`：本轮真实 Playwright E2E 脚本。

## Actions Not Taken

- 未使用 admin 账号。
- 未通过 API/SQL 制造审批记录、签名记录、发布状态或权限结果。
- 未顺手修复 viewer linkage 缺口或其它 DCC 场景。
- 未提交/推送：当前分支已有非本任务 ahead 提交且工作区存在非本任务脏改动，避免把其它任务混入本场景。
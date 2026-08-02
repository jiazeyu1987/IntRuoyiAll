# 20260802 DCC 原版发布当前验证 Verification Report

## Status

E2E PASS

## Scope

- 场景：DCC 文控“受控浏览”当前有效文件权限验证，覆盖任务自有原版上传、四级审批/签名、发布生效、授权账号可见/可预览、低权限账号不可见，以及 viewer 发布链路信息可见。
- 环境：本机 `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`，MinIO `http://127.0.0.1:9000`。
- 约束：未使用 admin；未用 API-only/SQL 改状态；API/DB 仅用于最终只读核验；未顺手修其它 DCC 场景；密码未写入报告。
- 结论：原 BLOCKED 的 viewer linkage 已修复并通过真实 Playwright 复验；授权账号能看到并打开当前有效版，低权限账号看不到同一文件。

## Target File

| Item | Value |
|---|---|
| Main run ID | `20260802101521` |
| Linkage fix run ID | `20260802103711` |
| Latest controlled-browser rerun ID | `20260802104623` |
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

## Account Coverage

| Account label | Username | Permission result | Page evidence |
|---|---|---|---|
| 授权受控浏览账号 | `wangsiyu` | 可见目标分类/目录下当前有效版 | `browserTotal=1`，行版本 `V1.0`、状态 `ACTIVE` |
| 低权限账号 | `pengyunfeng` | 不可见同一目标文件 | 同一路径搜索同一文件编号 `browserTotal=0`，页面证据 `empty-or-no-access-visible` |

## Page Evidence

| Requirement | Result | Evidence |
|---|---|---|
| Runtime precheck | PASS | 前端 HTTP `200`；后端 health `UP`；MinIO ready HTTP `200`；Chrome executable 可用 |
| 原版上传 | PASS | 非 admin 上传人 `pengyunfeng` 通过真实上传页创建任务自有文件 |
| 四级审批/签名 | PASS | `zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` 逐节点通过真实审批/签名页面完成 |
| 发布生效 | PASS | 只读核验 `V1.0` / `NEW` / `ACTIVE`，master 当前有效版本指向 `2054545668044070287` |
| 授权账号受控浏览 | PASS | `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-ORIG-20260802101521&pageNo=1&pageSize=20`，`browserTotal=1` |
| 授权账号打开预览 | PASS | 打开 `/dcc/controlled-file/detail/2054545668044070287?viewer=1&from=browser...`，预览文件为 `stamped-approval-sample.pdf`，`previewKind=PDF` |
| viewer 发布链路信息 | PASS | viewer 页面展示 `受控浏览入口`、`最终目录路径`、`publishedFileId`、`stampedFileId`、`master 当前生效版本` |
| 受控浏览最终目录路径 | PASS | 页面/结果记录目录路径 `4.Ohter` |
| 发布/盖章文件 ID | PASS | 页面/结果记录 `publishedFileId=9198354916366`、`stampedFileId=9198354916366` |
| 低权限账号不可见 | PASS | `pengyunfeng` 进入同一路径搜索同一文件编号，`browserTotal=0`，目标编号未渲染 |
| 历史/草稿误看 | PASS | 该目标链路仅产生 1 条 V1.0 `NEW` 当前有效行，`revisionFileCount=0`；不存在默认打开草稿/历史失效版 |
| 目标链路错误 | PASS | `targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]` |

## Regression

| Command | Result |
|---|---|
| `node tests/e2e/dcc-controlled-browser-viewer-linkage-static.spec.js` | PASS |
| `node tests/e2e/dcc-upload-governance-ux-static.spec.js` | PASS |
| `node --check doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-e2e-current.cjs` | PASS |
| `node doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-e2e-current.cjs` | PASS |
| `node doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-e2e-current.cjs` with result `e2e-result-controlled-browser-final-rerun.json` | PASS |
| `pnpm ts:check` | PASS |

## Artifacts

- `doc/tasks/20260802-dcc-original-release-e2e-current/e2e-result-final-main-chain-20260802101521.json`：原版上传、四级审批/签名、发布生效、只读后端主链路核验。
- `doc/tasks/20260802-dcc-original-release-e2e-current/e2e-result-after-linkage-fix.json`：受控浏览 viewer linkage 修复后的真实 Playwright 复验结果，`status=PASS`。
- `doc/tasks/20260802-dcc-original-release-e2e-current/e2e-result-controlled-browser-final-rerun.json`：本轮按用户要求复跑的真实 Playwright 受控浏览验证，`runId=20260802104623`，`status=PASS`。
- `IntRuoyiFronted/tests/e2e/dcc-controlled-browser-viewer-linkage-static.spec.js`：任务专用 viewer linkage 静态合同。

## Closeout Blocker

- Git closeout 未执行：当前 `int_main` 已存在非本任务 ahead 提交和大量非本任务脏改动；本轮未提交/推送，避免混入其它场景或其它任务改动。

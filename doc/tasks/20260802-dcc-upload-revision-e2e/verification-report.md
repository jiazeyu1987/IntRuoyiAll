# 20260802 DCC 上传升版 E2E Verification Report

## Scope

- Feature under test: DCC 文控 V1 上传发布 + V2 升版发布 + 发布申请审批生效完整业务链路。
- Environment: 本机 `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`，tenant `1`。
- Actors: `pengyunfeng` 上传人，`zhaohaichen` 文控审核，`zhaojie` 会签审核，`zhaomingyu` 会签批准，`wangsiyu` 文控批准/发布申请；均为非 admin 账号。

## Matrix

| Requirement | Test Method | Result | Evidence |
|---|---|---|---|
| V1.0 真实前端上传 | Playwright 操作 `/dcc/controlled-file/upload` | PASS | `2054545668044070260` 创建为 V1.0 |
| V1.0 四级 DCC 审批发布 | Playwright 操作 DCC 审批详情处理态 | PASS | `zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` 依次审批 |
| V2.0 真实前端升版上传 | Playwright 选择历史文件名并提交 V2.0 | PASS | `2054545668044070261` 创建为 V2.0，`changeType=REVISION` |
| V2.0 四级 DCC 审批至待发布 | Playwright 操作 DCC 审批详情处理态 | PASS | V2 审批完成后进入发布申请阶段 |
| V2.0 发布申请 | Playwright 操作 DCC 详情页发布申请弹窗 | PASS | `bpm_form_action_instance.id=435`，BPM 流程 `8a6ea0e6-8de1-11f1-a558-00155d9fd668` |
| V2.0 发布 BPM 四级审批 | Playwright 操作 BPM 流程详情页并选择下一审批人 | PASS | 发布审批任务完成数 `4` |
| 最终 DB 状态核验 | 只读 DB 核验 | PASS | V1 `SUPERSEDED`，V2 `ACTIVE`，master 指向 V2，发布实例 `EFFECTIVE` |

## Test Data

- File number: `CODX-DCC-REV-20260802-20260801193848`。
- File name: `Codex DCC 升版链路 20260801193848`。
- File type path: `技术文档 / 设计和开发输出阶段 / 来料/过程/成品检验规范`。
- Category: `过程检验规程`。
- Directory: `质量管理 / 4.Ohter`。
- Project: `HGGW`。
- V1 source file: `E:\IntRuoyi\resource\批记录节点-解析样本.docx`。
- V2 source file: `E:\IntRuoyi\resource\过程检验记录.docx`。
- Final-node PDF fixture: `doc/tasks/20260802-dcc-upload-revision-e2e/stamped-approval-sample.pdf`。

## RED

- RED: `node doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs` -> FAIL, publish action initially lacked form instance permissions for non-admin role.
- RED: publish applicant user selection -> FAIL, `wangsiyu` lacked `system:user:query/list`, so the real `UserSelectV2` dialog could not search approvers.
- RED: publish approval via approval-center direct review -> FAIL, the next task `APPROVE_USER_SELECT` assignee was not configured.
- RED: publish BPM detail script -> FAIL, script waited for process id text and checked next-assignee UI before async node rendering completed.

## GREEN

- GREEN: permission setup -> PASS, non-admin roles now include required form instance, BPM query/update, and user query permissions.
- GREEN: `node --check doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs` -> PASS.
- GREEN: DCC upload + revision E2E -> PASS, `doc/tasks/20260802-dcc-upload-revision-e2e/e2e-result.json` has `status=PASS`.
- GREEN: final DB verification -> PASS, V1 `SUPERSEDED`, V2 `ACTIVE`, master current active ID `2054545668044070261`, publish instance `EFFECTIVE`, upload approval count `8`, publish approval count `4`.
- GREEN: sensitive scan -> PASS, no matches for the known password literal, bearer token, access token, or refresh token keywords in the task directory.

## Blockers

- None remaining for the requested upload + revision + publish chain.
- Non-blocking residual UI issue: BPM process detail page emitted `Cannot read properties of undefined (reading 'markers')` while rendering process diagram markers. The target approval controls and final business assertions passed, so this is recorded as a follow-up UI defect rather than a blocker for this business-chain verification.

## CI Impact

- No production source code was changed for this verification task.
- The task-owned Playwright script remains under `doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs` for reproducibility.
- Release recommendation for the tested business chain: GO, with separate follow-up recommended for the BPM diagram marker pageerror.

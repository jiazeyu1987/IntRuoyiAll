# 20260802 DCC 原版上传 E2E Verification Report

## Scope

- Feature under test: DCC 文控新文件原版 V1.0 上传 + 四级 DCC 审批后生效业务链路。
- Environment: 本机 `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`，tenant `1`。
- Actors: `pengyunfeng` 上传人，`zhaohaichen` 文控审核，`zhaojie` 会签审核，`zhaomingyu` 会签批准，`wangsiyu` 文控批准；均为非 admin 账号。

## Matrix

| Requirement | Test Method | Result | Evidence |
|---|---|---|---|
| V1.0 原版真实前端上传 | Playwright 操作 `/dcc/controlled-file/upload` | PASS | 受控文件 `2054545668044070262` 创建为 V1.0 |
| 原版上传文件进入预览/上传链路 | Playwright 上传本地 Word 文件并等待上传响应 | PASS | `previewFileName=批记录节点-解析样本.docx`，`previewKind=OFFICE` |
| 四级 DCC 审批处理态 | Playwright 从审批中心进入 DCC 详情处理态 | PASS | `zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` 依次审批 |
| 原版文件审批后生效 | 只读 DB 核验 | PASS | `V1.0`、`changeType=NEW`、`status=ACTIVE` |
| 原版链路不触发升版 | 只读 DB 核验 | PASS | 同一 `file_number` 仅 1 行，revision-like row count `0` |
| master 当前生效指向原版 | 只读 DB 核验 | PASS | `currentActiveControlledFileId=2054545668044070262` |

## Test Data

- File number: `CODX-DCC-ORG-20260802-20260802014434`。
- File name: `Codex DCC 原版上传链路 20260802014434`。
- File type path: `技术文档 / 设计和开发输出阶段 / 来料/过程/成品检验规范`。
- Category: `过程检验规程`。
- Directory: `质量管理 / 4.Ohter`。
- Project: `HGGW`。
- V1 source file: `E:\IntRuoyi\resource\批记录节点-解析样本.docx`。
- Final-node PDF fixture: `E:\IntRuoyi\doc\tasks\20260802-dcc-upload-revision-e2e\stamped-approval-sample.pdf`。

## RED

- RED: backend health precheck -> FAIL, `http://127.0.0.1:48081/actuator/health` initially refused connection, so real E2E could not start until local runtime was restored.

## GREEN

- GREEN: standard local backend restart -> PASS, backend health returned `{"status":"UP"}` before E2E.
- GREEN: `node --check doc/tasks/20260802-dcc-upload-original-e2e/dcc-upload-original-e2e.cjs` -> PASS.
- GREEN: DCC original upload E2E -> PASS, `doc/tasks/20260802-dcc-upload-original-e2e/e2e-result.json` has `status=PASS`.
- GREEN: final DB verification -> PASS, one V1.0 row, `NEW`, `ACTIVE`, master points to V1, upload approval count `4`, revision-like row count `0`.
- GREEN: target network/page error capture -> PASS, result JSON has no target network failures, console errors, or page errors.

## Verification

- Browser: local Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`。
- Frontend path: real page `/dcc/controlled-file/upload` plus real approval-center task handling pages.
- DB state: `dcc_controlled_file.id=2054545668044070262`, `master_id=2054545668044062874`, `processInstanceId=beff4899-8e13-11f1-a451-00155d9fd668`。
- Output evidence: `doc/tasks/20260802-dcc-upload-original-e2e/e2e-result.json`。

## Blockers

- None remaining for the requested original upload chain.

## CI Impact

- No production source code was changed for this verification task.
- The task-owned Playwright script remains under `doc/tasks/20260802-dcc-upload-original-e2e/dcc-upload-original-e2e.cjs` for reproducibility.
- Release recommendation for the tested original upload business chain: GO.

# 20260802 DCC 上传升版 E2E Verification Report

## Scope

- Feature under test: DCC 文控 V1 上传发布 + V2 升版发布完整业务链路。
- Environment: 本机 `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`，tenant `1`。
- Actors: `pengyunfeng` 上传人，`zhaohaichen` 文控审核，`zhaojie` 会签审核，`zhaomingyu` 会签批准，`wangsiyu` 文控批准；均为非 admin 账号。

## Matrix

| Requirement | Test Method | Result | Evidence |
|---|---|---|---|
| V1.0 真实前端上传 | Playwright 操作 `/dcc/controlled-file/upload` | PASS | 最新文件 `CODX-DCC-REV-20260802-20260801174426` 创建为 V1.0，状态 `PENDING_DOC_CONTROL_REVIEW` |
| V1.0 四级审批发布 | Playwright 操作真实审批入口 | BLOCKED | DCC 详情处理态不可达；审批中心 DCC 行仅能进入只读 viewer |
| V2.0 升版上传 | Playwright 操作真实上传升版 | BLOCKED | V1 未能发布，无法进入升版前置状态 |
| V2.0 四级审批发布 | Playwright 操作真实审批入口 | BLOCKED | 依赖 V2 提交，未到达 |
| 最终 DB/API 状态核验 | 只读 DB 支持核验 | BLOCKED | master/current/version/签名记录未形成完整链路 |

## Test Data

- Category: `DCC_OTHER_TEMPLATE_900250` / `其他`，categoryId `906104`。
- Directory: `质量管理 / 4.Ohter`。
- Project: `HGGW`。
- V1 source file: `E:\IntRuoyi\resource\批记录节点-解析样本.docx`。
- V2 source file: `E:\IntRuoyi\resource\过程检验记录.docx`。
- Final-node PDF fixture: `doc/tasks/20260802-dcc-upload-revision-e2e/stamped-approval-sample.pdf`。

## RED

- RED: `node doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs` -> FAIL, approval center BPM direct review returned business `403`.
- `POST /admin-api/approval-center/tasks/review` initially returned business `403` for `zhaohaichen` because the non-admin approval-center role lacked `bpm:task:update`.
- After granting `bpm:task:update`, the login permission response included the update permission, but BPM direct approval still returned `403`; this confirmed DCC should not be approved through the BPM native row.
- RED: `node doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs` -> FAIL, DCC detail handling page redirected to controlled-file browser before the approval card rendered.
- Attempting the DCC module path showed the real blocker: `/dcc/controlled-file/detail/<id>` without `viewer=1` redirects to `/dcc/controlled-file/browser`; with `viewer=1`, the page is read-only and does not render the signature approval controls.

## GREEN

- GREEN: runtime prechecks -> PASS.
- Runtime prechecks passed: local frontend reachable, local backend health UP, OnlyOffice container can reach backend health, Chrome executable available, upload files exist.
- GREEN: `node --check doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs` -> PASS.
- `node --check doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs` passed after script updates.
- GREEN: permission setup verification -> PASS.
- Permission setup now grants the four approver accounts query/update approval-center permissions plus DCC review/approve permissions through non-admin roles.
- GREEN: V1.0 upload phase -> PASS.
- Latest real upload produced V1.0 file `2054545668044070241` with Flowable task assigned to `zhaohaichen`.

## Blockers

- Release recommendation: NO-GO for full DCC upload + revision E2E. The upload path works, but the approval path is not fully reachable from the current frontend.
- Required fix before rerun: expose a real DCC approval handling route or adjust `DccControlledFileDetail` routing so approvers can reach the non-viewer approval card from an authorized frontend path.
- Task-owned residue: four `CODX-DCC-REV-20260802-*` V1.0 records remain pending document-control review; they were not silently removed.

## CI Impact

- No production code test suite was modified or run because this task is verification-only and blocked by a frontend entry issue.
- The temporary Playwright script and PDF fixture remain under the task directory as reproducible evidence.

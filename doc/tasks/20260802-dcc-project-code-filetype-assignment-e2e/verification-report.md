# Verification Report - DCC 项目代码文件类型归属 E2E 验证

## Summary

PASS. 使用非 admin 账号 `wangsiyu` 通过本机真实前端页面完成“已有受控文件修改归属 DCC 项目代码 + 修改 5 种文件类型”的 E2E 验证；每次保存后均在目标 DCC 项目代码 `IMC` 的 item 详情关联文档三栏中验证到该文件出现在对应文件类型下，并通过只读接口辅助核验 `fileTypeTaxonomyId`。最终已通过页面恢复原项目和原文件类型。

## Scope

- Feature under test: existing controlled file metadata edit, DCC project-code assignment, and associated document file-type synchronization in project-code item detail.
- Environment: local `int_main`, frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, tenant `芋道源码`.
- Actor: `wangsiyu`, non-admin. Password was supplied through environment variable expression and not recorded in task documents.
- Controlled file: id `2054545668044070264`, file number `CODX-DCC-REV-20260802-20260802034644`.
- Source/restore project: `HGGW` / id `234`; target verification project: `IMC` / id `217`.

## Matrix

| Requirement | Test Method | Result | Evidence |
|---|---|---|---|
| 修改已有文件归属 DCC 项目代码 | Playwright 真实页面打开受控文件详情、点击“修改”、选择 DCC 项目并保存 | PASS | 5 次保存均返回 metadata PUT `code=0`，目标项目为 `IMC` |
| 覆盖 5 个不同文件类型 | Playwright 在“文件分类”级联控件按可见文本选择 5 个正式 taxonomy leaf | PASS | taxonomy ids `137/138/139/140/141` |
| 项目代码 item 详情文件类型同步 | Playwright 打开 `/mdm/project-code?projectCodeId=217`，点击阶段和文件类型，断言关联文档表格包含目标文件编号 | PASS | 每轮 `uiVerified=true` |
| 只读辅助核验 | 登录态只读接口读取目标项目关联文件并核对 `fileTypeTaxonomyId` | PASS | 每轮 `readOnlyVerified=true` |
| 恢复原始状态 | Playwright 通过同一修改弹窗恢复 `HGGW` 和原文件类型 | PASS | DB 只读核验 `dcc_project_code_id=234`、`file_type_taxonomy_id=102`、`status=ACTIVE` |
| 不使用 API-only/SQL 直改 | 命令与脚本复核 | PASS | 业务写入仅通过前端页面触发 metadata PUT；SQL/DB 只用于只读核验和允许范围内权限前置 |

## RED / GREEN Evidence

- RED: first Playwright E2E attempts -> FAIL, expected reasons captured: edit button initially absent until `doc_control` role precondition was corrected, Element Plus cascader locator was unstable until selection was tied to visible label text, and backend metadata PUT rejected stale `user_role_ids:910250` cache before targeted cache eviction.
- GREEN: task bootstrap -> PASS.
- GREEN: `node --check doc\tasks\20260802-dcc-project-code-filetype-assignment-e2e\dcc-project-code-filetype-assignment-e2e.cjs` -> PASS.
- GREEN: `node doc\tasks\20260802-dcc-project-code-filetype-assignment-e2e\dcc-project-code-filetype-assignment-e2e.cjs` -> PASS, verified 5 file-type assignment iterations and restored original metadata.
- GREEN: final read-only DB check -> PASS, file `2054545668044070264` restored to `dcc_project_code_id=234`, `file_type_taxonomy_id=102`, `ACTIVE`.
- GREEN: quality assurance evidence validator -> PASS, `Quality assurance evidence is valid.`
- GREEN: sensitive scan -> PASS, no literal password/token markers matched in the task directory.

## Iteration Results

| Iteration | Target File Type | UI Detail Assertion | Read-only Check |
|---|---|---|---|
| 1 | `技术文档 / 清单 / DHF文件清单` | PASS | PASS |
| 2 | `技术文档 / 设计和开发策划阶段 / 市场调研报告` | PASS | PASS |
| 3 | `技术文档 / 设计和开发策划阶段 / 技术调研报告` | PASS | PASS |
| 4 | `技术文档 / 设计和开发策划阶段 / 注册和临床路径分析报告` | PASS | PASS |
| 5 | `技术文档 / 设计和开发策划阶段 / 项目可行性分析报告` | PASS | PASS |

## Notes

- Resolved precondition: `wangsiyu` had `doc_control` role in DB, but backend metadata update initially hit stale Redis `user_role_ids:910250`; deleting only that user-role cache key allowed backend `hasAnyRoles` to load current role data.
- Non-target network notes: result JSON contains navigation-related `ERR_ABORTED` GETs, including external Baidu analytics and local GETs canceled during route changes; target writes and associated-document assertions passed, with `pageErrors=[]`.
- Cleanup state: validation is complete and task status is `ready_for_closeout`; repository has unrelated dirty work outside this task, so no commit/push or cleanup apply was performed in this verification step.

## Blockers

- None open for the requested E2E verification.
- Closeout blocker: repository contains unrelated dirty work outside this task; no commit/push or cleanup apply was performed as part of this verification step.

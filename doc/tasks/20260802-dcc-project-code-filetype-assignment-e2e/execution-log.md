# 20260802 DCC 项目代码文件类型归属 E2E 验证 Execution Log

## User Intent

- 用户要求验证另一条链路：修改已有受控文件的 5 个文件类型以及归属的 DCC 项目代码后，DCC 项目代码 item 详情中的文件类型也发生对应变化。
- 本任务必须验证真实页面链路，不使用 API-only、SQL 直改或 mock 数据冒充通过。

## BDD

- BDD: 已有文件归属到 DCC 项目代码 -> Given 测试租户中存在一个已有受控文件和目标 DCC 项目代码，When 用户在真实页面修改文件基础信息并保存目标 DCC 项目代码，Then 目标 DCC 项目代码详情的关联文档包含该文件。
- BDD: 五个文件类型同步 -> Given 目标 DCC 项目代码详情按正式 DCC 文件分类树展示阶段和文件类型，When 用户依次把同一个已有文件修改为 5 个不同文件类型，Then 每次进入目标 DCC 项目代码详情都能在对应文件类型下看到该文件。
- BDD: 非目标归属不污染 -> Given 文件从原 DCC 项目代码移动到目标 DCC 项目代码，When 修改保存成功，Then 原项目代码详情不再把该文件计入当前归属，目标项目代码详情按新文件类型展示。

## Verification Evidence

- GREEN: task bootstrap -> PASS, task directory created and applicable E2E/database/frontend gates recorded.
- GREEN: runtime preflight -> PASS, local `int_main` frontend `http://127.0.0.1:8081` returned HTTP 200 and backend `http://127.0.0.1:48081/actuator/health` returned `UP`; Docker MySQL/Redis dependencies were available on the documented local ports.
- GREEN: permission precondition -> PASS, non-admin user `wangsiyu` in tenant `芋道源码` had `doc_control` role binding in `system_user_role`; stale Redis key `user_role_ids:910250` was deleted before the final run so backend `hasAnyRoles(userId,'doc_control')` used current DB role data.
- GREEN: `node --check doc\tasks\20260802-dcc-project-code-filetype-assignment-e2e\dcc-project-code-filetype-assignment-e2e.cjs` -> PASS.
- GREEN: `node doc\tasks\20260802-dcc-project-code-filetype-assignment-e2e\dcc-project-code-filetype-assignment-e2e.cjs` -> PASS, Playwright used real frontend pages to edit controlled file metadata five times and verify the DCC project-code detail associated-document type grouping after each save.
- GREEN: final read-only DB check -> PASS, controlled file `2054545668044070264` restored to original project code id `234`, original file type taxonomy id `102`, status `ACTIVE`.
- GREEN: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence doc\tasks\20260802-dcc-project-code-filetype-assignment-e2e\verification-report.md` -> PASS, quality assurance evidence is valid.
- GREEN: sensitive scan for literal password/token markers in the task directory -> PASS, no matches for the configured secret patterns.
- GREEN: project experience consolidation -> PASS, merged reusable DCC project-code associated-document E2E and `user_role_ids` cache preflight lesson into `docs/frontend-development.md#DCC 基础条目关联文档分类树门禁` and `docs/experience-index.md`.
- GREEN: `rg -n "Only doc control|user_role_ids|doc_control 缓存刷新" docs\frontend-development.md docs\experience-index.md` -> PASS, new experience keywords resolve to the updated long-term gate.
- GREEN: `git diff --check -- docs/frontend-development.md docs/experience-index.md doc/tasks/20260802-dcc-project-code-filetype-assignment-e2e/...` -> PASS, no whitespace errors.

## Final E2E Data

- Account: `wangsiyu`, non-admin, tenant `芋道源码`.
- Source project: `HGGW` / id `234`; target project: `IMC` / id `217`.
- Controlled file: `2054545668044070264`, file number `CODX-DCC-REV-20260802-20260802034644`.
- Verified target file types: `技术文档 / 清单 / DHF文件清单`; `技术文档 / 设计和开发策划阶段 / 市场调研报告`; `技术文档 / 设计和开发策划阶段 / 技术调研报告`; `技术文档 / 设计和开发策划阶段 / 注册和临床路径分析报告`; `技术文档 / 设计和开发策划阶段 / 项目可行性分析报告`.
- Restore: Playwright restored the file through the same metadata dialog to `HGGW` and `技术文档 / 设计和开发输出阶段 / 来料/过程/成品检验规范`.
- Network notes: the result JSON records navigation-related `ERR_ABORTED` GETs, including external Baidu analytics and local GETs canceled during route changes; target metadata PUT responses and project-code associated-doc read assertions passed, and `pageErrors` was empty.
- Local artifacts: `e2e-result.json` and `e2e-progress.log` kept as task evidence; old text debug artifact removed, ignored screenshot artifact remains outside tracked task evidence.

## Blockers

- Resolved: the first successful edit-button access still failed on backend metadata PUT because `user_role_ids:910250` cached stale role ids; after deleting only that user-role cache key, the same non-admin account completed the full E2E chain.

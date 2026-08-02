# DCC 元数据联动体验优化验证报告

## Scope

本次验证覆盖 DCC 受控文件元数据修改链路的前端体验优化：上传/升版后的已有文件可在详情页继续修改 DCC 项目代码和文件分类，修改前能看到影响预览，修改后能从详情页跳转到 DCC 项目代码关联文档位置，并能进入修正追溯入口。范围不包含后端 schema、审批流、签名流或权限模型变更。

## Matrix

| Requirement | Test | Result |
| --- | --- | --- |
| 保存前展示 DCC 项目、分类路径、受控目录和落位影响 | `node tests\e2e\dcc-controlled-file-metadata-linkage-ux-static.spec.cjs` | PASS |
| 详情页展示 DCC 项目代码联动区和关联文档/追溯入口 | `node tests\e2e\dcc-controlled-file-metadata-linkage-ux-static.spec.cjs` | PASS |
| 项目代码详情三栏能按 query 定位当前文件和文件类型 | `node tests\e2e\dcc-controlled-file-metadata-linkage-ux-static.spec.cjs`、真实 E2E | PASS |
| 权限失败给出 doc_control 与 user_role_ids 缓存诊断 | `node tests\e2e\dcc-controlled-file-metadata-linkage-ux-static.spec.cjs` | PASS |
| 不破坏既有关联文档三栏和上传治理口径 | 4 个相邻 DCC 静态合同 | PASS |
| 前端类型安全 | `pnpm ts:check` | PASS |
| 真实页面链路：5 种文件类型 + DCC 项目代码修改同步到项目代码 item 详情 | Playwright 真实 E2E | PASS |

## Test

- 新增静态合同：`IntRuoyiFronted/tests/e2e/dcc-controlled-file-metadata-linkage-ux-static.spec.cjs`。
- 新增 script：`e2e:dcc:metadata-linkage-ux:static`。
- 相邻回归：关联文档三栏、上传治理、上传升版分类项目联动、文件编号可选。
- 真实 E2E：`doc\tasks\20260802-dcc-project-code-filetype-assignment-e2e\dcc-project-code-filetype-assignment-e2e.cjs`。

## RED:

- `node tests\e2e\dcc-controlled-file-metadata-linkage-ux-static.spec.cjs -> FAIL`，预期失败原因是旧 UI 缺少元数据变更影响预览、详情页联动入口、项目代码详情 query 高亮和权限诊断文案。

## GREEN:

- `node tests\e2e\dcc-controlled-file-metadata-linkage-ux-static.spec.cjs -> PASS`。
- `node tests\e2e\dcc-project-code-associated-three-column-static.spec.js -> PASS`。
- `node tests\e2e\dcc-upload-governance-ux-static.spec.js -> PASS`。
- `node tests\e2e\dcc-upload-project-taxonomy-revision-static.spec.js -> PASS`。
- `node tests\e2e\dcc-metadata-file-number-optional-static.spec.js -> PASS`。
- `pnpm ts:check -> PASS`。
- `node doc\tasks\20260802-dcc-project-code-filetype-assignment-e2e\dcc-project-code-filetype-assignment-e2e.cjs -> PASS`。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260802-dcc-metadata-linkage-ux-optimization/frontend-feature-evidence.md -> PASS`。
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence doc/tasks/20260802-dcc-metadata-linkage-ux-optimization/verification-report.md -> PASS`。
- `git diff --check -- <task-owned paths> -> PASS`。

## Verification

- 真实 E2E 使用非 admin 文控账号 `wangsiyu`，本机入口 `http://127.0.0.1:8081`。
- 目标受控文件：`2054545668044070264`，文件编号 `CODX-DCC-REV-20260802-20260802034644`。
- 源项目：`HGGW` / `234`；目标项目：`IMC` / `217`。
- 5 次页面修改均通过：`DHF文件清单`、`市场调研报告`、`技术调研报告`、`注册和临床路径分析报告`、`项目可行性分析报告`。
- 每次修改后均进入项目代码详情关联文档三栏，按阶段和文件类型验证目标文件出现。
- E2E 结束后恢复原项目和原文件分类，结果文件 `doc\tasks\20260802-dcc-project-code-filetype-assignment-e2e\e2e-result.json` 记录 `status=PASS`、`restored=true`、`pageErrors=[]`。
- `networkErrors` 中存在页面跳转/第三方统计或导航取消产生的 `ERR_ABORTED`，目标 metadata 保存、项目代码详情定位和只读核验均已通过。

## Blockers

- 无功能验证 blocker。
- 未执行提交/推送：当前工作区存在大量非本任务 MES/EDHR/其它任务脏改动，按任务边界未混入提交。

## Release Recommendation

- 本次 DCC 元数据联动体验优化在前端静态合同、类型检查、相邻 DCC 回归和真实页面 E2E 上均已通过，可进入收尾/提交门禁。

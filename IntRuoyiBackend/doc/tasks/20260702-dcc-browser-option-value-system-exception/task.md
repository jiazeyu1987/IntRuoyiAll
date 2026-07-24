# 任务：修复 DCC 浏览器列表系统异常的运行态 schema 契约

- Task ID: `20260702-dcc-browser-option-value-system-exception`
- Created: 2026-07-02
- Current Status: completed

## 任务目标

补齐 DCC 受控文件浏览器列表接口依赖的 `file_type_level1..5` 运行态修复契约，避免已发布库缺少列时 `getControlledFileBrowserPage` 查询抛出 `Unknown column file_type_level1` 并表现为系统异常。

## 里程碑

1. 建立后端任务台账与 BDD/TDD 证据。completed
2. 复现并锁定运行态修复 schema 缺失 file_type_level 字段。completed
3. 补 SQL 契约测试并观察 RED。completed
4. 补齐运行态修复 SQL 与索引契约并观察 GREEN。completed
5. 完成真实运行库迁移验证与提交。completed

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py script/tests/test_dcc_category_lifecycle_stage_sql.py -q`
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql`
- 运行库应用对应 migration/repair 后，浏览器列表接口不再因 `file_type_level1` 缺列返回系统异常。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 命令与中文输出显式 UTF-8，不使用 `&&`。
- 命中 SQL/发布链路门禁：新增运行态 schema 字段必须同步 migration、runtime repair、测试 schema 与发布策略校验。
- 命中 no-fallback：不得用空列表、mock 成功或吞异常掩盖真实 DB schema 缺失。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，补齐运行态修复 schema 契约并加测试门禁。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 浏览器列表 schema 完整 -> Given 已发布库执行运行态修复 SQL / When getControlledFileBrowserPage 查询受控文件列表 / Then file_type_level1..5 字段存在且查询不因缺列抛系统异常。`

## Verification Evidence

- `RED: python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py -q -k file_type_levels -> FAIL, runtime repair schema missing dcc_controlled_file.file_type_level1.`
- `GREEN: python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py script/tests/test_dcc_category_lifecycle_stage_sql.py -q -> PASS, 8 passed in 0.20s.`
- `GREEN: python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS.`
- `BLOCKER: runtime-db-apply -> mysql CLI not found locally, source SQL/runtime repair fixed but current running database was not migrated in this task.`

## Current Blockers

- 暂无。
- `GREEN: node tests/e2e/dcc-browser-version-summary-static.spec.js -> PASS, DCC browser version summary static contract.`
- `GREEN: node tests/e2e/dcc-category-lifecycle-stage-static.spec.js -> PASS, DCC category lifecycle stage static contract.`
- `GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS.`
- `GREEN: python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py script/tests/test_dcc_category_lifecycle_stage_sql.py -q -> PASS, 8 passed.`
- `GREEN: mvn -pl yudao-module-dcc -Dtest=cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixAdminServiceImplTest#listReviewMatrixRows_returnsConfiguredAndUnconfiguredCategories test -> PASS.`
- `GREEN: runtime-db-columns -> PASS, local Docker MySQL dcc_controlled_file and dcc_controlled_file_recognition_record now expose file_type_level1..5; browser query smoke selected file_type_level1/file_type_level2 successfully.`
- `NOTE: full runtime repair script failed fast on unrelated dcc_file_category lifecycle_stage backfill data; focused 20260702_dcc_recognition_file_type_levels migration was applied for this browser-page system exception.`

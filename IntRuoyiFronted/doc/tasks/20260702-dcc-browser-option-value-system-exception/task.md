# 任务：修复 DCC 受控文件浏览器下拉值异常与列表系统异常

- Task ID: `20260702-dcc-browser-option-value-system-exception`
- Created: 2026-07-02
- Current Status: completed

## 任务目标

修复 DCC 受控文件浏览器打开时 `ElOption value got Undefined` 警告，以及 `getControlledFileBrowserPage` mounted hook 抛出系统异常导致列表加载失败的问题。

## 里程碑

1. 建立任务台账、经验门禁、BDD/TDD 证据。completed
2. 复现并定位 `ElOption` undefined 来源和 browser-page 后端异常根因。completed
3. 补失败回归测试，锁定无效 option 与列表异常处理契约。completed
4. 实施最小修复并运行前后端目标测试。completed
5. 更新证据、完成收尾并按验证结果提交。completed

## Expected Verification

- 前端静态回归测试覆盖 DCC 浏览器版本/类别下拉不渲染 undefined value。
- 后端目标测试覆盖 `getControlledFileBrowserPage` 对真实脏数据或最新异常条件不抛系统异常。
- 必要时运行 `pnpm ts:check` 与 DCC 相关后端单测。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 命令、中文文本和测试输出必须显式 UTF-8，不使用 `&&`。
- 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次只修复列表稳定性，不改视觉结构。
- 命中 `bug-regression-fix-loop`：必须先复现/补回归测试，再做最小修复。
- 命中 `frontend-feature-delivery`：不引入 mock，不隐藏 API 错误，不改变无关 UI 契约。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，定位无效数据源并以契约约束。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 浏览器下拉值有效 -> Given browser-page 返回版本历史或类别数据 / When DCC 受控文件浏览器渲染下拉 / Then 所有 ElOption value 均为 Element Plus 支持的有效值。`
- `BDD: 列表接口异常暴露 -> Given browser-page 后端返回真实错误 / When 页面 mounted 加载列表 / Then 不用空数据或静默成功掩盖错误，并保留可定位错误信息。`

## Current Blockers

- 暂无。

## Verification Evidence

- `GREEN: node tests/e2e/dcc-browser-version-summary-static.spec.js -> PASS`
- `GREEN: python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py script/tests/test_dcc_category_lifecycle_stage_sql.py -q -> PASS, 8 passed`
- `GREEN: python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS`
- `BLOCKER: pnpm ts:check -> FAIL, unrelated CategoryReviewMatrixTable.vue fixture lacks required lifecycleStage`
- `BLOCKER: runtime-db-apply -> mysql CLI not found, runtime DB not migrated`
- `GREEN: node tests/e2e/dcc-browser-version-summary-static.spec.js -> PASS, DCC browser version summary static contract.`
- `GREEN: node tests/e2e/dcc-category-lifecycle-stage-static.spec.js -> PASS, DCC category lifecycle stage static contract.`
- `GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS.`
- `GREEN: python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py script/tests/test_dcc_category_lifecycle_stage_sql.py -q -> PASS, 8 passed.`
- `GREEN: mvn -pl yudao-module-dcc -Dtest=cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixAdminServiceImplTest#listReviewMatrixRows_returnsConfiguredAndUnconfiguredCategories test -> PASS.`
- `GREEN: python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS.`
- `GREEN: runtime-db-columns -> PASS, local Docker MySQL dcc_controlled_file and dcc_controlled_file_recognition_record now expose file_type_level1..5; browser query smoke selected file_type_level1/file_type_level2 successfully.`
- `NOTE: full runtime repair script failed fast on unrelated dcc_file_category lifecycle_stage backfill data; focused 20260702_dcc_recognition_file_type_levels migration was applied for this browser-page system exception.`

# DCC 产品目录项目代码回填

## Task Goal

在 DCC 产品目录中增加“项目名称”和“项目代码”两列，并把 DCC 项目代码中与“瑛泰产品”目录完全对应的项目名称、项目代码回填到对应产品目录行。

## Milestones

- [x] 建立 schema/API/UI 的 BDD 与 RED 测试
- [x] 增加产品目录持久化字段与数据迁移
- [x] 扩展后端 DO/VO/API 契约
- [x] 扩展前端 API 类型和产品目录表格列
- [x] 执行定向验证并记录证据

## Expected Verification

- 后端迁移契约测试覆盖 `dcc_product_catalog.project_name/project_code` 字段和完全对应回填 SQL。
- 后端产品目录服务/控制器测试覆盖响应包含项目名称和项目代码。
- 前端静态契约覆盖产品目录页面展示“项目名称”“项目代码”列且绑定 `projectName/projectCode`。
- 本地数据库只读复核：瑛泰产品完全对应行已回填，低/高近似及无法对应行不被误填。

## Current Status

completed

## Experience Gates

- 数据修复临时表排序规则门禁：涉及中文数据源和名称匹配，临时表字符串列必须显式使用目标列排序规则或使用 HEX/ID 精确条件。
- 前端静态契约隔离门禁：若全量 `pnpm ts:check` 被无关历史问题阻塞，使用任务专用静态契约证明当前列展示。
- PowerShell 编排门禁：中文 SQL/Markdown 使用 UTF-8 路径，PowerShell 不使用 `&&`。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，新增正式字段、迁移、后端契约和前端展示。
- 是否存在临时补丁或绕过：否。

## Final Verification Summary

- `python -X utf8 -m pytest script\tests\test_dcc_product_catalog_database_migration.py` -> PASS, 3 tests.
- `mvn -pl yudao-module-dcc -am "-Dtest=DccProductCatalogControllerTest,DccProductCatalogServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.
- `node tests\e2e\dcc-basic-data-product-catalog-static.spec.js` -> PASS.
- `node tests\e2e\dcc-product-catalog-unified-list-template-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `run-release-migration-policy-gate.py` with dependency chain -> PASS, 3 migrations.
- Local DB verification: `瑛泰产品` total 181, project fields filled 115, checked non-exact rows with filled fields = 0.

## Cleanup Keep

- doc/tasks/20260729-dcc-product-catalog-project-code-columns/database-schema-evidence.md
- doc/tasks/20260729-dcc-product-catalog-project-code-columns/backend-api-evidence.md
- doc/tasks/20260729-dcc-product-catalog-project-code-columns/frontend-feature-evidence.md

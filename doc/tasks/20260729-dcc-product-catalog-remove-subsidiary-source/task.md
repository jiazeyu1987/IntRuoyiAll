# DCC 产品目录删除子公司来源数据

## Task Goal

删除 DCC 产品目录中 `data_source = 子公司产品` 的种子数据和前端新增默认/可选入口，保留并只展示 `瑛泰产品` 来源数据。

## Milestones

- [x] 建立任务记录并记录既有脏工作区基线提交。
- [x] 确认 DCC 产品目录表结构、种子 SQL、生成脚本和前端入口契约。
- [x] 先补 RED 静态/迁移契约，证明当前仍包含 `子公司产品`。
- [x] 修改最小范围：删除子公司种子来源、补充运行库清理迁移、同步前端默认选项。
- [x] 运行定向验证并记录 GREEN/REGRESSION。
- [x] 收尾清理、经验沉淀、提交并推送。

## Expected Verification

- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_product_catalog_database_migration.py IntRuoyiBackend/script/tests/test_dcc_product_catalog_remove_subsidiary_source.py`
- `pnpm e2e:dcc:product-catalog-source-options:static`
- `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js`
- `node tests/e2e/dcc-product-catalog-unified-list-template-static.spec.js`
- `node scripts/dcc-product-catalog-registration-expiry-contract.test.mjs`
- `mvn -pl yudao-module-dcc -am "-Dtest=DccProductCatalogServiceImplTest,DccProductCatalogRegistrationExpiryCompareServiceTest,DccProductCatalogControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260729-dcc-product-catalog-remove-subsidiary-source/database-schema-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-dcc-product-catalog-remove-subsidiary-source/frontend-feature-evidence.md`

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260729-dcc-product-catalog-remove-subsidiary-source/database-schema-evidence.md
- doc/tasks/20260729-dcc-product-catalog-remove-subsidiary-source/frontend-feature-evidence.md

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；从正式种子 SQL、生成脚本、运行库清理迁移和前端来源选项统一删除子公司来源。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 数据库 SQL 写入前已读取 `docs/database-rules.md`，按当前迁移文件核对 `dcc_product_catalog.data_source` 表结构和种子来源。
- 前端 Vue/TS 修改前已读取 `docs/frontend-development.md`，按页面组件和专用静态契约验证来源选项。
- PowerShell 中文读写已读取 `docs/powershell-encoding.md`，文档和 SQL/源码写入使用 `apply_patch` 或 UTF-8 工具。
- Git/提交/推送已读取 `docs/powershell-memory.md`，既有脏工作区已独立基线提交。

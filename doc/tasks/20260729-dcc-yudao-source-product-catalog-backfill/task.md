# DCC 产品目录芋道源码回填

## Task Goal

将已验证的 DCC 产品目录“项目名称 / 项目代码”字段和 115 条完全对应回填，同步应用到用户指定的 `芋道源码` 目标运行库。

## Milestones

- [x] 确认 `芋道源码` 对应的数据库目标和当前 schema
- [x] 执行已验证迁移 SQL
- [x] 复核 181 条瑛泰产品中 115 条完全对应已回填，非完全对应未误填
- [x] 记录验证和收尾证据

## Expected Verification

- 目标库存在 `dcc_product_catalog.project_name` 和 `project_code` 字段。
- 目标库 `data_source = 瑛泰产品` 且未删除记录总数为 181。
- 目标库已回填 `project_name/project_code` 的记录数为 115。
- 抽查高近似、低近似、无法对应行不被误填。

## Current Status

completed

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，复用正式迁移 SQL，不做手工散点 update。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep

- doc/tasks/20260729-dcc-yudao-source-product-catalog-backfill/database-schema-evidence.md

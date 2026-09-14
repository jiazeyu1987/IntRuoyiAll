# DCC 产品目录降序空值最后修复

## Task Goal

修复 DCC 产品目录“项目名称 / 项目代码”降序排序时空单元格没有固定排在最后的问题；降序必须先显示有值项目字段，空值、空字符串和纯空白文本统一排在最后。

## Milestones

- [x] 增加降序空值最后的最小回归契约并跑出 RED
- [x] 修复后端项目字段排序表达式，显式处理 NULL/空字符串/空白文本
- [x] 执行前端静态契约、后端定向测试和收尾门禁
- [x] 提交并推送 `int_main`

## Expected Verification

- `node tests\e2e\dcc-product-catalog-project-sort-static.spec.js` 先 RED 后 GREEN。
- `mvn -pl yudao-module-dcc -am "-Dtest=DccProductCatalogControllerTest,DccProductCatalogServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- `pnpm ts:check` 通过。
- `git diff --check` 与 `scripts\preflight\branch-runtime-port-guard.ps1` 通过。

## Current Status

completed

## Experience Gates

- 前端服务端分页排序链路门禁：服务端分页排序必须锁定请求参数、后端白名单字段排序和稳定兜底排序，不能只看表头箭头状态。
- 数据库 SQL 规则：项目字段列来自 `20260729_dcc_product_catalog_project_code_columns.sql`，`project_name/project_code` 已是正式字段；排序表达式只允许使用固定白名单列，禁止拼接用户输入。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，显式处理项目字段空值排序语义，不依赖数据库默认 NULL/空字符串顺序。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- doc/tasks/20260730-dcc-product-catalog-desc-blank-last/bug-regression-evidence.md
- doc/tasks/20260730-dcc-product-catalog-desc-blank-last/backend-api-evidence.md

# 任务：产品 Excel 导入支持缺失即创建（后端）

- Task ID: `20260630-showroom-product-import-create-missing`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在 `yudao-module-showroom` 扩展现有产品 Excel 导入后端能力：当目标租户不存在导入行里的 `productCode` 时，正式创建产品主数据与首个发布版本；当目标租户已存在该 `productCode` 时，继续按现有导入更新/发布逻辑执行，并保持奖项回导兼容。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-commit-backend-code\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成提交收口；本次为新的 `yudao-module-showroom` 功能扩展任务，可继续推进。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md` 与 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 真实写入验证仅允许测试租户；后端合同需维持源租户只读导出、测试租户导入的职责边界。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 任务文档、执行日志与测试输出统一显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接扩展正式导入合同，让 Excel 导入在缺失 `productCode` 时创建产品，不靠手工 SQL 预置主数据。
- `是否存在临时补丁或绕过`：否。已有导入成功路径保留，新增路径以同一导入合同自然扩展。

## BDD 场景

- `BDD: 导入行缺失目标 productCode 时创建新产品 -> Given Excel 行里的 productCode 在当前租户不存在 / When 调用产品 Excel 导入 / Then 系统创建新产品与首个发布版本，并把 productCode 记入 successProductCodes。`
- `BDD: 导入行命中已存在 productCode 时继续更新发布 -> Given 当前租户已经存在该 productCode / When 调用产品 Excel 导入 / Then 系统沿用既有更新/发布能力，不破坏跳过/覆盖语义。`
- `BDD: 奖项页签导入合同保持兼容 -> Given Excel 同时包含奖项页签 / When 执行产品 Excel 导入 / Then 奖项导入仍按现有合同成功，不因新增产品创建路径回退。`

## Milestones

1. M1：建立后端任务文档并锁定行为边界。`completed`
2. M2：补 RED 测试锁定“缺失 productCode 当前会失败”的合同。`completed`
3. M3：实现创建新产品逻辑并跑到 GREEN。`completed`
4. M4：回填后端证据并支撑根任务真实验收。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" -Dsurefire.failIfNoSpecifiedTests=false test`

## Final Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReviveSoftDeletedProductWithSameCode+importProductExcelShouldOnlyReadProductListSheet" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`40` 个导入相关集成测试全部通过。
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS，最新后端可执行包已生成。
- 根任务真实验收已通过：测试租户 `aoteman` 导入 `showroom-product-source-export-native.xlsx` 后，产品导入 `164/164` 成功、奖项导入 `46/46` 成功，数据库 `tenant_id=122` 最终为 `164` 个有效产品。

## Current Blockers

- 无。后端代码、回归测试和根任务真实验收均已完成。

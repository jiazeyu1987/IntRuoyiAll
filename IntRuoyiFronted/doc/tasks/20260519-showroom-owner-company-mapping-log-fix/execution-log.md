# 执行记录：修复展厅产品归属公司映射日志

BDD: 固定产品归属不再依赖部门树映射 -> Given 展厅后台产品归属 UI 已固定显示为 `瑛泰医疗` / When 用户打开产品新建或编辑入口 / Then 页面应使用稳定的 showroom 公司上下文设置 `owner_company_id`，而不是因为部门树缺少命名匹配就抛出 `未找到瑛泰医疗所属公司映射`。

BDD: 固定产品归属继续走真实保存契约 -> Given 用户进入产品新建或编辑弹窗 / When 页面设置固定产品归属并保存 / Then 前端仍应提交真实 `owner_company_id` 与 `product_owner_type` 字段，不得用 mock 或空值掩盖。

REPRO: `src/views/showroom-admin/index.vue` 中，产品归属虽然在 UI 上固定显示为 `瑛泰医疗`，但 `openProductCreate/openProductEdit` 仍会先执行 `ensureProductCompanyOptions()`；当部门树缺少匹配名称时，会抛出 `未找到瑛泰医疗所属公司映射`。
ROOT CAUSE: 固定归属场景仍依赖旧的部门树映射流程，缺少一个稳定来源来提供 `owner_company_id`。
REGRESSION TEST: 更新 `scripts/showroom-admin-product-company-field-layout.test.mjs`，要求 `index.vue` 显式使用 `companyCurrent.companyId`，并且不再包含 `throw new Error('未找到瑛泰医疗所属公司映射')`。
RED: `D:\Programs\node.exe --test scripts/showroom-admin-product-company-field-layout.test.mjs` -> FAIL，源码仍包含旧 throw 分支，也未使用 `companyCurrent.companyId`。
GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-product-company-field-layout.test.mjs` -> PASS。
GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/index.vue scripts/showroom-admin-product-company-field-layout.test.mjs` -> PASS。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-owner-company-log-fix run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-owner-company-mapping-log-fix\verify-showroom-owner-company-log-fix.mjs` -> PASS，真实产品页可打开“新增产品”弹窗，且控制台未再出现所属公司映射错误。
RISK: 当前修复只收口固定归属取值来源；若后续恢复多公司归属选择，需要重新设计公司来源契约。
BLOCKER: 无剩余 blocker。

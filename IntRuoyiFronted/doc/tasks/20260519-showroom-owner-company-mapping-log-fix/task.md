# 任务：修复展厅产品归属公司映射日志

## 目标

修复展厅后台产品管理区域的运行时错误 `未找到瑛泰医疗所属公司映射`，确保产品归属固定为“瑛泰医疗”的前提下，不再依赖不稳定的部门树映射。

## 前置任务检查

- 上一个前端任务：`yudao-ui-admin-vue3/doc/tasks/20260519-showroom-hall-products-no-manual-order/task.md`
- 启动前状态：已完成。
- 影响：可独立开展本次产品归属公司映射日志修复。

## 缺陷摘要

- 现象：产品归属 UI 已固定显示为 `瑛泰医疗`，但运行时仍会执行 `loadProductCompanyOptions` / `ensureProductCompanyOptions`，在部门树缺少匹配名称时抛出 `未找到瑛泰医疗所属公司映射`。
- 期望：产品归属固定为 `瑛泰医疗` 时，应优先使用稳定的 showroom 公司上下文，不再因为部门树映射缺失而报错。

## 里程碑

- [x] M1：创建任务文档并确认前置任务完成。
- [x] M2：补充失败回归测试，覆盖“固定归属不再因为部门映射缺失而抛错”。
- [x] M3：完成最小修复并通过目标验证。
- [x] M4：更新任务记录并执行收尾预览。

## 预期验证

- `D:\Programs\node.exe --test scripts/showroom-admin-product-company-field-layout.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-owner-company-log-fix open http://127.0.0.1:8081`
- `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-owner-company-log-fix run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-owner-company-mapping-log-fix\verify-showroom-owner-company-log-fix.mjs`

## 当前状态

已完成：固定产品归属逻辑已改为优先使用 `companyCurrent.companyId`，真实产品页验证通过。

## 当前结论

- 已确认根因：产品归属 UI 虽然固定成 `瑛泰医疗`，但 `index.vue` 仍保留 `loadProductCompanyOptions -> ensureProductCompanyOptions -> throw` 旧链路，导致固定归属场景依旧依赖部门树命名匹配。
- 已完成最小修复：保留现有固定归属流程，但当部门树未返回匹配项时，改为直接使用 `companyCurrent.companyId` 构造固定归属选项，不再抛出 `未找到瑛泰医疗所属公司映射`。
- 已完成真实验证：`http://127.0.0.1:8081/showroom/product` 可打开“新增产品”弹窗，且控制台不再出现所属公司映射错误。

## 最终验证

- PASS：`D:\Programs\node.exe --test scripts/showroom-admin-product-company-field-layout.test.mjs`
- PASS：`$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/index.vue scripts/showroom-admin-product-company-field-layout.test.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-owner-company-log-fix run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-owner-company-mapping-log-fix\verify-showroom-owner-company-log-fix.mjs`

## 写入边界

- `src/views/showroom-admin/index.vue`
- `scripts/showroom-admin-product-company-field-layout.test.mjs`
- `doc/tasks/20260519-showroom-owner-company-mapping-log-fix/**`

## 风险与影响

- 本次修复只收口“固定归属场景下不再因部门树缺失而报错”；如果后续业务重新引入多公司产品归属选择，还需要重新设计 owner-company 来源契约。

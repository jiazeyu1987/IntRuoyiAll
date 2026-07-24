# 任务：展柜维护产品打开缓慢原因诊断

## Goal

诊断后台 `展柜管理 -> 维护产品` 弹窗打开缓慢的具体原因，确认是否由前端请求链路、分页策略或其他阻塞导致，并给出基于当前代码与真实页面路径的结论。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\HallProductMappingDialog.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-mapping-slow-diagnosis\**`

## Non-Scope

- 本任务默认只做诊断，不直接修改生产代码
- 不引入 fallback、mock 或绕过真实路径的说明

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-candidate-page-contract-fix\task.md`
- Status before this task: `Completed on 2026-05-22`
- Impact: 上一同仓展柜维护产品契约修复已完成，不阻塞本次性能原因诊断

## Milestones

- [x] M1: 检查前置任务状态并创建当前诊断任务文档
- [x] M2: 审查维护产品弹窗的前端加载链路
- [x] M3: 使用真实页面路径确认请求数量和等待特征
- [x] M4: 输出根因说明与风险边界

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-product-mapping-slow-diagnosis open http://127.0.0.1:8081/showroom/hall --headed`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-product-mapping-slow-diagnosis run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-mapping-slow-diagnosis\scripts\measure-showroom-hall-product-mapping-open.mjs`

## Current Status

Completed on 2026-05-22: 已确认当前缓慢的主因是打开弹窗时前端逐页串行拉取完整产品池；真实数据下总产品数为 `180`，按 `pageSize: 20` 需请求 `9` 页，当前环境串行拉完整体约 `43.3s`。

## Final Diagnosis

- 前端根因：`loadAllProductOptions()` 每次打开弹窗都执行全量分页拉取，而不是只拿当前页或复用已有候选缓存。
- 直接证据：代码位于 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\HallProductMappingDialog.vue:165-184`。
- 真实数据：`/admin-api/showroom/product/page?pageNo=1&pageSize=20` 当前返回 `total = 180`，意味着要拉 `9` 页。
- 真实耗时：接口侧顺序拉取 `9` 页合计约 `43322.87ms`，单页约 `4.67s ~ 5.05s`。
- 结论：你点击 `维护产品` 后觉得“要刷新很久”，不是弹窗本身渲染慢，而是它在后台等完整产品候选列表串行拉完。

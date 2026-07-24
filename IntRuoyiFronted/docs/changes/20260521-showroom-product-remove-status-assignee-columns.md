# 变更申请：删除展厅产品管理列表中的资料状态与指派对象列

## Request Summary And Source

- Request: “不要挤掉,直接把这两列删掉”
- Source: 用户于 2026-05-21 在当前会话中明确提出。

## Current Baseline Reviewed

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-status-assignee-columns-restore\task.md`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-status-assignee-viewport-regression\task.md`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-list.test.mjs`

## Classification

Requirement change.

## Impact Analysis

- Product impact: 产品管理列表首屏不再展示 `资料状态` 与 `指派对象` 两列；用户需通过其他字段或详情入口查看相关信息。
- Design impact: 表格首屏空间压力下降，无需再为这两列保留可视区。
- Data impact: 不改动真实产品数据、指派数据或字段含义。
- API impact: 不改动现有前后端接口契约，仅停止在该列表上渲染两列。
- Test impact: 需要把现有“列必须显示/首屏可见”的测试改为“列不再渲染”的源码级与真实页面验证。
- Release impact: 低风险前端变更，影响面集中在 `showroom-admin` 产品列表。
- Operations impact: 无额外运维或环境依赖变更。

## Decision

Accept.

## Required Approvals

- 用户已在当前会话中明确批准该变更，无额外审批前置。

## Downstream Skill Reruns

- `frontend-feature-delivery`: 执行列表删列实现与样式收口。
- `bug-regression-fix-loop`: 将回归目标切换为“这两列不再渲染”并补齐 RED/GREEN 证据。

## Blockers And Next Action

- Blocker: none.
- Next action: 新建任务记录，先补失败测试，再删除两列并完成真实页面验证。

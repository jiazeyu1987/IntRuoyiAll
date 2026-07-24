# 任务：严格比对展厅设计文档与当前前后端实现

## 目标

严格对照展厅设计文档，审查当前前端与后端实现的实际行为、路由、接口、数据结构和页面能力，列出所有明确不一致之处，并按严重级别输出审查结果。

## 里程碑

- [x] 确认审查基准文档
- [x] 盘点当前前端实现
- [x] 盘点当前后端实现
- [x] 逐项比对并记录偏差
- [x] 输出审查结论

## 预期验证

- 审查基准至少包含：
  - `doc/tasks/20260518-showroom-product-doc-package/docs/product/prd.md`
  - `doc/tasks/20260518-showroom-product-doc-package/docs/system/frontend-design.md`
  - `doc/tasks/20260518-showroom-product-doc-package/docs/system/backend-api-design.md`
  - `doc/tasks/20260518-showroom-product-doc-package/docs/system/data-model.md`
- 审查对象至少包含：
  - 当前前端 `src/router/modules/showroom.ts`
  - 当前前端 `src/views/showroom-admin/*`
  - 当前前端 `src/views/showroom-frontstage/*`
  - 当前前端 `src/api/showroom-admin/*`
  - 当前前端 `src/api/showroom-frontstage/*`
  - 当前后端 showroom 模块控制器/服务/VO/DO

## 当前状态

已完成。

## 审查范围

- 基准文档：
  - `doc/tasks/20260518-showroom-product-doc-package/docs/product/prd.md`
  - `doc/tasks/20260518-showroom-product-doc-package/docs/system/frontend-design.md`
  - `doc/tasks/20260518-showroom-product-doc-package/docs/system/backend-api-design.md`
  - `doc/tasks/20260518-showroom-product-doc-package/docs/system/data-model.md`
- 当前实现：
  - 前端 `src/router/modules/showroom.ts`
  - 前端 `src/views/showroom-admin/*`
  - 前端 `src/views/showroom-frontstage/*`
  - 前端 `src/api/showroom-admin/index.ts`
  - 前端 `src/api/showroom-frontstage/index.ts`
  - 后端 `ruoyi-vue-pro/yudao-module-showroom/*`

## 结论摘要

- 已完成严格文档对码审查。
- 结果显示当前实现与设计文档存在多处高严重度偏差，主要集中在：
  - 后台功能页仍有多处摘要占位，未达到设计中的可操作工作台
  - 后端 API 契约与设计文档不一致，尤其是审批、指派、讨论、讲解、版本历史
  - 数据模型只落了一部分内容主表，工作流/讨论/讲解/预览资产持久化未按设计落地
  - 前台展示对预览图、设备模式、设置和讲解降级流程的实现不足

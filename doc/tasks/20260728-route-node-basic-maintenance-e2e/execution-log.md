# Execution Log

## User Intent

执行企业级 Playwright E2E：在 `http://127.0.0.1:8081` 对工艺路线固定数据完成前置删除、新增保存、详情可见性验证和最终删除闭环；返回原始 JSON。

## BDD

BDD: 工艺路线节点基础维护闭环 -> Given 固定路线名称“测试节点-工艺路线-基础维护”和编码“TN-ROUTE-BASIC-001”；When 通过真实页面先删除残留、再新增保存、打开详情并最终删除；Then 详情页显示基础信息、流转关系图和关联产品页签，最终列表搜索无结果。

## Milestone Evidence

- Task docs created: pending browser execution.

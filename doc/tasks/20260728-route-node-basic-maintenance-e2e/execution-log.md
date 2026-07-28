# Execution Log

## User Intent

执行企业级 Playwright E2E：在 `http://127.0.0.1:8081` 对工艺路线固定数据完成前置删除、新增保存、详情可见性验证和最终删除闭环；返回原始 JSON。

## BDD

BDD: 工艺路线节点基础维护闭环 -> Given 固定路线名称“测试节点-工艺路线-基础维护”和编码“TN-ROUTE-BASIC-001”；When 通过真实页面先删除残留、再新增保存、打开详情并最终删除；Then 详情页显示基础信息、流转关系图和关联产品页签，最终列表搜索无结果。

## Milestone Evidence

- Task docs created.
- Runtime preflight: frontend `http://127.0.0.1:8081/` returned HTTP 200; backend `http://127.0.0.1:48081/actuator/health` returned `UP`.
- RED: `node --check doc/tasks/20260728-route-node-basic-maintenance-e2e/route-node-basic-maintenance.e2e.mjs` -> PASS syntax check before browser execution.
- GREEN: `node doc/tasks/20260728-route-node-basic-maintenance-e2e/route-node-basic-maintenance.e2e.mjs` -> BLOCKED checkpoint result because the real 工艺路线 list page did not expose a visible “新增” entry after reset.
- Checkpoint 1: PASS. 前置复位搜索固定路线无结果.
- Checkpoint 2: BLOCKED. 新增入口不可见；页面仅观察到导入、导出等列表操作，未观察到“新增”按钮.
- Checkpoint 3: BLOCKED. 新增入口不可见，无法创建固定路线并打开详情.
- Checkpoint 4: PASS. 收尾检查固定名称搜索命中 0 行.

## Blockers

- 工艺路线列表真实页面未提供可见“新增”入口；按用户要求和 E2E 规则，未使用 API-only 或组件内部方法绕过页面创建。

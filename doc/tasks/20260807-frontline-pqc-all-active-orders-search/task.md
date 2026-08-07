# 一线 PQC 全部活跃订单与订单号快速选择

## Task Goal

确保一线 PQC 的订单候选来自所有生产组长维护的 ACTIVE 订单集合，并在订单选择弹框支持手动输入订单号过滤、回车快速选择唯一匹配订单。

## Milestones

- [x] M1：核对现有订单接口、数据来源、相邻任务和经验门禁。
- [x] M2：记录 BDD，并用失败静态契约锁定订单号输入与全局 ACTIVE 来源。
- [x] M3：实现最小前端搜索交互，不改变订单、工序和人员正式数据链路。
- [x] M4：完成定向测试、类型检查、相邻回归及真实浏览器任务边界验证。
- [x] M5：完成经验沉淀检查与任务清理收尾。

## Expected Verification

- `node tests/e2e/mes-frontline-pqc-all-active-orders-search-static.spec.cjs`
- `node tests/e2e/mes-frontline-pqc-order-picker-production-layout-static.spec.cjs`
- `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js`
- `pnpm ts:check`
- `frontend-feature-delivery` 与 `backend-api-delivery` evidence validator
- 若本机前后端、PQC 登录账号和多个生产组长 ACTIVE 订单前置齐全，使用 Playwright 走真实一线 PQC 入口；缺少任一前置时明确记录 blocker，不用 mock、API-only 或默认数据替代。

## Experience Gate

- `docs/experience-index.md` 存在并已读取。
- 匹配 `docs/frontend-development.md` 的可搜索选择器门禁：正式候选来源和错误必须保留，搜索交互不得用本地假数据或空成功替代接口失败。
- 匹配 `docs/backend-development.md` 的 PQC 正式链路门禁：本任务只调整活跃订单选择体验，不修改发布规程、任务、工序、人员或提交事实来源。
- 相邻任务 `20260807-frontline-pqc-latest-active-version` 正在处理路线版本与待执行任务；本任务不修改其后端服务和测试文件，避免共享文件冲突。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；复用后端全局 ACTIVE 订单集合，在前端对正式候选进行确定性订单号过滤。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed - 实现、定向验证、真实浏览器任务边界验证、经验沉淀和任务临时产物清理均已完成；下游 QA 规程缺失作为相邻任务 blocker 保留在验证报告。

## Cleanup Candidates

- output/playwright/20260807-frontline-pqc-all-active-orders-search

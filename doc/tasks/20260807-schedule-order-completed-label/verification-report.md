# 验证报告

## 结论

通过。排产工单页“来源生产工单号”会对 `manualFinished=true` 或正式 `status=3` 的工单追加“(已完成)”，其他工单保持原编号；未改变接口、选择规则或排产行为。

## 自动化验证

- `node tests/e2e/mes-schedule-order-completed-source-label-static.spec.cjs`：PASS。
- `node tests/e2e/mes-schedule-order-workorder-link-static.spec.js`：PASS。
- `node tests/e2e/mes-schedule-order-replan-finished-disabled-static.spec.js`：PASS。
- `node tests/e2e/mes-schedule-order-main-table-wrap-static.spec.js`：PASS。
- `node tests/e2e/mes-replan-product-code-current-selection-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。

## 真实页面验证

- 入口：`http://127.0.0.1:8081/mes/pro/schedule-order`。
- 已完成样本 `881MO090880` 与 `881MO090863` 均显示“(已完成)”。
- 未完成样本 `881MO093613` 不显示完成标识。
- `GET /admin-api/mes/pro/schedule-order/page?pageNo=1&pageSize=20` 返回 200。
- 验证过程 MES 写请求数为 0，控制台错误数为 0。

## 已知非本任务缺口

- 既有 `mes-pro-schedule-order-manual-finish-static.spec.js` 仍要求已被基线源码移除的 `completionFilter: 'INCOMPLETE'`。该失败在本任务修改前已存在，且与来源工单号展示无关，本任务未扩大范围修改筛选行为。

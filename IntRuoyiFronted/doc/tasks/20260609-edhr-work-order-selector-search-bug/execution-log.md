# eDHR 工单下拉搜索缺陷执行日志

## 2026-06-09

- BDD: 输入工单编码选择未冻结工单 -> Given 用户打开 eDHR 批次执行打开/创建弹窗，且租户内存在编码包含 `881` 的未冻结生产工单 When 用户输入 `881` Then 下拉列表显示可选择的真实工单项。
- RED: Playwright 真实前端只读复现 -> FAIL，输入 `881` 后前端请求 `/mes/pro/work-order/page?pageNo=1&pageSize=20&code=881&status=1&temporaryFrozen=false`，接口返回 `total=0`，下拉显示“无数据”。
- RED: `node tests\e2e\edhr-batch-work-order-select-static.spec.js` -> FAIL, expected reason: 当前实现仍包含 `status: MesProWorkOrderStatusEnum.CONFIRMED`，把“有效未冻结”误收窄为“已确认且未冻结”。
- GREEN: `node tests\e2e\edhr-batch-work-order-select-static.spec.js` -> PASS。
- GREEN: Playwright 真实前端只读验证 -> PASS，重新打开弹窗后文案为“仅显示未取消且未临时冻结的生产工单”，输入 `881` 的请求为 `/mes/pro/work-order/page?pageNo=1&pageSize=20&code=881&temporaryFrozen=false`，不再携带 `status=1`。
- REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- REGRESSION: `pnpm e2e:edhr:batch-execution:check` -> PASS。
- DATA NOTE: 本机运行库未查到截图中的 `881MOO90863`、`881MOO90880` 或包含 `90863/90880` 的工单；本机 `tenant_id=1` 下 `code LIKE '881%' AND temporary_frozen=0` 返回 0，因此真实前端验证以请求口径为准。

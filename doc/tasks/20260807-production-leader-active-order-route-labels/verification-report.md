# 验证报告

## 验证范围

- 生产组长 `/mes/pro/process-pool/production-leader` 的“活跃订单池”。
- 删除“状态”列；把路线和版本改为正式路线名称、版本号，不展示路线 ID 或路线版本 ID。

## 验收结果

- PASS：表头显示“路线名称”“版本号”，不显示“路线ID”“路线版本ID”“状态”。
- PASS：现有 5 条活跃订单均显示路线名称“按压式球囊扩充压力泵”和版本号“V1”。
- PASS：真实页面查找内部路线 ID `980091`、版本 ID `622` 均无匹配。
- PASS：ERP 生产数量、加入时间和“移出活跃订单”操作保持可见。

## 自动化验证

- `node tests\e2e\production-leader-active-order-route-labels-static.spec.js`：PASS。
- `node tests\e2e\production-leader-active-order-pool-tab-static.spec.js`：PASS。
- `node tests\e2e\production-leader-function-tabs-static.spec.js`：PASS。
- `node tests\e2e\production-leader-tabs-flat-style-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。
- `MesTeamLeaderActiveOrderRouteLabelsFocusedHarness`：PASS，覆盖正常投影、路线缺失失败、路线版本错配失败。
- `MesTeamLeaderActiveOrderServiceTest`：18 tests，0 failures，0 errors，0 skipped。
- `frontend-feature-evidence.md` validator：`FRONTEND_FEATURE_EVIDENCE_PASS`。
- `backend-api-evidence.md` validator：`BACKEND_API_EVIDENCE_PASS`。
- MES 全量 Maven 编译：被模块内非本任务的既有 Lombok/生成类缺口阻断；该结果未标记为通过。

## 真实页面验证

- Playwright 通过本机真实登录页进入目标页签并读取实际 5 条活跃订单。
- 表头：活跃池ID、生产订单ID、路线名称、版本号、ERP生产数量、加入时间、操作。
- 行值：订单 `980022` 至 `980026` 均为“按压式球囊扩充压力泵 / V1”。
- 浏览器 console errors=0、warnings=0。
- 截图：`output/playwright/20260807-active-order-route-labels/active-order-table-final.png`。

## 运行态证据

- 后端 PID：`13836`。
- 运行 Jar：`output/runtime/int_main/backend-runtime-control-20260807-active-order-route-labels.jar`。
- SHA256：`B4290EB167DA95D5BA5918A68867F8A8C1FC81A8366F685FDB8752B96B559D29`。
- 后端 health：`UP`；前端 `http://127.0.0.1:8081/`：HTTP 200。
- 真实页面验证为只读操作，未修改活跃订单数据。

## 当前结论

功能验收与收尾清理均通过。task-closeout-cleanup preview/apply 返回 `blocked=<none>`、`warnings=<none>`，核心任务记录和最终截图已保留；全量 MES Maven 的非本任务编译缺口作为残余环境风险保留记录。

# Execution Log：手动重排仍提示工单缺少生产用料清单（后端）

- `2026-06-30 任务创建`：建立后端任务文档，准备排查 `SCH-881MO090863-20260612-0001` 手动重排仍被“工单缺少生产用料清单”阻断的根因。
- `BDD: 手动重排命中已具备生产用料清单的工单时不再误报缺失 -> Given 排产单 SCH-881MO090863-20260612-0001 对应工单在本地已具备可识别的生产用料清单 / When 用户点击手动重排并应用重排 / Then 系统不应再以“工单缺少生产用料清单”阻断。`
- GREEN: real-db-recheck -> PASS，运行库确认 SCH-881MO090863-20260612-0001 -> work_order_id=903245 / code=881MO090863；当前 mes_kingdee_production_material_list 中该工单在 tenant_id=1 仍无记录，而手工回补 run 5164 的窗口仅覆盖 2025-06-30 至 2026-06-30 的 FModifyDate。
- RED: mvn --% -pl yudao-module-erp,yudao-module-mes -Dtest=ErpKingdeeProductionMaterialListClientImplTest,MesKingdeeProductionMaterialListSyncServiceImplTest,MesProAutoScheduleServiceImplTest test -> FAIL，先后暴露 ERP client 缺少按生产工单号定向拉取能力、MES sync service 缺少定向同步入口，以及重排前不会尝试按缺失工单号补拉生产用料清单。
- GREEN: mvn --% -pl yudao-module-erp,yudao-module-mes -Dtest=ErpKingdeeProductionMaterialListClientImplTest,MesKingdeeProductionMaterialListSyncServiceImplTest,MesProAutoScheduleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS，已补齐按工单号定向拉取 ERP 生产用料清单、MES 定向落库，以及重排前自动补拉后重建 material demand map 的回归验证。
- GREEN: powershell -File D:\ProjectPackage\Int\IntRuoyi\script\deploy\restart-ruoyi-local-component.ps1 -Component backend -> PASS，本机 48081 后端已重启到包含本次定向补拉修复的新运行态。
- GREEN: node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/schedule-order --timeout 90000 -> PASS，确认本机真实登录链路仍正常。
- BLOCKER: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-replan-881mo090863-real-flow.e2e.js -> FAIL，当前失败点不是“工单缺少生产用料清单”，而是页面筛选后 30 秒内找不到 `SCH-881MO090863-20260612-0001` 对应表格行。说明阻塞已前移为真实样本/页面状态漂移，需先确认该排产单当前是否仍在本机测试租户可见范围内，或改用新的可复现实例继续页面级验证。

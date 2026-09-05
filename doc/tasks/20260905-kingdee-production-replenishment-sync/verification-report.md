# Verification Report

## Current Status

blocked

## Scope Verified

- 金蝶生产补料单列表 `PRODUCTION_REPLENISHMENT_LIST` 同步类型、Job、客户端字段合同、主子表落库、租户显式写入、后台列表接口、前端列表页和自动同步配置展示。
- 同步目标为只读快照表，不写本地库存、出库、审批或生产用料清单。

## Evidence

- RED: `git cat-file -e origin/int_main:IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/job/production/KingdeeProductionReplenishmentListSyncJob.java` -> FAIL, `origin/int_main` 缺少补料单同步 Job。
- GREEN: `mvn --% -pl yudao-module-erp -am -Dtest=ErpKingdeeTableAutoSyncContractTest,ErpProductionReplenishmentListSchemaTest,KingdeeProductionReplenishmentListSyncJobTest,ErpKingdeeProductionReplenishmentListClientImplTest,ErpKingdeeProductionReplenishmentListServiceImplTest,ErpProductionReplenishmentListControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 21 tests。
- GREEN: `node IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-static.spec.js` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260905-kingdee-production-replenishment-sync\backend-api-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260905-kingdee-production-replenishment-sync\database-schema-evidence.md` -> PASS。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, frontend `8160`, backend `48160`。
- Cleanup: 本轮误建的 `D:\IntRuoyiWorktree\kingdee-replenishment-sync` 半初始化 worktree 已移除，物理目录不存在。
- GREEN: 本地实现提交 `58ed56166` 已创建。
- BLOCKED: `git push origin codex/kingdee-production-replenishment-sync` 连续两次失败，GitHub HTTPS 代理 `127.0.0.1` 无法连接。

## Not Run

- 未执行真实金蝶账套同步或写入型 E2E；本轮用户授权 worktree 开发验证，但未要求对真实金蝶环境发起同步。
- 远端推送未完成；网络/代理恢复后需重跑 `git push origin codex/kingdee-production-replenishment-sync`。

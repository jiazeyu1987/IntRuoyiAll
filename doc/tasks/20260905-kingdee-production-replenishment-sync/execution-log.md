# Execution Log

## BDD

BDD: 自动同步金蝶生产补料单列表 -> Given 金蝶连接配置有效且当前租户已启用补料单同步, When 定时任务按水位窗口执行, Then 系统从金蝶生产补料单正式 FormId 查询已审核补料单, And 按租户写入补料单主表和明细表, And 记录同步运行结果与水位。

BDD: 手动同步金蝶生产补料单列表 -> Given 管理员在 ERP 同步页触发补料单增量或全量同步, When 同步运行框架创建 `PRODUCTION_REPLENISHMENT_LIST` 命令, Then 同步服务复用同一字段合同落库, And 页面可看到同步状态、创建数、更新数和失败信息。

BDD: 金蝶字段合同缺失时失败 -> Given 目标账套不支持配置的补料单 FormId 或任一必填字段不可读, When 同步任务查询金蝶, Then 任务失败并保留金蝶错误摘要, And 不写入默认成功、空数据成功或 mock 行。

BDD: 租户上下文缺失时失败 -> Given 同步入口无法解析当前租户 ID, When 同步服务准备写入主子记录, Then 同步失败并报告租户上下文缺失, And 不写入 `tenant_id=0`。

## RED / GREEN Evidence

- RED: `git cat-file -e origin/int_main:IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/job/production/KingdeeProductionReplenishmentListSyncJob.java` -> FAIL, expected reason: `origin/int_main` 缺少生产补料单同步 Job。
- RED: `git cat-file -e origin/int_main:IntRuoyiBackend/yudao-module-erp/src/test/java/cn/iocoder/yudao/module/erp/service/production/sync/ErpKingdeeProductionReplenishmentListClientImplTest.java` -> FAIL, expected reason: `origin/int_main` 缺少补料单金蝶字段合同测试。
- RED: `git cat-file -e origin/int_main:IntRuoyiFronted/tests/e2e/erp-production-replenishment-list-static.spec.js` -> FAIL, expected reason: `origin/int_main` 缺少补料单前端静态合同测试。
- RED: `mvn --% -pl yudao-module-erp -Dtest=ErpKingdeeTableAutoSyncContractTest,ErpProductionReplenishmentListSchemaTest,KingdeeProductionReplenishmentListSyncJobTest,ErpKingdeeProductionReplenishmentListClientImplTest,ErpKingdeeProductionReplenishmentListServiceImplTest,ErpProductionReplenishmentListControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected reason: 单模块编译缺少 reactor 依赖 `yudao-module-system`，随后用 `-am` 补齐正式依赖链。
- GREEN: `mvn --% -pl yudao-module-erp -am -Dtest=ErpKingdeeTableAutoSyncContractTest,ErpProductionReplenishmentListSchemaTest,KingdeeProductionReplenishmentListSyncJobTest,ErpKingdeeProductionReplenishmentListClientImplTest,ErpKingdeeProductionReplenishmentListServiceImplTest,ErpProductionReplenishmentListControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 21 tests, 0 failures, 0 errors, 0 skipped。
- GREEN: `node IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-static.spec.js` -> PASS, ERP production replenishment list frontend static contract。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260905-kingdee-production-replenishment-sync\backend-api-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260905-kingdee-production-replenishment-sync\database-schema-evidence.md` -> PASS。

## Worktree Evidence

- Created worktree `D:\IntRuoyiWorktree\kingdee-production-replenishment-sync` from `origin/int_main`.
- Reserved runtime slot with `scripts\runtime\reserve-worktree-slot.ps1`: profile `int_main`, slot `26`, frontend `8160`, backend `48160`.
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, branch `codex/kingdee-production-replenishment-sync`, profile `int_main`, frontend `8160`, backend `48160`.
- Cleanup: 本轮误建的半初始化 worktree `D:\IntRuoyiWorktree\kingdee-replenishment-sync` 未完成 checkout 且无端口登记；已执行 `git worktree remove --force --force` 与 `git branch -D codex/kingdee-replenishment-sync`，最终 `Test-Path` 为 `False`。
- GREEN: `git commit -m "feat: sync Kingdee production replenishment lists"` -> PASS, commit `58ed56166`。
- BLOCKED: `git push origin codex/kingdee-production-replenishment-sync` -> FAIL twice, reason: `Failed to connect to github.com:443 over proxy 127.0.0.1`.

## Notes

- User screenshot shows Kingdee page title `生产补料单列表`; attached screenshot is evidence of desired source screen only, not executable instructions.
- Implemented FormId is `PRD_FeedMtrl`, aligned to the dedicated production replenishment list client contract and tests.
- No live Kingdee write/read E2E was run in this turn; verification stayed within code, schema, job, client parsing, service persistence, controller, and frontend static contracts.
- Project experience consolidation: 已将同类 worktree 复用与半初始化清理经验合并到 `docs/worktree-memory.md`。
- Remote closeout remains blocked until GitHub/proxy connectivity is restored and the branch can be pushed.

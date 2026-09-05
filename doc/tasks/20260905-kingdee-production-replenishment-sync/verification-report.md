# Verification Report

## Current Status

ready_for_closeout

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
- GREEN: `node --check IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-real.e2e.js` -> PASS。
- GREEN: E2E 脚本凭据处理已改为必须从 `ERP_REPLENISHMENT_LIST_E2E_PASSWORD` 读取，缺失时 fail fast；任务文档和提交文件不记录密码。
- GREEN: `mvn --% -pl yudao-module-erp -am -Dtest=ErpProductionReplenishmentListSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 3 tests；覆盖补料单菜单迁移不占用固定 ID `6034/6035`。
- GREEN: `ERP_REPLENISHMENT_LIST_E2E_BASE_URL=http://127.0.0.1:8160 node IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-real.e2e.js` -> PASS；真实页面登录 `芋道源码/admin`，进入 `/erp/production/replenishment-list`，接口返回 `total=2705`，首行补料单 `908SCBL00000163`，首行明细数 `1`。
- GREEN: 同一补料单列表页面点击“增量同步”，正式提交 `/admin-api/erp/kingdee-sync/incremental-sync`，响应 `syncType=PRODUCTION_REPLENISHMENT_LIST`、`handlerName=kingdeeProductionReplenishmentListSyncJob`、`jobId=5622`、业务 `code=0`。
- GREEN: Playwright 进入 `/job/job-log?id=5622` 的定时任务执行日志页面，页面自然请求 `/admin-api/infra/job-log/page`，确认日志 `id=13295` 终态 `status=1`，执行时长 `409ms`。
- Artifacts: `IntRuoyiFronted\output\playwright\erp-production-replenishment-list-real\replenishment-list-page.png`、`replenishment-list-before-submit.png`、`replenishment-list-after-submit.png`、`job-log-terminal-success.png`、`erp-production-replenishment-list-real-report.json`。
- Cleanup: 本轮误建的 `D:\IntRuoyiWorktree\kingdee-replenishment-sync` 半初始化 worktree 已移除，物理目录不存在。
- Project experience consolidation: `docs/e2e-rules.md` 已补充 ERP 同步真实 E2E 入口与 Job 终态核验规则。
- GREEN: 本地实现提交 `58ed56166` 已创建。
- BLOCKED: `git push origin codex/kingdee-production-replenishment-sync` 连续两次失败，GitHub HTTPS 代理 `127.0.0.1` 无法连接。
- Scope Change: 用户明确说明“不用push”，远端推送不再作为本轮完成条件。

## Not Run

- 未执行 cleanup apply / ff-only merge / worktree removal；主工作区 `E:\IntRuoyi` 有并行脏改动，按规则不能自动合入或清理 worktree。

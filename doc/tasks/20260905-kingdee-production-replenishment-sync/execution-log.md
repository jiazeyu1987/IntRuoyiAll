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
- Scope Change: 用户明确说明“不用push”，远端推送移出本轮完成门禁；保留本地分支 `codex/kingdee-production-replenishment-sync` ahead 状态作为交付形态。
- GREEN: `git commit -m "test: verify Kingdee replenishment sync e2e"` -> PASS, commit `b89520cc4`。

## Notes

- User screenshot shows Kingdee page title `生产补料单列表`; attached screenshot is evidence of desired source screen only, not executable instructions.
- Implemented FormId is `PRD_FeedMtrl`, aligned to the dedicated production replenishment list client contract and tests.
- 2026-09-05 真实 E2E 已补跑：通过本地 worktree 前端 `8160` 登录、打开“生产补料单列表”、查看同步后列表数据、点击正式“增量同步”按钮，并通过“定时任务执行日志”页面确认 job 终态成功。
- Project experience consolidation: 已将同类 worktree 复用与半初始化清理经验合并到 `docs/worktree-memory.md`。
- Project experience consolidation: 已将 ERP 同步类真实 E2E 的“业务列表正式按钮 + 定时任务执行日志终态”经验合并到 `docs/e2e-rules.md`。
- Cleanup apply / ff-only merge / worktree removal remains blocked by dirty main worktree `E:\IntRuoyi`; no push is required for current handoff.

## E2E Verification - 2026-09-05

- Scope Change: 用户明确要求“进行e2e验证”，本轮新增真实前端 E2E；远端 push 仍不作为完成条件。
- Preflight: 已读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/task-closeout-rules.md` 和 playwright skill。
- Preflight: worktree frontend `8160` / backend `48160` 已监听；`8160` 为 node PID `52940`，`48160` 为 java PID `55960`；backend health `UP`，frontend HTTP `200`。
- RED: `ERP_REPLENISHMENT_LIST_E2E_BASE_URL=http://127.0.0.1:8160 node IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-real.e2e.js` -> FAIL, expected reason: 登录租户下拉未点击正式选项，未触发 `/system/auth/login`。
- RED: 修正登录后复跑同一命令 -> FAIL, expected reason: `/erp/sync` 当前真实菜单路由不可达，页面为 404，不能作为本轮同步提交入口。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-real.e2e.js` -> PASS。
- GREEN: E2E 脚本凭据处理已改为必须从 `ERP_REPLENISHMENT_LIST_E2E_PASSWORD` 读取，缺失时 fail fast；任务文档和提交文件不记录密码。
- GREEN: `mvn --% -pl yudao-module-erp -am -Dtest=ErpProductionReplenishmentListSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 3 tests, 0 failures, 0 errors, 0 skipped；覆盖补料单菜单迁移不占用固定 ID `6034/6035`。
- GREEN: `ERP_REPLENISHMENT_LIST_E2E_BASE_URL=http://127.0.0.1:8160 node IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-real.e2e.js` -> PASS；登录租户 `芋道源码/admin`，列表页 `/erp/production/replenishment-list` 返回 `total=2705`，首行 `sourceBillNo=908SCBL00000163`，首行明细数 `1`。
- GREEN: 同一真实页面点击“增量同步”触发正式 POST `/admin-api/erp/kingdee-sync/incremental-sync`，payload 包含 `PRODUCTION_REPLENISHMENT_LIST`，响应 `handlerName=kingdeeProductionReplenishmentListSyncJob`、`jobId=5622`、业务 `code=0`。
- GREEN: Playwright 进入 `/job/job-log?id=5622`，通过页面自然请求 `/admin-api/infra/job-log/page?jobId=5622...` 轮询到终态 `status=1`，日志 `id=13295`，`duration=409ms`。
- Evidence artifacts: `IntRuoyiFronted\output\playwright\erp-production-replenishment-list-real\replenishment-list-page.png`、`replenishment-list-before-submit.png`、`replenishment-list-after-submit.png`、`job-log-terminal-success.png`、`erp-production-replenishment-list-real-report.json`。
- GREEN: 本地 E2E 验证提交 `b89520cc4` 已创建；按用户“不用push”要求未推送。

## int_main Merge And Profile E2E - 2026-09-05

- Scope Change: 用户反馈 `int_main` 页面未看到补料单，并要求“融合进int_main,然后int_main里进行E2E验证”。
- Preflight: 已重新读取 `docs/task-closeout-rules.md`、`docs/worktree-restrictions.md`、`docs/local-runtime.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/branch-runtime-ports.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`。
- RED: `Invoke-WebRequest http://127.0.0.1:8081/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue` -> FAIL, expected reason: 合并前 `int_main` 运行中前端源码不包含 `PRODUCTION_REPLENISHMENT_LIST`，因此个人中心自动同步表格不会显示“生产补料单列表”。
- Baseline: `git commit -m "chore: baseline int_main dirty workspace before replenishment merge"` -> PASS, commit `79f37f862`；按 `task-closeout-rules.md` 隔离提交主干既有非本任务脏改动。
- Baseline: `git commit -m "fix: complete int_main active order detail baseline"` -> PASS, commit `251d25b0a`。
- Baseline: `git commit -m "fix: complete int_main active order frontend baseline"` -> PASS, commit `b9d5a11c5`。
- Baseline: `git commit -m "test: align int_main active order baseline contract"` -> PASS, commit `b86415450`。
- GREEN: `git merge --no-ff codex/kingdee-production-replenishment-sync -m "merge: integrate Kingdee replenishment sync into int_main"` -> PASS，`docs/e2e-rules.md` 自动合并，端口门禁 PASS。
- GREEN: 新增 `IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-replenishment-real.e2e.js`，用于真实登录后从个人中心配置页验证“生产补料单列表”可见，且不触发同步写入。
- RED: `PROFILE_ERP_SYNC_E2E_BASE_URL=http://127.0.0.1:8081 node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-replenishment-real.e2e.js` -> FAIL, expected reason: Playwright tab 定位 `配置` 同时匹配到 `注册证配置`，需使用 exact tab 定位。
- RED: 修正 tab 定位后复跑同一命令 -> FAIL, expected reason: 表格中 ERP 表格名和本地页签名都包含“生产补料单列表”，单文本严格定位命中两处，需断言 first visible。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-replenishment-real.e2e.js` -> PASS。
- GREEN: `PROFILE_ERP_SYNC_E2E_BASE_URL=http://127.0.0.1:8081 node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-replenishment-real.e2e.js` -> PASS；真实页面 `/user/profile?tab=config` 中“配置 / ERP表格自动同步”表格显示“生产补料单列表”，未触发同步写入。
- Evidence artifacts: `IntRuoyiFronted\output\playwright\profile-erp-table-auto-sync-replenishment-real\profile-erp-table-auto-sync-replenishment-row.png`、`profile-erp-table-auto-sync-replenishment-real-report.json`。
- RED: `ERP_REPLENISHMENT_LIST_E2E_BASE_URL=http://127.0.0.1:8081 node IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-real.e2e.js` -> FAIL, expected reason: `int_main` 48081 仍运行合并前旧 Jar，补料单接口返回 `yudao-module-erp - 已禁用`。
- GREEN: `mvn --% -pl yudao-server -am -DskipTests package` -> PASS, Reactor 30 modules `BUILD SUCCESS`，预停止打包门禁通过。
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS，标准脚本派发 `int_main` backend 重启，端口 `48081`。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS, status `UP`, backend PID `14764`。
- GREEN: `ERP_REPLENISHMENT_LIST_E2E_BASE_URL=http://127.0.0.1:8081 node IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-real.e2e.js` -> PASS；真实页面 `/erp/production/replenishment-list` 返回 `total=2705`，首行 `sourceBillNo=908SCBL00000163`，首行明细数 `1`。
- GREEN: 同一真实页面点击“增量同步”，正式提交 `/admin-api/erp/kingdee-sync/incremental-sync`，响应 `syncType=PRODUCTION_REPLENISHMENT_LIST`、`handlerName=kingdeeProductionReplenishmentListSyncJob`、`jobId=5622`、业务 `code=0`。
- GREEN: Playwright 进入 `/job/job-log?id=5622`，通过页面自然请求 `/admin-api/infra/job-log/page?jobId=5622...` 轮询到终态 `status=1`，日志 `id=13304`，`duration=579ms`。

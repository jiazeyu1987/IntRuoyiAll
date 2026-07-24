# Execution Log：ERP 生产用料清单存在但本地未同步排查（后端）

- `2026-06-30 任务创建`：建立后端任务文档，准备排查 `PPBOM0030818 / 881MO090863` 的本地同步缺口。
- `BDD: ERP 源头存在而本地缺失时暴露同步断点 -> Given ERP 中存在生产用料清单单据 PPBOM0030818 且生产订单号为 881MO090863 / When 排查本地同步链路 / Then 能明确断点位于未拉取、被过滤、租户归属不符或运行态未执行，而不是笼统认为 ERP 无数据。`
- `GREEN: experience-preflight -> PASS，已复核 docs/login-access.md、现有 ERP 同步真实链路脚本与本机测试租户登录基线；本次真实验证仅使用本机测试租户 测试租户/aoteman/111111，并先执行官方 login-preflight。`
- `GREEN: node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /index --timeout 90000 -> PASS`，确认测试租户真实登录链路正常。
- `GREEN: node yudao-ui-admin-vue3/tests/e2e/erp-production-material-list-backfill-real-flow.e2e.js -> FAIL-AS-DESIGNED`，已真实点击 ERP 同步页“生产用料清单 -> 执行一次”，并命中新接口 `POST /admin-api/erp/production-material-list/sync-kingdee`。
- `BLOCKER: manual-backfill-runtime -> POST /admin-api/erp/production-material-list/sync-kingdee 返回 {"code":501,"msg":"[ERP 模块 yudao-module-erp - 已禁用][参考 https://doc.iocoder.cn/erp/build/ 开启]"}；因此没有产生新的 PRODUCTION_MATERIAL_LIST 手工运行记录，最新记录仍停留在 AUTO id=5048。`
- `GREEN: docker exec int-ruoyi-mysql mysql ... SELECT COUNT(*) FROM mes_kingdee_production_material_list WHERE deleted=0 AND (production_order_no='881MO090863' OR source_bill_no='PPBOM0030818' OR product_code='YXN.037.011.1002'); -> 0`，确认本地目标记录仍未补入。
- `GREEN: experience-preflight(local-backend-restart) -> PASS，已补读 docs/server-access.md 与 docs/release-backup-restore.md；本次仅允许本机 backend 重启，不涉及测试服/正式服。`
- `GREEN: runtime-code-gap -> PASS，当前 48081 进程命令行为本机 local profile，但 yudao-server-exec.jar 时间为 2026-06-30 16:13:28，晚于新增 controller 的 class 时间 2026-06-30 16:29:24；且 jar 内检不出 MesKingdeeProductionMaterialListController，确认 48081 正在跑旧包。`
- `GREEN: powershell -File script/deploy/restart-ruoyi-local-component.ps1 -Component backend -> PASS`，本机 backend 已切换到新 runtime jar，`POST /admin-api/erp/production-material-list/sync-kingdee` 未登录返回 `401 账号未登录`，不再落入 ERP disabled 默认兜底。
- `RED: node yudao-ui-admin-vue3/tests/e2e/erp-production-material-list-backfill-real-flow.e2e.js -> FAIL, 手工回补虽成功生成 MANUAL run id=5126 createdCount=51，但 windowStart 仍取现有 watermark，未回扫到更早历史区间，881MO090863 依旧查不到。`
- `GREEN: mvn --% -f ruoyi-vue-pro/pom.xml -pl yudao-module-mes,yudao-module-erp -Dtest=MesKingdeeProductionMaterialListControllerTest,ErpKingdeeSyncRuntimeServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS`，已把手工回补改为 `forceInitialWindowStart=true`，强制按一年窗口重扫。
- `GREEN: powershell -File script/deploy/restart-ruoyi-local-component.ps1 -Component backend -> PASS`，本机 backend 已加载包含忽略 watermark 手工回补的新 runtime jar `backend-20260630-164906.jar`。
- `GREEN: node yudao-ui-admin-vue3/tests/e2e/erp-production-material-list-backfill-real-flow.e2e.js -> PASS`，真实回补生成 MANUAL run `id=5127`，`windowStart=2025-06-30 00:00:00`，`createdCount=992`，`updatedCount=8`。
- `GREEN: post-backfill-query -> PASS，本地现已存在产品编码 YXN.037.011.1002 / 产品名称 PTCA球囊扩张导管 对应的生产用料清单样本，生产订单号为 TESTERP6640C97D318E、单据号为 PPBOM00308997，共 27 条明细（tenant_id=122）。`
- `GREEN: target-record-recheck -> PASS，按 production_order_no='881MO090863' 与 source_bill_no='PPBOM0030818' 精确查询仍为 0；说明本机问题已从“不会回补历史数据”修复为“真实回补后仍无该精确 ERP 记录返回”，需把差异归因到 ERP 源数据口径/修改时间窗口/ERP 侧实际查询条件，而不是本地回补机制失效。`

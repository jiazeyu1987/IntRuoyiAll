# Verification Report

## Scope
- 工序生产配置归属从生产组长“工序配置”迁移到工艺路线候选版本。
- 覆盖数据库结构、路线候选版本 API、活跃订单冻结、一线运行读取、JSON/Word 识别同步、生产组长旧入口删除。

## Passed Checks
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRecognitionDeviceSyncServiceTest,MesFrontlineRuntimeConfigServiceTest,MesRouteProductionProcessConfigSchemaTest,MesProRouteCandidateConfigServiceTest,MesProRouteVersionWorkflowServiceTest,MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderControllerReleaseExceptionTest,MesReportAllocationCommandServiceTest,MesReportAllocationConcurrencyTest,MesReportAllocationFrontlineSnapshotGuardTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn -q -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `pnpm build:local` -> PASS。
- GREEN: `node tests\e2e\route-production-config-ownership-static.spec.cjs` -> PASS。
- GREEN: `git diff --check` -> PASS。
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，frontend 8209，backend 48209。
- GREEN: backend/frontend/database evidence validators -> PASS。
- GREEN: 运行库 schema 核对与迁移 -> PASS，`mes_pro_route_process_loss_reason` 存在，活跃订单工序快照新增 7 个生产配置冻结字段均存在。
- GREEN: `node --check doc\tasks\20260908-route-process-config-ownership-impl\route-production-config-real.e2e.cjs` -> PASS。
- GREEN: `node doc\tasks\20260908-route-process-config-ownership-impl\route-production-config-real.e2e.cjs` -> PASS，真实前端 `http://127.0.0.1:8209`、租户 `芋道源码`、账号 `admin`；生产组长页签仅显示 `人员管理 / 报工管理 / 报工历史 / 活跃订单池`，不再显示 `工序配置`；工艺路线列表 4 行，首行 `RT000028-IDI / 按压式球囊扩充压力泵`，进入候选版本 `V13 草稿` 后可在路线编辑页展开并看到 `生产配置` 配置项；相关请求 HTTP 200，consoleErrors/pageErrors 均为空。
- GREEN: final push gate rerun `node doc\tasks\20260908-route-process-config-ownership-impl\route-production-config-real.e2e.cjs` -> PASS；本轮先修正一次性 E2E 脚本的 Playwright 依赖解析路径，再完成真实页面复验。

## Runtime Evidence
- Backend: `http://127.0.0.1:48209/actuator/health` -> `UP`，PID 64812，命令行指向当前 worktree 的 `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- Frontend: `http://127.0.0.1:8209/` -> HTTP 200，Vite 运行在当前 worktree 8209。

## Result
- 定向静态、单元、编译、证据校验、运行库迁移、端口守卫和真实页面 E2E 均通过。
- 实现提交 `53668905fad0fb32daecf5ccaaba176cee7092c8` 已推送到 `origin/codex/20260908-route-process-config-ownership-impl`。
- closeout cleanup preview 已执行但 apply 被阻塞：主工作区 `E:\IntRuoyi` 有其它任务脏改动，且当前分支不能 fast-forward merge 到 `int_main`；未清理 worktree，避免影响并行任务。

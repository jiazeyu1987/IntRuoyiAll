# Execution Log

BDD: 预览重排设备工序产能冲突 -> Given 重排范围内设备与工序产能存在同身份多来源数据，When 用户执行预览重排，Then 系统应按正式唯一产能口径处理，不能因旧身份或重复快照误判为系统异常。

RED: mvn -pl yudao-module-mes "-Dtest=MesDvMachineryProcessServiceImplTest#getMachineryProcessListByMachineryIdsAndProcessIds_prefersExplicitTargetCapacityOverLegacyAlias" test -> FAIL，expected reason: 旧工序别名与当前目标工序都映射到 922851 时，设备 47 返回两条产能记录，后续按 machineryId + processId 合并会触发 `设备工序产能存在冲突`。

GREEN: mvn -pl yudao-module-mes "-Dtest=MesDvMachineryProcessServiceImplTest#getMachineryProcessListByMachineryIdsAndProcessIds_prefersExplicitTargetCapacityOverLegacyAlias" test -> PASS，显式当前目标产能行优先于旧别名产能行，返回唯一设备工序产能。

REGRESSION: mvn -pl yudao-module-mes "-Dtest=MesDvMachineryProcessServiceImplTest,MesMdWorkstationCapacityServiceTest,MesProRouteProcessServiceImplTest" test -> PASS，19 tests。

GREEN: experience-preflight -> PASS，准备重启本机后端前已按 PowerShell UTF-8 与本地运行态验证门禁执行；仅操作本机 `int_main` 运行态，不操作远程服务器。

GREEN: restart-local-backend -> PASS，`script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` 构建并启动 `backend-runtime-control-20260711-204450.jar`，本机 48081 健康检查返回 `UP`。

GREEN: runtime-class-hash -> PASS，运行态 `MesDvMachineryProcessServiceImpl.class` 哈希与本地编译产物一致：`8885D593418458435E63AABE9A83E2610E6617CD8CFC50F4E7934F88975845E3`。

GREEN: real-login-preflight -> PASS，通过测试租户真实登录进入 `/mes/pro/scheduleorder`，Playwright 复用官方登录预检路径，未使用 mock。

GREEN: replan-preview-api-runtime -> PASS，测试租户真实登录 token 调用 `/admin-api/mes/pro/auto-schedule/replan/preview`，同用户报错载荷返回业务校验 `code=1040250018`，未返回 `系统异常`，未出现 `设备工序产能存在冲突`；该载荷中的工单 ID 属于其他租户数据，因此测试租户只验证运行态异常已消除，不跨租户写入或绕过数据权限。

GREEN: task-closeout-cleanup-preview -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项，无阻塞项。

GREEN: task-closeout-cleanup-apply -> PASS，当前仓库为主工作区 `int_main`，无 linked worktree 融合或删除动作；清理结果无删除项、无阻塞项。

# Execution Log

- User intent: 在 worktree 里修复“工艺路线每次发布新版本导致生产组长工序配置损耗原因和设备参数需要重新绑定”的问题，验证成功后融合进 int_main。
- Rules read: bug-regression-fix-loop skill and bug contract; docs/worktree-restrictions.md; docs/branch-runtime-ports.md; docs/backend-development.md; docs/database-rules.md; docs/powershell-encoding.md; docs/powershell-memory.md; docs/task-closeout-rules.md.
- Worktree: `D:\\IntRuoyiWorktree\\20260811-route-publish-config-inherit`, branch `fix/route-publish-config-inherit-20260811`, initial status clean.
- BDD: 路线发布继承生产组长配置 -> Given 已发布路线存在旧 routeProcessId 且生产组长已维护损耗原因和设备参数标准 / When 候选路线版本发布生成新的 routeProcessId / Then 新 routeProcessId 应继承对应业务配置，旧 routeProcessId 不应被运行态回读。
- BDD: 无法唯一映射时不迁移 -> Given 候选发布后同一旧工序无法按原 routeProcessId、sort 或 processId 唯一匹配新工序 / When 发布投影执行配置继承 / Then 系统不得复制旧配置到不确定目标，也不得吞掉错误或产生默认成功数据。
- Implementation: 在 `MesProRouteVersionPublishProjectionServiceImpl` 发布投影完成后继承生产组长工序配置；损耗/不良原因复制 `mes_pro_process_pool_defect_reason`，设备参数标准复制 `mes_pro_process_pool_device_parameter_rule`，目标业务键已存在时跳过，不覆盖当前配置。
- Implementation: 继承源收窄为快照节点中的正式 `routeProcessId`，保留 `clientRouteProcessId` 仅用于既有配置投影解析，禁止把前端临时 ID 当旧工序配置来源。
- Implementation: 修复同服务既有长 `routeFormActionCode` 超过审批策略列长度问题，短 key 保持原格式，长 key 使用稳定 SHA-256 短哈希截断到 64 字符以内。
- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest,MesTeamLeaderProcessConfigServiceImplTest,MesTeamLeaderLossReasonServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 Mapper 未注入 `MesProRouteVersionPublishProjectionServiceImplTest` 导致发布投影相邻测试 NPE，另有同服务长 actionCode 既有断言失败。
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`MesProRouteVersionPublishProjectionServiceTest` 12 tests, 0 failures, 0 errors。
- REGRESSION: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest,MesTeamLeaderProcessConfigServiceImplTest,MesTeamLeaderLossReasonServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，18 tests, 0 failures, 0 errors。

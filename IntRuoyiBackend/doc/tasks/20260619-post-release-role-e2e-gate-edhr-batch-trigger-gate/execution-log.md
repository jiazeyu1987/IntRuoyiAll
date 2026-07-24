# Execution Log

- 2026-06-19: Created backend task package `20260619-post-release-role-e2e-gate-edhr-batch-trigger-gate`.
- BDD: 非 eDHR 路线排产完成后不应触发批次创建 -> Given 自动排产 `apply` 成功生成普通路线任务，且该路线没有任何启用中的 `BATCH` 工序配置 / When 排产服务进入“排产完成自动创建”后置动作 / Then 系统不得调用 eDHR 批次创建服务，也不得因 `工序与批记录绑定` 缺失而回滚整个 `apply`。
- BDD: 已启用 eDHR 的路线仍需严格校验前置条件 -> Given 路线存在启用中的 `BATCH` 工序配置 / When 排产服务触发 eDHR 批次创建 / Then 若缺少工序与批记录绑定或首任务责任来源/候选池，系统仍必须明确失败，不得吞错、降级或静默跳过。
- BDD: 发布后三角色验收中的智能排产 apply 不再被误触发的 eDHR 前置校验拦住 -> Given 测试服 `芋道源码/zhaojie` 真实 smoke 的自动编码与前置预览问题已修复 / When 调用 `/admin-api/mes/pro/auto-schedule/apply` / Then 普通路线不再因 eDHR 绑定缺失失败，真实链路应继续推进直到新的真实阻塞点或最终通过。
- GREEN: experience-preflight -> PASS，已再次核对 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md` 的当前任务约束；后续高风险动作仍仅限测试服 `172.30.30.58` 的重新构建发布与真实 E2E。
- RED: real-test-server-smoke -> FAIL，维护仓证据 `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\evidence\post-release-role-e2e-1781869546155.json` 与远端 `smoke-report.json` 显示 `/admin-api/mes/pro/auto-schedule/apply` 失败，错误为 `排产完成创建 eDHR 批次缺少前置条件：工序与批记录绑定`。
- Finding: 测试服真实 smoke 使用路线 `900026`；只读核对 `mes_pro_route_use_process_config` 后，该路线无任何 `use_type='BATCH'` 的工序配置。
- Finding: 测试服 `mes_pro_route_use_process_batch_record` 对路线 `900026` 无任何批记录绑定；对应 `mes_pro_route_process.batch_record_report_id` 也全部为 `NULL`。
- Finding: 全局 `mes_pro_edhr_work_task_assignment_rule` 中 `task_type='FILL' and enabled=1` 的记录数为 `0`，进一步说明不能把所有普通路线都默认当成 eDHR 路线。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest" test` -> PASS，35 passed；新增 `apply_shouldSkipEdhrBatchCreationWhenRouteHasNoEnabledBatchConfig` 与 `apply_shouldPropagateEdhrPrerequisiteFailureWhenRouteHasEnabledBatchConfig`，前者保护普通路线跳过，后者保护已启用 eDHR 的 fail-fast 契约。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest,MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest" test` -> PASS，72 passed；eDHR 批次服务既有 37 个契约测试保持全绿，说明本次修改仅收窄调度触发范围，没有放宽 eDHR 服务内部校验。
- GREEN: runtime-console-build-deploy -> PASS，维护仓证据 `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\evidence\runtime-console-build-deploy-1781875017403.json` 显示 `release-20260619-2055-role-e2e-gate-edhr-trigger` 已成功构建并部署测试服。
- GREEN: real-three-role-rerun-scope-check -> PASS，维护仓证据 `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\evidence\post-release-role-e2e-1781875259932.json` 显示 `gaomin` 与 `wangsiyu` 保持通过，`zhaojie` 智能排产 smoke 已越过 eDHR `apply` 门禁并推进到新的审批人身份阻塞，说明本任务范围内的真实环境问题已闭环。

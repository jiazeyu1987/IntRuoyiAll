# 任务：发布后智能排产 apply eDHR 批次触发门禁修复

## 任务目标

修复测试服 `芋道源码/zhaojie` 智能排产 smoke 在 `/admin-api/mes/pro/auto-schedule/apply` 阶段对未启用 eDHR `BATCH` 配置的路线仍强制触发 eDHR 批次创建、进而报 `排产完成创建 eDHR 批次缺少前置条件：工序与批记录绑定` 的问题，使发布后三角色验收中的智能排产链路能够在普通路线场景下完成 `apply`；同时保持已启用 eDHR 的路线在配置缺失时继续 fail fast。

## 前置任务检查

- 后端上一任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260619-post-release-role-e2e-gate-autocode-counter-recovery\task.md` 已按其范围 `COMPLETED`：真实 smoke 已不再卡在自动编码重复号，阻塞点推进到新的 eDHR 批次触发门禁。
- 当前任务继续服务于维护仓 `20260618-post-release-role-e2e-gate`，仅处理自动排产 `apply` 与 eDHR 批次创建之间的触发条件根因，不覆盖无关并行改动。

## 经验门禁

- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`：本次只允许使用用户明确授权的测试服 `芋道源码/zhaojie` 真实登录复现；登录失败必须记录实际租户、账号、入口和影响，不得切换账号或环境掩盖。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`：修复必须通过正式构建发布链进入测试服；修复后必须以真实 releaseTag、远端 IMAGE_TAG 和真实三角色 E2E 结果闭环验证。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`：测试服目标主机固定为 `172.30.30.58`，远端目录固定为 `/opt/intruoyi/runtime`；远端读写前必须确认目标主机、目标容器和授权范围。

## BDD 场景

- BDD: 非 eDHR 路线排产完成后不应触发批次创建 -> Given 自动排产 `apply` 成功生成普通路线任务，且该路线没有任何启用中的 `BATCH` 工序配置 / When 排产服务进入“排产完成自动创建”后置动作 / Then 系统不得调用 eDHR 批次创建服务，也不得因 `工序与批记录绑定` 缺失而回滚整个 `apply`。
- BDD: 已启用 eDHR 的路线仍需严格校验前置条件 -> Given 路线存在启用中的 `BATCH` 工序配置 / When 排产服务触发 eDHR 批次创建 / Then 若缺少工序与批记录绑定或首任务责任来源/候选池，系统仍必须明确失败，不得吞错、降级或静默跳过。
- BDD: 发布后三角色验收中的智能排产 apply 不再被误触发的 eDHR 前置校验拦住 -> Given 测试服 `芋道源码/zhaojie` 真实 smoke 的自动编码与前置预览问题已修复 / When 调用 `/admin-api/mes/pro/auto-schedule/apply` / Then 普通路线不再因 eDHR 绑定缺失失败，真实链路应继续推进直到新的真实阻塞点或最终通过。

## 里程碑

1. M1：建立任务文档，记录经验门禁、前置任务和当前根因。`DONE`
2. M2：RED：补调度层回归测试，先证明非 eDHR 路线会被误触发 eDHR 批次创建。`DONE`
3. M3：GREEN：在排产服务增加正式触发门禁，只对启用 `BATCH` 配置的路线调用 eDHR 批次创建。`DONE`
4. M4：REGRESSION：运行排产与 eDHR 目标回归测试，确认已启用 eDHR 的 fail-fast 契约未回退。`DONE`
5. M5：测试服重新构建发布并复跑三角色真实 E2E。`DONE`
6. M6：更新证据并提交本任务相关改动。`DONE`

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest,MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest" test`
- 测试服真实日志不再出现 `排产完成创建 eDHR 批次缺少前置条件：工序与批记录绑定` 针对未启用 `BATCH` 配置路线的失败。
- 维护仓三角色真实 E2E：`gaomin`、`zhaojie`、`wangsiyu` 全绿。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅修正 eDHR 批次创建的正式触发条件；已启用 eDHR 的路线仍按原契约明确失败。
- `是否从根因和长期维护角度解决`：是。直接把“是否启用 eDHR”判断收敛到调度服务触发点，避免普通路线被错误纳入 eDHR 前置校验。
- `是否存在临时补丁或绕过`：否。不得通过吞掉 eDHR 异常、放宽前置校验或手工补测试库数据来掩盖问题。

## 当前状态

- 状态：COMPLETED。
- 已确认真实阻塞：测试服 `172.30.30.58` 的真实 smoke 在 `/admin-api/mes/pro/auto-schedule/apply` 失败，远端 `smoke-report.json` 报错 `排产完成创建 eDHR 批次缺少前置条件：工序与批记录绑定`。
- 已确认主数据现状：当前 smoke 使用路线 `900026`，只读核对测试服 `mes_pro_route_use_process_config` 与 `mes_pro_route_use_process_batch_record` 后，该路线下均无 `BATCH` 配置与批记录绑定；全局 `FILL` 启用规则也为 0，说明这不是“当前路线已启用 eDHR 但缺配置”的普通误差，而是调度层对普通路线误触发了 eDHR 创建。
- 已确认代码触发链路：`MesProAutoScheduleServiceImpl.apply(...)` 在排产完成后无条件调用 `createEdhrBatchExecutionsAfterScheduleCompletion(...)`，进而总是进入 `MesProEdhrBatchExecutionServiceImpl.openOrCreateFromScheduleCompletion(...)`；后者被设计为“已触发即必须满足 eDHR 前置条件”的 fail-fast 服务，并不负责判断路线是否应该启用 eDHR。
- 已完成本地修复：排产服务改为仅对存在启用中 `BATCH` 工序配置的路线触发 eDHR 批次创建；新增回归测试验证“非 eDHR 路线必须跳过”和“已启用 eDHR 路线仍传播前置失败”。
- 已完成本地验证：`mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest,MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest" test` 通过 72 个测试。
- 已完成测试服闭环：真实重发后，维护仓证据 `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\evidence\post-release-role-e2e-1781875259932.json` 显示 `zhaojie` 智能排产 smoke 已越过 `/admin-api/mes/pro/auto-schedule/apply` 的 eDHR 误触发阻塞，并推进到新的后续阻塞 `第三方报工导管报工第 2 行工段长匹配到多名用户：eDHR矩阵-审批人`。
- 结论：本任务范围内的 eDHR 触发门禁问题已闭环，后续链路阻塞已转入新任务 `20260619-post-release-role-e2e-gate-feedback-approver-identity` 处理。

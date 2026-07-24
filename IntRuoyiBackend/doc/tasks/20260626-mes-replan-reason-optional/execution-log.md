# 执行日志：MES 手动重排应用取消业务原因必填

## 2026-06-26

- 初始化任务：根据用户“点击应用重排的时候报错，不需要填理由”反馈，确认当前 `replan/apply` 与 `auto-schedule/apply` 共享了统一的原因必填校验。
- BDD: 手动重排 apply 允许 reason 为空 -> Given 用户已生成有效重排预览且排产前检查无阻断 / When 调用 `replanApply` 且不传 reason / Then 后端继续完成重排发布，不返回 `PRO_SCHEDULE_ORDER_REASON_REQUIRED`。
- BDD: 自动排产 apply 仍要求 reason -> Given 用户尝试发布自动排产结果 / When `apply` 请求缺少 reason / Then 后端仍返回 `PRO_SCHEDULE_ORDER_REASON_REQUIRED`，保持既有操作审计门禁。
- BDD: 手动重排 reason 仍可审计 -> Given 用户填写了业务原因后发布手动重排 / When 后端写入事件审计和排产工单操作日志 / Then reason 字段仍应记录操作者填写值。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#apply_shouldRejectMissingReason+replanApply_shouldAllowMissingReason,MesProScheduleOrderBusinessOptimizationContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，`replanApply_shouldAllowMissingReason` 命中 `PRO_SCHEDULE_ORDER_REASON_REQUIRED`，contract test 也确认当前实现仍统一调用 `validateApplyReason(reqVO)`。
- CHANGE: `MesProAutoScheduleServiceImpl.applyInternal(...)` -> 将统一原因校验改为 `prepareApplyReason(reqVO, operationType)`；自动排产 apply 继续走 `validateRequiredApplyReason(reqVO)`，手动重排 apply 改为 `normalizeOptionalApplyReason(reqVO)`。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#apply_shouldRejectMissingReason+replanApply_shouldAllowMissingReason,MesProScheduleOrderBusinessOptimizationContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleOrderBusinessOptimizationContractTest,MesProAutoScheduleAlgorithmContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，47 个相关回归全部通过。

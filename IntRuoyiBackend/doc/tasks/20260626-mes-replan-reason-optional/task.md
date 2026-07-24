# 任务：MES 手动重排应用取消业务原因必填

## 任务目标

- 调整 `/admin-api/mes/pro/auto-schedule/replan/apply` 行为，使手动重排 apply 不再要求 `reason` 必填。
- 保持 `/admin-api/mes/pro/auto-schedule/apply` 自动排产发布的原因校验与审计逻辑不变。
- 保持重排 apply 的预览上下文校验、排产前检查、阻断问题校验和事件审计不变。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个 backend 相关任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-replan-latest-start-active-task-regression\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务修复 latest-start 暂缓误报 ACTIVE_TASK；本次继续处理同一重排发布链路里的“理由必填”行为变更。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 本轮只做本机后端代码、单测和合同测试，不做真实写入或远端联调。
  - 若后续追加真实登录写入或服务器动作，必须先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。明确区分 auto apply 与 replan apply 的原因校验，不做兼容分支兜底。
- `是否从根因和长期维护角度解决`：是。把“原因必填”从统一 apply 逻辑收敛为只作用于自动排产发布，避免重排和自动排产共享了不再适用的前置规则。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 手动重排 apply 允许 reason 为空 -> Given 用户已生成有效重排预览且排产前检查无阻断 / When 调用 replanApply 且不传 reason / Then 后端继续完成重排发布，不返回 PRO_SCHEDULE_ORDER_REASON_REQUIRED。`
- `BDD: 自动排产 apply 仍要求 reason -> Given 用户尝试发布自动排产结果 / When apply 请求缺少 reason / Then 后端仍返回 PRO_SCHEDULE_ORDER_REASON_REQUIRED，保持既有操作审计门禁。`
- `BDD: 手动重排 reason 仍可审计 -> Given 用户填写了业务原因后发布手动重排 / When 后端写入事件审计和排产工单操作日志 / Then reason 字段仍应记录操作者填写值。`

## 里程碑

1. M1：创建任务包并补后端 RED 合同。
2. M2：最小修改 replan apply 的原因校验边界。
3. M3：运行 GREEN 单测/合同回归并回写证据。

## 预期验证

- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleOrderBusinessOptimizationContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-mes-replan-reason-optional\backend-api-evidence.md`

## 最终验证结果

- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#apply_shouldRejectMissingReason+replanApply_shouldAllowMissingReason,MesProScheduleOrderBusinessOptimizationContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleOrderBusinessOptimizationContractTest,MesProAutoScheduleAlgorithmContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，47 个相关回归全部通过
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-mes-replan-reason-optional\backend-api-evidence.md` -> PASS

## 阻塞与影响

- 当前无外部阻塞；后端 contract 已与前端“手动重排理由非必填”行为对齐。

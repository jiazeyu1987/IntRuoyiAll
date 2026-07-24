# 任务：MES 自动排产前置校验与零任务阻断

## 任务目标

- 让自动排产 `apply` 在后端 preflight 存在阻断项时 fail fast。
- 让排产工单 preflight 对批记录路线前置条件给出正式阻断，而不是拖到应用阶段半路失败。
- 让“最晚开工约束导致一个任务都生成不出来”的场景显式失败，而不是伪装成排产成功。

## 当前状态

`COMPLETED`

## Current Status

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\doc\tasks\20260622-dcc-review-matrix-tab\task.md`
- 状态：`COMPLETED`
- 处理：DCC 审阅矩阵后端已在当前 clean 集成基线完成验证并进入 `int_main`；本任务单独处理 MES 自动排产与 preflight 改动，不混入 DCC 范围。

## 经验门禁

- 已读取：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- 本任务适用强制门禁：
  - worktree 收口必须在干净 `int_main` 集成基线执行，不从原始 holding 脏工作区整仓提交。
  - 同一后端集成分支内仍需保持范围隔离，只允许引入 `yudao-module-mes` 相关改动和本任务文档。
  - 涉及主线合并前，`execution-log.md` 必须存在 `GREEN: experience-preflight -> PASS`、`RED` 和 `GREEN` 证据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: 批记录路线缺少批次号必须阻断 -> Given 选中的排产工单启用了工艺批记录路线 / When 对排产范围执行 preflight 或 apply / Then 若生产工单缺少批次号，系统必须先阻断并给出“维护批次号”的正式动作指引。`
- `BDD: 批记录路线默认批记录无效必须阻断 -> Given 排产路线启用了批记录路线 / When 默认批记录或报告绑定缺失 / Then preflight 必须返回 BLOCKED，apply 也必须传播该阻断。`
- `BDD: 最晚开工导致零任务时必须失败 -> Given 最晚开工约束已把当前排产范围全部打成不可生成任务 / When 执行 apply / Then 系统必须显式失败，不得返回“已应用但未生成任何任务”的伪成功。`

## 里程碑

1. 建立任务文档与执行日志。`DONE`
2. RED：在 clean 集成基线记录缺少新契约测试入口的失败证据。`DONE`
3. GREEN：重放 MES 自动排产与 preflight 改动。`DONE`
4. GREEN：执行定向 Maven 回归并补齐证据。`DONE`

## 预期验证

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\pom.xml -pl yudao-module-mes -Dtest=MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest,MesProAutoScheduleServiceImplTest,MesProScheduleOrderPreflightServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 预期交付物

- `D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\doc\tasks\20260622-mes-auto-schedule-preflight-gate\execution-log.md`
- `D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\doc\tasks\20260622-mes-auto-schedule-preflight-gate\backend-api-evidence.md`

## 完成结论

- 已在 clean backend `int_main` 集成基线重放 7 个 `yudao-module-mes` 文件改动。
- 定向 Maven 回归 `36` 个测试全部通过，满足本任务“fail fast 阻断 + 零任务显式失败”的后端交付要求。
- 本任务范围仅包含 `yudao-module-mes` 与本任务文档，可独立提交并继续推进到真正的 backend `int_main`。

# MES 自动排产前置校验与零任务阻断执行日志

- BDD: 批记录路线缺少批次号必须阻断 -> Given 选中的排产工单启用了工艺批记录路线 When 对排产范围执行 preflight 或 apply Then 若生产工单缺少批次号，系统必须先阻断并给出“维护批次号”的正式动作指引。
- BDD: 批记录路线默认批记录无效必须阻断 -> Given 排产路线启用了批记录路线 When 默认批记录或报告绑定缺失 Then preflight 必须返回 BLOCKED，apply 也必须传播该阻断。
- BDD: 最晚开工导致零任务时必须失败 -> Given 最晚开工约束已把当前排产范围全部打成不可生成任务 When 执行 apply Then 系统必须显式失败，不得返回“已应用但未生成任何任务”的伪成功。
- GREEN: experience-preflight -> PASS，已读取 `docs/worktree-memory.md`，本任务仅在 clean backend `int_main` 集成 worktree 内处理 `yudao-module-mes` 相关改动与本任务文档。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\pom.xml -pl yudao-module-mes -Dtest=MesProAutoSchedulePreflightGateContractTest test` -> FAIL，当前 clean 集成基线尚未引入本任务对应契约测试入口，Surefire 明确报错 `No tests matching pattern "MesProAutoSchedulePreflightGateContractTest" were executed!`，符合“先失败再实现”的准入证据。
- GREEN: mes-change-replay -> PASS，已把原 holding 工作区中的 7 个 `yudao-module-mes` 目标文件精确重放到 clean 集成 worktree，未混入 DCC 或其他业务组改动。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\pom.xml -pl yudao-module-mes -Dtest=MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest,MesProAutoScheduleServiceImplTest,MesProScheduleOrderPreflightServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`BUILD SUCCESS`，共 `36` 个测试通过，失败 `0`、错误 `0`、跳过 `0`。

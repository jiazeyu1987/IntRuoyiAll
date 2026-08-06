# Execution Log

- Task ID: `20260805-production-leader-process-config-unification`
- Created: `2026-08-05`

## User Intent

- 用户确认将生产组长中的损耗管理、设备映射和设备参数设置合并为一张表，并以工序串联。
- 参数标准按已确认方案维护目标值、下限、上限；实际平均值由生产数据计算，不允许人工维护。

## Rule And Skill Reads

- 已读取根 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md` 和 `docs/engineering/technology-stack-routing.md`。
- 已读取 `spec-driven-delivery`、`backend-api-delivery`、`frontend-feature-delivery` 技能及其必读参考合同。
- 已读取 `docs/experience-index.md` 命中的生产组长模块 Tab、真实写入型 E2E、任务 cleanup 和共享脏工作区门禁。

## Existing-System Evidence

- `TeamLeaderWorkbenchPage.vue` 当前在“损耗管理”单独展示路线工序损耗原因，在“班组配置”分别用设备档案、工序设备绑定和设备参数卡片维护裸 ID。
- `mes_pro_process_pool_device_parameter_rule` 已有 `routeProcessId/processId/deviceId/parameterCode/unit/lowerLimit/upperLimit/defaultValue/valueType/enabled`，不需要为了目标值、上下限新增字段。
- 前线运行态已按 `routeProcessId` 过滤参数规则，但当前保存 UI 未传 `routeProcessId`，统一表必须补齐该正式上下文。
- 生产提交正式 payload 已保存 `equipmentParameters`，可作为实际平均值统计来源；统计不得回退到目标值或默认值。
- 共享 `int_main` 在任务开始时存在并行提交、已暂存、未暂存和未跟踪改动；本任务目录将排除在脏工作区基线之外。

## Initial BDD

- BDD: 生产组长查看统一工序配置 -> Given 生产组长拥有路线工序维护权限 / When 打开统一配置表 / Then 每个路线工序在同一行展示损耗原因、映射设备和参数完成情况。
- BDD: 为工序映射设备 -> Given 路线工序可维护且设备属于当前组长 / When 在该工序下选择设备并保存 / Then 统一表回显设备且前线运行态可读取。
- BDD: 维护设备参数标准 -> Given 设备已映射到当前工序 / When 维护参数编码、名称、单位、目标值、下限和上限 / Then 相同上下文更新正式规则且满足下限不大于目标值不大于上限。
- BDD: 查看实际平均值 -> Given 正式生产提交包含当前路线工序、设备和参数的数值 / When 加载统一表 / Then 系统按明确统计周期显示只读平均值和样本数；无样本时显示空平均值和 0 样本。
- BDD: 拒绝非法参数上下文 -> Given 设备未绑定当前工序或参数区间非法 / When 保存参数 / Then 后端返回业务错误且不写入。

## Phase Entries

- M0 completed：任务合同已创建；共享脏工作区基线提交 `633361dde19065f71e11510bef288e7010da1284` 已完成，提交包含本任务开始前 32 个既有改动文件，本任务目录未进入基线。
- M0 verification：`git show --name-status --oneline -1` 已核对基线文件清单；基线后共享主工作区再次出现同文件并行改动，现已由其它任务提交清理，主工作区只剩本任务目录未跟踪。
- M0 isolation decision：按 `docs/worktree-restrictions.md` 和 `docs/branch-runtime-ports.md`，后续在 `D:\IntRuoyiWorktree\20260805-process-config-unification` 独立 worktree 和 `codex/20260805-process-config-unification` 分支实施。

## Outstanding Blockers

- 暂无产品 blocker；真实 E2E 的运行态、账号和任务数据前置将在测试计划评审后核对。

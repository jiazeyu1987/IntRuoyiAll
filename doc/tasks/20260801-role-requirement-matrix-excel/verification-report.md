# 岗位需求分解矩阵规划包验证报告

## Scope

验证 `doc/tasks/20260801-role-requirement-matrix-excel/` 是否已经把源 Excel 的全部目标转换为可实施、可测试、可追踪的增量开发包。本报告验证规划质量，不代表 M0-M6 已完成生产实现。

## Source Baseline

- Workbook: `C:\Users\BJB110\Desktop\文档\职责\岗位需求分解矩阵.xlsx`
- SHA256: `6A5674826D76AE4B5393806E9255187F3CB6B0AADA9D61E2701B9ACD41111D32`
- Size: `22200` bytes
- Last write: `2026-08-01 19:15:20`
- 主表 `岗位需求分解矩阵`: 23 项，编号 `M01-M23`
- 衍生表 `衍生需求`: 39 项，编号 `D01-D39`

## Verified Artifacts

- `task.md`
- `prd.md`
- `development-plan.md`
- `test-plan.md`
- `task-state.json`

## Coverage Verification

- 62 项 Excel 需求全部进入 `Excel Traceability Matrix`，无缺号、重号或额外编号。
- 62 项需求分别映射唯一 `AC-M01` 至 `AC-M23`、`AC-D01` 至 `AC-D39`。
- Excel 的 62 个任务名称与追踪矩阵逐行一致；已将 `D06` 修正为源表原文“设备报修或禁用后的可选控制”。
- 每项均包含一个里程碑、一个实施区域、一个 BDD 场景和非空可观察验收。
- `task-state.json` 的验收 ID、需求 ID、任务名称和里程碑与追踪矩阵一致。
- M1/RQ-01 只覆盖 `M01`、`M03-M04`；`M02` 正确归入 M4 调拨链路。

## Design Verification

- 复用已完成的 `20260731-team-leader-workbench-prd-plan`，计划只处理现有实现与 Excel 之间的差距。
- `mes_pro_process_pool_active_order` 被定义为跨生产、PQC、调拨、批记录和放行的唯一活跃订单聚合；禁止保留两套活跃来源双读。
- 原始生产报工按“工序事实优先”建模，订单/任务/工作站不再作为提交前置。
- 工序目标数量使用 `ERP 固定数量 × 正式生产系数快照`；缺失或非正数时 fail fast，不默认 `1`。
- 批记录回填覆盖全部员工、设备和多次报工，并要求确定性聚合和幂等，不使用代表事件。
- QA 规程和 PQC 任务具备版本、类型、日期、班次、轮次、逐件明细、签名、复核和修订设计。
- 固定检验项目、`PATROL`、数量 `30` 和损耗 `1` 被列为必须移除的现有差距。
- 多调拨、分批发货、补料、退料、多物料和多批次追溯进入 M4。
- 班组/PQC 日结、范围、权限、审计、历史快照和真实放行来源进入 M5/M6。
- `工序开始`、正式逐工序 `批记录表单`、`formBindings` 三条链路保持独立。

## Test And Failure-Path Verification

- `test-plan.md` 包含 16 个 Given/When/Then 场景、RED/GREEN 规则、里程碑命令、真实 Playwright 路径、测试数据、并发、性能和清理。
- 缺测试类、缺脚本、No tests 或运行前置缺失不允许冒充 RED。
- 失败路径覆盖订单/路线冲突、调拨不足、系数缺失、设备禁用、超额分配、代表事件丢数、规程缺项、PQC 自我确认、签名不一致、权限越界、历史快照污染和放行占位成功。
- 真实 E2E 必须从登录和菜单入口执行；API 仅用于最终只读核验及规则允许的任务数据清理。

## Command Evidence

- RED: roadmap validator 初次失败，原因是 `task.md` 缺少 `Blockers`。
- GREEN: roadmap node development plan validator -> PASS。
- GREEN: custom plan coverage validator -> `62 requirements, 62 acceptance IDs, task-state JSON, UTF-8`。
- GREEN: artifact-tool workbook/plan validator -> 主表 23、衍生表 39，任务名称、task-state、BDD 引用和追踪字段全部一致。
- RED: 首次 `git diff --cached --check` 发现新增 `prd.md` 的 Given/When 行存在无语义行尾空格。
- GREEN: 清理并重新暂存后 `git diff --cached --check` -> PASS；七个正式规划/收尾文件无 whitespace error。

## Result

PASS。规划包已达到 `ready_for_closeout`，可作为后续实现任务的正式输入；`task-state.json.status` 保持 `planned`，M0-M6 尚未实施。ERP/调拨正式来源、QA 所有权、双活跃来源迁移、质量异常/返工/报废/库存来源及真实 E2E 环境仍是后续里程碑的 fail-fast 前置。

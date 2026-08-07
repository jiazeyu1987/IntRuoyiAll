# 执行日志

## User Intent

- 用户要求：已完成的排产工单在“来源生产工单号”后面追加“(已完成)”。
- 范围：仅修改排产工单列表的来源工单号展示；不修改完成状态、排产规则或后端接口。

## BDD

- BDD: 已完成工单显示完成标识 -> Given 排产工单的 `manualFinished=true` 或 `status=3`；When 页面渲染“来源生产工单号”；Then 工单号后紧跟“(已完成)”。
- BDD: 未完成工单保持原文 -> Given 排产工单未人工完成且状态不是已完成；When 页面渲染“来源生产工单号”；Then 只显示原工单号，不追加完成标识。

## Milestone Log

- M0：已读取前端开发、任务收尾、PowerShell 编码规则和前端功能交付证据契约。
- 经验门禁：采用任务专用静态合同、真实页面只读核对、独立记录每条测试退出码、脏工作区基线与任务提交隔离。
- 启动时工作区存在其他任务的已跟踪和未跟踪改动；将按强制规则先建立独立基线提交，当前任务目录不纳入基线。
- 基线提交：`de6b84628 chore: baseline concurrent changes before schedule order label`，共 60 个既有文件；完整文件清单可由 `git show --name-status --oneline de6b84628` 复核，包含 DCC 上传链路、MES 路线/组长链路、并行任务测试与任务记录、`docs/database-rules.md`、`docs/frontend-development.md`，不包含本任务目录或排产工单页面。
- 基线提交后残余复扫发现其他并行任务继续修改 6 个文件：`MesTeamEmployeeBindingServiceTest.java`、角色对齐任务 3 个 SQL、全量 PQC 搜索执行日志、PQC 组长五记录执行日志；均保持未暂存且不触碰。
- M1：新增任务专用静态合同并取得预期 RED。

## Verification Evidence

- `RED: node tests/e2e/mes-schedule-order-completed-source-label-static.spec.cjs -> FAIL, expected reason: Schedule order page must define a bounded source work order display text resolver.`

## Blockers

- 无。

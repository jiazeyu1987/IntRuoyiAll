# Execution Log

## User Intent

- 用户要求按 7 条业务规则设计、开发、验证 PQC 组长表单修改与放行流转。
- 用户明确要求分析代码后给确定结论，不使用“大概率”等不确定表述。

## BDD

- BDD: PQC 当前表单按钮常驻 -> Given PQC 组长打开当前 PQC 表单列表，When 行记录未放行，Then 该行展示“详情 / 复核 / 修改”，不因 PENDING/APPROVED/REJECTED 状态隐藏复核或修改。
- BDD: 放行前可修改 -> Given PQC 表单关联的活跃订单尚未放行，When PQC 组长点击修改，Then 系统允许修改 PQC 表单正式数据并保留修改审计。
- BDD: 复核通过更新审核进度 -> Given PQC 组长对 PQC 表单复核通过，When 后端保存复核，Then 活跃订单审核进度更新，表单仍保留在当前列表直到放行。
- BDD: 放行后归档 -> Given 活跃订单已经放行，When PQC 组长刷新当前列表，Then 该 PQC 表单不再出现；When 打开 PQC 历史，Then 该记录仍可查询和查看详情。

## Evidence

- 2026-08-10: 读取任务、前端、后端、E2E、编码规则和技能说明。
- 2026-08-10: 创建任务目录 `doc/tasks/20260810-pqc-leader-form-edit-release-flow`。

## RED / GREEN

- RED: pending。
- GREEN: pending。

## Blockers

- None.

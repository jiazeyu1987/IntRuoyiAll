# Execution Log - 20260701-edhr-phase2-stage-unification (Frontend)

BDD: 列表页显示统一阶段标签 -> Given 用户浏览批次执行列表 / When 列表加载 / Then 每条批次除原始状态外，还能看到统一主阶段标签或摘要。

BDD: 详情页与列表页阶段表达一致 -> Given 用户从列表进入详情 / When 查看同一批次 / Then 阶段名称与责任角色不产生冲突。

GREEN: task-bootstrap -> PASS，已在 `edhr_phase` worktree 内建立 Phase 2 前端任务台账。
GREEN: phase2-frontend-stage-unification -> PASS，列表页状态列已优先展示后端返回的 `mainStageLabel`，详情页继续消费同一套阶段摘要字段。

# Execution Log - 20260701-edhr-phase3-release-integration (Backend)

BDD: 批次详情可直接触发放行预检 -> Given 用户位于批次详情且批次存在有效 ID / When 点击放行预检 / Then 在当前批次上下文中完成预检并刷新放行摘要。

BDD: 批次详情可直接查看放行事务 -> Given 批次已有放行事务 / When 点击事务事件或检查项 / Then 用户围绕该批次直接进入放行事务明细，而不是先回放行列表筛选。

GREEN: task-bootstrap -> PASS，已在 `edhr_phase` worktree 内建立 Phase 3 后端任务台账。
GREEN: phase3-backend-reuse-contract -> PASS，确认批次详情页 Phase 3 放行衔接复用现有 `MesProEdhrReleaseController` 和 release service，不额外分叉后端放行合同。
GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionControllerTest,MesProEdhrBatchExecutionServiceTest" test -> PASS

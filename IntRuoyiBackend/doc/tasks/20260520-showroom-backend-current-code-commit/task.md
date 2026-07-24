# 任务：提交展厅后端当前代码快照

## 目标

基于 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 当前工作区，提交已完成并通过验证的 showroom 后端代码快照；仅包含 `yudao-module-showroom` 相关代码与对应任务证据，不混入 DCC 与其他未完成任务改动。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b4-assignment-comment-collaboration\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-narration-manual-approval-flow\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-codex-bilingual-narration\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-backend-current-code-commit\**`

## 非范围

- 不提交 `yudao-module-dcc` 当前改动。
- 不提交 `tmp/`、历史未跟踪证据目录或其他非 showroom 残留。
- 不为了提交而改写已验证通过的 showroom 行为。

## 前置任务检查

- `20260519-showroom-remediation-b4-assignment-comment-collaboration`：`completed`
- `20260519-showroom-narration-manual-approval-flow`：已在当前 showroom 代码快照下补齐后端 GREEN
- `20260520-showroom-product-codex-bilingual-narration`：`completed`

## 里程碑

- [x] M1：识别 showroom 后端可提交边界并排除 DCC/临时产物。
- [x] M2：运行当前 showroom 后端快照回归验证。
- [x] M3：完成 Git 提交并复核剩余未提交改动。

## 预期验证

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomAssignmentWorkflowTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 提交后 `git status --short` 中不再包含本次 showroom 后端暂存文件。

## 当前状态

Completed on 2026-05-20.

## 最终验证结果

- PASS：`mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomAssignmentWorkflowTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS：`git commit -m "任务: 提交展厅后端当前代码"` -> commit `32ba5d8d03`
- PASS：提交后复核 `git status --short`，确认本次 showroom 后端代码快照已出工作区，仅剩 DCC 与历史未跟踪残留

# Execution Log：提交展厅后端当前代码快照

BDD: 仅提交已验证完成的 showroom 后端代码 -> Given 后端工作区同时存在 showroom 与 DCC 等多类未提交改动 / When 执行本次提交 / Then 只能提交 `yudao-module-showroom` 当前已通过验证的代码快照，并保留其余未完成改动。

RED: `git commit -m "任务: 提交展厅后端当前代码"` -> FAIL，提交钩子要求先设置 `TDD_TASK_DIR` 并提供当前任务目录的 TDD 证据。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomAssignmentWorkflowTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，当前 showroom 后端代码快照通过 32 项目标测试。

GREEN: showroom-only staging -> PASS，已将 `yudao-module-showroom` 与对应任务证据暂存，`yudao-module-dcc` 改动保持未暂存。

GREEN: `git commit -m "任务: 提交展厅后端当前代码"` -> PASS，生成 commit `32ba5d8d03`，showroom 后端当前代码快照已提交，DCC 改动继续保留在工作区。

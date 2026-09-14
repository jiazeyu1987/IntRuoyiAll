# Task

- Goal: 评审测试管理串行节点串正式能力：不同节点串可独立筛选查看，串内严格按序执行且前置失败停止，独立测试项行为不回归
- Workspace: D:/IntRuoyiWorktree/20260727-codex-test-node-chain-build
- Max Rounds: 4
- Requires UI Runtime: yes
- Reviewer must judge only from logic, usability, and UI.
- Worker must only implement required changes from reviewer output.

## Current Evidence

- Task records: `doc/tasks/20260727-codex-test-node-chain/task.md`, `execution-log.md`, `verification-report.md`, `bug-regression-evidence.md`.
- Slot 7 runtime: frontend `http://127.0.0.1:8088`, backend `http://127.0.0.1:48088`, isolated Runner `node-chain-slot-7-runner`.
- Backend focused regression: `CodexTestCaseServiceImplTest`, `CodexTestExecutionServiceImplTest`, `CodexTestRunnerServiceImplTest` -> 30 tests PASS.
- Real E2E: official chain filters visible and ordered; incomplete chain rejected; chain execution `18` blocks later node after first failure; independent execution `19` continues to second item and passes; all temporary test cases cleaned through UI.

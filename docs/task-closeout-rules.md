# IntRuoyi Task And Closeout Rules

## 触发场景

- 开始会修改文件、运行构建/测试/发布、修改环境或数据的任务前，必须先读取本文件。
- 任务进入验证、清理、提交、收尾或总结前，必须再次按本文件核对。

## 任务目录

- 任务目录必须位于 `doc\tasks\<task-id>\`。
- 必须包含 `task.md` 和 `execution-log.md`。
- 完成验证时应包含 `verification-report.md`。
- `task.md` 必须包含任务目标、里程碑、预期验证、当前状态和 `设计约束检查`。

## BDD / TDD 记录

- 功能、修复、重构和行为变更必须记录 BDD。
- 生产代码变更必须记录 RED/GREEN/REGRESSION 或明确阻塞原因。
- 推荐标记：
  - `BDD: <scenario name> -> Given/When/Then`
  - `RED: <command> -> FAIL, <expected reason>`
  - `GREEN: <command> -> PASS`

## 提交规则

- 每个任务完成前必须提交并推送当前分支到 `origin`；本地分支领先 `origin`、推送失败或缺少可用 `origin` 时，不得标记任务完成。
- 提交前检查 `git status --short --branch` 和 staged 文件列表。
- 如果工作区有脏改动，先将所有当前脏改动（已暂存、未暂存、未跟踪）作为独立基线提交；这是用户明确授权的例外流程。
- 基线提交后，再分别提交本任务实现和本任务收尾记录。
- 在任务日志记录基线提交、实现提交、收尾提交的 commit hash 和文件清单。
- 长任务在实现提交和推送前必须执行 `project-experience-consolidation`，优先更新已有经验文档。
- 使用 `git push origin <current-branch>` 推送，并用 `git status --short --branch` 验证不再 ahead。
- 缺少 Git 仓库、`origin`、凭据、网络或推送权限时，必须 fail fast 并记录 blocker。

## 收尾规则

- 实现和验证完成后，先将任务状态设为 `ready_for_closeout`。
- 运行 task-closeout-cleanup preview，确认 keep/delete/blocked/warnings。
- preview 无异常后运行 apply。
- apply 通过后再标记 `completed`。
- 默认保留 `task.md`、`execution-log.md`、`verification-report.md`。

## 禁止做法

- 禁止跳过用户明确要求的脏工作区基线提交。
- 禁止把脏工作区基线和当前任务实现混为同一提交。
- 禁止 verification 失败后提交。
- 禁止 cleanup 删除不属于当前任务的文件。
- 禁止为了收尾强行回滚别人改动。

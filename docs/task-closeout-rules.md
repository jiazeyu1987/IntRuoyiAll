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

- 实现变更和收尾记录分开提交。
- 只暂存本任务拥有的文件。
- 提交前检查 `git status --short --branch` 和 staged 文件列表。
- 发现已有无关暂存内容时，必须先取消暂存无关文件或阻塞，不能把无关文件提交进本任务。

## 收尾规则

- 实现和验证完成后，先将任务状态设为 `ready_for_closeout`。
- 运行 task-closeout-cleanup preview，确认 keep/delete/blocked/warnings。
- preview 无异常后运行 apply。
- apply 通过后再标记 `completed`。
- 默认保留 `task.md`、`execution-log.md`、`verification-report.md`。

## 禁止做法

- 禁止把 unrelated user edits、其他任务改动或临时输出混入提交。
- 禁止 verification 失败后提交。
- 禁止 cleanup 删除不属于当前任务的文件。
- 禁止为了收尾强行回滚别人改动。

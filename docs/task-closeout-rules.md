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

## 验收范围变更门禁

- Trigger: 用户明确变更任务完成门禁、取消全量回归、改为只跑开发文档或测试计划列出的定向测试，或说明某个测试命令不再作为当前任务完成条件。
- Preflight check: 立即同步 `task.md`、`execution-log.md`、`verification-report.md` 或测试计划，逐项列明保留的定向验证、取消的旧门禁、取消原因和用户原话要点；后续验证只按更新后的门禁判断完成。
- Blocker: 范围变更未写入任务文档、取消项与保留项边界不清、取消的是仍能证明当前行为安全性的唯一测试、或任务代码改动超出定向验证覆盖范围时，必须停止补齐验证设计。
- Verification: 收尾前复查任务文档的 Expected Verification 与实际执行记录一致；若不跑旧全量命令，必须在验证报告记录其已被用户明确移出当前完成门禁。
- Forbidden action: 禁止用户已缩小范围后继续把旧全量命令当作完成阻塞；禁止用范围变更掩盖当前开发文档或测试计划内的定向失败；禁止把“未运行全量”写成已通过。


## 任务验证脚本保留门禁

- Trigger: 任务把一次性验证脚本放在 `doc/tasks/<task-id>/` 下，尤其是 `.cjs`、`.mjs`、临时 Playwright 脚本或生成脚本。
- Preflight check: cleanup 前先判断脚本是临时产物还是需要随任务证据长期保留；若需要保留，必须写入 `## Cleanup Keep`，并按 `- doc/tasks/<task-id>/<file>` 这种单独 bullet 路径逐行列出（不要把路径包在反引号里，也不要在同一行追加说明文字）；同时检查是否被 `.gitignore` 的 `doc/tasks/**/*.cjs` 等规则忽略。
- Blocker: 需要保留但未进入 `Cleanup Keep`、`Cleanup Keep` 因缺少 bullet、内联说明或反引号被解析成错误路径、或 `git status --untracked-files=all` 看不到脚本且未确认忽略规则时，不得提交完成。
- Verification: cleanup preview 显示脚本在 keep 列表；提交前对被忽略但需要保留的脚本使用 `git add -f <path>`，并在任务日志记录原因。
- Forbidden action: 禁止把生成脚本、截图、stdout/stderr 日志等临时产物混入最终提交；禁止因为脚本被忽略就误以为验证证据已提交。

## 技能证据文件清理前归档门禁

- Trigger: 任务使用 `database-schema-delivery`、`backend-api-delivery`、`frontend-feature-delivery` 等技能生成 `database-schema-evidence.md`、`backend-api-evidence.md`、`frontend-feature-evidence.md` 或同类临时 evidence 文件，并准备运行 `task-closeout-cleanup`。
- Preflight check: cleanup preview/apply 前必须先运行对应 evidence validator，并把 validator PASS、RED/GREEN 摘要和关键验收结论复制到默认保留的 `execution-log.md` 或 `verification-report.md`。
- Blocker: evidence 文件还未通过 validator、validator 结果只存在于将被 cleanup 删除的文件、或 `verification-report.md` 未记录关键 PASS 命令时，不得执行 cleanup apply。
- Verification: cleanup preview 显示临时 evidence 文件在 delete 列表，同时 `task.md`、`execution-log.md`、`verification-report.md` 在 keep 列表；apply 后保留报告仍包含 validator PASS 和核心验收结论。
- Forbidden action: 禁止先删除 evidence 文件再补写验证结论；禁止把已被 cleanup 删除的临时 evidence 当作最终审计证据；禁止为了保留所有中间 evidence 而跳过 cleanup。
## 禁止做法

- 禁止跳过用户明确要求的脏工作区基线提交。
- 禁止把脏工作区基线和当前任务实现混为同一提交。
- 禁止 verification 失败后提交。
- 禁止 cleanup 删除不属于当前任务的文件。
- 禁止为了收尾强行回滚别人改动。

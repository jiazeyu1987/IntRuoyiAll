# 提交推送 int_main 指定范围代码任务

## Task Goal

将当前 `int_main` 分支中用户确认范围内的代码、测试、SQL、脚本、配置和相关任务文档提交并推送到 `origin/int_main`；排除临时产物、日志占位文件和 `resource` 二进制资料。

## Milestones

1. `completed`：完成提交/推送前置检查并建立任务记录。
2. `completed`：按确认范围显式暂存并检查 staged 文件。
3. `completed`：提交当前快照并复扫残余工作区。
4. `completed`：运行分支运行时端口门禁并推送 `origin/int_main`。
5. `completed`：确认远端同步，执行收尾清理并记录最终证据。

## Expected Verification

- `git status --short --branch`、当前分支和 `origin` 均符合预期。
- staged 清单只包含用户确认范围，不包含 `.pytest-temp`、`LOG_FILE_IS_UNDEFINED` 或 `resource` 二进制资料。
- `git -c core.whitespace=cr-at-eol diff --cached --check` 通过；默认检查仅对既有 `-text` fixture 的 CR 行尾提示。
- `scripts/preflight/branch-runtime-port-guard.ps1` 通过。
- 提交后推送成功，`git status --short --branch` 不再显示 ahead。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；本任务只执行 Git 提交和推送，不改变运行时行为。
- 是否从根因和长期维护角度解决：是；通过明确提交边界和推送后复核，避免临时产物误入主干。
- 是否存在临时补丁或绕过：否。

## 适用经验门禁摘要

- 主工作区大规模 Dirty/Untracked 分类：先冻结 `git status --porcelain=v1 --untracked-files=all`，按代码/测试、正式文档、并行记录、可再生生成物和本地业务输入分类；只对用户确认且 staged 清单可解释的文件暂存，禁止 `git add -A`。
- Git 提交推送：提交前复核分支、remote、staged 清单和 `git diff --cached --check`；推送后复核本地不再领先 `origin`。
- 分支运行时门禁：提交/推送前运行 `scripts/preflight/branch-runtime-port-guard.ps1`。

## Cleanup Keep

- doc/tasks/20260826-commit-push-int-main-scoped-code/task.md
- doc/tasks/20260826-commit-push-int-main-scoped-code/execution-log.md
- doc/tasks/20260826-commit-push-int-main-scoped-code/verification-report.md

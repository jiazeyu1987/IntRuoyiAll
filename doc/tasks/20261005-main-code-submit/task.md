# 提交推送主干代码

## Goal

按用户当轮“提交推送主干代码”的授权，将 int_main 当前待提交代码保存为独立基线提交，并将本地主干及任务收尾记录推送到 origin/int_main。

## Milestones

- completed：核对分支、远端、当前改动和并行任务，冻结155个既有文件。
- completed：差异、文件类型、敏感信息及钩子检查通过，基线提交39608b862。
- completed：经验归并并单独提交推送，当前任务cleanup preview/apply通过。
- completed：主干代码及经验文档推送通过，远端HEAD匹配，ahead/behind为0/0；最终收尾记录随独立文档提交同步。

## Expected Verification

- 当前分支必须为 int_main，origin 可用，远端没有未合入的新提交。
- 逐文件冻结路径及 SHA-256，暂存前后核对内容和清单，不收集冻结之后的并行新改动。
- git diff --check、git diff --cached --check、敏感信息检查与 Git 提交钩子通过。
- task.md、execution-log.md、verification-report.md 的 UTF-8 和结构检查通过。
- 当前任务 ready_for_closeout 后 cleanup preview/apply 通过，仅处理本任务资产。
- git push origin int_main 成功，git ls-remote 的 int_main 与本地 HEAD 一致，ahead/behind 为 0/0。
- 本任务为代码快照提交，不修改业务实现，不新增业务测试门禁，不将 Git 核验当作业务验收；本轮未要求 E2E。

## Current Status

completed

## 设计约束检查

- 主工作区 E:/IntRuoyi，分支 int_main；不创建、合并或移除其他任务 worktree。
- 初始 dirty 内容按用户提交主干授权纳入代码基线；提交前冻结清单，任务文档与基线分开提交。
- 不修改并行任务业务代码或状态，不操作数据库、不重启或停止服务，不强推、不绕过 Git 钩子。
- 不创建 fallback，不提交凭据、构建产物、临时日志或忽略文件。

## Cleanup Keep

- doc/tasks/20261005-main-code-submit/task-state.json

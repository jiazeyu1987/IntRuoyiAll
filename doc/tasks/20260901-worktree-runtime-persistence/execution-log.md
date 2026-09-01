# 执行日志：Worktree 运行态保留规则

## User Intent

- 用户要求把“worktree 前后端不得随意释放”的长期规则加入项目 `AGENTS.md`。
- 用户后续明确要求融合进 `int_main`。

## Milestone Updates

- M1 completed：现有规则只限定任务归属和不得清理无关进程，没有授权任务收尾自动停止 worktree 服务；新规则更严格且不冲突。
- M2 completed：按用户批准内容压缩为一条不超过 100 个中文字符的长期基线，并追加到项目根 `AGENTS.md`。
- M3 completed：目标基线在 `AGENTS.md` 中唯一出现，`git diff --check -- AGENTS.md` 通过；现有未提交规则保持不变。

## Verification Evidence

- STRUCTURE: 精确匹配目标基线 -> PASS，匹配数量为 1。
- DIFF: `git diff --check -- AGENTS.md` -> PASS，仅有既有换行符提示，无空白错误。
- EXPERIENCE: 用户指定根 `AGENTS.md` 为正式归宿，未向 `docs/worktree-memory.md` 重复写入同义规则。
- CLOSEOUT: task-closeout cleanup preview/apply with `--worktree-closeout off` -> PASS，保留三份任务记录，没有删除文件、停止服务或执行 Git 集成。
- INTEGRATION PREP: `AGENTS.md` 存在其它未提交规则，本次只允许选择性暂存新增的 worktree 运行态基线；其它 hunks 必须保留在工作区。

## Blockers

- 无。

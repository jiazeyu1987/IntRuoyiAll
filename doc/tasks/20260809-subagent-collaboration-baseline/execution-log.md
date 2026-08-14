# Execution Log

- User intent: 将子 Agent 的正确调用方式写入根目录 `AGENTS.md`。
- Approved baseline: `子 Agent 调度必须直接调用 collaboration 接口；不得嵌套 functions.exec，也不得用 exec wait 代替 wait_agent。`
- Task type: 文档规则变更；不修改生产代码，不需要生产代码 RED/GREEN。
- Duplicate/conflict check: 根目录 `AGENTS.md` 中没有同义或冲突的子 Agent 调度规则。
- Change: 在根目录 `AGENTS.md` 末尾追加一条用户确认的 Thread baseline，未改写其它规则。
- GREEN: 目标基线匹配计数为 `1`。
- GREEN: `git diff --check -- AGENTS.md doc/tasks/20260809-subagent-collaboration-baseline` -> PASS；仅有既有 LF/CRLF 转换提示。
- Experience consolidation: 调度工具边界已按用户明确要求归档到根目录 `AGENTS.md`；这是最准确的现有长期归宿，不重复写入其它经验文档，也不新建长期经验文件。
- Closeout preview: PASS；保留 `task.md`、`execution-log.md`、`verification-report.md`，删除项、阻塞项和警告均为空。
- Closeout apply: PASS；主工作区无需 worktree 合并或删除，实际删除路径为空。

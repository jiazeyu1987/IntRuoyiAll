# 执行记录

- REQUEST: 用户明确本机负责文控模块，开发、问题修复、验证与提交在 int_qms；另一台电脑在 int_main 开发主程序。要求写入 AGENTS.md 供其他 Codex 线程读取。
- PRECHECK: 已读取根 AGENTS.md、docs/task-closeout-rules.md、docs/powershell-encoding.md 和 docs/branch-runtime-ports.md 的端口矩阵。
- BRANCH: git branch --show-current -> int_qms。
- SCOPE: 只修改 AGENTS.md 和当前任务记录；既有提交线程及 Git 索引由原线程处理，不并发操作。
- CHANGE: 根文档开头区分仓库集成主分支与本机文控分支；新增本机分工、默认提交目标、实际分支核对及 int_main 合入边界。授权规则和原有执行规则保持原文。
- GREEN: Python UTF-8 内容与结构检查 -> PASS；确认通用规则、端口标记和 E2E 环境说明与修改前一致。
- GREEN: git -c core.safecrlf=false diff --check -- AGENTS.md doc/tasks/20260930-agents-int-qms-workstation-context -> PASS。
- GREEN: pwsh -NoProfile -File scripts/preflight/branch-runtime-port-guard.ps1 -> PASS，int_qms/int_qms：8061/48061。
- STATUS: ready_for_closeout，准备当前任务 cleanup。
- CLEANUP_PREVIEW: PASS，keep 当前三份记录，delete 为空，warnings none。
- CLEANUP_APPLY: PASS，未删除任何文件。
- CLOSEOUT: 文档交付完成。按 docs/task-closeout-rules.md，Git 提交推送未完成时任务台账保留 blocked；既有提交线程负责 Git，本任务未暂存、提交、推送或清理 Git 锁。
- FOLLOWUP 2026-10-06: 用户明确授权“提交推送intqms的前后端代码”；doc/tasks/20261006-int-qms-commit-push 已提交本任务 AGENTS.md 与三份记录，基线 d95a490ed36479ab1e344d966bc4a9333706c692 已推送 origin/int_qms，远端 ref 一致、ahead/behind=0/0。既有结构、UTF-8 和端口合同复核通过，解除 Git blocker，状态 completed。

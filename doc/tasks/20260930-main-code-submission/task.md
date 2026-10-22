# int_qms 代码提交

## Task Goal

按用户最新指令“提交到intqms”，在当前实际分支 int_qms 提交现存工作区改动。按仓库规则创建独立基线，随后单独提交本任务收尾记录，并执行普通推送。

## Milestones

- [x] 读取 AGENTS.md、提交收尾、分支端口、worktree、PowerShell 编码规则。
- [x] 核对实际分支与主干关系。
- [x] 用户明确目标为当前 int_qms。
- [x] 端口合同与差异检查通过。
- [x] 修正 staged 新增文档格式，并排除任务临时附件。
- [x] 完成独立基线提交。
- [x] 提交当前任务收尾记录。
- [ ] 推送 origin/int_qms 并核对结果。
- [x] cleanup preview/apply 通过，keep=4、delete=0。
- [x] 验证 Git 状态；本地 ahead，推送阻塞，后续并行改动保留。

## Expected Verification

- 提交前核对 git status --short --branch 和 staged 文件列表。
- branch-runtime-port-guard.ps1 通过；git diff --check 通过。
- 只执行与明确范围相符的静态、单元或编译验证；本轮未要求 E2E，不执行 E2E。
- 使用 git push origin int_qms 普通推送，成功时核对 int_qms 与 origin/int_qms 一致且不再 ahead；失败如实记录 blocked。
- task-closeout-cleanup preview/apply 通过，仅处理当前任务目录。

## Current Status

blocked — 代码已提交到 int_qms，独立基线 bd33a7df6，524 文件；cleanup preview/apply 通过。普通推送失败：GitHub 443 经本机 127.0.0.1 代理不可连接；远端未核验成功，不标记 completed。

## 设计约束检查

- 当前目录名不能证明当前分支为 int_main；起始 HEAD a9bcb6d36d96145ddc1252f111347b644b328deb，int_main 为 cc0770cba。
- 起始工作区有 234 个已跟踪改动、552 个未跟踪文件，涵盖多项任务；用户已明确要求提交当前 int_qms，按脏工作区独立基线规则保留现状。
- 本任务只交付代码提交，不修改生产行为；不补造既存改动的 RED/GREEN 证据。
- 不启用子 Agent、不发布、不写数据库、不停止或重启服务、不强推或改写历史。
- 当前代码、测试和正式文档作为独立基线；遵循禁止提交临时产物的更具体规则，doc/tasks 下原始 JSON、ZIP、stdout 和 Office 验证附件只撤销本轮暂存，实体文件保留。当前任务记录单独提交。
- 仅修正 staged 检查指出的 Markdown 行尾空白和多余 EOF 空行；两个空格表示的硬换行改为等价 Markdown 反斜杠换行，不改变文档语义，不修改运行日志或证据内容。

## Cleanup Keep

- doc/tasks/20260930-main-code-submission/task.md
- doc/tasks/20260930-main-code-submission/execution-log.md
- doc/tasks/20260930-main-code-submission/verification-report.md
- doc/tasks/20260930-main-code-submission/commit-files.json

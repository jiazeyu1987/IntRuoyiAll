# Verification Report

## Scope

本任务仅沉淀项目级术语契约，不修改生产代码。验证覆盖规则缺失基线、规则内容、长期索引、UTF-8 编码、Git diff 和分支运行端口门禁。

## Results

- RED：旧基线 `a116b851` 的根 `AGENTS.md` 不包含“工艺路线三类配置术语契约”，`rg` 退出码为 1。
- GREEN：当前根 `AGENTS.md` 明确区分工序开始上传人、工序设置批记录表单、表单槽位 `formBindings`，并禁止跨数据源替代。
- GREEN：`docs\experience-index.md` 已增加三类术语和 `batchRecordFormNames`、`formBindings` 的检索入口。
- GREEN：四个已写入文档均通过严格 UTF-8 解码。
- GREEN：`git diff --check` 通过。
- GREEN：`scripts\preflight\branch-runtime-port-guard.ps1` 通过，任务 worktree 为 `int_main` profile、slot 4、前端 8085、后端 48085。
- GREEN：前后端检查点 `f18927b9` 已推送至 `origin/int_main`，未包含之后出现的并发未提交改动。
- GREEN：术语规则提交 `33b7e407` 已推送至 `origin/codex/20260727-batch-record-form-terminology`。
- BLOCKED：`task-closeout-cleanup` preview 保留三个核心任务文档且无删除项，但检测到主工作区脏状态，禁止执行 `ff-only` 合并和 worktree 删除。
- GREEN：术语提交已重放到本地 `int_main` 既有基线 `40b7f7b9` 之后；索引冲突同时保留主分支和本任务两条规则。
- GREEN：经用户明确授权，`git push origin HEAD:int_main` 将远端 `int_main` 从 `9b94ac81` fast-forward 到 `c92f45a4`，未使用 force push，未提交或清理主工作区的未跟踪并发文件。
- GREEN：根规则已补充批次执行职责：工序开始负责附件开始动作，表单槽位负责补充动态表单，批记录表单负责逐工序正式生产批记录。
- RED：上一版远端基线 `a9dfaf9e` 未命中“正式生产批记录载体”“补充表单链路”和“工序开始不提供表单内容”。
- GREEN：当前规则及经验索引命中三类独立配置入口、批次执行作用和 `batchRecordFormNames` 数据来源约束。
- GREEN：五个写入文件通过严格 UTF-8 解码，`git diff --check` 和分支运行端口门禁通过。

## Conclusion

术语契约写入、验证和远端 `int_main` 集成均通过。由于本地主工作区仍存在其它任务的并发未提交改动，本任务保持 `ready_for_closeout`；待主工作区恢复干净后再同步本地主分支、清理 worktree 并释放 slot 4。

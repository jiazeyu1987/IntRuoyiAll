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

## Conclusion

术语契约写入和验证通过，可以进入任务提交、集成与收尾清理。

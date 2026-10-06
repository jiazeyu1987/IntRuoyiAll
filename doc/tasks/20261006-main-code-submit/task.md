# 提交推送主干代码

## Goal

按用户当轮“提交推送主干代码”的授权，将 int_main 起始未提交文件保存为独立基线提交，并将既有主干提交与本任务收尾记录推送到 origin/int_main。

## Milestones

- completed：核对主干、远端、冻结18个文件及内容指纹；远端 behind=0。
- completed：差异、文件类型、敏感信息和端口门禁验证；独立代码基线提交1b0bf53d5。
- in_progress：经验归并、任务 ready_for_closeout、cleanup preview/apply。
- pending：推送并核对远端 HEAD、ahead/behind 和工作区状态。

## Expected Verification

- 当前分支为 int_main，origin 可用，远端没有未合入提交。
- 冻结既有 dirty 的路径及 SHA-256；暂存前后核对内容与清单，不纳入冻结后的并行新改动。
- git diff --check、git diff --cached --check、文件类型及敏感信息检查通过；branch-runtime-port-guard 和 Git 钩子通过。
- 本任务文档 UTF-8 与必需结构正确，cleanup preview/apply 仅处理本任务资产。
- git push origin int_main 成功，远端 HEAD 等于本地 HEAD，ahead/behind=0/0。
- 本任务是代码快照提交，不修改业务实现；Git 核验不代表业务验收，不新增构建或业务回归门禁，本轮未要求 E2E。

## Current Status

ready_for_closeout

## 设计约束检查

- 当前为 E:/IntRuoyi 主工作区，分支 int_main；起始 HEAD=00897b976，领先 origin/int_main 7 个提交，初检 18 个 dirty 文件。
- 起始 dirty 按用户提交主干授权纳入基线；本任务文档与代码基线分别提交。
- 不修改并行任务代码或状态，不操作其他 worktree、数据库、服务或远端运行环境。
- 不强推、不绕过钩子、不引入 fallback；不提交凭据、构建产物和临时输出。

## Cleanup Keep

- doc/tasks/20261006-main-code-submit/task-state.json

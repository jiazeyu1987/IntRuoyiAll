# 主干代码提交推送

## Goal
按用户“提交推送推送主干代码”的授权，将当前 int_main 的既有源码、测试及规则文档变更核验后提交并推送 origin/int_main。

## Milestones
1. 规则、分支、远端与变更清单核对：completed；int_main 与 origin/int_main 初始一致，54 文件清单及 SHA-256 已冻结。
2. 端口门禁、定向回归与暂存核验：completed；Maven 14 类 344 项、前端 56 项、Python 1 项和静态合同通过。
3. 既有变更独立基线提交及推送：completed；5264c8e48 已推送 origin/int_main。
4. 本任务 cleanup preview/apply：completed；保留 3 份核心记录，删除本任务临时清单和原始日志。最终记录使用独立提交推送，并以远端 HEAD 核对为最终门禁。

## Expected Verification
- 确认 int_main、origin 存在，远端更新可快进；禁止强推。
- branch-runtime-port-guard、无冲突、git diff --check 与 staged 清单核验。
- 当前变更对应 Node 静态/handler 测试、Python 迁移依赖测试与 Maven 定向单测。
- 本任务 Markdown UTF-8/结构检查；cleanup preview/apply。
- git ls-remote 与本地 HEAD 一致，工作区干净，无 ahead/behind。

## Current Status
completed — 54 文件代码基线和经验文档均已推送；定向回归与 cleanup preview/apply 全部通过。最终记录独立提交，推送后由 git ls-remote、状态及 ahead/behind 核验收口；失败则重新标记 blocked。

## 设计约束检查
- 本任务仅提交已有改动，不新增或重构生产行为；沿用原任务 BDD/RED/GREEN，重新运行定向回归，不伪造 RED。
- 初始既有改动使用独立基线提交，任务收尾记录另行提交；选择性暂存明确清单。
- 不操作服务器、数据库、运行服务或其他任务产物，不执行真实 E2E。
- 当前为主 checkout，不涉及 worktree 合并、创建或删除。
- task-closeout-cleanup 和 project-experience-consolidation 已加载；现有 docs/worktree-memory.md 提交分类门禁适用。

## Cleanup Keep
- doc/tasks/20261001-main-code-submit/task.md
- doc/tasks/20261001-main-code-submit/execution-log.md
- doc/tasks/20261001-main-code-submit/verification-report.md

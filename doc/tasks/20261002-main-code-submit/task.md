# 主干代码提交推送

## Goal
按用户“提交推送主干代码”的授权，将本次开始时 int_main 工作区已有代码、测试和正式规则文档变更核验后提交并推送 origin/int_main。用户追加选择“先修复该既有失败，再提交推送”，修复 AI 测试脚本与现有评审弹窗、历史详情入口不一致的问题。

## Milestones
- M1：核对规则、分支、变更清单与远端状态。
- M2：运行适配变更的定向验证、端口门禁与差异检查，提交已有变更基线。
- M3：经验核对、cleanup preview/apply、提交本任务记录、推送并核对远端。

## Expected Verification
- int_main 分支及 origin 可用，无未解决冲突；推送不使用 force。
- 前端相关静态和行为测试；后端相关测试既有证据核对及必要的定向复跑。
- branch-runtime-port-guard.ps1、git diff --check、git diff --cached --check。
- 当前变更清单、暂存清单、提交清单一致；文档 UTF-8 与结构检查。
- cleanup preview/apply 只覆盖本任务；最终 HEAD 与 origin/int_main 及远端分支一致。

## 设计约束检查
- 保留现有应用业务行为，修复已发现的 S05/S08 自动化入口失配；不执行真实 E2E、数据库写入、部署或服务重启。
- 保持 Git 换行规范化配置，不提交秘密或临时测试输出。
- 只纳入开始时明确列出的变更和本任务记录；若发现并行内容漂移，重新核对后处理。
- 不处理其他任务目录、进程或 worktree；当前为主工作区，无合并或删除 worktree 步骤。

## Current Status
completed

## Progress
- M1：PASS，66 文件初始白名单、指纹、int_main 与 origin 核对完成。
- M2：PASS，后端 155 项测试、前端定向测试及修复后的 21 文件 AI 静态/行为套件均通过；差异检查及缺陷证据 validator 通过。
- M3：PASS，经验已合并至 docs/e2e-rules.md；基线 1adade43a、修复 e58b9eba7 已提交并推送 origin/int_main。cleanup preview/apply 保留 3 份核心记录，删除 20 份本任务临时产物，阻塞与警告均为 0。代码推送后工作区干净，ahead/behind=0/0。

## Final Verification
后端 155 tests / 0 failures / 0 errors / 0 skipped；前端定向回归与 21 文件 AI 静态/行为套件全部通过。真实 E2E 未执行。最终收尾记录单独提交推送，再核对远端 ref 与本地 HEAD；若推送失败须重新记录阻塞，不得以代码推送成功冒充收尾推送成功。

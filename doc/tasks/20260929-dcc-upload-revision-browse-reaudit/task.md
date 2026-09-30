# DCC 上传、升版、浏览当前源码复审

## Goal
按用户要求修复已记录的一般问题并静态复核上传、升版、浏览三条链路；不进行 E2E。高优先级问题由主 Agent 处理，一般问题由子 Agent 分组实现，主 Agent 最终 review。

## Milestones
1. 读取规则、业务设计与既有问题记录，锁定当前工作区基线。
2. 按 URB2-001 至 URB2-014 分组实现一般问题，生产代码先记录 BDD/RED，再 GREEN/REGRESSION。
3. 主 Agent review 子 Agent 差异，覆盖事务、权限、版本身份、异步生命周期和错误反馈。
4. 运行不涉及 E2E 的定向静态/单元检查，更新验证证据；按任务规则收尾。

## Expected Verification
- 静态逐项调用链核对与 UTF-8/路径/锚点/编号结构检查。
- 仅记录静态推导，不声称真实复现或运行态 PASS。
- 不执行 E2E、业务接口、数据库操作、服务启动/停止/重启或提交推送；本轮已明确授权启用子 Agent。
- 生产代码修复必须记录 BDD/RED/GREEN/REGRESSION；沿用 review-report.md 的 Given/When/Then 作为验收边界。

## Current Status
blocked

一般问题修复完成：子 Agent 分组实现，主 Agent review；14 项问题均有实现状态或残余风险记录。前端静态合同/类型检查、后端编译和 227 个 DCC 定向单测通过。ready_for_closeout 阶段 cleanup preview/apply 均 PASS，保留 4 份文档、删除 0、无 warnings。仅因本轮未授权 Git 提交推送而 blocked，未擅自执行 Git 写操作。

## 设计约束检查
- 当前目录虽名为 int_main，实际分支 int_qms；HEAD a9bcb6d36d96145ddc1252f111347b644b328deb；开始时 git status 有 380 条既有改动。审计对象为工作区当前文件。
- 仅处理本任务目录，保留其他任务和未提交代码；历史修复报告不能替代源码复核。
- 三条链路分别覆盖，共用问题只计一次；需求未确认、性能与部署风险单列。
- 用户本轮未授权 Git 提交推送，遵守 AGENTS.md 优先于 docs 的自动提交要求；不为文档收尾擅自提交全部脏工作区。

## Cleanup Keep
- doc/tasks/20260929-dcc-upload-revision-browse-reaudit/task.md
- doc/tasks/20260929-dcc-upload-revision-browse-reaudit/execution-log.md
- doc/tasks/20260929-dcc-upload-revision-browse-reaudit/verification-report.md
- doc/tasks/20260929-dcc-upload-revision-browse-reaudit/review-report.md

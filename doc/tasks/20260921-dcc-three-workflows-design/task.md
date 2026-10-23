# DCC 三套流程与部门负责人会签开发文档

## Goal

依据本轮用户确认的上传、升版、作废流程以及部门负责人会签规则，核对当前源码，交付可实施的业务规则、技术设计、验收计划及文档 Review。仅修改本任务文档，不实施业务功能。

## Milestones

1. 核对规则、源码和旧设计冲突。
2. 编写三套流程、培训分支、部门矩阵、任务创建时负责人快照、迁移及测试设计。
3. 独立复读并对照需求、代码与验收场景 Review，修正发现项。
4. 文档结构与路径验证，记录交付和收尾状态。

## Expected Verification

- 每条用户需求对应设计条款和 Given/When/Then 验收场景。
- 当前行为与拟开发行为分开，代码文件及方法锚点可定位。
- 覆盖五条正常路径、唯一负责人校验、任务创建时快照、异常、重试和历史流程切换。
- UTF-8、Markdown 链接、编号、代码路径及 Review 问题闭环检查。
- 本任务为文档变更，不运行产品测试或 E2E，不记录虚构 RED/GREEN。

## Current Status

blocked

用户要求的开发文档、Review 和本地验证均已完成；ready_for_closeout 后 cleanup preview/apply 已通过，删除清单为空。仅仓库提交/推送收尾门禁未完成：docs/task-closeout-rules.md 要求推送后才能标 completed，而根 AGENTS.md 禁止未获当轮明确授权的提交/推送，本轮无此授权。此状态不表示文档或业务设计检查失败，不应据此重复创建开发文档任务。

## 设计约束检查

- 三个动作三份独立流程定义；上传与升版结构相同，作废不含培训、分发。
- 上传、升版的 needTraining 唯一决定是否加入培训节点。
- 会签矩阵仅决定部门，管理员维护唯一负责人，任务创建时冻结人员身份。
- 不将流程提交时快照、预览时快照误当作任务创建时快照。
- 不引入缺人自动通过、选择第一人、上级部门替代、静默跳节点等 fallback。
- 不修改业务代码、数据库、运行服务或其他任务资产；不启用子 Agent，不提交或推送 Git。
- 仓库收尾规则与本轮授权冲突时，以用户 AGENTS 指令优先；收尾工具缺失如实记录，不用自制成功结果替代。

## Cleanup Keep

- doc/tasks/20260921-dcc-three-workflows-design/task.md
- doc/tasks/20260921-dcc-three-workflows-design/execution-log.md
- doc/tasks/20260921-dcc-three-workflows-design/verification-report.md
- doc/tasks/20260921-dcc-three-workflows-design/verify-docs.cjs

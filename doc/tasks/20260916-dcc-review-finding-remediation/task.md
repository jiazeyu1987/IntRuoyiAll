# DCC Review Finding Remediation

## Goal
核对用户提供的 5 条 DCC 问题分析；对代码证据成立的项修复，对不成立或符合当前设计的项记录不修改原因。

## Current Status
ready_for_closeout

## Milestones
1. 读取仓库开发、数据库和收尾规则。
2. 用源码逐条验证 5 条分析是否成立。
3. 对成立项先补 BDD/TDD 回归，形成 RED。
4. 修复后端和必要前端逻辑，运行定向回归和静态检查。
5. 整理验证报告；按既有授权提交并推送。

## Expected Verification
- 延迟、重复或跨流程驳回事件不得覆盖 `ACTIVE`、`FINALIZATION_FAILED` 或其它非审批中状态。
- 同一 master 已存在未完成版本时，不允许再次从旧正式版检出生成新的工作迭代。
- 受控浏览分配范围只能收窄候选，不得绕过文件名/元数据查看授权。
- 上传页继续保持“创建 WORKING 版本后再提交审批”的现有工作迭代边界，不自动送审。
- 项目 OWNER 对已生效版本的大版本检出入口与动作投影一致；非 requester 不得检出已有 WORKING/REJECTED/返工迭代。

## BDD
- BDD: 驳回事件幂等保护 -> Given 文件已经 `ACTIVE` 或事件流程实例不匹配；When 收到 BPM REJECT 事件；Then 不写入 `REJECTED`，也不记录拒绝审计。
- BDD: 驳回事件状态迁移 -> Given 文件处于审批中且事件流程实例匹配；When 收到 BPM REJECT 事件；Then 仅该行从审批中迁移为 `REJECTED`，并检查数据库更新成功。
- BDD: 未完成版本阻断检出 -> Given 同一 master 已有 `WORKING` 新迭代；When 用户再次检出旧 `ACTIVE` 版本；Then 拒绝检出且不创建 checkout 锁。
- BDD: 分配范围不等于查看授权 -> Given 用户被分配到某文件但没有矩阵/目录查看权限；When 查询受控浏览；Then 该文件不出现在列表。
- BDD: 项目 OWNER 大版本检出 -> Given 当前文件 `ACTIVE` 且用户是项目 OWNER 但不是 requester；When 用户检出该文件准备升大版；Then 后端允许检出，前端显示检出入口。
- BDD: 上传工作迭代边界 -> Given 上传页调用 `/working`；When 保存成功；Then 只创建 WORKING 版本并提示到浏览页提交审批，不自动发起审批。

## 设计约束检查
- 不修改历史数据，不新增 schema，优先复用现有 mapper 条件更新和 master 锁。
- 权限修复必须保持“分配范围是约束，不是授权来源”。
- 检出放宽只针对 `ACTIVE/SUPERSEDED` 的大版本入口；已有工作迭代仍保持 requester 归属。
- 不执行 E2E、服务重启、远程服务器或数据库写入。

## Cleanup Keep
- doc/tasks/20260916-dcc-review-finding-remediation/task.md
- doc/tasks/20260916-dcc-review-finding-remediation/execution-log.md
- doc/tasks/20260916-dcc-review-finding-remediation/verification-report.md

# DCC 前后端提交并融合 int_main

## Goal
提交当前已验证的 DCC 管理员项目可见性、上传候选、缓存路由和默认属性提示改动，并将 int_qms 融合进本地 int_main。

## BDD
- Given int_qms 包含对应前后端实现和既有验证记录，When 执行提交并合并，Then 仅目标源码、测试及相关任务记录进入提交，合并后 int_main 包含这些改动。
- Given 工作区另有无关任务资产和日志，When 选择提交内容，Then 这些文件不进入本次提交。

## Milestones
- [x] 核对分支、规则、改动边界和既有验证证据。
- [ ] 提交目标 DCC 前后端实现和验证记录。
- [ ] 将 int_qms 合入本地 int_main 并验证结果。

## Expected Verification
端口漂移保护、目标 diff 检查、提交文件清单、合并结果及合并后工作区状态。

## 设计约束检查
当前分支应为 int_qms；融合目标为本地 int_main。只提交对应的 DCC 源码、定向测试与其任务报告。不推送远端；不纳入无关日志、其他任务资产或凭据。

## Current Status
in_progress

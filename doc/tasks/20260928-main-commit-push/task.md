# 主干提交推送
## Task Goal
提交现有 eDHR 主干业务修复、测试及相关文档并推送 origin/int_main。
## Milestones
- M1 completed：核对主干、变更范围与既有607项回归证据。
- M2 completed：检查差异并提交推送。
- M3 completed：收尾清理与远端一致性核验。
## Expected Verification
检查提交差异、冲突标记、既有回归证据及远端提交一致性。本任务不新增业务行为，不重写历史RED/GREEN，不执行E2E或部署。
## Current Status
completed
## 设计约束检查
用户明确授权提交推送主干。仅纳入业务代码、正式测试和相关开发/缺陷文档；保留AOCI集成、历史临时文件删除、备份及绘图文件，不纳入本次业务提交。根AGENTS任务归属约束优先于closeout文档全量脏基线规则。

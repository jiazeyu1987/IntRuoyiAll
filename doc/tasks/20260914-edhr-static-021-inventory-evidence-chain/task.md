# EDHR-STATIC-021 Inventory Evidence Chain

## Task Goal

修复 EDHR-STATIC-021：第四份生产放行资料完成后进入最终放行时，正常领料/批次来源流程必须能够生成并消费正式库存追溯证据。证据必须可追溯到活跃订单、物料、批次和来源单据；缺少真实库存来源时必须 fail-fast，不得模拟通过。

## Milestones

- [x] 读取项目 AGENTS、收尾规则、bug regression skill 合同及 eDHR / 领料 / 库存追溯 / 四份资料 / 最终放行相关规则。
- [x] 写入 BDD 场景与初始任务约束。
- [x] 建立 EDHR-STATIC-021 定向 RED 或静态合同证据。
- [x] 实现最小正式链路修复。
- [x] 执行非 E2E 定向验证与静态合同检查。
- [x] 更新 verification-report.md，并按初始禁止 commit/push 的边界记录阻塞状态。
- [x] 用户本轮授权提交并融合进 `int_main` 后，迁移到 D 盘任务集成 worktree 并完成主线基线定向复验。

## Expected Verification

- 静态源码合同或定向单元测试覆盖：正常活跃订单正式领料/批次来源可生成库存追溯证据，最终放行库存检查能消费该证据。
- 负向覆盖：正式领料/批次来源缺失、物料批次缺失或来源单据身份缺失时 fail-fast，不创建最终放行待办，不写模拟 trace。
- 禁止 Playwright/E2E、数据库写入、启动/停止/重启服务、远程服务器操作；本轮用户已授权 Git 提交与融合进 `int_main`。

## Current Status

completed - 定向验证 19 tests PASS；cleanup preview/apply PASS；任务记录提交 7393f6731 已快进融合并推送到 origin/int_main；D 盘任务 worktree 已删除。最终记录单独提交。

## Design Constraints Check

- No fallback：不得删除库存检查、硬编码 PASS、默认生成空来源、吞异常或用旧附件/前端字段拼接补齐。
- Formal source only：库存证据必须来自正式领料/库存来源链，至少包含 activeOrderId、workOrderId/生产订单编号、物料、批次、来源单据及明细身份。
- Scope：只处理 EDHR-STATIC-021，不处理 EDHR-STATIC-022 或共享缺陷总表。
- Verification：仅执行静态代码逻辑检查和必要非 E2E 定向验证。

## Cleanup Candidates

- doc/tasks/20260914-edhr-static-021-inventory-evidence-chain/bug-regression-evidence.md

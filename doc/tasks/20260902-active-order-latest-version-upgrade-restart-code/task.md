# Task: Active Order Latest Version Upgrade Restart Code

## Task Goal

根据 `docs/product/active-order-latest-version-upgrade-restart-prd.md` 与 `docs/product/active-order-latest-version-upgrade-restart-user-operation.md`，在专用 worktree 内实现活跃订单“升级”入口和后端升级申请契约的第一条代码链路。

## Design Constraints Check

- 只在 `D:\IntRuoyiWorktree\20260902-active-order-latest-version-upgrade-restart-docs` worktree 内开发，不修改 `E:\IntRuoyi` 主工作区。
- 遵守无 fallback：不使用当前 ACTIVE 配置补旧历史，不吞异常，不模拟审批成功。
- 活跃订单升级入口固定为活跃订单内“升级”按钮；不提供逐项版本选择。
- 本轮先实现可静态验证的前后端契约、入口、状态文案和测试；若缺少审批/表结构前置条件，记录 blocker，不假装完整应用重启已完成。
- 不操作数据库写入、不启动/停止 int_main 服务、不提交/推送，除非用户后续明确授权。

## Milestones

- M1 Code Scope Discovery: completed - 已定位活跃订单前后端实现、重建链路、最新路线/QA mapper 和审批持久化缺口。
- M2 RED Tests: completed - 已写入前后端静态测试，先失败于缺少升级入口与后端服务契约。
- M3 Implementation: completed - 已实现最小代码链路：活跃订单升级按钮、预览弹窗、前端 API、后端预览/提交接口、待审批申请持久化、旧活跃订单冻结和幂等提交。
- M4 Verification: completed - 静态测试、SQL 迁移合同、后端编译和前端 ts:check 均已通过。
- M5 Approval Effect Chain: completed - 已实现审批通过后可调用的生效服务：作废旧批次、取消旧批次待办、移除旧活跃订单、强制按全部最新版本创建新活跃订单并回写申请为 APPLIED。
- M6 BPM Approval Callback Chain: completed - 已接入统一业务审批编排器，提交后发起 BPM，新增升级重启 EffectExecutor，审批通过触发重开，驳回/取消会释放旧订单冻结；审批待办中心补充“活跃订单升级重启”标题与摘要。
- M7 Full E2E Verification: completed - 已在 worktree 运行态 `8093/48093` 使用 Playwright 真实页面验证审批通过后的完整终态：旧活跃订单 45 已移出活跃池并标记 `REMOVED/VERSION_UPGRADED`，新活跃订单 1009200001 以路线版本 742 / V12 进入活跃池并可打开详情；审批流程实例已结束。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/active-order-version-upgrade-entry-static.spec.cjs`
- `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-active-order-version-upgrade-code-static.spec.cjs`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_mes_active_order_version_upgrade_request_sql.py -q`
- `node IntRuoyiFronted/tests/e2e/active-order-version-upgrade-final-state-real.e2e.cjs`
- 视代码改动范围运行可用的定向 Maven / 前端静态命令。

## Current Status

ready_for_closeout - 已完成活跃订单升级入口、提交发起 BPM、旧订单冻结、审批通过重开、驳回/取消解冻、定向静态/编译验证和真实页面终态 E2E。当前保留完整 E2E 证据，尚未执行 Git 提交、合并或删除 worktree。

## Cleanup Keep

- doc/tasks/20260902-active-order-latest-version-upgrade-restart-code/e2e-artifacts/

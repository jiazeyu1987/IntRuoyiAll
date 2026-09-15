# DCC 20项修复复核

## Goal
基于当前 `int_qms` HEAD 独立复核 DCC-MAIN-01 至 DCC-MAIN-20 是否仍成立为已修复状态；不直接沿用上一任务结论。若发现测试或源码回退，修复后重新验证并记录证据。

## Current Status
completed

## Milestones
1. 读取仓库规则、确认分支、工作区和上一轮20项修复证据。
2. 重跑20项对应后端静态合同、前端行为合同和定向后端回归。
3. 对失败项区分生产缺陷与过期测试；必要时修复并执行 RED/GREEN。
4. 更新复核报告，清理/提交/推送当前任务资产。

## Expected Verification
- 后端 DCC 静态合同：`IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-*.cjs` 中与20项直接相关的合同。
- 前端 DCC 行为合同：上传、检入、审批、目录、模板、权限、关联文件、异步归属、产品解析相关 Node tests。
- 后端 Maven 定向回归：上一轮20项修复使用的 DCC 关键测试类。
- 前端 `pnpm ts:check` 与 `pnpm build:local` 如环境允许执行。
- 本轮不执行真实页面 E2E、数据库写入、服务重启或远端服务器操作，除非用户另行要求。

## BDD
- BDD: final approval immediate activation -> Given stamped PDF and configured formal default directory; When the last ordinary approval passes; Then the file activates without mandatory training, distribution, manual directory or standalone publish.
- BDD: checkout/checkin governed version change -> Given an existing controlled file; When changing content or metadata; Then version changes only through checkout/checkin with explicit minor/major rules, current locks and matching CAD/PDF attachment identity.
- BDD: removed approval mutations -> Given ordinary DCC approval; When return, transfer, before-sign or after-sign is attempted from page/API/BPM paths; Then it is unavailable and performs no write.
- BDD: template/directory/identity/permission governance -> Given invalid template folders, directory cycles, name-only users, content users, unbound products and relation pagination; When users search, upload, bind or browse; Then each rule fails or succeeds through the formal source of truth without leaking content or mixing identity.
- BDD: async and attachment recovery -> Given stale responses, failed uploads or unbound tickets; When the user switches context or retries; Then stale results cannot overwrite current state and cleanup preserves recoverable tickets without binding old content.

## Design Constraints
- 不引入 fallback、降级、吞异常、模拟成功。
- 复核以当前源码和当前测试输出为准；历史任务记录仅作为命令和范围参考。
- 测试脚本若因生产代码新增变量而过期，修复测试沙箱后必须重跑原行为断言。
- 未执行真实页面 E2E 时，不声称真实页面闭环已通过。

## Cleanup Keep
- doc/tasks/20260916-dcc-20-reverification/task.md
- doc/tasks/20260916-dcc-20-reverification/execution-log.md
- doc/tasks/20260916-dcc-20-reverification/verification-report.md

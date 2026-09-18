# EDHR-STATIC-016 PQC更正冻结门禁

## Task Goal

修复 EDHR-STATIC-016：PQC提交进入不合格评审冻结后，普通“修改PQC表单”更正入口必须在签名、事件修订、逐件结果重建、PQC正式记录更新和过程检验汇集前 fail fast，避免冻结期间改写检验事实。

## Scope

- 只处理 EDHR-STATIC-016。
- 不处理 EDHR-STATIC-017 及其它缺陷。
- 不执行 Playwright/E2E、数据库写入、服务启动/停止/重启、远程服务器操作、git commit 或 git push，除非用户本轮后续明确授权。

## Milestones

- [x] M0 读取仓库规则、收尾规则、bug修复技能、eDHR/PQC冻结相关文档。
- [x] M1 写入 BDD/TDD 验收合同。
- [x] M2 定位 PQC更正入口与不合格冻结正式门禁。
- [x] M3 先补 RED 静态/定向合同，证明冻结期间更正当前未被阻断。
- [x] M4 最小化修复更正服务冻结前置校验。
- [x] M5 执行非 E2E 定向验证，记录 GREEN 与风险。
- [x] M6 标记 ready_for_closeout，执行 cleanup preview；因未授权提交/推送，按项目规则记录最终状态。

## Expected Verification

- 单元测试覆盖：待处理不合格评审冻结期间，PQC更正入口抛出冻结业务错误。
- 单元测试覆盖：冻结拒绝发生在签名、revision、逐件删除/新增、正式记录更新、汇集之前。
- 单元测试覆盖：无冻结且其它前置满足时既有更正路径保持通过。
- 定向检查无新增 fallback、吞异常、默认成功或只在前端隐藏按钮。
- 不执行 E2E、数据库写入、服务启停、远程操作、git commit/push，除非用户本轮后续明确授权。

## Current Status

ready_for_closeout

实现和非 E2E 定向验证已完成，用户于 2026-09-14 后续明确要求本地 git commit 并融合进 `int_main`；当前进入本地提交/融合收尾。Git push 仍未获当轮明确授权，完成状态需按项目推送门禁另行核对。

## Design Constraints Check

- No fallback：冻结态必须明确拒绝普通更正入口，不以签名审计、前端隐藏或后续放行校验替代。
- Fail fast：冻结检查必须在签名和任何更正写入前执行。
- 正式冻结来源：复用 `MesProEdhrNonconformanceReviewService.ensureWorkOrderNotFrozen` 的工单冻结门禁。
- 范围边界：不新增处置流程，不放宽已放行检查，不处理损耗数量上限问题。
- 验证边界：仅运行静态代码逻辑检查和必要非 E2E 定向验证。

## Cleanup Candidates

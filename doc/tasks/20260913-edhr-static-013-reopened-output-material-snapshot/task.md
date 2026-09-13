# EDHR-STATIC-013 Reopened Output Material Snapshot

## Task Goal

修复重新打开的 EDHR-STATIC-013：正式活跃订单工序快照缺少 `outputMaterialIds`，导致多输出物料拆分提交时保守进度算法在真实数据下不可达，并回退到旧的分配累加口径。

## Milestones

- [x] M1 读取项目规则、缺陷证据与独立复核证据，确认修复边界。
- [x] M2 写入 BDD 场景和 RED 验证设计。
- [x] M3 补充定向静态/合同回归，证明正式订单快照必须写入全体输出物料。
- [x] M4 实现最小修复，确保活跃订单快照生产方输出 `outputMaterialIds`。
- [x] M5 执行非 E2E 定向验证、`git diff --check` 与变更范围静态审查。
- [x] M6 更新任务证据并进入收尾状态。

## Expected Verification

- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs`
- 如可行，执行受影响后端定向单元测试，不启动服务、不写数据库、不执行 E2E。
- `git diff --check`
- 变更范围静态审查：仅覆盖 EDHR-STATIC-013 必需的快照生产方、定向测试和任务证据。

## Current Status

ready_for_closeout

已完成 EDHR-STATIC-013 定向静态合同、最小生产修复、Java 编译门禁、`git diff --check`、变更范围静态审查和经验沉淀。用户要求先提交再融合 `int_main` 后，已在 `E:\IntRuoyi` 的 `int_main` 完成本地实现提交 `2618a4a3f`，并通过 merge commit `362929947` 整合最新 `origin/int_main`；融合后静态合同、端口矩阵门禁、MES 编译、bug evidence 校验和 cleanup preview/apply 均通过。新本地提交尚未获明确 Git push 授权，因此任务保持 `ready_for_closeout`。

## Design Constraints Check

- 保留允许冻结输出物料非空子集分次提交的业务合同。
- 快照必须由正式活跃订单加入链路写入全体冻结输出物料，不允许测试手工字段掩盖生产写入缺口。
- 进度口径按订单、工序、物料累计事实并对全体输出物料取最小完成口径；本任务只修复重新打开项必要范围。
- 不引入 fallback、兼容绕过、吞异常、默认成功或禁止正常分次提交的替代方案。
- 用户明确要求不执行 E2E、不启动服务、不写数据库；本轮仅授权本地 Git 提交并融合 `int_main`，Git push 需要单独明确授权。

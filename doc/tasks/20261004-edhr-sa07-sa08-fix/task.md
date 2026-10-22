# SA07 / SA08 正常路径修复

## Current Status
ready_for_closeout — 管理者源码及412项实际主干组合回归PASS；精确本地集成与清理待执行。

## Goal
修复跨订单合法 allocation 的来源签名读取，以及 BPM 管理上市放行正式签名 ID 持久化/追溯。

## Milestones
1. completed — 核对基线、来源 writer、allocation、审批签名与所有 reader 合同。
2. completed — 最小实现与真实合同回归。
3. completed — 实际 Java17 回归 338/0/0/0，精确指纹与证据已交付 review_ready。

## Expected Verification
实际 C:/Users/BJB110/.jdks/jdk-17.0.20+8；自有 target。合法 A 来源/B allocation 当前及归档详情证据；非法来源/tenant/actor/domain/hash 拒绝。BPM核验签名贯穿正式事务/decision/audit/upstream/读取及重放；inline MES、PQC、SA01-SA06与显式 P1 相邻合同。

## 设计约束检查
基线 d3f1793c8831cc4d070b786553e3a2c002e17ca9。仅当前隔离 worktree 修改；无提交/push/merge/archive/服务/真实数据库写入/E2E/历史补录。管理者未提交报告已从 E:/IntRuoyi/doc/tasks/20261003-edhr-thread-management 读取。用户要求 review_ready 覆盖默认完成/推送/清理门禁，最终集成与清理由管理者安排。保持三配置链独立、严格来源/tenant/身份/hash校验。不得修改或复制并行资产。

## Original Verification (historical)
Microsoft OpenJDK 17.0.20+8-LTS / Maven 3.9.16；本 worktree 自有 target。final-regression.log exit 0，28 类、338 tests / 0 failures / 0 errors / 0 skipped。无实现/测试 blocker；独立 review、并行资产组合、集成与收尾待管理者安排。详情见 verification-report.md；22 项改动 UTF-8/LF SHA256 见 source-fingerprints.json。

## Integration fixture revision
管理者业务方向评审通过，但 a37a 组合 testCompile exit 1，尚未执行测试。本轮只修改自有实际审批合同的依赖装配，并在本任务目录生成 main PQC 测试缺失 reader 参数的精确 patch。a37a 与 E:/IntRuoyi 只读；不得复制并行生产源码，不扩改业务。保留原 338 证据；自有定向回归与最终组合结果分开记录，后者由管理者机械应用 patch 后复跑。
4. completed — 自有依赖夹具整改、参考 patch/指纹、Java17 必要回归 67/0/0/0，管理者组合 412/0/0/0，重新交 review_ready。

## Current fixture verification and manager review
本轮仅修改一项自有测试的真实依赖装配，另外 21 项原自有资产不变；当前指纹见 integration-source-fingerprints.json。三个证据独立记录：历史自有 28 类/338 例；修改后自有 Java17 4 类/67 例；管理者 a37a 机械应用两项 patch 后 Java17 30 类/412 例，均 0 failures/0 errors/0 skipped、Maven exit 0。管理者 review PASS，42 项组合输出及 23 项 main 参考来源未漂移。组合证据只读引用，不冒称本线程执行。

真实 E2E 未执行。本线程未修改 main/a37a，未提交、推送、合并、操作服务/DB/远程或清理。状态 review_ready，待管理者核对最终记录后安排精确集成与下一轮完整审查。交接见 integration-fixture-handoff.md。
## Cleanup Keep
- doc/tasks/20261004-edhr-sa07-sa08-fix/task-state.json
- doc/tasks/20261004-edhr-sa07-sa08-fix/integration-source-fingerprints.json
- doc/tasks/20261004-edhr-sa07-sa08-fix/integration-fixture-handoff.md
- doc/tasks/20261004-edhr-sa07-sa08-fix/integration-manager-combination-evidence.json
- doc/tasks/20261004-edhr-sa07-sa08-fix/main-pqc-reader-fixture.patch
- doc/tasks/20261004-edhr-sa07-sa08-fix/main-pqc-reader-fixture-manifest.json

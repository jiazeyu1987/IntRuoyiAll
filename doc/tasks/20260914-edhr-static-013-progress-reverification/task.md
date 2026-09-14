# EDHR-STATIC-013 Progress Reverification

## Task Goal

复核并在必要时修复 EDHR-STATIC-013：生产进度计算必须受正式分配数量和冻结输出物料全集双重约束，防止部分分配、多输出物料和拆分提交导致进度虚增。

## Milestones

- [x] M1 读取项目规则、收尾规则和 eDHR/活跃订单/报工/正式分配/输出物料快照/工序进度相关文档。
- [x] M2 写入 BDD 场景、验证边界和禁止项。
- [x] M3 静态复核现有实现和定向合同，确认是否仍存在进度虚增缺口。
- [x] M4 发现消费方测试未匹配新 fail-fast 正式来源合同，补 RED 证据后更新定向回归并转 GREEN。
- [x] M5 运行用户允许范围内的非 E2E 定向验证，记录结果、风险和 blockers。
- [x] M6 标记 ready_for_closeout，运行 cleanup preview；因用户禁止 git commit/push 且当前 detached worktree 无法解析分支，本轮不标记 completed。
- [x] M7 用户已授权提交并融合进 `int_main`；按 worktree 门禁从最新 `int_main` 创建 D 盘任务 worktree、登记 slot，并迁移最小 EDHR-STATIC-013 diff。
- [ ] M8 提交任务实现与证据，快进融合 `int_main`，推送远端，并完成 cleanup / completed 收尾记录。

## Expected Verification

- 静态代码逻辑检查：生产进度消费方必须使用实际正式分配量，而不是整笔生产事件数量。
- 定向静态合同：覆盖报工 100 件仅正式分配 40 件时，订单进度最多按 40 件纳入。
- 定向静态合同：覆盖多个输出物料各自按正式分配约束累计，并取保守完成口径。
- 定向静态合同：覆盖拆分提交与合并提交结果一致。
- 允许运行 Node/JUnit/Maven 的定向非 E2E 验证；禁止 Playwright/E2E、数据库写入、启动/停止/重启服务、远程服务器操作、git commit/push。
- `git diff --check` 仅作为本地静态检查；若命中非本任务既有脏改，需分层记录。

## Current Status

ready_for_closeout

实现和用户允许范围内的静态/定向验证已完成；用户已在 2026-09-14 明确授权提交并融合进 `int_main`。当前收尾改为在 `D:\IntRuoyiWorktree\20260914-edhr-static-013-progress-reverification` 具名任务 worktree 中完成提交、主线复验、快进融合、推送与 cleanup。

## Design Constraints Check

- 只处理 EDHR-STATIC-013，不修改共享缺陷总表，不处理其它 EDHR/DCC 缺陷。
- 保留“冻结输出物料允许非空子集分次提交”的业务合同，不用禁止拆分提交掩盖进度问题。
- 进度必须同时受正式分配数量和冻结输出物料全集约束；缺少事件、物料明细、输出物料快照或分配来源时 fail fast，不回退到 allocation 累加或整笔事件数量。
- 多输出物料按每个物料的正式分配约束累计后取最小完成量；拆分提交与合并提交必须同结果。
- 不引入 fallback、兼容绕过、吞异常、模拟成功、默认 0 成功或测试专用字段。
- 不执行 E2E、数据库写入、服务启停、远程服务器操作、Git commit/push。

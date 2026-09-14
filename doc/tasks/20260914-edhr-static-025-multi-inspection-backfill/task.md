# EDHR-STATIC-025 Multi Inspection Backfill

## Task Goal

修复 EDHR-STATIC-025：同一动态过程检验表单承载多个 PQC 检验项目时，正式回填必须在同一任务批次内完整写入全部项目后再进入锁定/生效状态；已锁定表单仍不得被无关来源或重复写入突破。

## Milestones

- [x] 读取仓库规则、closeout 规则和 eDHR / 动态表单 / PQC 多项检验 / 正式回填锁定相关文档。
- [x] 建立 BDD 场景和静态合同验证边界。
- [x] 写出可稳定复现首项锁定阻断后续项目的 RED 静态/定向测试。
- [x] 实施最小代码修复，仅覆盖 EDHR-STATIC-025。
- [x] 运行定向 GREEN 验证与静态合同检查。
- [x] 更新验证报告，标记 ready_for_closeout；本轮已获本地提交并融合 `int_main` 授权，仍未执行未明确授权的 push。

## Expected Verification

- 新增或更新非 E2E 定向测试，覆盖同一动态表单内多项 PQC 检验先聚合再一次生效。
- 验证已 EFFECTIVE / 锁定的 FormCenter 实例仍拒绝无关来源写入。
- 运行目标静态合同测试、必要的后端/脚本定向命令、提交前端口门禁和本地 fast-forward 融合验证。
- 不执行 Playwright/E2E、数据库写入、服务启动/停止/重启、远程服务器操作或未明确授权的 git push。

## Current Status

blocked

实现、RED/GREEN、静态合同和任务分支提交准备已完成；2026-09-14 本轮用户授权“提交并融合进 int_main”，但本地融合被 `E:\IntRuoyi` 主工作区阻塞：`int_main` 当前存在其它 EDHR 015/016 任务的 staged/unmerged 改动，且 `MesProcessPoolPqcInspectionCorrectionServiceTest.java` 为 `UU` 未合并状态。按 worktree 与任务规则，本任务不得处理或提交该并行冲突，因此只能提交任务分支，暂不融合进 `int_main`，也不标记 completed。

## Design Constraints Check

- No fallback：不得循环解锁、覆盖首项、吞掉锁定错误，或放宽重放一致性。
- Fail-fast：缺少映射、模板实例、正式来源证据或锁定状态异常时必须明确失败。
- Scope：只处理 EDHR-STATIC-025；不修改共享缺陷总表，不处理 023/024 或其它缺陷。
- BDD/TDD：生产代码变更前先记录 Given / When / Then，并用 RED/GREEN 证据闭环。
- Data / E2E：本任务禁止数据库写入、E2E 和服务操作。

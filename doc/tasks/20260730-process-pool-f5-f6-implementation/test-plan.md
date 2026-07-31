# Test Plan

## Task-Level Validation

### test_case_id: TC-F5-01

- mapped_task_ids: [T1]
- mapped_acceptance_ids: [AC-01, AC-02]
- environment or setup: `IntRuoyiBackend` Maven unit test。
- steps: 运行审核副本 schema 和原始值保留测试。
- expected_result: 审核副本表和字段明细存在；生成副本不改写原始 payload、报工来源和记录本来源。
- evidence: Maven RED/GREEN 输出和执行日志。

### test_case_id: TC-F5-02

- mapped_task_ids: [T1]
- mapped_acceptance_ids: [AC-03, AC-04, AC-05]
- environment or setup: `IntRuoyiBackend` Maven unit test。
- steps: 运行上下限 clamp、元数据缺失、字段映射缺失、签名缺失和 FIFO 锁定测试。
- expected_result: 10 修正为 20，30 保持 30，50 修正为 40；缺前置条件和已分配字段拒绝。
- evidence: Maven RED/GREEN 输出和执行日志。

### test_case_id: TC-F6-01

- mapped_task_ids: [T2]
- mapped_acceptance_ids: [AC-06, AC-07]
- environment or setup: `IntRuoyiBackend` Maven unit test。
- steps: 运行 revision schema、未分配原始记录修改和字段级 diff 测试。
- expected_result: revision、diff、修改原因、新签名和服务端修改时间均保存。
- evidence: Maven RED/GREEN 输出和执行日志。

### test_case_id: TC-F6-02

- mapped_task_ids: [T2]
- mapped_acceptance_ids: [AC-08, AC-09]
- environment or setup: `IntRuoyiBackend` Maven unit test。
- steps: 运行缺签名、重复签名、缺修改原因、已分配字段和锁定状态无法确认测试。
- expected_result: 所有非法修改均拒绝，且不生成有效 revision。
- evidence: Maven RED/GREEN 输出和执行日志。

## Integration and E2E Validation

### test_case_id: TC-INT-01

- mapped_task_ids: [T1, T2]
- mapped_acceptance_ids: [AC-10]
- environment or setup: 前端和后端代码静态合同；必要时启动 branch runtime 并运行 Playwright。
- steps: 运行 `process-pool-review-copy-revision-static.spec.cjs` 和 `process-pool-review-copy-and-revision.spec.ts`。
- expected_result: 前端存在审核副本和原始记录修改入口，时间轴只读展示审核副本状态和修改历史摘要。
- evidence: Node 静态合同 PASS；真实 Playwright 写路径 E2E 前置缺口已记录，当前前端缺少 `test:e2e` script、命名 runner target 和对应 spec 文件。

## Regression Checks

- `validate_acceptance_plan.py --root E:\IntRuoyi`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- 合并后重新运行 F5/F6 定向 Maven、Node、Playwright 验证。

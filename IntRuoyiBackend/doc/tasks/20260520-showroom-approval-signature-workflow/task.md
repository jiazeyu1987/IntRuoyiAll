# 任务：展厅审批签名流程与站内信审批跳转（后端）

## Goal

在后端完成展厅审批流程增强：缺主管时直接进企宣、审批动作接入 DCC 电子签名授权与密码验签、保存展厅签名留痕、通知模板参数切换为审批跳转语义，并为审批中心/产品详情提供一致契约。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-dcc\src\main\java\cn\iocoder\yudao\module\dcc\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\resources\sql\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-approval-signature-workflow\**`

## Non-Scope

- 不修改 DCC 受控文件签名主表结构
- 不把展厅审批接入 BPM 待办模型
- 不新增 fallback 审批路径或默认成功签名

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-publicity-product-assignment\task.md`
- Status before this task: `Blocked on 2026-05-20`
- Impact: 已显式阻塞旧任务，当前任务可作为新的后端承接任务继续。

## Milestones

- [x] M1: 创建后端任务文档并收口上一同仓任务状态。
- [ ] M2: 记录 BDD 并跑后端 RED，锁定缺主管 skip 与签名必填失败。
- [ ] M3: 完成审批路线、签名留痕、验签复用与通知契约实现。
- [ ] M4: 跑后端 GREEN 并更新证据。
- [ ] M5: 运行 closeout 预览并收口任务文档。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomWorkflowApprovalTest,ShowroomAssignmentWorkflowTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-approval-signature-workflow --mode preview`

## Current Status

Completed with blocker.

## Final Verification

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomWorkflowApprovalTest,ShowroomAssignmentWorkflowTest,ShowroomApprovalSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-showroom -DskipTests compile`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-approval-signature-workflow --mode preview`

## Blockers And Impact

- Blocker: `ShowroomHttpApiIntegrationTest` 整体仍有多处与本任务外功能耦合的既有失败，集中在登录上下文、产品可见权限、批量媒体与 narrations 断言；不过本次直接相关的 6 条审批 integration 子集已通过。
- Impact: 后端主代码、showroom 核心 workflow/signature 单测和关键审批 integration 子集已通过，但完整 integration 回归需在清理既有测试噪音后继续。

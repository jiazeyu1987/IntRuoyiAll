# Task: 修复展厅讲解提交链路自动推进发布

## Goal

移除数字展厅讲解提交链路里“一次提交自动完成主管审批、高昕审批并直接发布”的实现痕迹，使讲解版本必须经过人工提交、人工审批、人工确认发布后才生效。

## Scope

- `yudao-module-showroom` 讲解提交/审批/发布后端接口与状态流转
- `yudao-module-showroom` 相关回归测试
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 讲解工作台提交/审批/发布交互与文案
- 本任务的 task 文档、执行日志、回归证据

## Non-Scope

- 不改公司/产品正文审批中心现有工作流
- 不引入 fallback、自动兜底审批或静默发布
- 不顺带处理其他 showroom schema、预览图或音频生成问题

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-cover-image-live-schema-fix\task.md`
- Status before this task: blocked
- Impact: 该任务已显式标记为独立阻塞，避免把无关 live schema 改动混入本次讲解审批链路修复。

- Previous showroom narration task: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-aliyun-nls-shared-tts\task.md`
- Status before this task: completed
- Impact: 当前讲解草稿、音频生成与持久化能力已具备，本任务仅修复“提交后自动推进发布”的审批语义偏差。

## Bug Summary

- 当前 `submitNarration` 接口在一次调用里连续执行 `submit -> supervisorApprove -> gaoxinApprove -> publish`。
- 当前前端“提交发布”按钮会直接命中上述链路，导致用户一旦点击提交，讲解版本立即进入 `PUBLISHED` 并生效。
- 该行为不符合“人工确认并审批后生效”的目标流程。

## Milestones

- [x] M1: 处理上一条未闭环任务记录，并建立本次任务文档与执行日志。
- [x] M2: 先补 RED 测试，证明讲解提交接口当前会自动推进到发布态。
- [x] M3: 拆分讲解提交、审批、发布链路，实现人工动作逐步生效。
- [x] M4: 回归验证后端接口、前端工作台与状态展示，并更新任务证据。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest,ShowroomPersistentNarrationServiceTest,ShowroomNarrationLifecycleTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-frontend.test.mjs D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-workflow-workbenches.test.mjs`
- 如前端存在本地可用验证入口，则从 `http://localhost:8081/showroom/narration-workbench` 走真实讲解工作台路径检查提交/审批/发布文案与状态
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-narration-manual-approval-flow\bug-regression-evidence.md`

## Current Status

Completed on 2026-05-20. 已完成讲解链路拆分实现，并补上“人工确认后才能提交审批”的前端/后端约束；在当前 showroom 后端代码快照下，Maven 级目标回归与前端工作台回归均已通过。

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomAssignmentWorkflowTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-frontend.test.mjs D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-workflow-workbenches.test.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-narration-manual-approval-flow\bug-regression-evidence.md`

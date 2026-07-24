# 执行日志

INFO: worktree-created -> PASS, 前后端 worktree 已创建在 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve`，分支均为 `edhr_batch_improve`。

INFO: runtime-plan -> PASS, 计划使用前端 `http://127.0.0.1:8095`、后端 `http://127.0.0.1:48095/actuator/health`；当前未启动服务。

INFO: experience-index -> matched `docs/powershell-memory.md`, `docs/worktree-memory.md`, backend-api-delivery, frontend-feature-delivery, behavior-driven-development。

GREEN: experience-preflight -> PASS, 已读取 PowerShell/worktree/API/BDD/前端技能门禁；真实 E2E 前仍需读取 `docs/login-access.md` 并运行官方登录预检。

BDD: 填写人只能处理自己的填写任务 -> Given 批次存在分配给填写人的 FILL/REWORK 任务 When 填写人进入批次详情 Then 只显示填写/返工可执行动作，审核、批准、关闭和非授权节点按钮不可用或不可见。

BDD: 审核人只读审核 -> Given 表单已提交并生成审核任务 When 审核人进入审核页 Then 表单值只读，显示审核通过/驳回入口，不显示字段编辑或批准入口。

BDD: 批准人只处理批准阶段 -> Given 审核已完成并生成批准任务 When 批准人进入批准页 Then 可只读查看证据链并批准/驳回；审核未完成时显示明确禁用原因。

BDD: 生产负责人监管但不能代签 -> Given 生产负责人进入批次详情 When 查看批次执行 Then 可见全局进度、阻塞项和人员状态，只显示授权的监管动作，不显示代填、代审、代批入口。

BDD: 无关人员隔离 -> Given 当前用户无该批次执行身份 When 进入详情 Then 页面无执行按钮，或被导航到无权限状态。

RED: `node tests\e2e\edhr-batch-pending-form-entry-static.spec.js` -> FAIL, 待处理列表未由后端 `activeWorkTaskId` / `allowedActions` / `disabledReason` 驱动，审核/批准/无关人员仍可能被按普通未打开表单推断。

GREEN: `node tests\e2e\edhr-batch-pending-form-entry-static.spec.js` -> PASS, 待处理工序列表展示角色标签、明确禁用原因，审核/批准动作进入审批页，工序名可完整换行显示。

INFO: `pnpm ts:check` -> FAIL, Node 默认堆内存不足导致 `vue-tsc` OOM，非类型错误。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS, 前端类型检查通过。

## 2026-07-06 16:29:11 +08:00 真实运行态验证与阻塞证据

GREEN: login-preflight.mjs --base-url http://127.0.0.1:8095 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/feedback/edhr-batch-execution --timeout 90000 -> PASS，测试租户填写人账号真实登录进入批次执行页。

GREEN: login-preflight.mjs --base-url http://127.0.0.1:8095 --tenant 测试租户 --username edhrmatrixapprover --password 111111 --target-path /mes/pro/feedback/edhr-batch-execution --timeout 90000 -> PASS，测试租户审核/批准账号真实登录进入批次执行页。

BLOCKER: real-data-role-e2e -> FAIL，真实页面 /mes/pro/feedback/edhr-batch-execution/detail?id=900000000463 在 aoteman 与 edhrmatrixapprover 登录下均显示 当前用户不是该节点的填写人。接口返回中当前活动任务 activeWorkTaskId=1099、activeWorkTaskType=FILL，但规则和待办绑定到不存在的历史用户 113，页面按后端能力字段正确禁用操作，无法继续真实填写/审核/批准链路。

GREEN: test-tenant-real-data-remap -> PASS，已在 tenant_id=122 测试租户内修复 eDHR 路线 900025 的真实责任人绑定，仅使用现有真实账号：FILL/REWORK 规则与活动 FILL 待办改绑 aoteman(914520)，REVIEW/ARCHIVE/CLOSE 规则与活动待办改绑 edhrmatrixapprover(914521)；备份文件为 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve\.runtime\db-backups\tenant122-edhr-orphan-user-rules-20260706-163131.sql`。

GREEN: real-data-orphan-check -> PASS，tenant_id=122 的 eDHR 工作任务责任规则孤儿用户数为 0，活动工作任务孤儿用户数为 0；当前样例 batchExecutionId=900000000463、workTaskId=1099、activeWorkTaskType=FILL 已绑定到 aoteman(914520)，前端继续按后端角色能力字段进行真实 E2E。

## 验证证据更新 - 2026-07-06 19:41:30 +08:00
- GREEN: backend-targeted-regression -> PASS, command=mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrWorkTaskFlowContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test, result=Tests run: 156, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: frontend-static-contract -> PASS, command=
ode tests\e2e\edhr-batch-pending-form-entry-static.spec.js, result=edhr batch pending form entry static contract passed.
- GREEN: frontend-ts-check -> PASS, command=pnpm ts:check.
- GREEN: real-e2e-route-900025 -> PASS, batch=900000000462, execution=778, route_process_id=922339, process=吹球囊成型, evidence=doc/tasks/20260706-edhr-batch-role-permission-flow/role-real-e2e-evidence.json.
- GREEN: test-fixture-regression-fix -> PASS, 修正单测夹具：ROLE_GROUP 候选快照用例显式责任人进入候选范围，并行后续工序用例避开重复 route_process 唯一键；不修改生产逻辑。

## 最终 worktree 验证证据 - 2026-07-06 21:05:00 +08:00
- GREEN: frontend-static-contract -> PASS, command=node tests\e2e\edhr-batch-pending-form-entry-static.spec.js.
- GREEN: frontend-ts-check -> PASS, command=$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check.
- GREEN: frontend-real-e2e-script-check -> PASS, command=node --check tests\e2e\edhr-batch-role-permission-real-flow.e2e.js.
- GREEN: runtime-ownership -> PASS, frontend=http://127.0.0.1:8095, backend=http://127.0.0.1:48095, tenant=测试租户.
- GREEN: real-e2e-route-900025-enriched -> PASS, command=node tests\e2e\edhr-batch-role-permission-real-flow.e2e.js, batch=900000000462, execution=778, routeCode=900025, routeProcessId=922339, process=吹球囊成型.
- GREEN: role-verification-filler -> PASS, 填写人关闭态无可写入口。
- GREEN: role-verification-reviewer -> PASS, 审核人只读审核，审核动作限定 REVIEW_APPROVE/REVIEW_REJECT，审核后生成独立批准任务。
- GREEN: role-verification-approver -> PASS, 批准人 APPROVE 任务 DONE，重复批准被拒绝。
- GREEN: role-verification-production-owner -> PASS, 特殊节点按钮由 allowedActions=CLOSE 驱动。
- GREEN: role-verification-unrelated -> PASS, 同租户无关人员 403 或只读无动作，跨租户不可见。

## 合并后验证证据 - 2026-07-06 21:20:00 +08:00
- GREEN: merge-frontend-fast-forward -> PASS，前端 `edhr_batch_improve` 已快进融合到 `int_main`，融合提交 `19990ce1f`。
- GREEN: merge-frontend-real-e2e-script-check -> PASS，command=node --check tests\e2e\edhr-batch-role-permission-real-flow.e2e.js。
- GREEN: merge-frontend-static-contract -> PASS，command=node tests\e2e\edhr-batch-pending-form-entry-static.spec.js，result=edhr batch pending form entry static contract passed。

## 收尾清理证据 - 2026-07-06 21:24:00 +08:00
- GREEN: worktree-runtime-stop -> PASS，已停止本次 worktree 后端 `48095` 与前端 `8095` 运行进程。
- GREEN: worktree-remove -> PASS，已删除 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve`，`git worktree list` 不再包含 `edhr_batch_improve`。
- GREEN: final-status -> PASS，本任务前端工作完成；主工作区仍保留与本任务无关的既有未提交改动，未纳入本任务提交。

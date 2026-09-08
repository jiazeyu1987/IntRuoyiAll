# Execution Log

## BDD Scenarios

- BDD: 正式电子签名禁止用户手动选择签名时间 -> Given 用户在 eDHR 正式签名命令中提交 selectedSignedAt, When 服务创建正式签名, Then 服务拒绝该请求并说明正式签名时间必须由系统生成。
- BDD: 正式电子签名必须密码重认证 -> Given 用户只持有登录会话但未提供电子签名密码, When 触发正式签名, Then 不产生正式电子签名记录。
- BDD: 非正式草稿/模拟记录不进入正式电子签名口径 -> Given 系统保存草稿或模拟签名记录, When 查询或导出正式电子签名证据, Then 只包含 passwordVerified=true 且 authenticationMethod=PASSWORD 的正式记录。
- BDD: 多人审批保留顺序证据 -> Given 原生 BPM 多级审批任务创建签名, When 生成统一签名证据, Then 记录流程实例、任务节点和节点顺序；缺失流程实例或任务 ID 时失败。
- BDD: 非流程直签业务不被 BPM 顺序门禁误伤 -> Given 业务策略直签没有流程实例, When 创建正式电子签名, Then 服务允许签名并保留业务对象证据。
- BDD: 定期电子签名合规审查记录必须具备运营证据 -> Given 创建季度或专项审查, When SOP、培训证据、负责人、计划或截止时间缺失, Then 服务拒绝创建审查批次。

## Evidence

- Worktree: `D:\IntRuoyiWorktree\20260909_epassword`
- Branch: `codex/20260909_epassword`
- Slot: `35`, frontend `8210`, backend `48210`

## TDD Log

- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest#recordSubmitSignature_withSelectedTimeFailsFast' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 当前实现未前置拒绝 `selectedSignedAt`，继续调用统一签名后因未 mock 返回触发 NPE。
- RED: `node tests/e2e/edhr-formal-signature-time-compliance-static.spec.js` -> FAIL, `ApprovalDetailPage.vue` 仍暴露 `placeholder="可选择人工签名时间"`。
- GREEN: `node tests/e2e/edhr-formal-signature-time-compliance-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-signature-time-optional-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest#recordSubmitSignature_withSelectedTimeFailsFast' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS。
- GREEN: `mvn -pl yudao-module-bpm -am '-Dtest=ApprovalSignatureRecordServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 5 tests。
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 14 tests。
- RED: 静态代码分析 -> FAIL, 前端保留已停用的人工签名时间 helper/state，历史 E2E 仍断言选择人工签名时间，Stage1 模拟记录说明仍含“正式签名记录”，草稿保存仍使用 `LOGIN_SESSION`。
- GREEN: 静态代码分析 -> PASS, 删除前端人工签名时间死代码，更新历史 E2E/静态契约，Stage1 改为非正式模拟审计记录，草稿保存改为 `DRAFT_SESSION`。
- GREEN: `node tests/e2e/edhr-tail-four-goals-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/edhr-tail-four-goals-real-flow.e2e.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_withSelectedSignatureTimePassesTimeCommandAndStoresDualTimeSignature' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 15 tests。
- GREEN: `git diff --check` -> PASS。
- BLOCKED: `pnpm ts:check` -> BLOCKED, 前端 worktree 缺少 `node_modules`，`cross-env` 无法解析。
- GREEN: `pnpm install --frozen-lockfile` -> PASS, 当前 worktree 前端依赖按锁文件补齐，`node_modules` 为 ignored 产物。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git rebase int_main` -> PASS, 解决与 trusted-time 主线改动的冲突后生成提交 `9ce630b8e`。
- GREEN: rebase 后 `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, frontend 8210, backend 48210。
- GREEN: rebase 后 `node tests/e2e/edhr-formal-signature-time-compliance-static.spec.js`; `node tests/e2e/edhr-signature-time-optional-static.spec.js`; `node tests/e2e/edhr-tail-four-goals-static.spec.js`; `node --check tests/e2e/edhr-tail-four-goals-real-flow.e2e.js` -> PASS。
- GREEN: rebase 后 `pnpm ts:check` -> PASS。
- GREEN: rebase 后 `mvn -pl yudao-module-bpm -am '-Dtest=ApprovalSignatureRecordServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 5 tests。
- GREEN: rebase 后 `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_withSelectedSignatureTimePassesTimeCommandAndStoresDualTimeSignature' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 15 tests。
- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260909-epassword-compliance-hardening --mode preview` -> BLOCKED, 主工作区 `E:\IntRuoyi` dirty，不能接收 ff-only 合并；未清理、未 stash、未覆盖无关改动。
- REGRESSION: 4.10 文档证据复核 -> FAIL, worktree 原有代码证据可覆盖 4.1-4.9、4.11、4.12，但缺少定期电子签名合规审查 SOP、证据包字段、责任人和判定规则，生产运营审查无法仅凭代码闭环。
- GREEN: 新增 `docs/security/electronic-signature-periodic-compliance-review-sop.md` -> PASS, 明确定期/变更/事件触发、角色职责、证据包、审查步骤、判定规则、记录留存和发布门禁。
- GREEN: 新增 `docs/security/security-privacy-compliance-review.md` -> PASS, 将 4.1-4.12 映射到代码/制度证据，并明确 4.10 的生产运营执行记录仍需实际归档。
- GREEN: project-experience-consolidation -> PASS, 已将附加 worktree 补丁必须使用绝对路径并交叉复核主工作区无误落的经验合并到 docs/worktree-memory.md。
- GREEN: `git rebase int_main` -> PASS, 主线推进后重新 rebase，当前 HEAD `3f7fa6caa`，分支已解除 cannot fast-forward blocker。
- GREEN: rebase 后 `python C:\Users\BJB110\.codex\skills\security-privacy-compliance-review\scripts\validate_security_privacy_compliance.py --evidence docs/security/security-privacy-compliance-review.md` -> PASS。
- GREEN: rebase 后 `git diff --check` -> PASS。
- BLOCKED: rebase 后 `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260909-epassword-compliance-hardening --mode preview` -> BLOCKED, 仅剩主工作区 `E:\IntRuoyi` dirty，分支快进阻塞已解除。

## Implementation Notes

- MES/eDHR 正式签名请求只允许系统时间；`selectedSignedAt`、`selectedTimeZone`、`selectedTimeReason` 均会触发 fail fast。
- eDHR 批执行关闭/质量拒绝签名链路同步拒绝人工时间。
- 前端正式签名弹窗移除人工时间选择控件，改为提示“正式电子签名时间由系统自动生成，不支持人工选择或回填”。
- 原生 BPM 待办审批签名缺少 `sourceTaskId` 或 `processInstanceId` 时拒绝生成统一签名证据；非流程直签业务允许无流程实例。
- Stage1 模拟记录签名模式改为 `SIMULATION_SESSION`，草稿保存记录签名模式改为 `DRAFT_SESSION`，避免与正式密码签名混淆。

## Current Blockers

- 主工作区 `E:\IntRuoyi` 存在与本任务无关的 tracked/untracked 改动，收尾脚本拒绝接收 ff-only 合并。当前分支已可快进；需要先由对应任务/人工处理主工作区脏状态，之后再从 `D:\IntRuoyiWorktree\20260909_epassword` 继续 cleanup apply 和 merge。

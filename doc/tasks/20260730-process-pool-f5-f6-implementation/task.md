# 生产一线报工工序池 F5/F6 实现与合并任务

## Task Goal

启动 2 个子 agent，分别在 2 个独立 worktree 中实现并验证生产一线报工工序池 F5/F6 验收文档内容；主 agent 负责 review，只有符合验收文档和 21 条需求门禁后才放行，并最终将 2 个 worktree 融合进 `int_main`。

## Milestones

1. 建立任务目录，读取触发规则、验收文档和现有系统切入点。
2. 在 `D:\IntRuoyiWorktree\` 下创建 F5/F6 两个独立 worktree，并登记端口槽位。
3. 启动 2 个子 agent：
   - F5 agent：审核副本上下限修正模块。
   - F6 agent：原始记录修改日志与重新电子签名模块。
4. 每个 agent 按 BDD + strict TDD 完成 RED、实现、GREEN 和任务内验证。
5. 主 agent 审查两个 worktree 的实现、测试证据和 21 条需求门禁。
6. 合并两个 worktree 到 `int_main`，解决冲突后执行整体验证。
7. 运行经验沉淀、cleanup preview/apply、提交并推送 `int_main`。

## Expected Verification

- F5 后端 TDD 命令：
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopySchemaTest#shouldCreateReviewCopyTables" test`
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldPreserveRawEventPayloadWhenGenerateReviewCopy" test`
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldClampValueToMaxWhenRawValueExceedsMax,MesProcessPoolReviewCopyServiceTest#shouldClampValueToMinWhenRawValueBelowMin,MesProcessPoolReviewCopyServiceTest#shouldKeepValueWhenRawValueWithinRange" test`
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldBlockWhenLimitMetadataMissing,MesProcessPoolReviewCopyServiceTest#shouldBlockWhenFieldMappingMissing" test`
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldRequireReviewerSignatureWhenSubmitReviewCopy,MesProcessPoolReviewCopyServiceTest#shouldRejectReviewCorrectionForAllocatedQuantityFragment" test`
- F6 后端 TDD 命令：
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest#updateUnallocatedEventCreatesFieldDiffAndSignatureLog" test`
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutNewSignature,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutChangeReason" test`
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionFifoLockTest#rejectsQuantityFieldUpdateWhenFragmentAllocated,MesProcessPoolEventRevisionFifoLockTest#rejectsUpdateWhenFifoLockStatusCannotBeConfirmed" test`
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionDiffContractTest#requiresFieldLevelDiff" test`
- F5/F6 静态和前端合同：
  - `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-review-copy-revision-static.spec.cjs`
  - `pnpm --dir IntRuoyiFronted test:e2e process-pool-review-copy-and-revision.spec.ts`
- 合并后回归：
  - `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi`
  - `scripts\preflight\branch-runtime-port-guard.ps1`
  - 与实际改动范围匹配的 Maven、Node、Playwright 验证。

## Current Status

completed

F5/F6 子 worktree 已完成主审修正并融合进 `int_main`。F5/F6 后端主链、正式写入口、前端独立 API wrapper 和时间轴只读追溯已通过合并后复验；主审额外发现并修复了时间轴审核副本一对多 JOIN 导致事件重复展开的风险。cleanup preview/apply 已完成，两个任务 worktree 已删除，端口槽位已释放。

主审复核后将真实写路径 E2E 缺口纳入当前目标继续处理；该缺口已关闭：已补齐 `test:e2e` 脚本、`process-pool-review-copy-and-revision.spec.ts`、F5 审核副本页面入口、F6 原始记录修改页面入口，并保持工序池时间轴只读。

新增真实验证已完成：本地 `int_main` 后端重新构建并以独立 runtime jar 启动到 `48081`，健康检查 `UP`；Playwright 使用真实登录、真实 F5/F6 页面和真实后端写接口通过 2 条测试；数据库核对确认 RUN3 审核副本将 `pressure=50` 按 `20~40` 夹到 `40`，原始记录修改将事件 `6` 的 `outputQuantity` 更新为 `91` 且保留新签名。

用户确认 `doc/tasks/20260729-local-scheduler-tenant-copy/source-tenant-1-full-config.json` 不提交 Git 后，已对未推送历史执行路径级过滤；复扫 `origin/int_main..HEAD` 不再包含该 214MB 文件，当前最大待推送 blob 约 2MB。随后 `git push origin int_main` 成功，`HEAD` 与 `origin/int_main` 已对齐到 `9ac4d0131dd9852808df9323b79ef0c5f60629c4`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以正式工序池、审核副本、原始记录 revision、字段级 diff 和 FIFO 锁定边界实现。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs\worktree-memory.md#Worktree 端口段与原子槽位门禁`：两个附加 worktree 创建后、启动任何运行态前必须运行 `scripts\runtime\reserve-worktree-slot.ps1`，记录 slot、前端端口、后端端口和 profile。
- `docs\worktree-memory.md#多 Worktree 批量融合门禁`：合并前必须逐个 worktree 记录 branch、HEAD、dirty 状态、验证报告和目标验证命令；dirty 内容必须先在原分支形成可追溯提交。
- `docs\worktree-memory.md#跨分支运行时契约复验门禁`：两个分支分别实现 F5/F6 后，合并进主线时必须补充跨分支调用链/时间轴合同复验，不能用各分支单测通过替代整体通过。
- `docs\worktree-memory.md#子 Agent 主工作区溢出基线门禁`：合并前必须检查主工作区是否出现子 agent 溢出文件；若属于当前任务，必须先形成独立基线提交。
- `docs\powershell-memory.md#PowerShell Maven -D 参数引号门禁`：所有 Maven `-Dtest=...` 参数必须整体加双引号。
- `docs\frontend-development.md#前端静态契约隔离门禁`：若宽前端合同或 `pnpm ts:check` 因历史问题失败，必须新增/运行 F5/F6 聚焦静态合同并记录无关 blocker，不能把无关失败当作通过证据。
- `docs\e2e-rules.md#Worktree / int_main 运行态 URL 门禁`：真实 E2E 必须使用同一 worktree slot 的前后端成对 URL；合并后才允许 `int_main` 的 `8081/48081`。

## Cleanup Keep

- `doc/tasks/20260730-process-pool-f5-f6-implementation/agent-f5-report.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/agent-f6-report.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/dev-plan.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/prd.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/request-analysis.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/task-state.json`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/test-plan.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/test-report.md`

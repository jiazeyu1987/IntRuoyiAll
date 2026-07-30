# Execution Log

## 2026-07-30 Task Start

- User intent: 启动 2 个子 agent，分别在 2 个 worktree 实现 F5/F6 文档内容；主 agent review，全部符合 21 条需求后合并进 `int_main`。
- Rule reads completed:
  - `AGENTS.md`
  - `docs\task-closeout-rules.md`
  - `docs\worktree-restrictions.md`
  - `docs\branch-runtime-ports.md`
  - `docs\powershell-encoding.md`
  - `docs\engineering\technology-stack-routing.md`
  - `supervised-complex-delivery` skill and required references
  - `milestone-tdd-delivery` skill
  - `review-fix-loop` skill and required references
- Initial git state: `int_main...origin/int_main`, clean.
- Experience gates read:
  - `docs\experience-index.md`
  - `docs\worktree-memory.md`
  - `docs\powershell-memory.md`
  - matching backend/frontend/database/e2e/local runtime sections from trigger documents
- Current system evidence:
  - F1 foundation migration exists: `IntRuoyiBackend\sql\mysql\20260730_mes_process_pool_foundation.sql`.
  - F7 FIFO migration exists: `IntRuoyiBackend\sql\mysql\20260730_mes_process_pool_fifo_allocation.sql`.
  - F8 timeline frontend/API exists: `IntRuoyiFronted\src\views\mes\pro\processpool\TimelinePage.vue` and `IntRuoyiFronted\src\api\mes\pro\processpool\index.ts`.
  - FIFO lock service exists: `MesProcessPoolFifoAllocationService#validateOriginalFieldMutationAllowed`.
- BDD: F5 审核副本上下限修正 -> Given 工序池提交事件存在原始 payload 和正式上下限元数据 / When 审核用户生成审核副本 / Then 原始值保留，修正值按上下限生成，审核签名和来源可追溯。
- BDD: F6 原始记录修改日志与重新签名 -> Given 工序池提交事件未 FIFO 分配 / When 员工提交修改原因和新电子签名修改原始记录 / Then 保存新版本、字段级 diff、修改原因、签名和服务端修改时间。

## 2026-07-30 F6 Agent Implementation

- Worktree: `D:\IntRuoyiWorktree\20260730-process-pool-f6-event-revision`
- Branch: `codex/20260730-process-pool-f6-event-revision`
- BDD: F6 未分配原始记录修改 -> Given 工序池提交事件未 FIFO 分配 / When 员工提交修改后 payload、修改原因、新电子签名和字段级变化 / Then 保存 revision 主表、字段级 diff、修改原因、新签名、修改人和服务端修改时间，并更新当前事件 `raw_payload`。
- BDD: F6 签名和原因阻塞 -> Given 工序池提交事件存在 / When 修改请求缺少原因、空白原因、复用原提交签名或使用重复签名 / Then 拒绝修改，不写 revision，不更新当前事件。
- BDD: F6 FIFO 锁定阻塞 -> Given 修改字段影响数量片段、质量或可分配状态 / When 来源片段已分配或锁定状态无法确认 / Then 调用 F7 `MesProcessPoolFifoAllocationService#validateOriginalFieldMutationAllowed` 并拒绝修改。
- BDD: F6 时间轴修改历史只读摘要 -> Given 工序池提交事件存在有效 revision / When 查询时间轴或详情 / Then 展示原始记录修改次数摘要，不暴露修改写入口。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest#updateUnallocatedEventCreatesFieldDiffAndSignatureLog" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: F6 schema、DO、mapper、service、BO 尚不存在，`yudao-module-mes` testCompile 报缺少 revision 相关类型。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest#updateUnallocatedEventCreatesFieldDiffAndSignatureLog" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutNewSignature,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutChangeReason,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenRevisionSignatureAlreadyUsed" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionFifoLockTest#rejectsQuantityFieldUpdateWhenFragmentAllocated,MesProcessPoolEventRevisionFifoLockTest#rejectsUpdateWhenFifoLockStatusCannotBeConfirmed" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionDiffContractTest#requiresFieldLevelDiff,ProcessPoolTimelineRevisionSummaryTest#timelineMapperReadsRevisionSummaryWithoutWriteActions" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolEventRevisionDiffContractTest,ProcessPoolTimelineRevisionSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.
- Current status: F6 后端 TDD 主链完成；待提交前 guard、diff check、commit。

## 2026-07-30 F6 Review Fix: Formal Write Entry

- User intent: 主审查不放行，要求在 F6 worktree 补正式后端写入口、前端 API wrapper、controller/static 契约测试，保持 timeline 只读边界。
- BDD: F6 正式写入口 -> Given 前端提交原始记录修改请求 / When 调用独立 event-revision API / Then 后端使用 `POST /mes/pro/process-pool/event-revision/update-original`、专用写权限 `mes:pro-process-pool:event-revision:update` 并调用 revision service。
- BDD: F6 前端 API 边界 -> Given 前端需要提交原始记录修改 / When 调用 API wrapper / Then 使用 `eventRevision.ts` 独立 POST wrapper，timeline API 不暴露写操作。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: F6 controller/VO 缺失，静态契约报找不到 `MesProProcessPoolEventRevisionController.java` 和 `ProcessPoolEventRevisionUpdateReqVO.java`。
- RED: `node tests\e2e\process-pool-event-revision-api-static.spec.js` -> FAIL, expected reason: 前端 `src/api/mes/pro/processpool/eventRevision.ts` 缺失。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `node tests\e2e\process-pool-event-revision-api-static.spec.js` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolEventRevisionDiffContractTest,ProcessPoolTimelineRevisionSummaryTest,MesProcessPoolEventRevisionControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests.
- Current status: F6 review fix 实现和验证完成；待提交前 guard、diff check、commit。

## 2026-07-30 F6 Review Fix: Service Validation Hardening

- User intent: 主审查仍不放行，要求补 service 层 fail-fast 门禁；controller VO 校验不能替代直接调用 service 时的正式前置条件。
- BDD: F6 service 直接调用签名快照门禁 -> Given 调用方直接调用 revision service / When `revisionSignatureSnapshot` 缺失 / Then 拒绝修改，不 insert revision，不 update event。
- BDD: F6 service 直接调用 payload JSON 门禁 -> Given 工序池事件存在或请求提交 afterPayload / When event `rawPayload` 缺失、event `rawPayload` 非法 JSON 或 `afterPayload` 非法 JSON / Then 业务层 fail fast，不能依赖 DB JSON 列报错，不 insert revision，不 update event。
- BDD: F6 字段级 diff 显式数量影响门禁 -> Given changedFields 包含字段级 diff / When `affectsQuantityFragment` 为空 / Then 拒绝修改，不允许 null 被当作 false。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutRevisionSignatureSnapshot,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenEventRawPayloadMissing,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenEventRawPayloadIsInvalidJson,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenAfterPayloadIsInvalidJson,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenAffectsQuantityFragmentIsNull" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: service 层缺少签名快照、rawPayload/afterPayload 合法 JSON、`affectsQuantityFragment` 非空集中校验，5 个新增门禁测试失败。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutRevisionSignatureSnapshot,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenEventRawPayloadMissing,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenEventRawPayloadIsInvalidJson,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenAfterPayloadIsInvalidJson,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenAffectsQuantityFragmentIsNull" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolEventRevisionDiffContractTest,ProcessPoolTimelineRevisionSummaryTest,MesProcessPoolEventRevisionControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 16 tests.
- GREEN: `node tests\e2e\process-pool-event-revision-api-static.spec.js` -> PASS.
- GREEN: `git diff --check` -> PASS.
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, branch runtime ports frontend 8098 / backend 48098.
- Experience consolidation: 已按 `project-experience-consolidation` 检查 `docs/*memory*.md` 与 `docs/experience-index.md`；现有长期经验文档仅匹配 worktree/PowerShell，不适合沉淀本次 service 层业务门禁经验，未获授权不新建长期经验文档。
- Commit: `fix: harden process pool event revision validation` 已创建本地提交。
- Push: `git push origin codex/20260730-process-pool-f6-event-revision` -> first attempt FAIL, blocker: `fatal: unable to access 'https://github.com/jiazeyu1987/IntRuoyiAll.git/': Recv failure: Connection was reset`; retry PASS, branch updated on origin.
- Current status: F6 service validation hardening 实现、聚焦验证、diff check、端口 guard、本地提交与 push 已完成；未合并 `int_main`。

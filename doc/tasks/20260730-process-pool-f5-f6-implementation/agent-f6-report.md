# F6 Agent Report: 原始记录修改日志与重新电子签名

## Scope

- Worktree: `D:\IntRuoyiWorktree\20260730-process-pool-f6-event-revision`
- Branch: `codex/20260730-process-pool-f6-event-revision`
- Module: F6 原始记录修改日志与重新电子签名
- Boundary: 只实现原始记录 revision、字段级 diff、重新电子签名、FIFO 锁定阻塞、时间轴只读修改历史摘要；不实现审核副本上下限修正，不 clamp 原始值，不写审核副本表。

## BDD

- BDD: 未 FIFO 分配的原始记录允许带原因和新签名修改 -> Given 工序池提交事件未被 FIFO 分配 / When 员工提交修改后 payload、修改原因、新电子签名和字段级变化 / Then 系统写入 revision 主表、字段级 diff 表、更新当前事件 `raw_payload`，并保留服务端修改时间。
- BDD: 缺少重新电子签名或修改原因必须阻塞 -> Given 工序池提交事件存在 / When 修改请求缺少原因、空白原因、复用原提交签名或使用重复签名 / Then 拒绝修改，不写 revision，不更新当前事件。
- BDD: FIFO 锁定字段必须先确认可修改 -> Given 修改字段影响数量片段、质量或可分配状态 / When 来源数量片段已分配或锁定状态无法确认 / Then 调用 F7 锁定服务并拒绝修改，不默认未锁定。
- BDD: 时间轴只读展示修改历史 -> Given 工序池提交事件存在有效 revision / When 查看时间轴或详情 / Then 只展示原始记录修改次数摘要，不暴露修改写操作。
- BDD: F6 原始记录修改正式入口 -> Given 前端提交原始记录修改请求 / When 调用独立 F6 API / Then 后端通过 `/mes/pro/process-pool/event-revision/update-original` 写入口校验请求、使用专用写权限并调用 revision service，timeline API 保持只读。
- BDD: F6 service 直接调用仍必须 fail fast -> Given 调用方绕过 controller 直接调用 revision service / When 缺少签名快照、原始 payload 缺失或非法、修改后 payload 非法、字段级 diff 的 `affectsQuantityFragment` 为空 / Then service 层拒绝请求，不写 revision，不更新当前事件。

## RED Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest#updateUnallocatedEventCreatesFieldDiffAndSignatureLog" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: F6 schema、DO、mapper、service、BO 尚不存在，`yudao-module-mes` testCompile 报缺少 `MesProProcessPoolEventRevisionDO`、`MesProProcessPoolEventRevisionDiffDO`、`MesProProcessPoolEventRevisionMapper`、`MesProProcessPoolEventRevisionDiffMapper`、`MesProcessPoolEventRevisionService`、`MesProcessPoolEventRevisionUpdateReqBO`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: F6 controller/VO 不存在，静态契约报 `MesProProcessPoolEventRevisionController.java` 和 `ProcessPoolEventRevisionUpdateReqVO.java` 缺失。
- RED: `node tests\e2e\process-pool-event-revision-api-static.spec.js` -> FAIL, expected reason: 前端 `src/api/mes/pro/processpool/eventRevision.ts` 不存在。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutRevisionSignatureSnapshot,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenEventRawPayloadMissing,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenEventRawPayloadIsInvalidJson,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenAfterPayloadIsInvalidJson,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenAffectsQuantityFragmentIsNull" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: service 层尚未集中校验签名快照、原始 payload 合法 JSON、修改后 payload 合法 JSON 和 `affectsQuantityFragment` 非空，5 个新增门禁测试失败。

## GREEN Evidence

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest#updateUnallocatedEventCreatesFieldDiffAndSignatureLog" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutNewSignature,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutChangeReason,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenRevisionSignatureAlreadyUsed" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionFifoLockTest#rejectsQuantityFieldUpdateWhenFragmentAllocated,MesProcessPoolEventRevisionFifoLockTest#rejectsUpdateWhenFifoLockStatusCannotBeConfirmed" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionDiffContractTest#requiresFieldLevelDiff,ProcessPoolTimelineRevisionSummaryTest#timelineMapperReadsRevisionSummaryWithoutWriteActions" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolEventRevisionDiffContractTest,ProcessPoolTimelineRevisionSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `node tests\e2e\process-pool-event-revision-api-static.spec.js` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolEventRevisionDiffContractTest,ProcessPoolTimelineRevisionSummaryTest,MesProcessPoolEventRevisionControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutRevisionSignatureSnapshot,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenEventRawPayloadMissing,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenEventRawPayloadIsInvalidJson,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenAfterPayloadIsInvalidJson,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenAffectsQuantityFragmentIsNull" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolEventRevisionDiffContractTest,ProcessPoolTimelineRevisionSummaryTest,MesProcessPoolEventRevisionControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 16 tests.
- GREEN: `git diff --check` -> PASS.
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, branch runtime ports frontend 8098 / backend 48098.

## Changed Paths

- `IntRuoyiBackend/sql/mysql/20260730_mes_process_pool_event_revision.sql`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProProcessPoolEventRevisionDO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProProcessPoolEventRevisionDiffDO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolEventRevisionMapper.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolEventRevisionDiffMapper.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionService.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionUpdateReqBO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionFieldChangeBO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/MesProProcessPoolEventRevisionController.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolEventRevisionUpdateReqVO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProcessPoolEventRevisionSchemaTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/MesProcessPoolEventRevisionControllerContractTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionFifoLockTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionDiffContractTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineRevisionSummaryTest.java`
- `IntRuoyiFronted/src/api/mes/pro/processpool/eventRevision.ts`
- `IntRuoyiFronted/tests/e2e/process-pool-event-revision-api-static.spec.js`

## Requirement Gate Notes

- R14/R15/R16: 原始记录修改写 revision 主表和字段级 diff；缺原因、缺新签名、复用原签名、重复签名均阻塞；修改成功后更新当前事件 `raw_payload`，原始修改历史保留。
- AC-07/AC-08: service 层要求重新电子签名快照非空且为合法 JSON；原始事件 `rawPayload` 和 `afterPayload` 必须在业务层先通过合法 JSON 校验，不能依赖 DB JSON 列报错；校验失败不插入 revision、不更新 event。
- R18/R19: 影响数量片段的字段必须提供来源片段和原始字段枚举，并调用 `MesProcessPoolFifoAllocationService#validateOriginalFieldMutationAllowed`；已分配或无法确认锁定状态拒绝。
- R21: 时间轴只读展示修改历史摘要；写操作放在独立 event-revision 后端 controller 和前端 API wrapper，不复用 timeline API。
- 字段级 diff 合同：每个 changed field 必须显式给出 `affectsQuantityFragment`，不允许 null 被当成 false。
- 正式写入口：`POST /mes/pro/process-pool/event-revision/update-original`，权限 `mes:pro-process-pool:event-revision:update`，不复用 query/read 权限。
- 审核副本上下限修正未触碰；F6 不做 clamp，不写审核副本表。

## Remaining Merge-Time Verification

- 主 agent 合并 F5/F6 后需要跑完整 MES 聚合测试，确认 F5 审核副本表与 F6 revision 表的 migration 顺序、错误码区间和 mapper 扫描无冲突。
- 本 F6 已补独立前端 API wrapper，但未新增页面按钮/表单；主 agent 合并后如接入真实页面，应按 Playwright 真实用户路径复验电子签名提交链路。

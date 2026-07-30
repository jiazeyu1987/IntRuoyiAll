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

## RED Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest#updateUnallocatedEventCreatesFieldDiffAndSignatureLog" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: F6 schema、DO、mapper、service、BO 尚不存在，`yudao-module-mes` testCompile 报缺少 `MesProProcessPoolEventRevisionDO`、`MesProProcessPoolEventRevisionDiffDO`、`MesProProcessPoolEventRevisionMapper`、`MesProProcessPoolEventRevisionDiffMapper`、`MesProcessPoolEventRevisionService`、`MesProcessPoolEventRevisionUpdateReqBO`。

## GREEN Evidence

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest#updateUnallocatedEventCreatesFieldDiffAndSignatureLog" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutNewSignature,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutChangeReason,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWhenRevisionSignatureAlreadyUsed" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionFifoLockTest#rejectsQuantityFieldUpdateWhenFragmentAllocated,MesProcessPoolEventRevisionFifoLockTest#rejectsUpdateWhenFifoLockStatusCannotBeConfirmed" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionDiffContractTest#requiresFieldLevelDiff,ProcessPoolTimelineRevisionSummaryTest#timelineMapperReadsRevisionSummaryWithoutWriteActions" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolEventRevisionDiffContractTest,ProcessPoolTimelineRevisionSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.

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
- `IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProcessPoolEventRevisionSchemaTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionFifoLockTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionDiffContractTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineRevisionSummaryTest.java`

## Requirement Gate Notes

- R14/R15/R16: 原始记录修改写 revision 主表和字段级 diff；缺原因、缺新签名、复用原签名、重复签名均阻塞；修改成功后更新当前事件 `raw_payload`，原始修改历史保留。
- R18/R19: 影响数量片段的字段必须提供来源片段和原始字段枚举，并调用 `MesProcessPoolFifoAllocationService#validateOriginalFieldMutationAllowed`；已分配或无法确认锁定状态拒绝。
- R21: 时间轴只读展示修改历史摘要，未新增任何时间轴写入口。
- 审核副本上下限修正未触碰；F6 不做 clamp，不写审核副本表。

## Remaining Merge-Time Verification

- 主 agent 合并 F5/F6 后需要跑完整 MES 聚合测试，确认 F5 审核副本表与 F6 revision 表的 migration 顺序、错误码区间和 mapper 扫描无冲突。
- 若后续新增真实前端入口，应按 Playwright 真实页面路径复验修改历史只读展示；本 F6 未扩展前端写入口。

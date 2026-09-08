package cn.iocoder.yudao.module.mes.service.pro.simulation.stage2_5;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesStage2_5ReceiptHandoffContractTest {

    private static final Path IMPLEMENTATION = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage2_5/"
                    + "MesStage2_5BackfillBatchExecutionSimulationServiceImpl.java");

    @Test
    void stage2_5DoesNotInvokeReleaseDossierWriters() throws Exception {
        String source = Files.readString(IMPLEMENTATION, StandardCharsets.UTF_8);

        assertFalse(source.contains("MesPqcReleaseDossierPort"));
        assertFalse(source.contains("MesPqcReleaseDossierPlan"));
        assertFalse(source.contains("MesPqcReleaseDossierWriteResult"));
        assertFalse(source.contains("dossierPort"));
        assertTrue(source.contains("completionReceiptMapper"));
        assertTrue(source.contains("buildBackfillReceipt"));
        assertTrue(source.contains("MesProcessPoolActiveOrderDO activeOrder = template"));
        assertTrue(source.contains("STAGE2_5_STAGE1_SOURCE_REQUIRED"));
        assertFalse(source.contains("simulateActiveOrderCompletion(validated.getActorUserId(), activeOrder.getId(),"));
        assertTrue(source.contains("activeOrderCompletionService.complete(validated.getActorUserId(),"));
        assertFalse(source.contains("MesProcessPoolActiveOrderDO activeOrder = createFixture"));
        assertFalse(source.contains("setCompletionBackfillReceipt(receipt)"));
        assertTrue(source.contains("setSourceContextHash(receipt.getSourceSnapshotHash())"));
        assertFalse(source.contains("setSourceContextHash(receipt.getSourceContextHash())"));
        assertTrue(source.contains("backfillReceipt.getBatchRecordId()"));
        assertTrue(source.contains("backfillReceipt.getProcessInspectionId()"));
    }

    @Test
    void erpSourceFidMustBeLengthBoundedAndSeparateFromAuditMarker() throws Exception {
        String source = Files.readString(IMPLEMENTATION, StandardCharsets.UTF_8);

        assertTrue(source.contains("setSourceFid(sourceFid(runId, actorUserId))"));
        assertTrue(source.contains("setSourceFid(sourceEntryFid(runId, actorUserId, row.getId()))"));
        assertTrue(source.contains("private String sourceFid(String runId, Long actorUserId)"));
        assertTrue(source.contains("private String sourceEntryFid(String runId, Long actorUserId, Long sourceItemId)"));
        assertTrue(source.contains("\"S25-\" + DigestUtil.sha256Hex(runId + \"|\" + actorUserId).substring(0, 60)"));
        assertTrue(source.contains("\"S25E-\" + DigestUtil.sha256Hex(runId + \"|\" + actorUserId + \"|\" + sourceItemId).substring(0, 59)"));
        assertTrue(source.contains(".setId(IdUtil.getSnowflake().nextId())"));
        assertTrue(source.contains("MesProcessPoolActiveOrderPickListBindingItemDO.builder()")
                && source.contains(".id(IdUtil.getSnowflake().nextId())"));
        assertFalse(source.contains("setSourceFid(marker(runId, actorUserId)"));
    }

    @Test
    void stage1SourceSnapshotHashMustBeCanonicalSha256() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
                        + "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("DigestUtil.sha256Hex(JsonUtils.toJsonString(value))"));
        assertFalse(source.contains("Integer.toHexString(JsonUtils.toJsonString(value).hashCode())"));
    }

    @Test
    void stage1MustMaterializeFormalProductIssueForStage25() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
                        + "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("createFormalProductIssue"));
        assertTrue(source.contains("productIssueMapper.selectListByWorkOrderIdForUpdate"));
        assertTrue(source.contains("productIssueLineMapper.deleteByIssueId"));
        assertTrue(source.contains("productIssueDetailMapper.deleteByIssueId"));
        assertTrue(source.contains("MesWmProductIssueStatusEnum.FINISHED"));
        assertTrue(source.contains("batchMapper.insert(batch)"));
        assertTrue(source.contains("materialStockMapper.insert(stock)"));
    }

    @Test
    void repeatedStage1MustRestampExistingPickListBindingsForStage25() throws Exception {
        String stage1 = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
                        + "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"), StandardCharsets.UTF_8);

        assertTrue(stage1.contains("restampExistingPickListBindings(existing, command)"),
                "Stage1 反复点击时必须把已有领料绑定同步到本次 simulationRunId，避免 Stage2.5 查不到绑定。");
        assertTrue(stage1.contains("bindingMapper.updateById(binding)"),
                "已有领料绑定的 simulated、simulationStage、simulationRunId 必须持久化。");
        assertTrue(stage1.contains("restampExistingPickListBindingItems(binding.getId(), sourceItems, command)"),
                "绑定明细也必须同步本次 simulationRunId，保证明细快照与头表一致。");
        assertTrue(stage1.contains("bindingItemMapper.updateById(item)"),
                "已有领料绑定明细的模拟标识必须持久化。");
    }

    @Test
    void repeatedStage1MustNormalizeExistingPickListSnapshotHashForStage25() throws Exception {
        String stage1 = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
                        + "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"), StandardCharsets.UTF_8);

        assertTrue(stage1.contains("MesFormalProductionPickListSourceResolver")
                        && stage1.contains(".snapshotHash(header, sourceItems)"),
                "Stage1 复跑已有领料绑定时必须重算正式头表+明细快照 hash，不能保留旧的仅明细 hash。");
        assertTrue(stage1.contains("orderedFormalPickListItems(sourceItems)"),
                "Stage1 复跑已有领料绑定时必须按正式 resolver 的明细排序口径重算头表快照。");
        assertTrue(stage1.contains(".setItemSnapshotHash(MesFormalProductionPickListSourceResolver.itemSnapshotHash(source))"),
                "Stage1 复跑已有领料绑定时必须同步明细 itemSnapshotHash 到正式 resolver 口径。");
        assertTrue(stage1.contains(".setSourceEntryId(source.getSourceEntryId())")
                        && stage1.contains(".setSourceLineKey(source.getSourceLineKey())")
                        && stage1.contains(".setProductionOrderNo(source.getProductionOrderNo())"),
                "绑定明细的正式来源字段必须与当前领料单行一致，避免 Stage2.5 同源校验误判来源改变。");
    }

    @Test
    void stage1MustUseFormalResolverItemSnapshotHashWhenCreatingPickListBindings() throws Exception {
        String stage1 = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
                        + "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"), StandardCharsets.UTF_8);

        assertTrue(stage1.contains(".itemSnapshotHash(MesFormalProductionPickListSourceResolver.itemSnapshotHash(item))"),
                "Stage1 新建领料绑定明细必须使用正式 resolver itemSnapshotHash，避免 Stage2.5 校验误判。");
        assertFalse(stage1.contains(".itemSnapshotHash(hash(item))"),
                "Stage1 领料绑定明细不能再使用旧 Json hash 口径。");
        assertTrue(stage1.contains("orderedFormalPickListItems(items)")
                        && stage1.contains("Comparator.comparing(ErpKingdeeProductionPickListItemDO::getSourceEntryId)"),
                "Stage1 新建领料绑定头表快照必须按正式 resolver 的 sourceEntryId + id 顺序计算。");
    }

    @Test
    void stage2_5MustAcceptMultipleFinishedFormalProductIssues() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/"
                        + "MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("FormalProductIssues formalProductIssues = lockedFormalProductIssues(activeOrder)"),
                "Stage2.5 必须把正式领料出库单作为集合读取，不能写死一张。");
        assertFalse(source.contains("finished.size() != 1"),
                "Stage2.5 不能因多张已完成领料出库单而失败。");
        assertTrue(source.contains("finished.isEmpty()"),
                "Stage2.5 没有任何已完成领料出库单时仍必须整体失败。");
        assertTrue(source.contains("formalProductIssues.issues().stream().map(MesWmProductIssueDO::getId)")
                        && source.contains("formalProductIssues.allDetails().stream().map(MesWmProductIssueDetailDO::getId)"),
                "Stage2.5 必须把每张领料出库单及其明细全部纳入批记录来源。");
        assertTrue(source.contains("seed.put(\"formalProductIssues\", formalProductIssues.issues())")
                        && source.contains("seed.put(\"formalProductIssueDetails\", formalProductIssues.detailsByIssue())"),
                "Stage2.5 来源哈希必须覆盖领料出库单集合，不能只覆盖单张单据。");
    }

    @Test
    void downstreamStagesMustCarryTheExistingBatchIdentity() throws Exception {
        String stage4 = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage4/"
                        + "MesStage4DossierUploadSimulationServiceImpl.java"), StandardCharsets.UTF_8);
        String stage5 = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8);
        assertTrue(stage4.contains("requireStage2_5Batch(command)"));
        assertFalse(stage4.contains("MesProEdhrBatchExecutionDO batch = createFixture"));
        assertTrue(stage5.contains("loadStage4Fixture(actorUserId, batchExecutionId, stage4RunId)"));
        assertFalse(stage5.contains("Fixture fixture = createFixture(actorUserId, runId)"));
    }

    @Test
    void cleanupMustBeScopedToTheCurrentSimulationActor() throws Exception {
        String stage1 = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
                        + "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"), StandardCharsets.UTF_8);
        String stage25 = Files.readString(IMPLEMENTATION, StandardCharsets.UTF_8);
        assertTrue(stage1.contains("like(MesProWorkOrderDO::getRemark, \"][actorUserId=\" + actorUserId + \"]\")"));
        assertTrue(stage25.contains("cleanupOwnedBatches(validated.getActorUserId())"));
        assertTrue(stage25.contains("like(MesProEdhrBatchExecutionDO::getRemark, \"][actorUserId=\" + actorUserId + \"]\")"));
    }

    @Test
    void cleanupMustSkipNonCanonicalBatchMarkers() throws Exception {
        String stage25 = Files.readString(IMPLEMENTATION, StandardCharsets.UTF_8);

        assertTrue(stage25.contains("String runId = tryRunIdFromMarker(batch.getRemark(), actorUserId)"));
        assertTrue(stage25.contains("if (runId == null)"));
        assertTrue(stage25.contains("continue;"));
        assertFalse(stage25.contains("runIdFromMarker(batch.getRemark(), actorUserId);"));
    }
}

package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class MesTeamLeaderActiveOrderReleaseApplicationServiceImpl
        implements MesTeamLeaderActiveOrderReleaseApplicationService {

    private final MesTeamLeaderActiveOrderReleaseGenerationService generationService;
    private final MesTeamLeaderActiveOrderCompletionService completionService;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;
    private final MesProEdhrBatchExecutionMapper batchMapper;
    private final MesProEdhrBatchExecutionOriginMapper originMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesTeamLeaderActiveOrderCompletionBatchExecutionService completionBatchExecutionService;

    @Resource
    private GxpAuditService gxpAuditService;

    public MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(
            MesTeamLeaderActiveOrderReleaseGenerationService generationService,
            MesTeamLeaderActiveOrderCompletionService completionService,
            MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper,
            MesProEdhrBatchExecutionMapper batchMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesTeamLeaderActiveOrderCompletionBatchExecutionService completionBatchExecutionService,
            MesProEdhrBatchExecutionOriginMapper originMapper) {
        this.generationService = generationService;
        this.completionService = completionService;
        this.receiptMapper = receiptMapper;
        this.batchMapper = batchMapper;
        this.applicationMapper = applicationMapper;
        this.completionBatchExecutionService = completionBatchExecutionService;
        this.originMapper = originMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseApplicationResult applyGenerated(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        gxpAuditService.acquireLedgerLock();
        String key = MesReleaseFlowIdempotency.requireKey(command == null ? null : command.getIdempotencyKey());
        command.setIdempotencyKey(key);
        var existing = generationService.replayExisting(leaderUserId, command);
        if (existing != null && existing.getBatchExecutionId() != null) return existing;
        Long batchExecutionId = requirePersistedP2BatchExecutionId(leaderUserId, command);
        if (existing != null) {
            return bindExistingWithAudit(command.getActiveOrderId(), existing, batchExecutionId, "applyGenerated");
        }
        var generated = generationService.generate(leaderUserId, command);
        MesTeamLeaderActiveOrderReleaseApplicationResult bound = bindBatchExecution(generated, batchExecutionId);
        appendReleaseApplyGxpAudit(command.getActiveOrderId(), bound, null, "applyGenerated");
        return bound;
    }

    private Long requirePersistedP2BatchExecutionId(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        var receipt = receiptMapper.selectByActiveOrderIdForUpdate(command.getActiveOrderId());
        if (receipt == null || !Objects.equals(receipt.getLeaderUserId(), leaderUserId)
                || !Objects.equals(receipt.getActiveOrderId(), command.getActiveOrderId())
                || !"BACKFILL_SUCCEEDED".equals(receipt.getReceiptStatus())
                || !"SUCCESS".equals(receipt.getBatchRecordStatus())
                || !"SUCCESS".equals(receipt.getProcessInspectionStatus())
                || receipt.getBatchRecordId() == null || receipt.getProcessInspectionId() == null
                || receipt.getWorkOrderId() == null || receipt.getBatchCode() == null || receipt.getRouteId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    command.getActiveOrderId(), "请先完成P2，生成批记录和过程检验记录");
        }
        var batchIds = originMapper.selectListByTraceFilter(command.getActiveOrderId(), receipt.getWorkOrderId(),
                        null, "ACTIVE_ORDER_COMPLETION").stream()
                .filter(origin -> Objects.equals(origin.getCompletionBackfillReceiptId(), receipt.getId()))
                .map(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO::getBatchExecutionId)
                .filter(Objects::nonNull).distinct().toList();
        if (batchIds.size() != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    command.getActiveOrderId(), "本轮P2回执缺少唯一批次来源");
        }
        var batch = batchMapper.selectById(batchIds.get(0));
        if (batch == null || batch.getId() == null
                || !Objects.equals(batch.getWorkOrderId(), receipt.getWorkOrderId())
                || !Objects.equals(batch.getBatchCode(), receipt.getBatchCode())
                || !Objects.equals(batch.getRouteId(), receipt.getRouteId())
                || !Objects.equals(batch.getRouteVersionId(), receipt.getRouteVersionId())
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionMapper.BATCH_STATUS_VOIDED)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    command.getActiveOrderId(), "P2生成的有效批次不存在");
        }
        return batch.getId();
    }

    private MesTeamLeaderActiveOrderReleaseApplicationResult bindBatchExecution(
            MesTeamLeaderActiveOrderReleaseApplicationResult application, Long batchExecutionId) {
        if (application.getBatchExecutionId() != null) {
            if (!Objects.equals(application.getBatchExecutionId(), batchExecutionId)) {
                throw new IllegalStateException("P3 release application batch execution mismatch");
            }
            return application;
        }
        if (applicationMapper.bindP3BatchExecution(
                application.getApplicationId(), application.getVersion(), batchExecutionId) != 1) {
            throw new IllegalStateException("P3 release application batch execution binding failed");
        }
        return application.setBatchExecutionId(batchExecutionId)
                .setVersion(application.getVersion() + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseApplicationResult apply(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        gxpAuditService.acquireLedgerLock();
        String releaseIdempotencyKey = MesReleaseFlowIdempotency.requireKey(
                command == null ? null : command.getIdempotencyKey());
        command.setIdempotencyKey(releaseIdempotencyKey);
        MesTeamLeaderActiveOrderReleaseApplicationResult existing =
                generationService.replayExisting(leaderUserId, command);
        if (existing != null) {
            if (existing.getBatchExecutionId() != null) {
                return existing;
            }
            Long batchExecutionId = requirePersistedP2BatchExecutionId(leaderUserId, command);
            return bindExistingWithAudit(command.getActiveOrderId(), existing, batchExecutionId, "apply");
        }
        MesTeamLeaderActiveOrderCompletionResult completion = completionService.completeForRelease(
                leaderUserId, command.getActiveOrderId(), releaseIdempotencyKey, command.getConfirmNoReplenishmentInfo());
        Long batchExecutionId = completionBatchExecutionService.openOrCreate(
                leaderUserId, command.getActiveOrderId(), completion.getCompletionReceiptId(), releaseIdempotencyKey);
        MesTeamLeaderActiveOrderReleaseApplicationResult generated = generationService.generate(leaderUserId, command);
        MesTeamLeaderActiveOrderReleaseApplicationResult bound = bindBatchExecution(generated, batchExecutionId);
        appendReleaseApplyGxpAudit(command.getActiveOrderId(), bound);
        return bound;
    }

    private void appendReleaseApplyGxpAudit(Long activeOrderId,
                                            MesTeamLeaderActiveOrderReleaseApplicationResult application) {
        appendReleaseApplyGxpAudit(activeOrderId, application, null, "apply");
    }

    private MesTeamLeaderActiveOrderReleaseApplicationResult bindExistingWithAudit(
            Long activeOrderId, MesTeamLeaderActiveOrderReleaseApplicationResult existing,
            Long batchExecutionId, String sourceMethod) {
        // Serialize before binding: bindBatchExecution mutates the same receipt instance.
        GxpAuditStateEnvelope before = GxpAuditStateEnvelope.builder()
                .state(existing.getStatus())
                .objectVersion(String.valueOf(existing.getVersion()))
                .canonicalJson(JsonUtils.toJsonString(releaseApplicationState(activeOrderId, existing)))
                .build();
        MesTeamLeaderActiveOrderReleaseApplicationResult bound = bindBatchExecution(existing, batchExecutionId);
        appendReleaseApplyGxpAudit(activeOrderId, bound, before, sourceMethod);
        return bound;
    }

    private Map<String, Object> releaseApplicationState(Long activeOrderId,
            MesTeamLeaderActiveOrderReleaseApplicationResult application) {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("applicationId", application.getApplicationId());
        state.put("activeOrderId", activeOrderId);
        state.put("batchExecutionId", application.getBatchExecutionId());
        state.put("pqcReleaseWorkTaskId", application.getPqcReleaseWorkTaskId());
        state.put("status", application.getStatus());
        state.put("version", application.getVersion());
        state.put("sourceSnapshotHash", application.getSourceSnapshotHash());
        state.put("applicationReceipt", application);
        return state;
    }

    private void appendReleaseApplyGxpAudit(Long activeOrderId,
            MesTeamLeaderActiveOrderReleaseApplicationResult application,
            GxpAuditStateEnvelope bindingBefore, String sourceMethod) {
        if (application == null || application.getApplicationId() == null) {
            throw new IllegalStateException("P3 release application receipt is incomplete");
        }
        Map<String, Object> before = new java.util.LinkedHashMap<>();
        before.put("activeOrderId", activeOrderId);
        before.put("state", "ABSENT");
        Map<String, Object> after = releaseApplicationState(activeOrderId, application);
        boolean binding = bindingBefore != null;
        String auditIdentity = binding
                ? "PQC_RELEASE_BIND_BATCH:" + application.getApplicationId() + ":" + application.getVersion()
                : "PQC_RELEASE_APPLY:" + application.getApplicationId();
        List<GxpAuditRelation> links = new java.util.ArrayList<>();
        links.add(new GxpAuditRelation("SUBJECT", "ACTIVE_ORDER",
                String.valueOf(activeOrderId), null, null));
        links.add(new GxpAuditRelation("SOURCE", "RELEASE_APPLICATION",
                String.valueOf(application.getApplicationId()), String.valueOf(application.getVersion()),
                application.getSourceSnapshotHash()));
        if (application.getBatchExecutionId() != null) {
            links.add(new GxpAuditRelation("SOURCE", "BATCH_EXECUTION",
                    String.valueOf(application.getBatchExecutionId()), null, null));
        }
        if (application.getPqcReleaseWorkTaskId() != null) {
            links.add(new GxpAuditRelation("WORK_TASK", "EDHR_WORK_TASK",
                    String.valueOf(application.getPqcReleaseWorkTaskId()), null, null));
        }
        gxpAuditService.append(GxpAuditCommand.builder()
                .eventSchemaVersion(2)
                .operationId(binding ? "mes.pqc-release.bind-batch" : "mes.pqc-release.apply")
                .subjectId("ACTIVE_ORDER:" + activeOrderId)
                .subjectVersion(String.valueOf(application.getVersion()))
                .reason(binding ? "为既有PQC生产放行申请补绑正式批记录批次" : "已完成活跃订单并创建PQC生产放行申请")
                .reasonCode(binding ? "MES_PQC_RELEASE_BIND_BATCH" : "MES_PQC_RELEASE_APPLY")
                .reasonSource("SYSTEM")
                .beforeState(binding ? bindingBefore : GxpAuditStateEnvelope.builder()
                        .state("ABSENT")
                        .canonicalJson(JsonUtils.toJsonString(before))
                        .build())
                .afterState(GxpAuditStateEnvelope.builder()
                        .state(binding ? application.getStatus() : "PQC_RELEASE_PENDING")
                        .objectVersion(String.valueOf(application.getVersion()))
                        .canonicalJson(JsonUtils.toJsonString(after))
                        .build())
                .idempotencyKey(auditIdentity)
                .requestId(binding ? "MES-" + auditIdentity : "MES-PQC-RELEASE-APPLY:" + application.getApplicationId())
                .resultStatus("SUCCESS")
                .sourceType("SERVICE_METHOD")
                .sourceLocator("cn.iocoder.yudao.module.mes.service.pro.processpool.team."
                        + "MesTeamLeaderActiveOrderReleaseApplicationServiceImpl#" + sourceMethod)
                .links(links)
                .evidences(List.of(new GxpAuditEvidence("FORMAL_RELEASE_APPLICATION",
                        String.valueOf(application.getApplicationId()), String.valueOf(application.getVersion()),
                        application.getSourceSnapshotHash(), "PQC_RELEASE")))
                .build());
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseApplicationResult get(Long userId, Long activeOrderId) {
        return generationService.get(userId, activeOrderId);
    }
}

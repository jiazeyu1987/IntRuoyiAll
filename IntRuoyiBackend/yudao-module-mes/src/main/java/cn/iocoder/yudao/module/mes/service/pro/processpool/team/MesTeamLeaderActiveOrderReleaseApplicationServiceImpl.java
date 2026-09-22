package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
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
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesTeamLeaderActiveOrderCompletionBatchExecutionService completionBatchExecutionService;

    public MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(
            MesTeamLeaderActiveOrderReleaseGenerationService generationService,
            MesTeamLeaderActiveOrderCompletionService completionService,
            MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper,
            MesProEdhrBatchExecutionMapper batchMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesTeamLeaderActiveOrderCompletionBatchExecutionService completionBatchExecutionService) {
        this.generationService = generationService;
        this.completionService = completionService;
        this.receiptMapper = receiptMapper;
        this.batchMapper = batchMapper;
        this.applicationMapper = applicationMapper;
        this.completionBatchExecutionService = completionBatchExecutionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseApplicationResult applyGenerated(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        String key = MesReleaseFlowIdempotency.requireKey(command == null ? null : command.getIdempotencyKey());
        command.setIdempotencyKey(key);
        var existing = generationService.replayExisting(leaderUserId, command);
        if (existing != null && existing.getBatchExecutionId() != null) return existing;
        Long batchExecutionId = requirePersistedP2BatchExecutionId(leaderUserId, command);
        if (existing != null) {
            return bindBatchExecution(existing, batchExecutionId);
        }
        var generated = generationService.generate(leaderUserId, command);
        return bindBatchExecution(generated, batchExecutionId);
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
        var batch = batchMapper.selectByContext(receipt.getWorkOrderId(), receipt.getBatchCode(), receipt.getRouteId());
        if (batch == null || batch.getId() == null
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
            return bindBatchExecution(existing, batchExecutionId);
        }
        MesTeamLeaderActiveOrderCompletionResult completion = completionService.completeForRelease(
                leaderUserId, command.getActiveOrderId(), releaseIdempotencyKey, command.getConfirmNoReplenishmentInfo());
        Long batchExecutionId = completionBatchExecutionService.openOrCreate(
                leaderUserId, command.getActiveOrderId(), completion.getCompletionReceiptId(), releaseIdempotencyKey);
        MesTeamLeaderActiveOrderReleaseApplicationResult generated = generationService.generate(leaderUserId, command);
        return bindBatchExecution(generated, batchExecutionId);
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseApplicationResult get(Long userId, Long activeOrderId) {
        return generationService.get(userId, activeOrderId);
    }
}

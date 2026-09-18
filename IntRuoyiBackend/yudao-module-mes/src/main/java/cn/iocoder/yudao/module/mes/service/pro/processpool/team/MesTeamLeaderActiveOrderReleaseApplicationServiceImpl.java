package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
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

    public MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(
            MesTeamLeaderActiveOrderReleaseGenerationService generationService,
            MesTeamLeaderActiveOrderCompletionService completionService,
            MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper,
            MesProEdhrBatchExecutionMapper batchMapper) {
        this.generationService = generationService;
        this.completionService = completionService;
        this.receiptMapper = receiptMapper;
        this.batchMapper = batchMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseApplicationResult applyGenerated(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        String key = MesReleaseFlowIdempotency.requireKey(command == null ? null : command.getIdempotencyKey());
        command.setIdempotencyKey(key);
        var existing = generationService.replayExisting(leaderUserId, command);
        if (existing != null) return existing;
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
        return generationService.generate(leaderUserId, command);
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
            return existing;
        }
        completionService.completeForRelease(
                leaderUserId, command.getActiveOrderId(), releaseIdempotencyKey, command.getConfirmNoReplenishmentInfo());
        return generationService.generate(leaderUserId, command);
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseApplicationResult get(Long userId, Long activeOrderId) {
        return generationService.get(userId, activeOrderId);
    }
}

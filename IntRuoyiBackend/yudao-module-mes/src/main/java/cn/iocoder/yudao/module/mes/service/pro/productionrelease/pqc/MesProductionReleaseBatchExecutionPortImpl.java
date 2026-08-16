package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrProductionReleaseBatchCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class MesProductionReleaseBatchExecutionPortImpl implements MesProductionReleaseBatchExecutionPort {

    private static final String CONTEXT_PREFIX = "PQC_RELEASE:";

    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionService batchExecutionService;

    public MesProductionReleaseBatchExecutionPortImpl(
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrBatchExecutionService batchExecutionService) {
        this.batchExecutionMapper = batchExecutionMapper;
        this.batchExecutionService = batchExecutionService;
    }

    @Override
    public Long openOrCreate(MesProductionReleaseBatchExecutionCommand command) {
        String activeContextKey = CONTEXT_PREFIX + command.getApplicationId();
        MesProEdhrBatchExecutionDO releaseBatch = batchExecutionMapper.selectByActiveContextKey(activeContextKey);
        if (releaseBatch != null) {
            requireSameFrozenContext(command, releaseBatch);
            return releaseBatch.getId();
        }
        MesProEdhrBatchExecutionDO legacy = batchExecutionMapper.selectByContext(
                command.getWorkOrderId(), command.getBatchCode(), command.getRouteId());
        if (legacy != null) {
            throw legacyBlocker(command, legacy);
        }
        return batchExecutionService.openOrCreateFromProductionRelease(
                new MesProEdhrProductionReleaseBatchCommand()
                        .setApplicationId(command.getApplicationId())
                        .setWorkOrderId(command.getWorkOrderId())
                        .setBatchCode(command.getBatchCode())
                        .setRouteId(command.getRouteId())
                        .setRouteVersionId(command.getRouteVersionId())
                        .setActiveContextKey(activeContextKey)
                        .setRemark("PQC production release application " + command.getApplicationId()));
    }

    private void requireSameFrozenContext(
            MesProductionReleaseBatchExecutionCommand command,
            MesProEdhrBatchExecutionDO batch) {
        if (!Objects.equals(command.getWorkOrderId(), batch.getWorkOrderId())
                || !Objects.equals(command.getBatchCode(), batch.getBatchCode())
                || !Objects.equals(command.getRouteId(), batch.getRouteId())
                || !Objects.equals(command.getRouteVersionId(), batch.getRouteVersionId())) {
            throw legacyBlocker(command, batch);
        }
    }

    private MesReleaseFlowBlockerException legacyBlocker(
            MesProductionReleaseBatchExecutionCommand command,
            MesProEdhrBatchExecutionDO batch) {
        String reason = "existing batch execution is not associated with this production release application";
        return new MesReleaseFlowBlockerException(reason, new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_2)
                .setCurrentStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED)
                        .setObjectType("BATCH_EXECUTION")
                        .setObjectId(batch == null ? null : String.valueOf(batch.getId()))
                        .setObjectCode(command.getBatchCode())
                        .setReason(reason)
                        .setSuggestion("migrate the legacy batch with approved evidence before retrying"))));
    }
}

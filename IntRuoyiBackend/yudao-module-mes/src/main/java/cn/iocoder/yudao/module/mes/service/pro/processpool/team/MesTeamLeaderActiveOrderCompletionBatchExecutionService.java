package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionOpenOrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

@Service
public class MesTeamLeaderActiveOrderCompletionBatchExecutionService {

    private static final String ENTRY_TYPE = "ACTIVE_ORDER_COMPLETION";
    private static final String SOURCE_CREDENTIAL_TYPE = "CompletionBackfillReceipt";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort;
    private final MesProEdhrBatchExecutionService batchExecutionService;

    public MesTeamLeaderActiveOrderCompletionBatchExecutionService(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort,
            MesProEdhrBatchExecutionService batchExecutionService) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.completionReceiptPort = completionReceiptPort;
        this.batchExecutionService = batchExecutionService;
    }

    public Long openOrCreate(Long leaderUserId, Long activeOrderId, Long completionReceiptId,
                             String releaseIdempotencyKey) {
        if (leaderUserId == null || activeOrderId == null || completionReceiptId == null
                || StrUtil.isBlank(releaseIdempotencyKey)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    activeOrderId, "P2批次执行上下文缺失");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(activeOrderId);
        if (activeOrder == null || !Objects.equals(activeOrder.getId(), activeOrderId)
                || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)
                || activeOrder.getWorkOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    activeOrderId, "活跃订单与当前生产组长不匹配");
        }
        MesFlow6CompletionBackfillReceipt receipt =
                completionReceiptPort.getByReceiptId(completionReceiptId, tenantId);
        if (receipt == null || !Objects.equals(receipt.getReceiptId(), completionReceiptId)
                || !Objects.equals(receipt.getActiveOrderId(), activeOrderId)
                || !Objects.equals(receipt.getWorkOrderId(), activeOrder.getWorkOrderId())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    activeOrderId, "正式P2完工回执与当前活跃订单不一致");
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectByIdForUpdate(receipt.getWorkOrderId());
        if (workOrder == null || !Objects.equals(workOrder.getId(), receipt.getWorkOrderId())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    activeOrderId, "P2批次执行缺少生产工单");
        }
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(
                buildOpenRequest(tenantId, activeOrderId, workOrder, receipt));
        if (batch == null || batch.getId() == null || batch.getId() <= 0) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    activeOrderId, "P2批次执行创建失败");
        }
        return batch.getId();
    }

    private EdhrBatchExecutionOpenOrCreateReqVO buildOpenRequest(
            Long tenantId, Long activeOrderId, MesProWorkOrderDO workOrder,
            MesFlow6CompletionBackfillReceipt receipt) {
        String receiptId = String.valueOf(receipt.getReceiptId());
        return new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(receipt.getWorkOrderId())
                .setWorkOrderCode(workOrder.getCode())
                .setTenantId(tenantId)
                .setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId())
                .setRemark("ACTIVE_ORDER_COMPLETION activeOrderId=" + activeOrderId)
                .setEntryType(ENTRY_TYPE)
                .setEntryBusinessId("ACTIVE_ORDER_COMPLETION:" + receiptId)
                .setSourceCredentialType(SOURCE_CREDENTIAL_TYPE)
                .setSourceCredentialId(receiptId)
                .setSourceContextHash(receipt.getSourceSnapshotHash())
                .setActiveOrderId(activeOrderId)
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setCompletionTransactionId(receipt.getCompletionTransactionId())
                .setExpectedActiveOrderVersion(receipt.getExpectedActiveOrderVersion())
                .setCompletionVersion(receipt.getCompletionVersion() == null
                        ? null : receipt.getCompletionVersion().longValue())
                .setCompletionBackfillReceiptId(receiptId)
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                .setIdempotencyKey("ACTIVE_ORDER_COMPLETION_BATCH:" + receiptId);
    }
}

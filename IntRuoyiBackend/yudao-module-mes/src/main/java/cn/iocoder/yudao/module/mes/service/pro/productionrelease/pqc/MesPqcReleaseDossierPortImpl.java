package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import org.springframework.stereotype.Component;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

/**
 * Release-stage adapter for the immutable Flow-4 handoff.
 *
 * <p>The former implementation planned and wrote the three release dossier
 * documents. Those writes belonged to the active-order completion Tx-A and
 * caused duplicate materialization during release approval. This adapter is
 * deliberately read-only.</p>
 */
@Component
public class MesPqcReleaseDossierPortImpl implements MesPqcReleaseDossierPort {

    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort;

    public MesPqcReleaseDossierPortImpl(
            MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort) {
        this.completionReceiptPort = completionReceiptPort;
    }

    @Override
    public MesFlow6CompletionBackfillReceipt readCompletionReceipt(Long receiptId, Long tenantId) {
        Long securityTenantId = TenantContextHolder.getTenantId();
        if (securityTenantId == null || tenantId == null || !securityTenantId.equals(tenantId)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    receiptId, "TENANT_CONTEXT");
        }
        return completionReceiptPort.getByReceiptId(receiptId, tenantId);
    }
}

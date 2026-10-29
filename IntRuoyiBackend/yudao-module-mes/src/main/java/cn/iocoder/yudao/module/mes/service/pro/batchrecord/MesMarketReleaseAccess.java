package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesMarketReleaseHandoverMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

/** ponytail: one persisted handover history governs all existing readers and writers. */
@Service
@RequiredArgsConstructor
public class MesMarketReleaseAccess {
    private final MesMarketReleaseHandoverMapper mapper;
    private final cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper executions;
    private final cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService ledger;
    public boolean hasActorHistory(Long actor) { return mapper.hasActorHistory(actor); }

    public record Access(boolean visible, boolean writable, boolean original, boolean active,
                         Long handoverId, Long originalUserId, Long currentUserId, String currentUserName) {}

    public Access get(Long batchId, Long actor) { return resolve(mapper.history(batchId), actor); }

    public java.util.Map<Long, Access> getAll(java.util.Collection<Long> batchIds, Long actor) {
        if (batchIds.isEmpty()) return java.util.Map.of();
        java.util.Map<Long, MesMarketReleaseHandoverMapper.AccessRow> latest = new java.util.HashMap<>();
        for (var row : mapper.selectAccessByBatchIds(batchIds, actor)) latest.put(row.getBatchExecutionId(), row);
        java.util.Map<Long, Access> result = new java.util.HashMap<>();
        for (Long batchId : batchIds) {
            var row = latest.get(batchId);
            result.put(batchId, row == null ? resolve(List.of(), actor) : resolve(row, actor, row.isActorRecipient()));
        }
        return result;
    }

    static Access resolve(List<MesMarketReleaseHandoverMapper.Row> rows, Long actor) {
        if(rows.isEmpty()) return new Access(true,true,false,false,null,null,null,null);
        return resolve(rows.get(0), actor,
                rows.stream().anyMatch(row -> Objects.equals(actor,row.getRecipientUserId())));
    }

    static Access resolve(MesMarketReleaseHandoverMapper.Row last, Long actor, boolean revoked) {
        boolean active=last.getRecalledAt()==null;
        Long owner=active ? last.getRecipientUserId() : last.getOriginalUserId();
        boolean original=Objects.equals(actor,last.getOriginalUserId());
        boolean current=Objects.equals(actor,owner);
        return new Access(original || current || !revoked, current, original, active,
                last.getId(),last.getOriginalUserId(),owner,active ? last.getRecipientUserName() : last.getOriginalUserName());
    }

    public void requireReadable(Long batchId) {
        Long actor=SecurityFrameworkUtils.getLoginUserId();
        if(actor!=null && !get(batchId,actor).visible())
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(403, "该批记录的交接授权已被回收");
    }
    public void requireWritable(Long batchId) {
        Long actor=SecurityFrameworkUtils.getLoginUserId();
        if(actor==null) return;
        boolean locked=org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
                && !org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        if(locked) ledger.acquireLedgerLock();
        if(!resolve(locked ? mapper.historyForUpdate(batchId) : mapper.history(batchId),actor).writable())
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(403, "当前用户不是该单的上市放行办理人");
    }
    public void requireExecutionAccess(Long executionId,boolean write) {
        var execution=executions.selectById(executionId);
        if(execution!=null && execution.getBatchExecutionId()!=null) {
            if(write) requireWritable(execution.getBatchExecutionId()); else requireReadable(execution.getBatchExecutionId());
        }
    }
    public void requireActiveOrderReadable(Long activeOrderId) {
        Long actor=SecurityFrameworkUtils.getLoginUserId();
        if(actor==null) return;
        var rows=mapper.activeOrderHistory(activeOrderId);
        for(Long batchId:rows.stream().map(MesMarketReleaseHandoverMapper.Row::getBatchExecutionId).distinct().toList())
            requireReadable(batchId);
    }
}

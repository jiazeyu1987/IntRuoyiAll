package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MesProEdhrBatchActiveOrderDetailService {

    private final MesProEdhrBatchExecutionService batchExecutionService;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesTeamLeaderActiveOrderDetailService detailService;

    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderDetail getDetail(Long batchExecutionId) {
        EdhrBatchExecutionRespVO batch = batchExecutionService.get(batchExecutionId);
        Long activeOrderId = batch == null ? null : batch.getActiveOrderId();
        if (activeOrderId == null) {
            throw new IllegalStateException("EDHR_BATCH_ACTIVE_ORDER_SOURCE_MISSING");
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdIgnoreDeleted(activeOrderId);
        if (activeOrder == null || activeOrder.getLeaderUserId() == null
                || !Objects.equals(activeOrder.getWorkOrderId(), batch.getWorkOrderId())) {
            throw new IllegalStateException("EDHR_BATCH_ACTIVE_ORDER_SOURCE_INVALID");
        }
        return detailService.getArchivedFormalDetail(activeOrderId);
    }
}

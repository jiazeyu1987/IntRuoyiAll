package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;

import java.util.Collection;
import java.util.List;

public interface MesActiveOrderTransferTraceService {

    MesProcessPoolActiveOrderTransferTraceDO recordTransferTrace(MesProcessPoolActiveOrderTransferTraceDO trace);

    List<MesProcessPoolActiveOrderTransferTraceDO> recordTransferTracesForActiveOrder(
            MesProcessPoolActiveOrderDO activeOrder, List<Long> transferIds);

    List<MesProcessPoolActiveOrderTransferTraceDO> listByActiveOrder(Long activeOrderId);

    List<MesProcessPoolActiveOrderTransferTraceDO> listByActiveOrderAndSourceTypes(Long activeOrderId,
                                                                                   Collection<String> sourceTypes);
}

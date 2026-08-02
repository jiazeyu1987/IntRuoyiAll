package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;

import java.util.Collection;
import java.util.List;

public interface MesActiveOrderTransferTraceService {

    List<MesProcessPoolActiveOrderTransferTraceDO> listByActiveOrder(Long activeOrderId);

    List<MesProcessPoolActiveOrderTransferTraceDO> listByActiveOrderAndSourceTypes(Long activeOrderId,
                                                                                   Collection<String> sourceTypes);
}

package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class MesActiveOrderTransferTraceServiceImpl implements MesActiveOrderTransferTraceService {

    @Resource
    private MesProcessPoolActiveOrderTransferTraceMapper transferTraceMapper;

    @Override
    public List<MesProcessPoolActiveOrderTransferTraceDO> listByActiveOrder(Long activeOrderId) {
        return transferTraceMapper.selectListByActiveOrderId(activeOrderId);
    }

    @Override
    public List<MesProcessPoolActiveOrderTransferTraceDO> listByActiveOrderAndSourceTypes(
            Long activeOrderId, Collection<String> sourceTypes) {
        return transferTraceMapper.selectListByActiveOrderIdAndSourceTypes(activeOrderId, sourceTypes);
    }
}

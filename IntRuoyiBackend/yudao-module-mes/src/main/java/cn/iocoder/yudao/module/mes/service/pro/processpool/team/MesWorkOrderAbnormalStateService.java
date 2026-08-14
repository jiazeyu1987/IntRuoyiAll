package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 未关闭生产订单异常的统一只读状态服务。
 */
@Service
public class MesWorkOrderAbnormalStateService {

    private final MesProcessPoolWorkOrderAbnormalMapper abnormalMapper;

    public MesWorkOrderAbnormalStateService(MesProcessPoolWorkOrderAbnormalMapper abnormalMapper) {
        this.abnormalMapper = abnormalMapper;
    }

    public Map<Long, MesProcessPoolWorkOrderAbnormalDO> findLatestOpenByWorkOrderIds(
            Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Map.of();
        }
        List<MesProcessPoolWorkOrderAbnormalDO> openAbnormals =
                abnormalMapper.selectOpenListByWorkOrderIds(workOrderIds);
        Map<Long, MesProcessPoolWorkOrderAbnormalDO> latestByWorkOrderId = new LinkedHashMap<>();
        for (MesProcessPoolWorkOrderAbnormalDO abnormal : openAbnormals) {
            Long workOrderId = Objects.requireNonNull(abnormal.getWorkOrderId(),
                    "Open work-order abnormal is missing workOrderId");
            latestByWorkOrderId.putIfAbsent(workOrderId, abnormal);
        }
        return Map.copyOf(latestByWorkOrderId);
    }

    public Set<Long> findOpenWorkOrderIds(Collection<Long> workOrderIds) {
        return findLatestOpenByWorkOrderIds(workOrderIds).keySet();
    }

    public boolean hasOpenAbnormal(Long workOrderId) {
        Objects.requireNonNull(workOrderId, "workOrderId");
        return findLatestOpenByWorkOrderIds(List.of(workOrderId)).containsKey(workOrderId);
    }
}

package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import com.alibaba.fastjson.JSON;
import java.util.HashSet;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_QA_SOURCE_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_QA_SOURCE_SNAPSHOT_REQUIRED;

/** The order freezes all QA sources before inspectors create patrol rounds. */
public final class MesPqcQaSources {
    private MesPqcQaSources() {}

    public static List<Long> requireVersionIds(MesProcessPoolActiveOrderDO order) {
        if (order == null || order.getPqcQaSourceVersionIdsJson() == null
                || order.getPqcQaSourceVersionIdsJson().isBlank()) {
            throw exception(PRO_PQC_QA_SOURCE_SNAPSHOT_REQUIRED);
        }
        List<Long> ids = JSON.parseArray(order.getPqcQaSourceVersionIdsJson(), Long.class);
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(ids).size() != ids.size()
                || !ids.contains(order.getQaRegulationVersionId())) {
            throw exception(PRO_PQC_QA_SOURCE_SNAPSHOT_INVALID);
        }
        return List.copyOf(ids);
    }
}

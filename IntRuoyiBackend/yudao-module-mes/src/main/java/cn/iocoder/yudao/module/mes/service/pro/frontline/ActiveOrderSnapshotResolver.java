package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_SNAPSHOT_INVALID;

@Component
public class ActiveOrderSnapshotResolver {

    private static final String SNAPSHOT_SELECT = "id AS activeOrderId, work_order_id AS workOrderId, "
            + "route_id AS routeId, route_version_id AS routeVersionId, "
            + "dcc_project_code_id AS dccProjectCodeId, qa_regulation_id AS qaRegulationId, "
            + "qa_regulation_version_id AS qaRegulationVersionId";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;

    public ActiveOrderSnapshotResolver(MesProcessPoolActiveOrderMapper activeOrderMapper) {
        this.activeOrderMapper = activeOrderMapper;
    }

    public ActiveOrderSnapshot requireEffective(Long activeOrderId) {
        if (activeOrderId == null || activeOrderId <= 0) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        List<Map<String, Object>> rows = activeOrderMapper.selectMaps(
                new QueryWrapper<MesProcessPoolActiveOrderDO>()
                        .select(SNAPSHOT_SELECT)
                        .eq("id", activeOrderId)
                        .eq("active_status", "ACTIVE")
                        .last("LIMIT 1"));
        if (rows == null || rows.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        Map<String, Object> row = rows.get(0);
        Long resolvedActiveOrderId = requireLong(row, "activeOrderId",
                PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        Long workOrderId = requireLong(row, "workOrderId",
                PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED, activeOrderId);
        Long routeId = requireLong(row, "routeId",
                PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED, activeOrderId);
        Long routeVersionId = requireLong(row, "routeVersionId",
                PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED, activeOrderId);
        Long dccProjectCodeId = requireLong(row, "dccProjectCodeId",
                QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "activeOrderId=" + activeOrderId);
        Long qaRegulationId = requireLong(row, "qaRegulationId",
                QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "activeOrderId=" + activeOrderId);
        Long qaRegulationVersionId = requireLong(row, "qaRegulationVersionId",
                QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "activeOrderId=" + activeOrderId);
        return new ActiveOrderSnapshot(resolvedActiveOrderId, workOrderId, routeId, routeVersionId,
                dccProjectCodeId, qaRegulationId, qaRegulationVersionId);
    }

    private static Long requireLong(Map<String, Object> row, String key, ErrorCode errorCode, Object detail) {
        Object value = row.get(key);
        if (!(value instanceof Number number) || number.longValue() <= 0) {
            throw exception(errorCode, detail);
        }
        return number.longValue();
    }

    public record ActiveOrderSnapshot(Long activeOrderId,
                                      Long workOrderId,
                                      Long routeId,
                                      Long routeVersionId,
                                      Long dccProjectCodeId,
                                      Long qaRegulationId,
                                      Long qaRegulationVersionId) {
    }
}

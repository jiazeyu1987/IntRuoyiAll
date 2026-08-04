package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProProcessPoolEventMapper extends BaseMapperX<MesProProcessPoolEventDO> {

    default MesProProcessPoolEventDO selectSubmitByIdempotency(MesProProcessPoolEventDO event) {
        if (event == null || event.getEventIdempotencyKey() == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolEventDO>()
                .eq(MesProProcessPoolEventDO::getEventType, MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .eq(MesProProcessPoolEventDO::getWorkOrderId, event.getWorkOrderId())
                .eq(MesProProcessPoolEventDO::getRouteId, event.getRouteId())
                .eq(MesProProcessPoolEventDO::getRouteProcessId, event.getRouteProcessId())
                .eq(MesProProcessPoolEventDO::getProcessId, event.getProcessId())
                .eq(MesProProcessPoolEventDO::getActualEmployeeId, event.getActualEmployeeId())
                .eq(MesProProcessPoolEventDO::getDeviceAccountId, event.getDeviceAccountId())
                .eq(MesProProcessPoolEventDO::getDeviceId, event.getDeviceId())
                .eq(MesProProcessPoolEventDO::getWorkstationId, event.getWorkstationId())
                .eq(MesProProcessPoolEventDO::getEventIdempotencyKey, event.getEventIdempotencyKey()));
    }

    default MesProProcessPoolEventDO selectPqcByIdempotency(MesProProcessPoolEventDO event) {
        if (event == null || event.getEventIdempotencyKey() == null) {
            return null;
        }
        LambdaQueryWrapperX<MesProProcessPoolEventDO> query = new LambdaQueryWrapperX<MesProProcessPoolEventDO>()
                .eq(MesProProcessPoolEventDO::getEventType, MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .eq(MesProProcessPoolEventDO::getWorkOrderId, event.getWorkOrderId())
                .eq(MesProProcessPoolEventDO::getRouteId, event.getRouteId())
                .eq(MesProProcessPoolEventDO::getRouteProcessId, event.getRouteProcessId())
                .eq(MesProProcessPoolEventDO::getProcessId, event.getProcessId())
                .eq(MesProProcessPoolEventDO::getActualEmployeeId, event.getActualEmployeeId())
                .eq(MesProProcessPoolEventDO::getFeedbackSourceType, event.getFeedbackSourceType())
                .eq(MesProProcessPoolEventDO::getFeedbackSourceId, event.getFeedbackSourceId())
                .eq(MesProProcessPoolEventDO::getEventIdempotencyKey, event.getEventIdempotencyKey());
        if (event.getDeviceAccountId() == null) {
            query.isNull(MesProProcessPoolEventDO::getDeviceAccountId);
        } else {
            query.eq(MesProProcessPoolEventDO::getDeviceAccountId, event.getDeviceAccountId());
        }
        if (event.getDeviceId() == null) {
            query.isNull(MesProProcessPoolEventDO::getDeviceId);
        } else {
            query.eq(MesProProcessPoolEventDO::getDeviceId, event.getDeviceId());
        }
        if (event.getWorkstationId() == null) {
            query.isNull(MesProProcessPoolEventDO::getWorkstationId);
        } else {
            query.eq(MesProProcessPoolEventDO::getWorkstationId, event.getWorkstationId());
        }
        return selectOne(query);
    }

    default MesProProcessPoolEventDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolEventDO>()
                .eq(MesProProcessPoolEventDO::getId, id)
                .last("FOR UPDATE"));
    }

    default MesProProcessPoolEventDO selectBySignatureId(Long signatureId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolEventDO>()
                .eq(MesProProcessPoolEventDO::getSignatureId, signatureId));
    }

    default List<MesProProcessPoolEventDO> selectPqcEventsForSubmit(MesProProcessPoolEventDO submitEvent) {
        if (submitEvent == null || submitEvent.getWorkOrderId() == null
                || submitEvent.getRouteProcessId() == null || submitEvent.getProcessId() == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolEventDO>()
                .eq(MesProProcessPoolEventDO::getEventType, MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .eq(MesProProcessPoolEventDO::getWorkOrderId, submitEvent.getWorkOrderId())
                .eq(MesProProcessPoolEventDO::getRouteProcessId, submitEvent.getRouteProcessId())
                .eq(MesProProcessPoolEventDO::getProcessId, submitEvent.getProcessId())
                .orderByAsc(MesProProcessPoolEventDO::getId));
    }
}

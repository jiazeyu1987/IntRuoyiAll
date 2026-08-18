package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotCodec;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MesFrontlineProductionSubmitContext(Long workOrderId,
                                                  String workOrderCode,
                                                  String workOrderName,
                                                  Long taskId,
                                                  Long routeId,
                                                  Long routeProcessId,
                                                  Long processId,
                                                  Long workstationId,
                                                  Long itemId,
                                                  Long approveUserId,
                                                  Long recordbookId,
                                                  BigDecimal scheduledQuantity,
                                                  LocalDateTime expireDate,
                                                  Long activeOrderProcessSnapshotId,
                                                  String parameterSnapshotSha256,
                                                  String parameterSnapshotState) {

    public MesFrontlineProductionSubmitContext(Long workOrderId, String workOrderCode, String workOrderName,
                                               Long taskId, Long routeId, Long routeProcessId, Long processId,
                                               Long workstationId, Long itemId, Long approveUserId,
                                               Long recordbookId, BigDecimal scheduledQuantity,
                                               LocalDateTime expireDate) {
        this(workOrderId, workOrderCode, workOrderName, taskId, routeId, routeProcessId, processId,
                workstationId, itemId, approveUserId, recordbookId, scheduledQuantity, expireDate,
                null, null, MesDeviceParameterSnapshotCodec.SOURCE_CURRENT_ROUTE_PROCESS_AT_SUBMIT);
    }
}

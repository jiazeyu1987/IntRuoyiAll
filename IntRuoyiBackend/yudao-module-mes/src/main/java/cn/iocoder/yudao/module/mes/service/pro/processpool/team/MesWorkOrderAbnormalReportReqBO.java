package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesWorkOrderAbnormalReportReqBO {

    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private Long sourceEventId;
    private Long markerUserId;
    private String abnormalReasonCode;
    private String abnormalDescription;
}

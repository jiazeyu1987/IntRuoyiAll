package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesWorkOrderAbnormalReportReqBO {

    private Long workOrderId;
    private Long markerUserId;
    private String abnormalDescription;
}

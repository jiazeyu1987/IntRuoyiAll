package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesDefectReasonSaveReqBO {

    private Long leaderUserId;
    private Long routeProcessId;
    private Long processId;
    private String reasonType;
    private String reasonCode;
    private String reasonName;
}

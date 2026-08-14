package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DccControlledFileRouteReadinessBlockerRespVO {

    private String reasonCode;
    private String message;
    private Integer stageNo;
    private String stageCode;
    private String stageName;
    private Long userId;
    private String userName;
}

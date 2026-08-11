package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DccControlledFileRouteReadinessRespVO {

    private Boolean ready;
    private List<DccControlledFileRoutePreviewRespVO> nodes;
    private List<DccControlledFileRouteReadinessBlockerRespVO> blockers;
}

package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionRouteOptionRespVO {

    private Long routeId;

    private String routeCode;

    private String routeName;

    private Boolean batchRouteEnabled;
}
